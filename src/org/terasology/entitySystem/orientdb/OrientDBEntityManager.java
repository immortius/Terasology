package org.terasology.entitySystem.orientdb;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.orientechnologies.orient.core.db.graph.OGraphDatabase;
import com.orientechnologies.orient.core.db.graph.OGraphDatabasePool;
import com.orientechnologies.orient.core.db.record.OIdentifiable;
import com.orientechnologies.orient.core.id.ORID;
import com.orientechnologies.orient.core.id.ORecordId;
import com.orientechnologies.orient.core.metadata.security.OSecurity;
import com.orientechnologies.orient.core.metadata.security.OUser;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.sql.query.OSQLSynchQuery;
import com.orientechnologies.orient.core.type.tree.OMVRBTreeRIDSet;
import org.terasology.entitySystem.Component;
import org.terasology.entitySystem.EntityManager;
import org.terasology.entitySystem.EntityRef;
import org.terasology.entitySystem.EventSystem;
import org.terasology.entitySystem.event.AddComponentEvent;
import org.terasology.entitySystem.event.ChangedComponentEvent;
import org.terasology.entitySystem.event.RemovedComponentEvent;

import java.lang.reflect.Field;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.junit.Assert.assertNotNull;

/**
 * @author Immortius <immortius@gmail.com>
 */
public class OrientDBEntityManager implements EntityManager {

    private static final String EntityVertexType = "Entity";
    private static final String ComponentVertexType = "Component";
    private static final String OwnsEdgeType = "owns";
    private static final String ReferencesEdgeType = "references";
    
    private Logger logger = Logger.getLogger(getClass().getName());
    
    private EventSystem eventSystem;
    private String dbLocation;
    private String dbUsername;
    private String dbPassword;

    private OGraphDatabasePool pool;
    
    private Map<String, Class<? extends Component>> registeredComponents = Maps.newHashMap();

    // ODatabaseDocument is not thread-safe, so we need to acquire a different copy per thread
    public ThreadLocal<OGraphDatabase> localDatabase = new ThreadLocal<OGraphDatabase>();
    
    public OrientDBEntityManager(String location, String username, String password) {
        this.dbLocation = location;
        this.dbUsername = username;
        this.dbPassword = password;
        OGraphDatabase db = new OGraphDatabase(location);
        if (!db.exists()) {
            createDatabase(db);
        }
        db.close();
        pool = new OGraphDatabasePool();
        pool.setup(1, 10);

        localDatabase.set(pool.acquire(dbLocation, dbUsername, dbPassword));
    }

    // TODO: Register component types
    public void registerComponentType(Class<? extends Component> componentClass) {
        OGraphDatabase db = getDB();
        if (db.getVertexType(getComponentVertexType(componentClass)) == null) {
            db.createVertexType(getComponentVertexType(componentClass), ComponentVertexType);
            db.getMetadata().getSchema().save();
        }
        registeredComponents.put(getComponentVertexType(componentClass), componentClass);
        for (Field field : componentClass.getDeclaredFields()) {
            field.setAccessible(true);
        }
    }

    public EntityRef create() {
        ODocument entity = getDB().createVertex(EntityVertexType);
        entity.save();
        return getEntityRef(entity.getIdentity());
    }

    public <T extends Component> T addComponent(ORID entityId, T component) {
        OGraphDatabase db = getDB();

        ODocument entity = db.load(entityId);
        if (entity != null) {
            boolean componentUpdate = true;
            ODocument componentDoc = findComponentOfEntity(entity, component.getClass());
            if (componentDoc == null) {
                componentDoc = db.createVertex(getComponentVertexType(component));
                componentDoc.field(OGraphDatabase.LABEL, getComponentVertexType(component));
                componentUpdate = false;
            }
            // TODO: Improved save
            serializeComponent(componentDoc, component);
            componentDoc.save();
            if (!componentUpdate) {
                ODocument edge = db.createEdge(entity, componentDoc, OwnsEdgeType);
                edge.field(OGraphDatabase.LABEL, getComponentVertexType(component));
                edge.save();
            }
            if (eventSystem != null) {
                if (componentUpdate) {
                    eventSystem.send(getEntityRef(entityId), ChangedComponentEvent.newInstance(), component);
                } else {
                    eventSystem.send(getEntityRef(entityId), AddComponentEvent.newInstance(), component);
                }
            }
        }
        return component;
    }

    public <T extends Component> T getComponent(ORID entityId, Class<T> componentClass) {
        OGraphDatabase db = getDB();
        ODocument entity = db.load(entityId);
        if (entity != null) {
            ODocument componentDoc = findComponentOfEntity(entity, componentClass);
            return deserializeComponent(componentDoc, componentClass);
        }
        return null;
    }

    public void destroy(ORID entityId) {
        OGraphDatabase db = getDB();
        ODocument entity = db.load(entityId);
        if (entity != null) {
            if (eventSystem != null) {
                eventSystem.send(getEntityRef(entityId), RemovedComponentEvent.newInstance());
            }

            // Remove all owned components
            for (OIdentifiable outEdge : db.getOutEdges(entity)) {
                ODocument component = db.getInVertex((ODocument) outEdge);
                db.removeVertex(component);
            }
            // Remove this vertex
            getDB().removeVertex(entity);
        }
    }

    public boolean hasComponent(ORID entityId, Class<? extends Component> componentClass) {
        OGraphDatabase db = getDB();
        ODocument entity = db.load(entityId);
        if (entity != null) {
            return findComponentOfEntity(entity, componentClass) != null;
        }
        return false;
    }

    public void removeComponent(ORID entityId, Class<? extends Component> componentClass) {
        OGraphDatabase db = getDB();
        ODocument entity = db.load(entityId);
        if (entity != null) {
            ODocument componentDoc = findComponentOfEntity(entity, componentClass);
            if (componentDoc != null) {
                if (eventSystem != null) {
                    eventSystem.send(getEntityRef(entityId), RemovedComponentEvent.newInstance(), deserializeComponent(componentDoc, componentClass));
                }
                db.removeVertex(componentDoc);
            }
        }
    }

    public <T extends Component> void saveComponent(ORID entityId, T component) {
        OGraphDatabase db = getDB();
        ODocument entity = db.load(entityId);
        if (entity == null)
            return;
        
        ODocument componentDoc = findComponentOfEntity(entity, component.getClass());
        if (componentDoc == null)
            return;
        
        serializeComponent(componentDoc, component);
        componentDoc.save();
        
        if (eventSystem != null) {
            eventSystem.send(getEntityRef(entityId), ChangedComponentEvent.newInstance(), component);
        }
    }


    public long getComponentCount(Class<? extends Component> componentClass) {
        return getDB().countClass(getComponentVertexType(componentClass));
    }

    public <T extends Component> Iterable<Map.Entry<EntityRef, T>> iterateComponents(final Class<T> componentClass) {
        return new Iterable<Map.Entry<EntityRef, T>>() {
            public Iterator<Map.Entry<EntityRef, T>> iterator() {
                return new ComponentsIterator(componentClass);
            }
        };
    }

    public Iterable<Component> iterateComponents(final ORID entityId) {
        ODocument entity = getDB().load(entityId);
        final OMVRBTreeRIDSet set = entity.field(OGraphDatabase.VERTEX_FIELD_OUT);

        List<Component> results = Lists.newArrayList();
        
        // Find matching label
        if (set != null) {
            for (OIdentifiable item : set) {
                ODocument edge = (ODocument) item;
                Class<? extends Component> componentClass = registeredComponents.get(edge.field(OGraphDatabase.LABEL));
                if (componentClass != null) {
                    results.add(deserializeComponent(getDB().getInVertex(edge), componentClass));
                }
            }
        }
        return results;
    }

    public Iterable<EntityRef> iteratorEntities(final Class<? extends Component>... componentClasses) {
        return new Iterable<EntityRef>() {
            public Iterator<EntityRef> iterator() {
                return new EntityWithComponentsIterator(componentClasses);
            }
        };
    }

    public EventSystem getEventSystem() {
        return eventSystem;
    }

    public void setEventSystem(EventSystem system) {
        this.eventSystem = system;
    }

    private void createDatabase(OGraphDatabase db) {
        db.create();
        db.createVertexType(EntityVertexType);
        db.createVertexType(ComponentVertexType);
        db.createEdgeType(OwnsEdgeType);
        db.createEdgeType(ReferencesEdgeType);
        db.getMetadata().getSchema().save();

        OSecurity securityManager = db.getMetadata().getSecurity();
        if (!"admin".equals(dbUsername)) {
            securityManager.getUser("admin").setAccountStatus(OUser.STATUSES.SUSPENDED);
            securityManager.createUser(dbUsername, dbPassword, new String[] {"admin"});
        }
    }

    private OGraphDatabase getDB() {
        OGraphDatabase db = localDatabase.get();
        if (db == null) {
            throw new RuntimeException("EntityManager cannot be access off of the main thread (for the moment)");
        }
        return db;
    }

    private <T extends Component> T deserializeComponent(ODocument componentDoc, Class<T> componentClass) {
        if (componentDoc != null) {
            try {
                // TODO: Improved load
                T component = componentClass.newInstance();
                for (Field field : component.getClass().getDeclaredFields()) {
                    field.setAccessible(true);
                    field.set(component, componentDoc.field(field.getName()));
                }
                return component;
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Failed to deserialize componentL " + componentDoc.toJSON(), e);
            }
        }
        return null;
    }

    private <T extends Component> void serializeComponent(ODocument componentDoc, T component) {
        try {
            for (Field field : component.getClass().getDeclaredFields()) {
                field.setAccessible(true);
                componentDoc.field(field.getName(), field.get(component));
            }
        } catch (IllegalAccessException e) {
            logger.log(Level.SEVERE, "Error serializing component: " + component, e);
        }
    }

    public ODocument findComponentOfEntity(final ODocument entityDoc, Class<? extends Component> componentClass) {
        final OMVRBTreeRIDSet set = entityDoc.field(OGraphDatabase.VERTEX_FIELD_OUT);

        // Find matching label
        if (set != null) {
            for (OIdentifiable item : set) {
                ODocument edge = (ODocument) item;
                if (getComponentVertexType(componentClass).equals(edge.field(OGraphDatabase.LABEL)))
                    return getDB().getInVertex(edge);
            }
        }

        return null;
    }
    
    private ODocument getComponentOwner(final ODocument componentDoc) {
        final OMVRBTreeRIDSet set = componentDoc.field(OGraphDatabase.VERTEX_FIELD_IN);

        // Find matching label
        if (set != null) {
            for (OIdentifiable item : set) {
                ODocument edge = (ODocument) item;
                    return getDB().getOutVertex(edge);
            }
        }

        return null;
    }
    
    private EntityRef getEntityRef(ORID entityId) {
        return new OrientDBEntityRef(this, entityId);
    }
    
    private EntityRef getEntityRef(ODocument entityDoc) {
        if (entityDoc != null) {
            return getEntityRef(entityDoc.getIdentity());
        }
        return null;
    }
    
    
    private String getComponentVertexType(Class<? extends Component> componentClass) {
        return componentClass.getSimpleName();
    }
    
    private String getComponentVertexType(Component component) {
        return getComponentVertexType(component.getClass());
    }
    
    
    private class ComponentsIterator<T extends Component> implements Iterator<Map.Entry<EntityRef, T>>
    {
        private final Class<T> componentClass;
        private OSQLSynchQuery<ODocument> query;
        private ORID last = new ORecordId();
        
        public ComponentsIterator(Class<T> componentClass) {
            this.componentClass = componentClass;

            // TODO: Explore prepared queries and native queries
            query = new OSQLSynchQuery<ODocument>("select * from " + getComponentVertexType(componentClass) + " where @rid > ? LIMIT 1");
        }

        // TODO: Wasteful to do the query and throw away the result? But need to check to ensure the next item is still available
        public boolean hasNext() {
            query.resetPagination();
            return !getDB().query(query, last).isEmpty();
        }

        public Map.Entry<EntityRef, T> next() {
            query.resetPagination();
            List<ODocument> next = getDB().query(query, last);
            if (!next.isEmpty()) {
                ODocument resultDoc = next.get(0);
                last = resultDoc.getIdentity();
                ODocument entity = getComponentOwner(resultDoc);
                T result = deserializeComponent(resultDoc, componentClass);
                return new AbstractMap.SimpleEntry<EntityRef, T>(getEntityRef(entity), result);
            }
            return null;
        }

        public void remove() {
            throw new UnsupportedOperationException("Removal during iteration not supported yet");
        }
    }
    
    private class EntityWithComponentsIterator implements Iterator<EntityRef> {

        private OSQLSynchQuery<ODocument> query;
        private ORID last = new ORecordId();

        public EntityWithComponentsIterator(Class<? extends Component> ... componentClasses) {
            // TODO: Explore alternative mechanisms
            StringBuilder queryStringBuilder = new StringBuilder();
            queryStringBuilder.append("select * from " + EntityVertexType);
            if (componentClasses.length > 0) {
                queryStringBuilder.append(" WHERE");
            }
            for (Class<? extends Component> componentClass : componentClasses) {
                queryStringBuilder.append(" out CONTAINS (label = '" + getComponentVertexType(componentClass) + "') and");
            }
            queryStringBuilder.append(" @rid > ? LIMIT 1");
            query = new OSQLSynchQuery<ODocument>(queryStringBuilder.toString());
        }

        public boolean hasNext() {
            query.resetPagination();
            return !getDB().query(query, last).isEmpty();
        }

        public EntityRef next() {
            query.resetPagination();
            List<ODocument> result = getDB().query(query, last);
            if (!result.isEmpty()) {
                last = result.get(0).getIdentity();
                return getEntityRef(result.get(0));
            }
            return null;
        }

        public void remove() {
            throw new UnsupportedOperationException("Removing during iteration not supported yet");
        }
    }

}
