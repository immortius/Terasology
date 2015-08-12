/*
 * Copyright 2015 MovingBlocks
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.terasology.engine.subsystem.common;

import org.terasology.assets.AssetFactory;
import org.terasology.assets.module.ModuleAwareAssetTypeManager;
import org.terasology.context.Context;
import org.terasology.engine.ComponentSystemManager;
import org.terasology.engine.GameEngine;
import org.terasology.engine.SimpleUri;
import org.terasology.engine.subsystem.EngineSubsystem;
import org.terasology.entitySystem.Component;
import org.terasology.entitySystem.entity.EntityManager;
import org.terasology.entitySystem.entity.internal.EngineEntityManager;
import org.terasology.entitySystem.entity.internal.PojoEntityManager;
import org.terasology.entitySystem.event.Event;
import org.terasology.entitySystem.event.internal.EventSystem;
import org.terasology.entitySystem.event.internal.EventSystemImpl;
import org.terasology.entitySystem.metadata.ComponentLibrary;
import org.terasology.entitySystem.metadata.EventLibrary;
import org.terasology.entitySystem.metadata.MetadataUtil;
import org.terasology.entitySystem.prefab.Prefab;
import org.terasology.entitySystem.prefab.PrefabData;
import org.terasology.entitySystem.prefab.PrefabManager;
import org.terasology.entitySystem.prefab.internal.PojoPrefab;
import org.terasology.entitySystem.prefab.internal.PojoPrefabManager;
import org.terasology.entitySystem.prefab.internal.PrefabDeltaFormat;
import org.terasology.entitySystem.prefab.internal.PrefabFormat;
import org.terasology.entitySystem.systems.internal.DoNotAutoRegister;
import org.terasology.module.ModuleEnvironment;
import org.terasology.network.NetworkSystem;
import org.terasology.persistence.typeHandling.TypeSerializationLibrary;
import org.terasology.reflection.copy.CopyStrategyLibrary;
import org.terasology.reflection.reflect.ReflectFactory;

/**
 *
 */
public class EntitySubsystem implements EngineSubsystem {
    private PojoEntityManager entityManager;
    private ComponentLibrary componentLibrary;
    private EventLibrary eventLibrary;

    private PrefabFormat registeredPrefabFormat;
    private PrefabDeltaFormat registeredPrefabDeltaFormat;

    private ComponentSystemManager componentSystemManager;

    @Override
    public String getName() {
        return "Entity";
    }

    @Override
    public void populateRootContext(Context rootContext) {
        entityManager = new PojoEntityManager();
        rootContext.put(EntityManager.class, entityManager);
        rootContext.put(EngineEntityManager.class, entityManager);
    }

    @Override
    public void initialise(GameEngine engine, Context rootContext) {
        entityManager.setTypeSerializerLibrary(rootContext.get(TypeSerializationLibrary.class));
    }

    @Override
    public void registerCoreAssetTypes(ModuleAwareAssetTypeManager assetTypeManager) {
        if (registeredPrefabFormat != null) {
            assetTypeManager.removeCoreFormat(Prefab.class, registeredPrefabFormat);
        }
        if (registeredPrefabDeltaFormat != null) {
            assetTypeManager.registerCoreDeltaFormat(Prefab.class, registeredPrefabDeltaFormat);
        }
        assetTypeManager.registerCoreAssetType(Prefab.class,
                (AssetFactory<Prefab, PrefabData>) PojoPrefab::new, false, "prefabs");
    }

    @Override
    public void preEnvironmentChange(ModuleEnvironment newEnvironment, Context environmentContext, ModuleAwareAssetTypeManager assetTypeManager) {
        ReflectFactory reflectFactory = environmentContext.get(ReflectFactory.class);
        CopyStrategyLibrary copyStrategyLibrary = environmentContext.get(CopyStrategyLibrary.class);
        TypeSerializationLibrary typeSerializationLibrary = environmentContext.get(TypeSerializationLibrary.class);

        this.componentLibrary = new ComponentLibrary(newEnvironment, reflectFactory, copyStrategyLibrary);
        this.eventLibrary = new EventLibrary(newEnvironment, reflectFactory, copyStrategyLibrary);
        environmentContext.put(ComponentLibrary.class, componentLibrary);
        environmentContext.put(EventLibrary.class, eventLibrary);

        registeredPrefabFormat = new PrefabFormat(componentLibrary, typeSerializationLibrary);
        assetTypeManager.registerCoreFormat(Prefab.class, registeredPrefabFormat);
        registeredPrefabDeltaFormat = new PrefabDeltaFormat(componentLibrary, typeSerializationLibrary);
        assetTypeManager.registerCoreDeltaFormat(Prefab.class, registeredPrefabDeltaFormat);

        registerComponents(componentLibrary, newEnvironment);

        PrefabManager prefabManager = new PojoPrefabManager(environmentContext);
        entityManager.setPrefabManager(prefabManager);
        environmentContext.put(PrefabManager.class, prefabManager);

        EventSystem eventSystem = new EventSystemImpl(eventLibrary, environmentContext.get(NetworkSystem.class));
        entityManager.setEventSystem(eventSystem);
        environmentContext.put(EventSystem.class, eventSystem);

        registerEvents(eventSystem, newEnvironment);

        if (componentSystemManager != null) {
            componentSystemManager.shutdown();
        }
        componentSystemManager = new ComponentSystemManager(environmentContext);
        environmentContext.put(ComponentSystemManager.class, componentSystemManager);
    }

    @Override
    public void postEnvironmentChange(ModuleEnvironment newEnvironment, Context environmentContext) {
        componentSystemManager.initialise();
    }

    public ComponentSystemManager getComponentSystemManager() {
        return componentSystemManager;
    }

    private static void registerComponents(ComponentLibrary library, ModuleEnvironment environment) {
        for (Class<? extends Component> componentType : environment.getSubtypesOf(Component.class)) {
            if (componentType.getAnnotation(DoNotAutoRegister.class) == null) {
                String componentName = MetadataUtil.getComponentClassName(componentType);
                library.register(new SimpleUri(environment.getModuleProviding(componentType), componentName), componentType);
            }
        }
    }

    private static void registerEvents(EventSystem eventSystem, ModuleEnvironment environment) {
        for (Class<? extends Event> type : environment.getSubtypesOf(Event.class)) {
            if (type.getAnnotation(DoNotAutoRegister.class) == null) {
                eventSystem.registerEvent(new SimpleUri(environment.getModuleProviding(type), type.getSimpleName()), type);
            }
        }
    }
}
