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
package org.terasology.logic.actions;

import org.terasology.logic.common.ActivateEvent;
import org.terasology.logic.location.LocationComponent;
import org.terasology.world.block.entity.DroppedBlockFactory;
import org.terasology.entitySystem.systems.ComponentSystem;
import org.terasology.entitySystem.EntityManager;
import org.terasology.entitySystem.EntityRef;
import org.terasology.entitySystem.systems.In;
import org.terasology.entitySystem.event.ReceiveEvent;
import org.terasology.entitySystem.RegisterMode;
import org.terasology.entitySystem.systems.RegisterSystem;
import org.terasology.engine.CoreRegistry;
import org.terasology.math.Vector3i;
import org.terasology.physics.ImpulseEvent;
import org.terasology.utilities.procedural.FastRandom;
import org.terasology.world.BlockEntityRegistry;
import org.terasology.world.WorldProvider;
import org.terasology.world.block.Block;
import org.terasology.world.block.management.BlockManager;

import javax.vecmath.Vector3f;

/**
 * @author Immortius <immortius@gmail.com>
 */
@RegisterSystem(RegisterMode.AUTHORITY)
public class ExplosionAction implements ComponentSystem {

    @In
    private WorldProvider worldProvider;

    @In
    private BlockEntityRegistry blockEntityRegistry;

    private FastRandom random = new FastRandom();
    private DroppedBlockFactory droppedBlockFactory;

    @Override
    public void initialise() {
        droppedBlockFactory = new DroppedBlockFactory(CoreRegistry.get(EntityManager.class));
    }

    @Override
    public void shutdown() {
    }

    @ReceiveEvent(components = {ExplosionActionComponent.class})
    public void onActivate(ActivateEvent event, EntityRef entity) {
        ExplosionActionComponent explosionComp = entity.getComponent(ExplosionActionComponent.class);
        Vector3f origin = null;
        switch (explosionComp.relativeTo) {
            case Self:
                LocationComponent loc = entity.getComponent(LocationComponent.class);
                if (loc != null) {
                    origin = loc.getWorldPosition();
                }
                break;
            case Instigator:
                origin = event.getInstigatorLocation();
                break;
            default:
                origin = event.getTargetLocation();
                break;
        }

        if (origin == null) {
            return;
        }

        Vector3i blockPos = new Vector3i();
        for (int i = 0; i < 256; i++) {
            // TODO: Add a randomVector3f method to FastRandom?
            Vector3f direction = new Vector3f(random.randomFloat(), random.randomFloat(), random.randomFloat());
            direction.normalize();
            Vector3f impulse = new Vector3f(direction);
            impulse.scale(150);

            for (int j = 0; j < 4; j++) {
                Vector3f target = new Vector3f(origin);

                target.x += direction.x * j;
                target.y += direction.y * j;
                target.z += direction.z * j;
                blockPos.set((int) target.x, (int) target.y, (int) target.z);
                Block currentBlock = worldProvider.getBlock(blockPos);

                if (currentBlock.getId() == 0)
                    continue;

                /* PHYSICS */
                if (currentBlock.isDestructible()) {
                    // TODO: this should be handled centrally somewhere. Actions shouldn't be determining world behaviour
                    // like what happens when a block is destroyed.
                    worldProvider.setBlock(blockPos, BlockManager.getAir(), currentBlock);

                    EntityRef blockEntity = blockEntityRegistry.getEntityAt(blockPos);
                    blockEntity.destroy();
                    if (random.randomInt(4) == 0) {
                        EntityRef block = droppedBlockFactory.newInstance(target, currentBlock.getBlockFamily(), 5);
                        block.send(new ImpulseEvent(impulse));
                    }
                }
            }
        }
    }
}
