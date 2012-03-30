package org.terasology.entitySystem.orientdbobject;

import com.google.common.collect.*;
import com.orientechnologies.orient.core.command.OCommandOutputListener;
import com.orientechnologies.orient.core.config.OGlobalConfiguration;
import com.orientechnologies.orient.core.db.ODatabaseRecordThreadLocal;
import com.orientechnologies.orient.core.db.graph.OGraphDatabase;
import com.orientechnologies.orient.core.db.graph.OGraphDatabasePool;
import com.orientechnologies.orient.core.db.object.ODatabaseObject;
import com.orientechnologies.orient.core.db.object.ODatabaseObjectPool;
import com.orientechnologies.orient.core.db.object.ODatabaseObjectTx;
import com.orientechnologies.orient.core.db.record.ODatabaseRecord;
import com.orientechnologies.orient.core.db.record.OIdentifiable;
import com.orientechnologies.orient.core.db.tool.ODatabaseExport;
import com.orientechnologies.orient.core.id.ORID;
import com.orientechnologies.orient.core.id.ORecordId;
import com.orientechnologies.orient.core.index.OIndex;
import com.orientechnologies.orient.core.index.OIndexManager;
import com.orientechnologies.orient.core.intent.OIntentMassiveInsert;
import com.orientechnologies.orient.core.metadata.schema.OClass;
import com.orientechnologies.orient.core.metadata.schema.OType;
import com.orientechnologies.orient.core.metadata.security.OSecurity;
import com.orientechnologies.orient.core.metadata.security.OUser;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.serialization.OSerializableStream;
import com.orientechnologies.orient.core.serialization.serializer.object.OObjectSerializer;
import com.orientechnologies.orient.core.serialization.serializer.object.OObjectSerializerContext;
import com.orientechnologies.orient.core.serialization.serializer.object.OObjectSerializerHelper;
import com.orientechnologies.orient.core.sql.OCommandSQL;
import com.orientechnologies.orient.core.sql.query.OSQLSynchQuery;
import com.orientechnologies.orient.core.type.tree.OMVRBTreeRIDSet;
import org.terasology.components.LocationComponent;
import org.terasology.entitySystem.*;
import org.terasology.entitySystem.common.NullIterator;
import org.terasology.entitySystem.event.AddComponentEvent;
import org.terasology.entitySystem.event.ChangedComponentEvent;
import org.terasology.entitySystem.event.RemovedComponentEvent;
import org.terasology.entitySystem.orientdb.ValueTypeHandler;
import org.terasology.entitySystem.orientdb.types.EnumTypeHandler;
import org.terasology.entitySystem.orientdb.types.ListTypeHandler;
import org.terasology.entitySystem.orientdb.types.MappedContainerTypeHandler;
import org.terasology.entitySystem.orientdb.types.SimpleTypeHandler;
import org.terasology.performanceMonitor.PerformanceMonitor;

import javax.persistence.Id;
import javax.vecmath.Vector3f;
import java.io.IOException;
import java.lang.reflect.*;
import java.math.BigDecimal;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Immortius <immortius@gmail.com>
 */
public class OrientDBObjEntityManager implements EntityManager {
    private static OrientDBObjEntityManager instance;
    
    private Logger logger = Logger.getLogger(getClass().getName());
    
    private EventSystem eventSystem;
    private String dbLocation;
    private String dbUsername;
    private String dbPassword;

    private ODatabaseObjectPool pool;

    // ODatabaseObject is not thread-safe, so we need to acquire a different copy per thread
    public ThreadLocal<ODatabaseObjectTx> localDatabase = new ThreadLocal<ODatabaseObjectTx>();

    private OObjectSerializerContext serializerContext;
    private Set<Component> immediatePendingSave = Collections.newSetFromMap(new IdentityHashMap<Component, Boolean>());
    private Set<Component> pendingSave = Collections.newSetFromMap(new IdentityHashMap<Component, Boolean>());
    private OIndex index;
    
    public OrientDBObjEntityManager(String location, String username, String password) {
        OGlobalConfiguration.OBJECT_SAVE_ONLY_DIRTY.setValue(true);
        OGlobalConfiguration.ENVIRONMENT_CONCURRENT.setValue(false);
        instance = this;
        this.dbLocation = location;
        this.dbUsername = username;
        this.dbPassword = password;
        ODatabaseObject db = new ODatabaseObjectTx(location);
        if (!db.exists()) {
            createDatabase(db);
        }
        db.close();
        pool = new ODatabaseObjectPool();
        pool.setup();

        localDatabase.set(pool.acquire(dbLocation, dbUsername, dbPassword));
        ODatabaseRecordThreadLocal.INSTANCE.set(localDatabase.get().getUnderlying());
        index = getDB().getMetadata().getIndexManager().getIndex("ENTITY_COMPONENTS_LOOKUP");

        serializerContext = new OObjectSerializerContext();
        OObjectSerializerHelper.bindSerializerContext(null, serializerContext);

    }

    OIndex getEntityIndex() {
        return index;
    }

    public void dispose() {
        getDB().close();
        pool.close();
    }

    public void dirtyComponent(Component comp) {
        getDB().setDirty(comp);
        if (comp.getClass().equals(LocationComponent.class)) {
            immediatePendingSave.add(comp);
        } else {
            pendingSave.add(comp);
        }
    }

    public void saveImmediateChanges() {
        for (Component comp : immediatePendingSave) {
            getDB().save(comp);
        }
        immediatePendingSave.clear();
    }

    public void saveChanges() {
        for (Component comp : pendingSave) {
            getDB().save(comp);
        }
        pendingSave.clear();
    }

    public void dumpDB(String file) throws IOException {
        ODatabaseExport exporter = new ODatabaseExport((ODatabaseRecord)getDB().getUnderlying(), file, new OCommandOutputListener() {
            public void onMessage(String iText) {
            }
        });
        exporter.exportDatabase();
        exporter.close();
        getDB().getLevel1Cache().setEnable(true);
        getDB().getLevel2Cache().setEnable(true);
    }

    public void setCacheEnabled(boolean enabled) {
        getDB().getLevel1Cache().setEnable(enabled);
        getDB().getLevel2Cache().setEnable(enabled);
    }

    public void registerComponentType(Class<? extends Component> componentClass) {
        // TODO: Analyse and ensure compatible.
        ODatabaseObject db = getDB();
        db.getEntityManager().registerEntityClass(componentClass);
        db.getMetadata().getSchema().getClass(componentClass);
        db.getMetadata().getSchema().save();
    }

    public void registerValueSerializer(OObjectSerializer serializer) {
        serializerContext.bind(serializer);
    }

    public EntityRef create() {
        EntityRef newRef = getDB().newInstance(OrientDBObjEntityRef.class);
        getDB().save(newRef);
        return newRef;
    }

    public long getComponentCount(Class<? extends Component> componentClass) {
        return getDB().countClass(componentClass);
    }

    public <T extends Component> Iterable<Map.Entry<EntityRef, T>> iterateComponents(final Class<T> componentClass) {
        // TODO: implement (or remove)
        return NullIterator.newInstance();
    }

    public Iterable<EntityRef> iteratorEntities(final Class<? extends Component>... componentClasses) {
        PerformanceMonitor.startActivity("Iterate Entities");
        try {
            if (componentClasses == null || componentClasses.length == 0) {
                // TODO: Iterate all entities?
                return NullIterator.newInstance();
            }
            // TODO: Make prepared query
            StringBuilder queryStringBuilder = new StringBuilder();
            queryStringBuilder.append("select from OrientDBObjEntityRef WHERE components containskey '");
            queryStringBuilder.append(componentClasses[0].getSimpleName());
            queryStringBuilder.append("'");

            OSQLSynchQuery<EntityRef> query = new OSQLSynchQuery(queryStringBuilder.toString());

            Iterable<EntityRef> result = getDB().<List<EntityRef>>query(query);
            List<EntityRef> processedResult = Lists.newArrayList();
            if (componentClasses.length > 1) {
                for (EntityRef entity : result) {
                    boolean ok = true;
                    for (int i = 1; ok && i < componentClasses.length; ++i) {
                        ok = entity.hasComponent(componentClasses[i]);
                    }
                    if (ok) {
                        processedResult.add(entity);
                    }
                }
                return processedResult;
            } else {
                return result;
            }
        }
        finally {
            PerformanceMonitor.endActivity();
        }
    }

    public EventSystem getEventSystem() {
        return eventSystem;
    }

    public void setEventSystem(EventSystem system) {
        this.eventSystem = system;
    }

    private void createDatabase(ODatabaseObject db) {
        db.create();
        db.getEntityManager().registerEntityClass(OrientDBObjEntityRef.class);
        OClass entityClass = db.getMetadata().getSchema().getClass(OrientDBObjEntityRef.class);
        entityClass.createProperty("components", OType.LINKMAP);
        db.getMetadata().getSchema().save();

        OIndexManager indexManager = db.getMetadata().getIndexManager();
        db.command(new OCommandSQL("CREATE INDEX ENTITY_COMPONENTS_LOOKUP ON OrientDBObjEntityRef(components by key) notunique")).execute();

        OSecurity securityManager = db.getMetadata().getSecurity();
        if (!"admin".equals(dbUsername)) {
            securityManager.getUser("admin").setAccountStatus(OUser.STATUSES.SUSPENDED);
            securityManager.createUser(dbUsername, dbPassword, new String[] {"admin"});
        }

    }

    private ODatabaseObjectTx getDB() {
        ODatabaseObjectTx db = localDatabase.get();
        if (db == null) {
            throw new RuntimeException("EntityManager cannot be access off of the main thread (for the moment)");
        }
        return db;
    }

    public void clearCache() {
        getDB().getLevel1Cache().invalidate();
        getDB().getLevel2Cache().clear();
    }

    public static class OrientDBObjEntityRef implements EntityRef {
        @Id
        private Object id;

        private Map<String, Component> components = Maps.newHashMap();

        public boolean exists() {
            return id != null && ((ORID)id).isValid();
        }

        public <T extends Component> T getComponent(Class<T> componentClass) {
            if (!exists()) return null;
            return componentClass.cast(components.get(componentClass.getSimpleName()));
        }

        public <T extends Component> T addComponent(T component) {
            PerformanceMonitor.startActivity("Add Component");
            try {
                if (!exists()) return component;
                boolean update = false;
                Component existingComponent = components.get(component.getClass().getSimpleName());
                if (existingComponent == component) {
                    entityManager().dirtyComponent(component);
                    if (entityManager().getEventSystem() != null) {
                        entityManager().getEventSystem().send(this, ChangedComponentEvent.newInstance(), component);
                    }
                    return component;
                }
                else if (existingComponent != null) {
                    components.remove(component.getClass().getSimpleName());
                    entityManager().getDB().delete(existingComponent);
                    update = true;
                }
                components.put(component.getClass().getSimpleName(), component);
                entityManager().getDB().setDirty(this);
                
                // TODO: If component exists, dirty it. (actually, move save into component?)
                entityManager().getDB().save(this);
                if (update) {
                    if (entityManager().getEventSystem() != null) {
                        entityManager().getEventSystem().send(this, ChangedComponentEvent.newInstance(), component);
                    }
                } else {
                    if (entityManager().getEventSystem() != null) {
                        entityManager().getEventSystem().send(this, AddComponentEvent.newInstance(), component);
                    }
                }
                return component;
            }
            finally {
                PerformanceMonitor.endActivity();
            }
        }

        public void removeComponent(Class<? extends Component> componentClass) {
            PerformanceMonitor.startActivity("Remove Component");
            try
            {
                if (!exists()) return;
                Component component = components.get(componentClass.getSimpleName());
                if (component != null) {
                    if (entityManager().getEventSystem() != null) {
                        entityManager().getEventSystem().send(this, RemovedComponentEvent.newInstance(), component);
                    }
                    components.remove(componentClass.getSimpleName());
                    entityManager().getDB().delete(component);
                    entityManager().getDB().setDirty(this);
                    entityManager().getDB().save(this);
                }
            }
            finally {
                PerformanceMonitor.endActivity();
            }
        }

        public void saveComponent(Component component) {

            if (!exists()) return;
            PerformanceMonitor.startActivity("Save Component");
            entityManager().dirtyComponent(component);
            PerformanceMonitor.endActivity();
            if (entityManager().getEventSystem() != null) {
                entityManager().getEventSystem().send(this, ChangedComponentEvent.newInstance(), component);
            }

        }

        public Iterable<Component> iterateComponents() {
            if (!exists()) return NullIterator.newInstance();
            return components.values();
        }

        public void destroy() {
            if (exists()) {
                if (entityManager().getEventSystem() != null) {
                    entityManager().getEventSystem().send(this, RemovedComponentEvent.newInstance());
                }
                for (Component component : components.values()) {
                    entityManager().getDB().delete(component);
                }
                components.clear();

                entityManager().getDB().delete(this);
                id = new ORecordId();
            }
        }

        public void send(Event event) {
            if (!exists()) return;
            if (entityManager().getEventSystem() != null) {
                entityManager().getEventSystem().send(this, event);
            }
        }

        public boolean hasComponent(Class<? extends Component> componentClass) {
            if (!exists()) return false;
            return components.containsKey(componentClass.getSimpleName());
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            OrientDBObjEntityRef that = (OrientDBObjEntityRef) o;

            if (id != null ? !id.equals(that.id) : that.id != null) return false;

            return true;
        }

        @Override
        public int hashCode() {
            return id != null ? id.hashCode() : 0;
        }

        @Override
        public String toString() {
            if (id != null) {
                return "Entity(" + id.toString() + ")";
            }
            return "Entity(#-1:-1)";
        }

        private OrientDBObjEntityManager entityManager() {
            return OrientDBObjEntityManager.instance;
        }
    }
}