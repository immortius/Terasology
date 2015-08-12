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
import org.terasology.engine.subsystem.EngineSubsystem;
import org.terasology.logic.behavior.asset.BehaviorTree;
import org.terasology.logic.behavior.asset.BehaviorTreeData;
import org.terasology.logic.behavior.asset.NodesClassLibrary;
import org.terasology.module.ModuleEnvironment;
import org.terasology.reflection.copy.CopyStrategyLibrary;
import org.terasology.reflection.reflect.ReflectFactory;
import org.terasology.rendering.nui.properties.OneOfProviderFactory;

/**
 *
 */
public class BehaviorSubsystem implements EngineSubsystem {
    @Override
    public String getName() {
        return "Behavior";
    }

    @Override
    public void registerCoreAssetTypes(ModuleAwareAssetTypeManager assetTypeManager) {
        assetTypeManager.registerCoreAssetType(BehaviorTree.class,
                (AssetFactory<BehaviorTree, BehaviorTreeData>) BehaviorTree::new, false, "behaviors");
    }

    @Override
    public void preEnvironmentChange(ModuleEnvironment newEnvironment, Context environmentContext, ModuleAwareAssetTypeManager assetTypeManager) {
        environmentContext.put(OneOfProviderFactory.class, new OneOfProviderFactory());

        // Behaviour Trees Node Library
        NodesClassLibrary nodesClassLibrary = new NodesClassLibrary(
                newEnvironment,
                environmentContext.get(ReflectFactory.class),
                environmentContext.get(CopyStrategyLibrary.class));
        environmentContext.put(NodesClassLibrary.class, nodesClassLibrary);
        nodesClassLibrary.scan(newEnvironment);
    }
}
