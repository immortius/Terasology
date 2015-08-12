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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terasology.context.Context;
import org.terasology.engine.subsystem.EngineSubsystem;
import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.entitySystem.entity.internal.EngineEntityManager;
import org.terasology.module.ModuleEnvironment;
import org.terasology.persistence.typeHandling.TypeSerializationLibrary;
import org.terasology.persistence.typeHandling.extensionTypes.CollisionGroupTypeHandler;
import org.terasology.persistence.typeHandling.extensionTypes.EntityRefTypeHandler;
import org.terasology.physics.CollisionGroup;
import org.terasology.physics.CollisionGroupManager;
import org.terasology.reflection.copy.CopyStrategy;
import org.terasology.reflection.copy.CopyStrategyLibrary;
import org.terasology.reflection.copy.RegisterCopyStrategy;
import org.terasology.reflection.reflect.ReflectFactory;
import org.terasology.reflection.reflect.ReflectionReflectFactory;
import org.terasology.utilities.ReflectionUtil;

/**
 *
 */
public class ObjectManipulationSubsystem implements EngineSubsystem {
    private static final Logger logger = LoggerFactory.getLogger(ObjectManipulationSubsystem.class);

    private ReflectFactory reflectFactory;

    @Override
    public String getName() {
        return "Object manipulation";
    }

    @Override
    public void populateRootContext(Context rootContext) {
        reflectFactory = new ReflectionReflectFactory();
        rootContext.put(ReflectFactory.class, reflectFactory);
    }

    @Override
    public void populateEnvironmentContext(ModuleEnvironment newEnvironment, Context environmentContext) {
        CopyStrategyLibrary copyStrategyLibrary = new CopyStrategyLibrary(reflectFactory);
        for (Class<? extends CopyStrategy> copyStrategy : newEnvironment.getSubtypesOf(CopyStrategy.class)) {
            if (copyStrategy.getAnnotation(RegisterCopyStrategy.class) == null) {
                continue;
            }
            Class targetType = ReflectionUtil.getTypeParameterForSuper(copyStrategy, CopyStrategy.class, 0);
            if (targetType != null) {
                try {
                    copyStrategyLibrary.register(targetType, copyStrategy.newInstance());
                } catch (InstantiationException | IllegalAccessException e) {
                    logger.error("Cannot register CopyStrategy '{}' - failed to instantiate", copyStrategy, e);
                }
            } else {
                logger.error("Cannot register CopyStrategy '{}' - unable to determine target type", copyStrategy);
            }
        }
        environmentContext.put(CopyStrategyLibrary.class, copyStrategyLibrary);

        TypeSerializationLibrary typeSerializationLibrary = TypeSerializationLibrary.createDefaultLibrary(
                reflectFactory,
                copyStrategyLibrary);
        typeSerializationLibrary.add(CollisionGroup.class, new CollisionGroupTypeHandler(environmentContext.get(CollisionGroupManager.class)));
        typeSerializationLibrary.add(EntityRef.class, new EntityRefTypeHandler(environmentContext.get(EngineEntityManager.class)));
        environmentContext.put(TypeSerializationLibrary.class, typeSerializationLibrary);
    }
}
