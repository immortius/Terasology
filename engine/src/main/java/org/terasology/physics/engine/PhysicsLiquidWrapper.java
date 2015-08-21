/*
 * Copyright 2014 MovingBlocks
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
package org.terasology.physics.engine;

import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.bullet.collision.btVoxelContentProvider;
import com.badlogic.gdx.physics.bullet.collision.btVoxelInfo;
import org.terasology.world.WorldProvider;
import org.terasology.world.block.Block;

/**
 * @author Immortius
 */
public class PhysicsLiquidWrapper extends btVoxelContentProvider {
    private WorldProvider world;

    public PhysicsLiquidWrapper(WorldProvider world) {
        this.world = world;
    }

    @Override
    public btVoxelInfo getVoxel(int x, int y, int z) {
        Block block = world.getBlock(x, y, z);
        Vector3 offset = new Vector3(block.getCollisionOffset().x(), block.getCollisionOffset().y(), block.getCollisionOffset().z());
        return new btVoxelInfo(block.getCollisionShape() != null && block.isLiquid(), false, block.getId(), 0, block.getCollisionShape(), offset, 0.5f, 0.5f, 0.5f);
    }

    public void dispose() {
        world = null;
    }

}
