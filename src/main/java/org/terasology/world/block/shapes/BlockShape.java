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
package org.terasology.world.block.shapes;

import com.bulletphysics.collision.shapes.BoxShape;
import com.bulletphysics.collision.shapes.CollisionShape;
import com.bulletphysics.collision.shapes.CompoundShape;
import com.bulletphysics.collision.shapes.CompoundShapeChild;
import com.bulletphysics.collision.shapes.ConvexHullShape;
import com.bulletphysics.linearmath.QuaternionUtil;
import com.bulletphysics.linearmath.Transform;
import com.bulletphysics.util.ObjectArrayList;
import com.google.common.collect.Maps;
import org.terasology.asset.Asset;
import org.terasology.asset.AssetUri;
import org.terasology.math.Pitch;
import org.terasology.math.Roll;
import org.terasology.math.Rotation;
import org.terasology.math.Side;
import org.terasology.math.Yaw;
import org.terasology.utilities.collection.EnumBooleanMap;
import org.terasology.world.block.BlockPart;

import javax.vecmath.Matrix4f;
import javax.vecmath.Quat4f;
import javax.vecmath.Vector3f;
import java.util.EnumMap;
import java.util.Map;

/**
 * Describes a shape that a block can take. The shape may also be rotated if not symmetrical.
 *
 * @author Immortius <immortius@gmail.com>
 */
public interface BlockShape extends Asset<BlockShapeData> {

    /**
     * @param part
     * @return The mesh part for the given part of the block, or null if it has none
     */
    BlockMeshPart getMeshPart(BlockPart part);

    /**
     * @param side
     * @return Whether the given side blocks
     */
    boolean isBlockingSide(Side side);

    /**
     *
     * @param rot
     * @return The collision shape for the given rotation
     */
    CollisionShape getCollisionShape(Rotation rot);

    /**
     * @param rot
     * @return The collision offset for the given rotation
     */
    Vector3f getCollisionOffset(Rotation rot);


    /**
     * @return Is this block shape's collision symmetric when altering yaw.
     */
    boolean isCollisionYawSymmetric();
}
