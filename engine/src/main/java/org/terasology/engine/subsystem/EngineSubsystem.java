/*
 * Copyright 2014 MovingBlocks
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.terasology.engine.subsystem;

import org.terasology.assets.module.ModuleAwareAssetTypeManager;
import org.terasology.context.Context;
import org.terasology.engine.ComponentSystemManager;
import org.terasology.engine.GameEngine;
import org.terasology.engine.modes.GameState;
import org.terasology.module.ModuleEnvironment;

public interface EngineSubsystem {

    /**
     * @return The name of the subsystem
     */
    String getName();

    /**
     * Called on each system before initialisation. This is an opportunity to add anything into the root context that will carry across the entire run
     * of the engine, and may be used by other systems. At this point other systems will not have populated the root context, not been initialised.
     *
     * @param rootContext The root context, that will survive the entire run of the engine
     */
    default void populateRootContext(Context rootContext) {
    }

    /**
     * Called to initialise the system. At this point other systems should have added to the root context anything that may be needed, but may not have been initialised.
     *
     * @param engine      The game engine
     * @param rootContext The root context, that will survive the entire run of the engine
     */
    default void initialise(GameEngine engine, Context rootContext) {
    }

    /**
     * Called to register any core asset types this system provides. This happens after initialise and before the first environment switch. Core asset types remain active for
     * the lifespan of the engine
     *
     * @param assetTypeManager The asset type manager to register asset types to
     */
    default void registerCoreAssetTypes(ModuleAwareAssetTypeManager assetTypeManager) {
    }

    /**
     * Called to populate a new, environment specific context. This occurs before the environment is changed, and before preEnvironmentChange() is called.
     *
     * @param newEnvironment
     * @param environmentContext
     */
    default void populateEnvironmentContext(ModuleEnvironment newEnvironment, Context environmentContext) {
    }

    /**
     * Called to do any setup required before environment change
     *
     * @param newEnvironment
     * @param environmentContext
     */
    default void preEnvironmentChange(ModuleEnvironment newEnvironment, Context environmentContext, ModuleAwareAssetTypeManager assetTypeManager) {
    }

    /**
     * Called after the module environment has changed.
     */
    default void postEnvironmentChange(ModuleEnvironment newEnvironment, Context environmentContext) {
    }

    /**
     * Called after module environment has changed to allow subsystems to provide component systems
     * @param componentSystemManager
     */
    default void registerSystems(ComponentSystemManager componentSystemManager) {
    }

    /**
     * Called to do any further work after initialisation (this is also after the first environment switch). All systems are initialised at this point. This
     * is the last opportunity to inject into the rootContext.
     * @param rootContext
     * @param environmentContext
     */
    default void postInitialise(Context rootContext, Context environmentContext) {
    }

    /**
     * Called before the main game logic update, once a frame/full update cycle
     * @param currentState The current state
     * @param delta The total time this frame/update cycle
     */
    default void preUpdate(GameState currentState, float delta) {
    }

    /**
     * Called after the main game logic update, once a frame/full update cycle
     * @param currentState The current state
     * @param delta The total time this frame/update cycle
     */
    default void postUpdate(GameState currentState, float delta) {
    }

    /**
     * Called just prior to shutdown. Allows for cleanup that requires other systems to still be running
     */
    default void prepareForShutdown() {
    }

    /**
     * Shut down this system. Other systems may have already been shut down.
     */
    default void shutdown() {
    }

}
