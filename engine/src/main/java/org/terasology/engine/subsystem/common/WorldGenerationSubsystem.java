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
import org.terasology.assets.AssetFactory;
import org.terasology.assets.module.ModuleAwareAssetTypeManager;
import org.terasology.context.Context;
import org.terasology.engine.GameEngine;
import org.terasology.engine.module.ModuleManager;
import org.terasology.engine.subsystem.EngineSubsystem;
import org.terasology.module.ModuleEnvironment;
import org.terasology.world.block.family.BlockFamilyFactory;
import org.terasology.world.block.family.BlockFamilyFactoryRegistry;
import org.terasology.world.block.family.DefaultBlockFamilyFactoryRegistry;
import org.terasology.world.block.family.RegisterBlockFamilyFactory;
import org.terasology.world.block.loader.BlockFamilyDefinition;
import org.terasology.world.block.loader.BlockFamilyDefinitionData;
import org.terasology.world.block.loader.BlockFamilyDefinitionFormat;
import org.terasology.world.block.shapes.BlockShape;
import org.terasology.world.block.shapes.BlockShapeData;
import org.terasology.world.block.shapes.BlockShapeImpl;
import org.terasology.world.block.sounds.BlockSounds;
import org.terasology.world.block.sounds.BlockSoundsData;
import org.terasology.world.block.tiles.BlockTile;
import org.terasology.world.block.tiles.TileData;
import org.terasology.world.generator.internal.WorldGeneratorManager;

/**
 *
 */
public class WorldGenerationSubsystem implements EngineSubsystem {
    private static final Logger logger = LoggerFactory.getLogger(WorldGenerationSubsystem.class);

    private DefaultBlockFamilyFactoryRegistry familyFactoryRegistry;
    @Override
    public String getName() {
        return "World Generation";
    }

    @Override
    public void populateRootContext(Context rootContext) {

    }

    @Override
    public void initialise(GameEngine engine, Context rootContext) {
        rootContext.put(WorldGeneratorManager.class, new WorldGeneratorManager(rootContext.get(ModuleManager.class)));
    }

    @Override
    public void registerCoreAssetTypes(ModuleAwareAssetTypeManager assetTypeManager) {
        assetTypeManager.registerCoreAssetType(BlockShape.class,
                (AssetFactory<BlockShape, BlockShapeData>) BlockShapeImpl::new, "shapes");
        assetTypeManager.registerCoreAssetType(BlockSounds.class,
                (AssetFactory<BlockSounds, BlockSoundsData>) BlockSounds::new, "blockSounds");
        assetTypeManager.registerCoreAssetType(BlockTile.class,
                (AssetFactory<BlockTile, TileData>) BlockTile::new, "blockTiles");
        assetTypeManager.registerCoreAssetType(BlockFamilyDefinition.class,
                (AssetFactory<BlockFamilyDefinition, BlockFamilyDefinitionData>) BlockFamilyDefinition::new, "blocks");
    }

    @Override
    public void populateEnvironmentContext(ModuleEnvironment newEnvironment, Context environmentContext) {
        familyFactoryRegistry = new DefaultBlockFamilyFactoryRegistry();
        loadFamilies(familyFactoryRegistry, newEnvironment);
    }

    @Override
    public void preEnvironmentChange(ModuleEnvironment newEnvironment, Context environmentContext, ModuleAwareAssetTypeManager assetTypeManager) {
        assetTypeManager.registerCoreFormat(BlockFamilyDefinition.class,
                new BlockFamilyDefinitionFormat(assetTypeManager.getAssetManager(), familyFactoryRegistry));
    }

    private static void loadFamilies(DefaultBlockFamilyFactoryRegistry registry, ModuleEnvironment environment) {
        registry.clear();
        for (Class<?> blockFamilyFactory : environment.getTypesAnnotatedWith(RegisterBlockFamilyFactory.class)) {
            if (!BlockFamilyFactory.class.isAssignableFrom(blockFamilyFactory)) {
                logger.error("Cannot load {}, must be a subclass of BlockFamilyFactory", blockFamilyFactory.getSimpleName());
                continue;
            }

            RegisterBlockFamilyFactory registerInfo = blockFamilyFactory.getAnnotation(RegisterBlockFamilyFactory.class);
            String id = registerInfo.value();
            logger.debug("Registering blockFamilyFactory {}", id);
            try {
                BlockFamilyFactory newBlockFamilyFactory = (BlockFamilyFactory) blockFamilyFactory.newInstance();
                registry.setBlockFamilyFactory(id, newBlockFamilyFactory);
                logger.debug("Loaded blockFamilyFactory {}", id);
            } catch (InstantiationException | IllegalAccessException e) {
                logger.error("Failed to load blockFamilyFactory {}", id, e);
            }
        }
    }
}
