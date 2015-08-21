/*
 * Copyright 2013 MovingBlocks
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

package org.terasology.physics.bullet;

import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.bullet.linearmath.btMotionState;
import com.bulletphysics.linearmath.Transform;

import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.logic.location.LocationComponent;
import org.terasology.math.VecMath;
import org.terasology.math.geom.Matrix4f;
import org.terasology.math.geom.Quat4f;
import org.terasology.math.geom.Vector3f;

/**
 * This motion state is used to connect rigid body entities to their rigid body in the bullet physics engine.
 * Bullet reads the initial state of the rigid body out of the entity, and then updates its location and rotation
 * as it moves under physics.
 *
 * @author Immortius
 */
public class EntityMotionState extends btMotionState {
    private EntityRef entity;

    /**
     * Only the BulletPhysics class is expected to create instances.
     *
     * @param entity The entity to relate this motion state to and set the
     *               LocationComponent of.
     */
    EntityMotionState(EntityRef entity) {
        this.entity = entity;
    }

    @Override
    public void getWorldTransform(Matrix4 worldTrans) {
        LocationComponent loc = entity.getComponent(LocationComponent.class);
        if (loc != null) {
            Quat4f worldRotation = loc.getWorldRotation();
            Vector3f worldPosition = loc.getWorldPosition();
            worldTrans.set(worldPosition.x, worldPosition.y, worldPosition.z, worldRotation.x, worldRotation.y, worldRotation.z, worldRotation.w, 1, 1, 1);
        }
    }

    @Override
    public void setWorldTransform(Matrix4 worldTrans) {
        LocationComponent loc = entity.getComponent(LocationComponent.class);
        if (loc != null) {
            javax.vecmath.Vector3f translation = VecMath.to(worldTrans.getTranslation(new Vector3()));
            javax.vecmath.Quat4f rotation = VecMath.to(worldTrans.getRotation(new Quaternion()));

            if (!translation.equals(VecMath.to(loc.getWorldPosition())) || !rotation.equals(VecMath.to(loc.getWorldRotation()))) {
                loc.setWorldPosition(VecMath.from(translation));
                loc.setWorldRotation(VecMath.from(rotation));
                entity.saveComponent(loc);
            }
        }
    }

}
