package org.terasology.entitySystem.orientdb;

import com.google.common.base.Joiner;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.orientechnologies.orient.core.command.OCommandOutputListener;
import com.orientechnologies.orient.core.db.graph.OGraphDatabase;
import com.orientechnologies.orient.core.db.graph.OGraphDatabasePool;
import com.orientechnologies.orient.core.db.record.OIdentifiable;
import com.orientechnologies.orient.core.db.tool.ODatabaseExport;
import com.orientechnologies.orient.core.id.ORID;
import com.orientechnologies.orient.core.id.ORecordId;
import com.orientechnologies.orient.core.index.OCompositeIndexDefinition;
import com.orientechnologies.orient.core.index.OIndex;
import com.orientechnologies.orient.core.index.OIndexManager;
import com.orientechnologies.orient.core.intent.OIntent;
import com.orientechnologies.orient.core.intent.OIntentMassiveInsert;
import com.orientechnologies.orient.core.intent.OIntentMassiveRead;
import com.orientechnologies.orient.core.metadata.schema.OClass;
import com.orientechnologies.orient.core.metadata.schema.OProperty;
import com.orientechnologies.orient.core.metadata.schema.OType;
import com.orientechnologies.orient.core.metadata.security.OSecurity;
import com.orientechnologies.orient.core.metadata.security.OUser;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.serialization.OSerializableStream;
import com.orientechnologies.orient.core.sql.OCommandSQL;
import com.orientechnologies.orient.core.sql.query.OSQLSynchQuery;
import com.orientechnologies.orient.core.type.tree.OMVRBTreeRIDSet;
import org.junit.runners.Parameterized;
import org.terasology.entitySystem.Component;
import org.terasology.entitySystem.EntityManager;
import org.terasology.entitySystem.EntityRef;
import org.terasology.entitySystem.EventSystem;
import org.terasology.entitySystem.event.AddComponentEvent;
import org.terasology.entitySystem.event.ChangedComponentEvent;
import org.terasology.entitySystem.event.RemovedComponentEvent;
import org.terasology.entitySystem.orientdb.types.EnumTypeHandler;
import org.terasology.entitySystem.orientdb.types.ListTypeHandler;
import org.terasology.entitySystem.orientdb.types.MappedContainerTypeHandler;
import org.terasology.entitySystem.orientdb.types.SimpleTypeHandler;
import org.terasology.performanceMonitor.PerformanceMonitor;

import javax.sql.rowset.Joinable;
import java.io.IOException;
import java.lang.reflect.*;
import java.math.BigDecimal;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.junit.Assert.assertNotNull;

/**
 * @author Immortius <immortius@gmail.com>
 */
// TODO: Support for sets
// TODO: Support for maps
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
    private Map<Class<?>, ValueTypeHandler<?>> customTypeHandlers = Maps.newHashMap();
    private Map<Class<? extends Component>, SerializationInfo> componentSerializationLookup = Maps.newHashMap();

    private static class SerializationInfo {
        public List<FieldInfo> fields = Lists.newArrayList();
        public List<RelationshipInfo> relationships = Lists.newArrayList();
    }
    
    private static class FieldInfo {
        public Field field;
        public ValueTypeHandler serializationHandler;
        
        public FieldInfo(Field field, ValueTypeHandler handler) {
            this.field = field;
            this.serializationHandler = handler;
        }
    }
    
    private static class RelationshipInfo {
        public Field field;
        public boolean isList;
        
        public RelationshipInfo(Field field, boolean isList) {
            this.field = field;
            this.isList = isList;
        }
    }

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
        pool.setup();

        localDatabase.set(pool.acquire(dbLocation, dbUsername, dbPassword));

        SimpleTypeHandler simpleTypeHandler  = new SimpleTypeHandler();
        customTypeHandlers.put(Boolean.class, simpleTypeHandler);
        customTypeHandlers.put(Integer.class, simpleTypeHandler);
        customTypeHandlers.put(Short.class, simpleTypeHandler);
        customTypeHandlers.put(Long.class, simpleTypeHandler);
        customTypeHandlers.put(Float.class, simpleTypeHandler);
        customTypeHandlers.put(Double.class, simpleTypeHandler);
        customTypeHandlers.put(Boolean.TYPE, simpleTypeHandler);
        customTypeHandlers.put(Integer.TYPE, simpleTypeHandler);
        customTypeHandlers.put(Short.TYPE, simpleTypeHandler);
        customTypeHandlers.put(Long.TYPE, simpleTypeHandler);
        customTypeHandlers.put(Float.TYPE, simpleTypeHandler);
        customTypeHandlers.put(Double.TYPE, simpleTypeHandler);
        customTypeHandlers.put(Byte.TYPE, simpleTypeHandler);
        customTypeHandlers.put(Date.class, simpleTypeHandler);
        customTypeHandlers.put(String.class, simpleTypeHandler);
        customTypeHandlers.put(Array.class, simpleTypeHandler);
        customTypeHandlers.put(OSerializableStream.class, simpleTypeHandler);
        customTypeHandlers.put(Byte.class, simpleTypeHandler);
        customTypeHandlers.put(BigDecimal.class, simpleTypeHandler);
    }

    public void init() {
    }
    
    public void dumpDB(String file) throws IOException {

        ODatabaseExport exporter = new ODatabaseExport(getDB(), file, new OCommandOutputListener() {
            public void onMessage(String iText) {
            }
        });
        exporter.exportDatabase();
        exporter.close();
    }

    public void setCacheEnabled(boolean enabled) {
        getDB().getLevel2Cache().setEnable(enabled);
    }

    public void registerComponentType(Class<? extends Component> componentClass) {
        OGraphDatabase db = getDB();
        if (db.getVertexType(getComponentVertexType(componentClass)) == null) {
            db.createVertexType(getComponentVertexType(componentClass), ComponentVertexType);
            OClass entityType = db.getVertexType(EntityVertexType);
            if (entityType.getProperty(getComponentVertexType(componentClass)) == null) {
                entityType.createProperty(getComponentVertexType(componentClass), OType.BOOLEAN);
            }
            OIndexManager indexManager = db.getMetadata().getIndexManager();
            OIndex index = indexManager.getIndex(getComponentVertexType(componentClass));
            if (index == null) {
                StringBuilder indexStringBuilder = new StringBuilder();
                indexStringBuilder.append("CREATE INDEX ");
                indexStringBuilder.append(getComponentVertexType(componentClass));
                indexStringBuilder.append(" ON ");
                indexStringBuilder.append(EntityVertexType);
                indexStringBuilder.append("(");
                indexStringBuilder.append(getComponentVertexType(componentClass));
                indexStringBuilder.append(") nonunique");
                db.command(new OCommandSQL(indexStringBuilder.toString()));
            }
            db.getMetadata().getSchema().save();
        }
        registeredComponents.put(getComponentVertexType(componentClass), componentClass);
        SerializationInfo info = new SerializationInfo();
        for (Field field : componentClass.getDeclaredFields()) {
            field.setAccessible(true);
            if (isRelationshipField(field)) {
                info.relationships.add(new RelationshipInfo(field, List.class.isAssignableFrom(field.getType())));
            } else {
                ValueTypeHandler handler = getHandlerFor(field.getGenericType(), true);
                if (handler == null) {
                    logger.log(Level.SEVERE, "Unsupported field type in component type " + componentClass.getSimpleName() + ", " + field.getName() + " : " + field.getGenericType());
                } else {
                    info.fields.add(new FieldInfo(field, handler));
                }
            }
        }
        componentSerializationLookup.put(componentClass, info);
    }

    public void dispose() {
    }

    private boolean isRelationshipField(Field field) {
        if (EntityRef.class.isAssignableFrom(field.getType())) {
            return true;
        }
        if (List.class.isAssignableFrom(field.getType())) {
            Type type = field.getGenericType();
            if (type instanceof ParameterizedType) {
                ParameterizedType genericType = (ParameterizedType)type;
                if (genericType.getActualTypeArguments().length > 0 && genericType.getActualTypeArguments()[0] == EntityRef.class) {
                    return true;
                }
            }
        }
        return false;
    }

    public <T> void registerValueTypeHandler(Class<? extends T> type, ValueTypeHandler<T> handler) {
        customTypeHandlers.put(type, handler);
    }

    private ValueTypeHandler getHandlerFor(Type type, boolean allowMappedContainer) {
        Class typeClass = null;
        if (type instanceof Class) {
            typeClass = (Class) type;
        } else if (type instanceof ParameterizedType) {
            typeClass = (Class) ((ParameterizedType) type).getRawType();
        }

        if (Enum.class.isAssignableFrom(typeClass)) {
            return new EnumTypeHandler(typeClass);
        }
        else if (List.class.isAssignableFrom(typeClass)) {
            if (type instanceof ParameterizedType && ((ParameterizedType) type).getActualTypeArguments().length > 0)
            {
                ValueTypeHandler innerHandler = getHandlerFor(((ParameterizedType)type).getActualTypeArguments()[0], allowMappedContainer);
                if (innerHandler != null) {
                    return new ListTypeHandler(innerHandler);
                }
            }
        } else if (customTypeHandlers.containsKey(typeClass)) {
            return customTypeHandlers.get(typeClass);
        } else if (allowMappedContainer && !typeClass.isLocalClass() && !(typeClass.isMemberClass() && !Modifier.isStatic(typeClass.getModifiers()))) {
            logger.log(Level.WARNING, "Handling serialization of type " + typeClass + " via MappedContainer");
            MappedContainerTypeHandler mappedHandler = new MappedContainerTypeHandler(typeClass);
            for (Field field : typeClass.getDeclaredFields()) {
                field.setAccessible(true);
                ValueTypeHandler handler = getHandlerFor(field.getGenericType(), false);
                mappedHandler.addField(field, handler);
            }
            return mappedHandler;
        }

        return null;
    }

    public boolean exists(ORID id) {
        return getDB().load(id) != null;
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
            boolean hasComponent = entity.containsField(getComponentVertexType(component));
            ODocument componentDoc = null;
            if (hasComponent) {
                componentDoc = findComponentOfEntity(entity, component.getClass());
            }
            else {
                componentDoc = db.createVertex(getComponentVertexType(component));
                componentDoc.field(OGraphDatabase.LABEL, getComponentVertexType(component));
            }
            if (!hasComponent) {
                entity.field(getComponentVertexType(component), true);
                ODocument edge = db.createEdge(entity, componentDoc, OwnsEdgeType);
                edge.field(OGraphDatabase.LABEL, getComponentVertexType(component));
            }
            serializeComponent(componentDoc, component);
            if (eventSystem != null) {
                if (hasComponent) {
                    eventSystem.send(getEntityRef(entityId), ChangedComponentEvent.newInstance(), component);
                } else {
                    eventSystem.send(getEntityRef(entityId), AddComponentEvent.newInstance(), component);
                }
            }
        }
        return component;
    }

    public <T extends Component> T getComponent(ORID entityId, Class<T> componentClass) {
        PerformanceMonitor.startActivity("getComponent");
        OGraphDatabase db = getDB();
        ODocument entity = db.load(entityId);
        if (entity != null) {
            ODocument componentDoc = findComponentOfEntity(entity, componentClass);
            if (componentDoc != null) {
                T result =  deserializeComponent(componentDoc, componentClass);
                PerformanceMonitor.endActivity();
                return result;
            }
        }
        PerformanceMonitor.endActivity();
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

            // Relationships will have changed from the above, reload.
            entity.reload();
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
                componentDoc.removeField(getComponentVertexType(componentClass));
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
        PerformanceMonitor.startActivity("deserializeComponent");
        SerializationInfo info = componentSerializationLookup.get(componentClass);
        try {
            T component = componentClass.newInstance();
            for (FieldInfo fieldInfo : info.fields) {
                Object value = componentDoc.field(fieldInfo.field.getName());
                if (value == null) continue;
                Object deserializedValue = fieldInfo.serializationHandler.deserialize(value);
                if (deserializedValue == null) continue;
                fieldInfo.field.set(component, deserializedValue);
            }
            for (RelationshipInfo relationshipInfo : info.relationships) {
                for (RelationshipInfo relationship : info.relationships) {
                    if (relationship.isList) {
                        List<EntityRef> refs = getReferences(componentDoc, relationship.field.getName());
                        relationship.field.set(component, refs);
                    } else {
                        EntityRef ref = getReference(componentDoc, relationship.field.getName());
                        if (ref != null) {
                            relationship.field.set(component, ref);
                        }
                    }
                }
            }
            PerformanceMonitor.endActivity();
            return component;
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to deserialize component: " + componentDoc.toJSON(), e);
        }
        PerformanceMonitor.endActivity();
        return null;
    }

    private <T extends Component> void serializeComponent(ODocument componentDoc, T component) {
        PerformanceMonitor.startActivity("serializeComponent");
        OGraphDatabase db = getDB();
        if (component == null || componentDoc == null) return;
        try {
            SerializationInfo info = componentSerializationLookup.get(component.getClass());
            for (FieldInfo fieldInfo : info.fields) {
                Object value = fieldInfo.field.get(component);
                Object serializedValue = fieldInfo.serializationHandler.serialize(value);
                componentDoc.field(fieldInfo.field.getName(), serializedValue);
            }
            PerformanceMonitor.startActivity("saveDoc");
            componentDoc.save();
            PerformanceMonitor.endActivity();

            for (RelationshipInfo relationship : info.relationships) {
                if (relationship.isList) {
                    List<EntityRef> refs = (List<EntityRef>)relationship.field.get(component);
                    setReferences(componentDoc, refs, relationship.field.getName());
                } else {
                    EntityRef ref = (EntityRef)relationship.field.get(component);
                    setReference(componentDoc, ref, relationship.field.getName());
                }
            }

        } catch (IllegalAccessException e) {
            logger.log(Level.SEVERE, "Error serializing component: " + component, e);
        }
        PerformanceMonitor.endActivity();
    }
    
    public EntityRef getReference(ODocument component, String label) {
        OGraphDatabase db = getDB();
        for (OIdentifiable e : db.getOutEdges(component)) {
            ODocument edgeDoc = (ODocument) e;

            if (ReferencesEdgeType.equals(edgeDoc.getClassName())) {
                if (label.equals(edgeDoc.field(OGraphDatabase.LABEL))) {
                    OIdentifiable v = edgeDoc.field(OGraphDatabase.EDGE_FIELD_IN);
                    return getEntityRef(v.getIdentity());
                }
            }
        }
        return null;
    }

    public List<EntityRef> getReferences(ODocument component, String label) {
        OGraphDatabase db = getDB();
        List<EntityRef> result = Lists.newArrayList();
        for (OIdentifiable e : db.getOutEdges(component)) {
            ODocument edgeDoc = (ODocument) e;

            if (ReferencesEdgeType.equals(edgeDoc.getClassName())) {
                if (label.equals(edgeDoc.field(OGraphDatabase.LABEL))) {
                    OIdentifiable v = edgeDoc.field(OGraphDatabase.EDGE_FIELD_IN);
                    result.add(getEntityRef(v.getIdentity()));
                }
            }
        }
        return result;
    }
    
    private void setReference(ODocument component, EntityRef entityRef, String label) {
        OGraphDatabase db = getDB();
        ODocument entity = null;
        if (entityRef instanceof OrientDBEntityRef) {
            entity = db.load(((OrientDBEntityRef)entityRef).getId());
        }
        ODocument edge = null;

        // Check Edges, find existing match and remove existing with same label
        for (OIdentifiable e : db.getOutEdges(component)) {
            ODocument edgeDoc = (ODocument) e;
            
            if (ReferencesEdgeType.equals(edgeDoc.getClassName())) {
                if (label.equals(edgeDoc.field(OGraphDatabase.LABEL))) {
                    ODocument targetDoc = db.getInVertex(edgeDoc);
                    if (edge == null && targetDoc.equals(entity)) {
                        edge = edgeDoc;
                    } else {
                        db.removeEdge(edge);
                    }
                }
            }
        }
        
        // Create new edge
        if (edge == null && entity != null) {
            edge = db.createEdge(component, entity, ReferencesEdgeType);
            edge.field(OGraphDatabase.LABEL, label);
            edge.save();
        }
    }

    private void setReferences(ODocument component, List<EntityRef> entityRefs, String label) {
        OGraphDatabase db = getDB();

        // Check Edges, find existing matchs
        Multimap<ORID, ODocument> existingEdges = HashMultimap.create();
        for (OIdentifiable e : db.getOutEdges(component)) {
            ODocument edgeDoc = (ODocument) e;

            if (ReferencesEdgeType.equals(edgeDoc.getClassName())) {
                if (label.equals(edgeDoc.field(OGraphDatabase.LABEL))) {
                    ORID target = edgeDoc.<OIdentifiable>field(OGraphDatabase.EDGE_FIELD_IN).getIdentity();
                    existingEdges.put(target, edgeDoc);
                }
            }
        }

        if (entityRefs != null) {
            for (EntityRef entityRef : entityRefs) {
                ODocument entity = null;
                if (entityRef instanceof OrientDBEntityRef) {
                    entity = db.load(((OrientDBEntityRef)entityRef).getId());
                }
                if (entity == null) continue;
                Iterator<ODocument> edges = existingEdges.get(entity.getIdentity()).iterator();
                // We already have an edge for this reference, remove from collection so it isn't destroyed later
                if (edges.hasNext()) {
                    edges.next();
                    edges.remove();
                } else {
                    ODocument edge = db.createEdge(component, entity, ReferencesEdgeType);
                    edge.field(OGraphDatabase.LABEL, label);
                    edge.save();                     
                }
            }
        }

        // Remove unmatched edges
        for (ODocument edge : existingEdges.values()) {
            db.removeEdge(edge);
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
        Iterator<ODocument> iterator;


        public EntityWithComponentsIterator(Class<? extends Component> ... componentClasses) {
            PerformanceMonitor.startActivity("IterateEntities");
            PerformanceMonitor.startActivity("BuildQuery");
            // TODO: Explore alternative mechanisms
            StringBuilder queryStringBuilder = new StringBuilder();
            queryStringBuilder.append("select * from " + EntityVertexType);
            if (componentClasses.length > 0) {
                queryStringBuilder.append(" WHERE");
            }
            boolean first = true;
            for (Class<? extends Component> componentClass : componentClasses) {
                if (!first) {
                    queryStringBuilder.append(" and");
                }
                first = false;
                queryStringBuilder.append(" " + getComponentVertexType(componentClass) + " = true");
            }
            query = new OSQLSynchQuery<ODocument>(queryStringBuilder.toString());
            iterator = getDB().<List<ODocument>>query(query).iterator();
            PerformanceMonitor.endActivity();
            PerformanceMonitor.endActivity();
        }

        public boolean hasNext() {
            return iterator.hasNext();
        }

        public EntityRef next() {
            EntityRef result = getEntityRef(iterator.next());
            return result;
        }

        public void remove() {
            throw new UnsupportedOperationException("Removing during iteration not supported yet");
        }
    }

}
