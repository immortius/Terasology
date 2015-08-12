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
package org.terasology.engine.subsystem.lwjgl;

import org.lwjgl.LWJGLException;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.terasology.assets.module.ModuleAwareAssetTypeManager;
import org.terasology.config.Config;
import org.terasology.context.Context;
import org.terasology.engine.ComponentSystemManager;
import org.terasology.engine.modes.GameState;
import org.terasology.input.InputSystem;
import org.terasology.input.cameraTarget.CameraTargetSystem;
import org.terasology.input.lwjgl.LwjglKeyboardDevice;
import org.terasology.input.lwjgl.LwjglMouseDevice;
import org.terasology.module.ModuleEnvironment;

public class LwjglInput extends BaseLwjglSubsystem {
    private Context context;
    private CameraTargetSystem cameraTargetSystem;
    private InputSystem inputSystem;

    @Override
    public String getName() {
        return "Input";
    }


    @Override
    public synchronized void populateRootContext(Context rootContext) {
        this.context = rootContext;
        // TODO: Reduce coupling between Input system and CameraTargetSystem,
        // TODO: potentially eliminating the following lines. See Issue #1126
        cameraTargetSystem = new CameraTargetSystem();
        rootContext.put(CameraTargetSystem.class, cameraTargetSystem);

    }

    @Override
    public void registerCoreAssetTypes(ModuleAwareAssetTypeManager assetTypeManager) {
    }

    @Override
    public void registerSystems(ComponentSystemManager componentSystemManager) {
        if (inputSystem != null) {
            componentSystemManager.register(inputSystem, "engine:InputSystem");
        }
        componentSystemManager.register(cameraTargetSystem, "engine:CameraTargetSystem");
    }

    @Override
    public void preEnvironmentChange(ModuleEnvironment newEnvironment, Context environmentContext, ModuleAwareAssetTypeManager assetTypeManager) {
        if (inputSystem != null) {
            inputSystem.clearInputEntities();
        }
    }

    @Override
    public void postInitialise(Context rootContext, Context environmentContext) {
        this.context = rootContext;
        initControls();
        updateInputConfig();
        Mouse.setGrabbed(false);
        environmentContext.get(ComponentSystemManager.class).register(inputSystem);
    }

    @Override
    public void postUpdate(GameState currentState, float delta) {
        currentState.handleInput(delta);
    }

    @Override
    public void shutdown() {
        Mouse.destroy();
        Keyboard.destroy();
    }

    private void initControls() {
        try {
            Keyboard.create();
            Keyboard.enableRepeatEvents(true);
            Mouse.create();
            inputSystem = new InputSystem();
            context.put(InputSystem.class, inputSystem);
            inputSystem.setMouseDevice(new LwjglMouseDevice());
            inputSystem.setKeyboardDevice(new LwjglKeyboardDevice());
        } catch (LWJGLException e) {
            throw new RuntimeException("Could not initialize controls.", e);
        }
    }

    private void updateInputConfig() {
        Config config = context.get(Config.class);
        config.getInput().getBinds().updateForChangedMods(context);
        config.save();
    }
}
