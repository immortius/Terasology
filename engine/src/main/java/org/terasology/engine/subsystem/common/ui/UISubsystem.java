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
package org.terasology.engine.subsystem.common.ui;

import org.terasology.assets.AssetFactory;
import org.terasology.assets.module.ModuleAwareAssetTypeManager;
import org.terasology.context.Context;
import org.terasology.engine.ComponentSystemManager;
import org.terasology.engine.GameEngine;
import org.terasology.engine.subsystem.EngineSubsystem;
import org.terasology.rendering.nui.NUIManager;
import org.terasology.rendering.nui.asset.UIData;
import org.terasology.rendering.nui.asset.UIElement;
import org.terasology.rendering.nui.internal.CanvasRenderer;
import org.terasology.rendering.nui.internal.NUIManagerInternal;
import org.terasology.rendering.nui.skin.UISkin;
import org.terasology.rendering.nui.skin.UISkinData;

/**
 *
 */
public class UISubsystem implements EngineSubsystem {
    private NUIManagerInternal nuiManager;

    @Override
    public String getName() {
        return "UI";
    }

    @Override
    public void postInitialise(Context rootContext, Context environmentContext) {
        nuiManager = new NUIManagerInternal(rootContext.get(CanvasRenderer.class), environmentContext);
        rootContext.put(NUIManager.class, nuiManager);
        environmentContext.get(ComponentSystemManager.class).register(nuiManager);
    }

    @Override
    public void registerCoreAssetTypes(ModuleAwareAssetTypeManager assetTypeManager) {
        assetTypeManager.registerCoreAssetType(UISkin.class,
                (AssetFactory<UISkin, UISkinData>) UISkin::new, "skins");
        assetTypeManager.registerCoreAssetType(UIElement.class,
                (AssetFactory<UIElement, UIData>) UIElement::new, "ui");
    }

    @Override
    public void registerSystems(ComponentSystemManager componentSystemManager) {
        if (nuiManager != null) {
            componentSystemManager.register(nuiManager);
        }
    }
}
