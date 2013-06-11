/*
 * Copyright 2013 Moving Blocks
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

package org.terasology.world.block.management;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terasology.world.block.Block;
import org.terasology.world.block.BlockUri;
import org.terasology.world.block.family.BlockFamily;
import org.terasology.world.block.loader.BlockLoader;
import org.terasology.world.block.loader.FreeformFamily;

import java.util.List;
import java.util.Map;

/**
 * @author Immortius
 */
public class BlockManagerClient extends BlockManager {

    private static final Logger logger = LoggerFactory.getLogger(BlockManagerClient.class);
    // This is the id we assign to blocks whose mappings are missing. This shouldn't happen, but in case it does
    // we set them to the last id (don't want to use 0 as they would override air)
    private static final byte UNKNOWN_ID = -1;

    private BlockLoader blockLoader;

    public BlockManagerClient() {
        this(Lists.<String>newArrayList(), Maps.<String, Byte>newHashMap());
    }

    public BlockManagerClient(List<String> registeredBlockFamilies, Map<String, Byte> knownBlockMappings) {
        blockLoader = new BlockLoader();
        BlockLoader.LoadBlockDefinitionResults blockDefinitions = blockLoader.loadBlockDefinitions();
        addBlockFamily(getAirFamily(), true);
        for (BlockFamily family : blockDefinitions.families) {
            addBlockFamily(family, false);
        }
        for (FreeformFamily freeformFamily : blockDefinitions.shapelessDefinitions) {
            addFreeformBlockFamily(freeformFamily.uri, freeformFamily.categories);
        }

        for (String rawFamilyUri : registeredBlockFamilies) {
            BlockUri familyUri = new BlockUri(rawFamilyUri);
            BlockFamily family;
            if (isFreeformFamily(familyUri)) {
                family = blockLoader.loadWithShape(familyUri);
            } else {
                family = getAvailableBlockFamily(familyUri);
            }
            if (family != null) {
                for (Block block : family.getBlocks()) {
                    Byte id = knownBlockMappings.get(block.getURI().toString());
                    if (id != null) {
                        block.setId(id);
                    } else {
                        logger.error("Missing id for block {} in provided family {}", block.getURI(), family.getURI());
                        block.setId(UNKNOWN_ID);
                    }
                }
                registerFamily(family);
            } else {
                logger.error("Family not available: {}", rawFamilyUri);
            }
        }

        for (String uri : knownBlockMappings.keySet()) {
            BlockUri blockUri = new BlockUri(uri);
            BlockUri familyUri = blockUri.getRootFamilyUri();


        }
    }

    @Override
    public void buildAtlas() {
        blockLoader.buildAtlas();
    }

    public void receiveFamilyRegistration(BlockUri familyUri, Map<String, Integer> registration) {
        BlockFamily family;
        if (isFreeformFamily(familyUri.getRootFamilyUri())) {
            family = blockLoader.loadWithShape(familyUri);
        } else {
            family = getAvailableBlockFamily(familyUri);
        }
        if (family != null) {
            for (Block block : family.getBlocks()) {
                Integer id = registration.get(block.getURI().toString());
                if (id != null) {
                    block.setId((byte)id.intValue());
                } else {
                    logger.error("Missing id for block {} in registered family {}", block.getURI(), familyUri);
                    block.setId(UNKNOWN_ID);
                }
                registerFamily(family);
            }
        } else {
            logger.error("Block family not available: {}", familyUri);
        }
    }

}
