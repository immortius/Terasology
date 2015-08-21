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
import com.badlogic.gdx.physics.bullet.Bullet;
import com.badlogic.gdx.physics.bullet.collision.CollisionConstants;
import com.badlogic.gdx.physics.bullet.collision.LocalRayResult;
import com.badlogic.gdx.physics.bullet.collision.RayResultCallback;
import com.badlogic.gdx.physics.bullet.collision.btBoxShape;
import com.badlogic.gdx.physics.bullet.collision.btBroadphaseInterface;
import com.badlogic.gdx.physics.bullet.collision.btBroadphaseProxy;
import com.badlogic.gdx.physics.bullet.collision.btCapsuleShape;
import com.badlogic.gdx.physics.bullet.collision.btCollisionConfiguration;
import com.badlogic.gdx.physics.bullet.collision.btCollisionDispatcher;
import com.badlogic.gdx.physics.bullet.collision.btCollisionObject;
import com.badlogic.gdx.physics.bullet.collision.btConvexHullShape;
import com.badlogic.gdx.physics.bullet.collision.btConvexShape;
import com.badlogic.gdx.physics.bullet.collision.btCylinderShape;
import com.badlogic.gdx.physics.bullet.collision.btDbvtBroadphase;
import com.badlogic.gdx.physics.bullet.collision.btDefaultCollisionConfiguration;
import com.badlogic.gdx.physics.bullet.collision.btGhostObject;
import com.badlogic.gdx.physics.bullet.collision.btGhostPairCallback;
import com.badlogic.gdx.physics.bullet.collision.btPairCachingGhostObject;
import com.badlogic.gdx.physics.bullet.collision.btSphereShape;
import com.badlogic.gdx.physics.bullet.collision.btVoxelShape;
import com.badlogic.gdx.physics.bullet.dynamics.btDiscreteDynamicsWorld;
import com.badlogic.gdx.physics.bullet.dynamics.btRigidBody;
import com.badlogic.gdx.physics.bullet.dynamics.btSequentialImpulseConstraintSolver;
import com.badlogic.gdx.physics.bullet.linearmath.btDefaultMotionState;
import com.badlogic.gdx.physics.bullet.linearmath.btVector3;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terasology.entitySystem.entity.EntityManager;
import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.logic.characters.CharacterMovementComponent;
import org.terasology.logic.location.LocationComponent;
import org.terasology.math.AABB;
import org.terasology.math.VecMath;
import org.terasology.monitoring.PerformanceMonitor;
import org.terasology.physics.CollisionGroup;
import org.terasology.physics.HitResult;
import org.terasology.physics.StandardCollisionGroup;
import org.terasology.physics.components.RigidBodyComponent;
import org.terasology.physics.components.TriggerComponent;
import org.terasology.physics.engine.CharacterCollider;
import org.terasology.physics.engine.PhysicsEngine;
import org.terasology.physics.engine.PhysicsLiquidWrapper;
import org.terasology.physics.engine.PhysicsSystem;
import org.terasology.physics.engine.PhysicsWorldWrapper;
import org.terasology.physics.engine.RigidBody;
import org.terasology.physics.shapes.BoxShapeComponent;
import org.terasology.physics.shapes.CapsuleShapeComponent;
import org.terasology.physics.shapes.CylinderShapeComponent;
import org.terasology.physics.shapes.HullShapeComponent;
import org.terasology.physics.shapes.SphereShapeComponent;
import org.terasology.registry.CoreRegistry;
import org.terasology.world.BlockEntityRegistry;
import org.terasology.world.WorldProvider;

import javax.vecmath.Vector3f;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Physics engine implementation using TeraBullet (a customised version of JBullet)
 *
 * @author Benjamin Glatzel
 */
public class BulletPhysics implements PhysicsEngine {

    private static final Logger logger = LoggerFactory.getLogger(BulletPhysics.class);

    private final Deque<RigidBodyRequest> insertionQueue = Lists.newLinkedList();
    private final Deque<BulletRigidBody> removalQueue = Lists.newLinkedList();

    private final btCollisionDispatcher dispatcher;
    private final btBroadphaseInterface broadphase;
    private final btDiscreteDynamicsWorld discreteDynamicsWorld;
    private final BlockEntityRegistry blockEntityRegistry;
    private final PhysicsWorldWrapper wrapper;
    private final PhysicsLiquidWrapper liquidWrapper;
    private Map<EntityRef, BulletRigidBody> entityRigidBodies = Maps.newHashMap();
    private Map<EntityRef, BulletCharacterMoverCollider> entityColliders = Maps.newHashMap();
    private Map<EntityRef, btPairCachingGhostObject> entityTriggers = Maps.newHashMap();
    private List<PhysicsSystem.CollisionPair> collisions = new ArrayList<>();
    private EntityManager entityManager;

    public BulletPhysics(WorldProvider world, EntityManager entityManager) {

        this.entityManager = entityManager;
        broadphase = new btDbvtBroadphase();
        broadphase.getOverlappingPairCache().setInternalGhostPairCallback(new btGhostPairCallback());
        btCollisionConfiguration defaultCollisionConfiguration = new btDefaultCollisionConfiguration();
        dispatcher = new btCollisionDispatcher(defaultCollisionConfiguration);
        btSequentialImpulseConstraintSolver sequentialImpulseConstraintSolver = new btSequentialImpulseConstraintSolver();
        discreteDynamicsWorld = new btDiscreteDynamicsWorld(dispatcher, broadphase, sequentialImpulseConstraintSolver, defaultCollisionConfiguration);
        discreteDynamicsWorld.setGravity(new Vector3(0f, -15f, 0f));
        blockEntityRegistry = CoreRegistry.get(BlockEntityRegistry.class);

        wrapper = new PhysicsWorldWrapper(world);
        btVoxelShape worldShape = new btVoxelShape(wrapper, new Vector3(Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE),
                new Vector3(Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE));

        liquidWrapper = new PhysicsLiquidWrapper(world);
        btVoxelShape liquidShape = new btVoxelShape(liquidWrapper, new Vector3(Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE),
                new Vector3(Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE));

        btDefaultMotionState blockMotionState = new btDefaultMotionState(new Matrix4(new Vector3(0, 0, 0), new Quaternion(), new Vector3(1, 1, 1)));
        btRigidBody solidRb = new btRigidBody(0, blockMotionState, worldShape, new Vector3());
        BulletRigidBody rigidBody = new BulletRigidBody(solidRb);
        solidRb.setCollisionFlags(btCollisionObject.CollisionFlags.CF_STATIC_OBJECT | rigidBody.rb.getCollisionFlags());
        short mask = (short) (~(btBroadphaseProxy.CollisionFilterGroups.StaticFilter | StandardCollisionGroup.LIQUID.getFlag()));
        discreteDynamicsWorld.addRigidBody(rigidBody.rb, combineGroups(StandardCollisionGroup.WORLD), mask);

        btRigidBody liquidRb = new btRigidBody(0, blockMotionState, liquidShape, new Vector3());
        BulletRigidBody liquidBody = new BulletRigidBody(liquidRb);
        liquidBody.rb.setCollisionFlags(btCollisionObject.CollisionFlags.CF_STATIC_OBJECT | rigidBody.rb.getCollisionFlags());
        discreteDynamicsWorld.addRigidBody(liquidBody.rb, (short) combineGroups(StandardCollisionGroup.LIQUID),
                (short) btBroadphaseProxy.CollisionFilterGroups.SensorTrigger);
    }

    //*****************Physics Interface methods******************\\

    @Override
    public List<PhysicsSystem.CollisionPair> getCollisionPairs() {
        List<PhysicsSystem.CollisionPair> temp = collisions;
        collisions = new ArrayList<>();
        return temp;
    }

    @Override
    public void dispose() {
        discreteDynamicsWorld.dispose();
        wrapper.dispose();
        liquidWrapper.dispose();
    }

    @Override
    public short combineGroups(CollisionGroup... groups) {
        return combineGroups(Arrays.asList(groups));
    }

    @Override
    public short combineGroups(Iterable<CollisionGroup> groups) {
        short flags = 0;
        for (CollisionGroup group : groups) {
            flags |= group.getFlag();
        }
        return flags;
    }

    @Override
    public List<EntityRef> scanArea(AABB area, CollisionGroup... collisionFilter) {
        return scanArea(area, Arrays.asList(collisionFilter));
    }

    @Override
    public List<EntityRef> scanArea(AABB area, Iterable<CollisionGroup> collisionFilter) {
        // TODO: Add the aabbTest method from newer versions of bullet to TeraBullet, use that instead
        btBoxShape shape = new btBoxShape(new Vector3(area.getExtents().x, area.getExtents().y, area.getExtents().z));
        btGhostObject scanObject = createCollider(new Vector3(area.getCenter().x, area.getCenter().y, area.getCenter().z), shape,
                (short) btBroadphaseProxy.CollisionFilterGroups.SensorTrigger,
                combineGroups(collisionFilter), btCollisionObject.CollisionFlags.CF_NO_CONTACT_RESPONSE);
        // This in particular is overkill
        broadphase.calculateOverlappingPairs(dispatcher);
        List<EntityRef> result = Lists.newArrayList();
        for (int i = 0; i < scanObject.getNumOverlappingObjects(); ++i) {
            btCollisionObject other = scanObject.getOverlappingObject(i);
            Object userObj = other.getUserPointer();
            if (userObj instanceof EntityRef) {
                result.add((EntityRef) userObj);
            }
        }
        removeCollider(scanObject);
        return result;
    }

    @Override
    public HitResult rayTrace(org.terasology.math.geom.Vector3f from1, org.terasology.math.geom.Vector3f direction, float distance, CollisionGroup... collisionGroups) {
        Vector3 to = new Vector3(direction.x, direction.y, direction.z);
        Vector3 from = new Vector3(from1.x, from1.y, from1.z);
        to.scl(distance);
        to.add(from);

        short filter = combineGroups(collisionGroups);

        ClosestRayResultWithUserDataCallback closest = new ClosestRayResultWithUserDataCallback(from, to);
        closest.setCollisionFilterGroup((short) btBroadphaseProxy.CollisionFilterGroups.AllFilter);
        closest.setCollisionFilterMask(filter);

        discreteDynamicsWorld.rayTest(from, to, closest);
        if (closest.hasHit()) {
            // TODO: What to do about user data?
//            if (closest.userData instanceof Vector3i) { //We hit a world block
//                final EntityRef entityAt = blockEntityRegistry.getEntityAt((Vector3i) closest.userData);
//                return new HitResult(entityAt,
//                        VecMath.from(closest.hitPointWorld),
//                        VecMath.from(closest.hitNormalWorld),
//                        (Vector3i) closest.userData);
//            } else if (closest.userData instanceof EntityRef) { //we hit an other entity
//                return new HitResult((EntityRef) closest.userData,
//                        VecMath.from(closest.hitPointWorld),
//                        VecMath.from(closest.hitNormalWorld));
//            } else { //we hit something we don't understand, assume its nothing and log a warning
//                logger.warn("Unidentified object was hit in the physics engine: {}", closest.userData);
//                return new HitResult();
//            }
            return new HitResult();
        } else { //nothing was hit
            return new HitResult();
        }
    }

    @Override
    public void update(float delta) {
        processQueuedBodies();
        applyPendingImpulsesAndForces();
        try {
            PerformanceMonitor.startActivity("Step Simulation");
            if (discreteDynamicsWorld.stepSimulation(delta, 8) != 0) {
                for (BulletCharacterMoverCollider collider : entityColliders.values()) {
                    collider.pending = false;
                }
            }
            PerformanceMonitor.endActivity();
        } catch (Exception e) {
            logger.error("Error running simulation step.", e);
        }
        collisions.addAll(getNewCollisionPairs());
    }

    @Override
    public boolean removeRigidBody(EntityRef entity) {
        BulletRigidBody rigidBody = entityRigidBodies.remove(entity);
        if (rigidBody != null) {
            removeRigidBody(rigidBody);
            return true;
        } else {
            logger.warn("Deleting non existing rigidBody from physics engine?! Entity: {}", entity);
            return false;
        }
    }

    @Override
    public boolean updateRigidBody(EntityRef entity) {
        LocationComponent location = entity.getComponent(LocationComponent.class);
        RigidBodyComponent rb = entity.getComponent(RigidBodyComponent.class);
        BulletRigidBody rigidBody = entityRigidBodies.get(entity);

        if (location == null) {
            logger.warn("Updating rigid body of entity that has no "
                    + "LocationComponent?! Nothing is done, except log this"
                    + " warning instead. Entity: {}", entity);
            return false;
        } else if (rigidBody != null) {
            float scale = location.getWorldScale();
            if (Math.abs(rigidBody.rb.getCollisionShape().getLocalScaling().x - scale) > getEpsilon()
                    || rigidBody.collidesWith != combineGroups(rb.collidesWith)) {
                removeRigidBody(rigidBody);
                newRigidBody(entity);
            } else {
                if (!VecMath.to(rigidBody.rb.getAngularFactor()).equals(VecMath.to(rb.angularFactor))) {
                    rigidBody.rb.setAngularFactor(new Vector3(rb.angularFactor.x, rb.angularFactor.y, rb.angularFactor.z));
                }
                if (!VecMath.to(rigidBody.rb.getLinearFactor()).equals(VecMath.to(rb.linearFactor))) {
                    rigidBody.rb.setLinearFactor(new Vector3(rb.linearFactor.x, rb.linearFactor.y, rb.linearFactor.z));
                }
                rigidBody.rb.setFriction(rb.friction);
            }

            updateKinematicSettings(entity.getComponent(RigidBodyComponent.class), rigidBody);
            return true;
        } else {
            newRigidBody(entity);
            return false;
        }

        // TODO: update if mass or collision groups change
    }

    @Override
    public boolean hasRigidBody(EntityRef entity) {
        return entityRigidBodies.containsKey(entity);
    }

    @Override
    public RigidBody getRigidBody(EntityRef entity) {
        RigidBody rb = entityRigidBodies.get(entity);
        if (rb == null) {
            rb = newRigidBody(entity);
        }
        return rb;
    }

    @Override
    public boolean removeTrigger(EntityRef entity) {
        btGhostObject ghost = entityTriggers.remove(entity);
        if (ghost != null) {
            removeCollider(ghost);
            return true;
        } else {
            return false;
        }
    }

    @Override
    //TODO: update if detectGroups changed
    public boolean updateTrigger(EntityRef entity) {
        LocationComponent location = entity.getComponent(LocationComponent.class);
        btPairCachingGhostObject triggerObj = entityTriggers.get(entity);

        if (location == null) {
            logger.warn("Trying to update or create trigger of entity that has no LocationComponent?! Entity: {}", entity);
            return false;
        }
        if (triggerObj != null) {
            float scale = location.getWorldScale();
            float existingScale = triggerObj.getCollisionShape().getLocalScaling().x;
            if ((existingScale - scale) > getEpsilon()) {
                discreteDynamicsWorld.removeCollisionObject(triggerObj);
                newTrigger(entity);
            } else {
                org.terasology.math.geom.Quat4f worldRotation = location.getWorldRotation();
                org.terasology.math.geom.Vector3f worldPosition = location.getWorldPosition();
                triggerObj.setWorldTransform(new Matrix4(new Vector3(worldPosition.x, worldPosition.y, worldPosition.z),
                        new Quaternion(worldRotation.x, worldRotation.y, worldRotation.z, worldRotation.w), new Vector3(1, 1, 1)));
            }
            return true;
        } else {
            newTrigger(entity);
            return false;
        }
    }

    @Override
    public boolean hasTrigger(EntityRef entity) {
        return entityTriggers.containsKey(entity);
    }

    @Override
    public boolean removeCharacterCollider(EntityRef entity) {
        BulletCharacterMoverCollider toRemove = entityColliders.remove(entity);
        if (toRemove == null) {
            logger.warn("Trying to remove CharacterCollider of entity that has "
                    + "no CharacterCollider in the physics engine. Entity: {}", entity);
            return false;
        } else {
            removeCollider(toRemove.collider);
            return true;
        }
    }

    @Override
    public CharacterCollider getCharacterCollider(EntityRef entity) {
        CharacterCollider cc = entityColliders.get(entity);
        if (cc == null) {
            cc = createCharacterCollider(entity);
        }
        return cc;
    }

    @Override
    public boolean hasCharacterCollider(EntityRef entity) {
        return entityColliders.containsKey(entity);
    }

    @Override
    public Set<EntityRef> getPhysicsEntities() {
        return ImmutableSet.copyOf(entityRigidBodies.keySet());
    }

    @Override
    public Iterator<EntityRef> physicsEntitiesIterator() {
        return entityRigidBodies.keySet().iterator();
    }

    @Override
    public void awakenArea(org.terasology.math.geom.Vector3f pos, float radius) {
        Vector3f min = new Vector3f(VecMath.to(pos));
        min.sub(new Vector3f(0.6f, 0.6f, 0.6f));
        Vector3f max = new Vector3f(VecMath.to(pos));
        max.add(new Vector3f(0.6f, 0.6f, 0.6f));
        //TODO discreteDynamicsWorld.awakenRigidBodiesInArea(min, max);
    }

    @Override
    public float getEpsilon() {
        return 1.1920929E-7F;
    }

    //*******************Private helper methods**************************\\

    /**
     * Creates a new trigger.
     *
     * @param entity the entity to create a trigger for.
     */
    private boolean newTrigger(EntityRef entity) {
        LocationComponent location = entity.getComponent(LocationComponent.class);
        TriggerComponent trigger = entity.getComponent(TriggerComponent.class);
        btConvexShape shape = getShapeFor(entity);
        if (shape != null && location != null && trigger != null) {
            float scale = location.getWorldScale();
            shape.setLocalScaling(new Vector3(scale, scale, scale));
            List<CollisionGroup> detectGroups = Lists.newArrayList(trigger.detectGroups);
            org.terasology.math.geom.Vector3f worldPosition = location.getWorldPosition();
            btPairCachingGhostObject triggerObj = createCollider(
                    new Vector3(worldPosition.x, worldPosition.y, worldPosition.z),
                    shape,
                    StandardCollisionGroup.SENSOR.getFlag(),
                    combineGroups(detectGroups),
                    btCollisionObject.CollisionFlags.CF_NO_CONTACT_RESPONSE);
            triggerObj.setUserPointer(entity.getId());
            btPairCachingGhostObject oldTrigger = entityTriggers.put(entity, triggerObj);
            if (oldTrigger != null) {
                logger.warn("Creating a trigger for an entity that already has a trigger. " +
                        "Multiple trigger pre entity are not supported. Removing old one. Entity: {}", entity);
                removeCollider(oldTrigger);
                return false;
            } else {
                return true;
            }
        } else {
            logger.warn("Trying to create trigger for entity without ShapeComponent or without LocationComponent or without TriggerComponent. Entity: {}", entity);
            return false;
        }
    }

    /**
     * Creates a Collider for the given entity based on the LocationComponent
     * and CharacterMovementComponent.
     * All collision flags are set right for a character movement component.
     *
     * @param owner the entity to create the collider for.
     * @return
     */
    private CharacterCollider createCharacterCollider(EntityRef owner) {
        LocationComponent locComp = owner.getComponent(LocationComponent.class);
        CharacterMovementComponent movementComp = owner.getComponent(CharacterMovementComponent.class);
        if (locComp == null || movementComp == null) {
            throw new IllegalArgumentException("Expected an entity with a Location component and CharacterMovementComponent.");
        }
        Vector3f pos = VecMath.to(locComp.getWorldPosition());
        final float worldScale = locComp.getWorldScale();
        final float height = (movementComp.height - 2 * movementComp.radius) * worldScale;
        final float width = movementComp.radius * worldScale;
        btConvexShape shape = new btCapsuleShape(width, height);
        shape.setMargin(0.1f);
        return createCustomCollider(pos, shape, movementComp.collisionGroup.getFlag(), combineGroups(movementComp.collidesWith),
                btCollisionObject.CollisionFlags.CF_CHARACTER_OBJECT, owner);
    }

    private RigidBody newRigidBody(EntityRef entity) {
        LocationComponent location = entity.getComponent(LocationComponent.class);
        RigidBodyComponent rigidBody = entity.getComponent(RigidBodyComponent.class);
        btConvexShape shape = getShapeFor(entity);
        if (location != null && rigidBody != null && shape != null) {
            float scale = location.getWorldScale();
            shape.setLocalScaling(new Vector3(scale, scale, scale));

            if (rigidBody.mass < 1) {
                logger.warn("RigidBodyComponent.mass is set to less than 1.0, this can lead to strange behaviour, such as the objects moving through walls. " +
                        "Entity: {}", entity);
            }
            Vector3 fallInertia = new Vector3();
            shape.calculateLocalInertia(rigidBody.mass, fallInertia);

            btRigidBody rb = new btRigidBody(rigidBody.mass, new EntityMotionState(entity), shape, fallInertia);
            rb.setUserPointer(entity.getId());
            rb.setAngularFactor(new Vector3(rigidBody.angularFactor.x, rigidBody.angularFactor.y, rigidBody.angularFactor.z));
            rb.setLinearFactor(new Vector3(rigidBody.linearFactor.x, rigidBody.linearFactor.y, rigidBody.linearFactor.z));
            rb.setFriction(rigidBody.friction);
            rb.setRollingFriction(rigidBody.friction);
            rb.setCollisionFlags(combineGroups(rigidBody.collidesWith));

            BulletRigidBody collider = new BulletRigidBody(rb);
            collider.rb.setFriction(rigidBody.friction);
            collider.collidesWith = combineGroups(rigidBody.collidesWith);
            updateKinematicSettings(rigidBody, collider);
            BulletRigidBody oldBody = entityRigidBodies.put(entity, collider);
            addRigidBody(collider, Lists.<CollisionGroup>newArrayList(rigidBody.collisionGroup), rigidBody.collidesWith);
            if (oldBody != null) {
                removeRigidBody(oldBody);
            }
            collider.setVelocity(rigidBody.velocity, rigidBody.angularVelocity);
            collider.setTransform(location.getWorldPosition(), location.getWorldRotation());
            return collider;
        } else {
            throw new IllegalArgumentException("Can only create a new rigid body for entities with a LocationComponent," +
                    " RigidBodyComponent and ShapeComponent, this entity misses at least one: " + entity);
        }
    }

    private void removeCollider(btCollisionObject collider) {
        discreteDynamicsWorld.removeCollisionObject(collider);
    }

    /**
     * Creates a new Collider. Colliders are similar to rigid bodies, except
     * that they do not respond to forces from the physics engine. They collide
     * with other objects and other objects may move if colliding with a
     * collider, but the collider itself will only respond to movement orders
     * from outside the physics engine. Colliders also detect any objects
     * colliding with them. Allowing them to be used as sensors.
     *
     * @param pos            The initial position of the collider.
     * @param shape          The shape of this collider.
     * @param groups
     * @param filters
     * @param collisionFlags
     * @param entity         The entity to associate this collider with. Can be null.
     * @return The newly created and added to the physics engine, Collider object.
     */
    private CharacterCollider createCustomCollider(Vector3f pos, btConvexShape shape, short groups, short filters, int collisionFlags, EntityRef entity) {
        if (entityColliders.containsKey(entity)) {
            entityColliders.remove(entity);
        }
        final BulletCharacterMoverCollider bulletCollider = new BulletCharacterMoverCollider(new Vector3(pos.x, pos.y, pos.z), shape, groups, filters, collisionFlags, entity);
        entityColliders.put(entity, bulletCollider);
        return bulletCollider;
    }

    /**
     * To make sure the state of the physics engine is constant, all changes are
     * stored and executed at the same time. This method executes the stored
     * additions and removals of bodies to and from the physics engine. It also
     * ensures that impulses requested before the body is added to the engine
     * are applied after the body is added to the engine.
     */
    // TODO: None of the above is true.
    // TODO: This isn't necessary, create and remove bodies immediately
    private synchronized void processQueuedBodies() {
        while (!insertionQueue.isEmpty()) {
            RigidBodyRequest request = insertionQueue.poll();
            discreteDynamicsWorld.addRigidBody(request.body.rb, request.groups, request.filter);
        }
        while (!removalQueue.isEmpty()) {
            BulletRigidBody body = removalQueue.poll();
            discreteDynamicsWorld.removeRigidBody(body.rb);
        }
    }

    /**
     * Applies all pending impulses to the corresponding rigidBodies and clears
     * the pending impulses.
     */
    private void applyPendingImpulsesAndForces() {
        for (Map.Entry<EntityRef, BulletRigidBody> entree : entityRigidBodies.entrySet()) {
            BulletRigidBody body = entree.getValue();
            body.rb.applyCentralImpulse(new Vector3(body.pendingImpulse.x, body.pendingImpulse.y, body.pendingImpulse.z));
            body.rb.applyCentralForce(new Vector3(body.pendingForce.x, body.pendingForce.y, body.pendingForce.z));
            body.pendingImpulse.x = 0;
            body.pendingImpulse.y = 0;
            body.pendingImpulse.z = 0;

            body.pendingForce.x = 0;
            body.pendingForce.y = 0;
            body.pendingForce.z = 0;
        }
    }

    private void addRigidBody(BulletRigidBody body) {
        short filter = (short) (btBroadphaseProxy.CollisionFilterGroups.DefaultFilter | btBroadphaseProxy.CollisionFilterGroups.StaticFilter
                | btBroadphaseProxy.CollisionFilterGroups.SensorTrigger);
        insertionQueue.add(new RigidBodyRequest(body, (short) btBroadphaseProxy.CollisionFilterGroups.DefaultFilter, filter));
    }

    private void addRigidBody(BulletRigidBody body, List<CollisionGroup> groups, List<CollisionGroup> filter) {
        insertionQueue.add(new RigidBodyRequest(body, combineGroups(groups), combineGroups(filter)));
    }

    private void addRigidBody(BulletRigidBody body, short groups, short filter) {
        insertionQueue.add(new RigidBodyRequest(body, groups, (short) (filter | btBroadphaseProxy.CollisionFilterGroups.SensorTrigger)));
    }

    private void removeRigidBody(BulletRigidBody body) {
        removalQueue.add(body);
    }

    /**
     * Returns the shape belonging to the given entity. It currently knows 4
     * different shapes: Sphere, Capsule, Cylinder or arbitrary.
     * The shape is determined based on the shape component of the given entity.
     * If the entity has somehow got multiple shapes, only one is picked. The
     * order of priority is: Sphere, Capsule, Cylinder, arbitrary.
     * <br><br>
     * TODO: Flyweight this (take scale as parameter)
     *
     * @param entity the entity to get the shape of.
     * @return the shape of the entity, ready to be used by Bullet.
     */
    private btConvexShape getShapeFor(EntityRef entity) {
        BoxShapeComponent box = entity.getComponent(BoxShapeComponent.class);
        if (box != null) {
            Vector3 halfExtents = new Vector3(box.extents.x, box.extents.y, box.extents.z);
            halfExtents.scl(0.5f);
            return new btBoxShape(halfExtents);
        }
        SphereShapeComponent sphere = entity.getComponent(SphereShapeComponent.class);
        if (sphere != null) {
            return new btSphereShape(sphere.radius);
        }
        CapsuleShapeComponent capsule = entity.getComponent(CapsuleShapeComponent.class);
        if (capsule != null) {
            return new btCapsuleShape(capsule.radius, capsule.height);
        }
        CylinderShapeComponent cylinder = entity.getComponent(CylinderShapeComponent.class);
        if (cylinder != null) {
            return new btCylinderShape(new Vector3(cylinder.radius, 0.5f * cylinder.height, cylinder.radius));
        }
        HullShapeComponent hull = entity.getComponent(HullShapeComponent.class);
        if (hull != null) {
            return new btConvexHullShape(FloatBuffer.wrap(hull.sourceMesh.getVertices().toArray()));
        }
        CharacterMovementComponent characterMovementComponent = entity.getComponent(CharacterMovementComponent.class);
        if (characterMovementComponent != null) {
            return new btCapsuleShape(characterMovementComponent.radius, characterMovementComponent.height);
        }
        logger.error("Creating physics object that requires a ShapeComponent or CharacterMovementComponent, but has neither. Entity: {}", entity);
        throw new IllegalArgumentException("Creating physics object that requires a ShapeComponent or CharacterMovementComponent, but has neither. Entity: " + entity);
    }

    private void updateKinematicSettings(RigidBodyComponent rigidBody, BulletRigidBody collider) {
        if (rigidBody.kinematic) {
            collider.rb.setCollisionFlags(collider.rb.getCollisionFlags() | btCollisionObject.CollisionFlags.CF_KINEMATIC_OBJECT);
            collider.rb.setActivationState(CollisionConstants.DISABLE_DEACTIVATION);
        } else {
            collider.rb.setCollisionFlags(collider.rb.getCollisionFlags() & ~btCollisionObject.CollisionFlags.CF_KINEMATIC_OBJECT);
            collider.rb.setActivationState(CollisionConstants.ACTIVE_TAG);
        }
    }

    private btPairCachingGhostObject createCollider(Vector3 pos, btConvexShape shape, short groups, short filters, int collisionFlags) {
        Matrix4 startTransform = new Matrix4(pos, new Quaternion(), new Vector3(1, 1, 1));
        btPairCachingGhostObject result = new btPairCachingGhostObject();
        result.setWorldTransform(startTransform);
        result.setCollisionShape(shape);
        result.setCollisionFlags(collisionFlags);
        discreteDynamicsWorld.addCollisionObject(result, groups, filters);
        return result;
    }

    private Collection<? extends PhysicsSystem.CollisionPair> getNewCollisionPairs() {
        List<PhysicsSystem.CollisionPair> collisionPairs = Lists.newArrayList();

        // TODO: Need a new solution for triggers
//        btDynamicsWorld world = discreteDynamicsWorld;
//        //ObjectArrayList<PersistentManifold> manifolds = new ObjectArrayList<PersistentManifold>();
//        for (btPairCachingGhostObject trigger : entityTriggers.values()) {
//            EntityRef entity = entityManager.getEntity(trigger.getUserPointer());
//            for (btBroadphasePair initialPair : trigger.getOverlappingPairCache().getOverlappingPairArray()) {
//                EntityRef otherEntity = null;
//                initialPair.get
//                if (initialPair.getPProxy0().getClientObject() == trigger.getCPointer()) {
//                    if (((btCollisionObject) initialPair.getPProxy0().getClientObject()).getUserPointer() instanceof EntityRef) {
//                        otherEntity = (EntityRef) ((CollisionObject) initialPair.pProxy1.clientObject).getUserPointer();
//                    }
//                } else {
//                    if (((CollisionObject) initialPair.pProxy0.clientObject).getUserPointer() instanceof EntityRef) {
//                        otherEntity = (EntityRef) ((CollisionObject) initialPair.pProxy0.clientObject).getUserPointer();
//                    }
//                }
//                if (otherEntity == null) {
//                    continue;
//                }
//                BroadphasePair pair = world.getPairCache().findPair(initialPair.pProxy0, initialPair.pProxy1);
//                if (pair == null) {
//                    continue;
//                }
//                manifolds.clear();
//                if (pair.algorithm != null) {
//                    pair.algorithm.getAllContactManifolds(manifolds);
//                }
//                for (PersistentManifold manifold : manifolds) {
//                    for (int point = 0; point < manifold.getNumContacts(); ++point) {
//                        ManifoldPoint manifoldPoint = manifold.getContactPoint(point);
//                        if (manifoldPoint.getDistance() < 0) {
//                            collisionPairs.add(new PhysicsSystem.CollisionPair(entity, otherEntity));
//                            break;
//                        }
//                    }
//                }
//            }
//        }
        return collisionPairs;
    }

    //********************Private helper classes*********************\\

    private static class ClosestRayResultWithUserDataCallback extends RayResultCallback {
        public final Vector3 rayFromWorld;
        public final Vector3 rayToWorld;
        public Vector3 hitNormalWorld;
        public long userData;

        public ClosestRayResultWithUserDataCallback(Vector3 rayFromWorld, Vector3 rayToWorld) {
            this.rayFromWorld = new Vector3(rayFromWorld);
            this.rayToWorld = new Vector3(rayToWorld);
        }

        @Override
        public float addSingleResult(LocalRayResult rayResult, boolean normalInWorldSpace) {
            this.userData = this.getCollisionObject().getUserPointer();
            if (normalInWorldSpace) {
                btVector3 hitNormalLocal = rayResult.getHitNormalLocal();
                this.hitNormalWorld = new Vector3(hitNormalLocal.x(), hitNormalLocal.y(), hitNormalLocal.z());
            } else {
                btVector3 hitNormalLocal = rayResult.getHitNormalLocal();
                this.hitNormalWorld = new Vector3(hitNormalLocal.x(), hitNormalLocal.y(), hitNormalLocal.z());
                hitNormalWorld.traMul(this.getCollisionObject().getWorldTransform());
            }
            return rayResult.getHitFraction();
        }
    }

    private static class RigidBodyRequest {
        final BulletRigidBody body;
        final short groups;
        final short filter;

        public RigidBodyRequest(BulletRigidBody body, short groups, short filter) {
            this.body = body;
            this.groups = groups;
            this.filter = filter;
        }
    }

    private static class BulletRigidBody implements RigidBody {

        public final btRigidBody rb;
        public short collidesWith;
        private final Vector3f pendingImpulse = new Vector3f();
        private final Vector3f pendingForce = new Vector3f();

        BulletRigidBody(btRigidBody rb) {
            this.rb = rb;
        }

        @Override
        public void applyImpulse(org.terasology.math.geom.Vector3f impulse) {
            pendingImpulse.add(VecMath.to(impulse));
        }

        @Override
        public void applyForce(org.terasology.math.geom.Vector3f force) {
            pendingForce.add(VecMath.to(force));
        }

        @Override
        public void translate(org.terasology.math.geom.Vector3f translation) {
            rb.translate(new Vector3(translation.x, translation.y, translation.z));
        }

        @Override
        public org.terasology.math.geom.Quat4f getOrientation(org.terasology.math.geom.Quat4f out) {
            Quaternion vm = rb.getOrientation();
            out.set(vm.x, vm.y, vm.z, vm.w);
            return out;
        }

        @Override
        public org.terasology.math.geom.Vector3f getLocation(org.terasology.math.geom.Vector3f out) {
            Vector3 vm = rb.getCenterOfMassPosition();
            out.set(vm.x, vm.y, vm.z);
            return out;
        }

        @Override
        public org.terasology.math.geom.Vector3f getLinearVelocity(org.terasology.math.geom.Vector3f out) {
            Vector3 vm = rb.getLinearVelocity();
            out.set(vm.x, vm.y, vm.z);
            return out;
        }

        @Override
        public org.terasology.math.geom.Vector3f getAngularVelocity(org.terasology.math.geom.Vector3f out) {
            Vector3 vm = rb.getAngularVelocity();
            out.set(vm.x, vm.y, vm.z);
            return out;
        }

        @Override
        public void setLinearVelocity(org.terasology.math.geom.Vector3f value) {
            rb.setLinearVelocity(new Vector3(value.x, value.y, value.z));
        }

        @Override
        public void setAngularVelocity(org.terasology.math.geom.Vector3f value) {
            rb.setAngularVelocity(new Vector3(value.x, value.y, value.z));
        }

        @Override
        public void setOrientation(org.terasology.math.geom.Quat4f orientation) {
            Matrix4 transform = rb.getWorldTransform();
            Vector3 translation = transform.getTranslation(new Vector3());
            transform.set(translation, new Quaternion(orientation.x, orientation.y, orientation.z, orientation.w));
            rb.proceedToTransform(transform);
        }

        @Override
        public void setLocation(org.terasology.math.geom.Vector3f location) {
            Matrix4 transform = rb.getWorldTransform();
            transform.setTranslation(location.x, location.y, location.z);
            rb.proceedToTransform(transform);
        }

        @Override
        public void setVelocity(org.terasology.math.geom.Vector3f linear, org.terasology.math.geom.Vector3f angular) {
            rb.setLinearVelocity(new Vector3(linear.x, linear.y, linear.z));
            rb.setAngularVelocity(new Vector3(angular.x, angular.y, angular.z));
        }

        @Override
        public void setTransform(org.terasology.math.geom.Vector3f location, org.terasology.math.geom.Quat4f orientation) {
            Matrix4 newTranform = new Matrix4(new Vector3(location.x, location.y, location.z),
                    new Quaternion(orientation.x, orientation.y, orientation.z, orientation.w), new Vector3(1, 1, 1));
            rb.proceedToTransform(newTranform);
        }

        @Override
        public boolean isActive() {
            return rb.isActive();
        }
    }

    private final class BulletCharacterMoverCollider implements CharacterCollider {

        boolean pending = true;

        private final Matrix4 temp = new Matrix4();

        //If a class can figure out that its Collider is a BulletCollider, it
        //is allowed to gain direct access to the bullet body:
        private final btPairCachingGhostObject collider;

        private BulletCharacterMoverCollider(Vector3 pos, btConvexShape shape, List<CollisionGroup> groups, List<CollisionGroup> filters, EntityRef owner) {
            this(pos, shape, groups, filters, 0, owner);
        }

        private BulletCharacterMoverCollider(Vector3 pos, btConvexShape shape, List<CollisionGroup> groups, List<CollisionGroup> filters, int collisionFlags, EntityRef owner) {
            this(pos, shape, combineGroups(groups), combineGroups(filters), collisionFlags, owner);
        }

        private BulletCharacterMoverCollider(Vector3 pos, btConvexShape shape, short groups, short filters, int collisionFlags, EntityRef owner) {
            collider = createCollider(pos, shape, groups, filters, collisionFlags);
            collider.setUserPointer(owner.getId());
        }

        @Override
        public boolean isPending() {
            return pending;
        }

        @Override
        public org.terasology.math.geom.Vector3f getLocation() {
            collider.getWorldTransform(temp);
            Vector3 pos = temp.getTranslation(new Vector3());

            return new org.terasology.math.geom.Vector3f(pos.x, pos.y, pos.z);
        }

        @Override
        public void setLocation(org.terasology.math.geom.Vector3f loc) {
            collider.getWorldTransform(temp);
            temp.setTranslation(loc.x, loc.y, loc.z);
            collider.setWorldTransform(temp);
        }

        @Override
        public BulletSweepCallback sweep(org.terasology.math.geom.Vector3f startPos, org.terasology.math.geom.Vector3f endPos, float allowedPenetration, float slopeFactor) {
            Matrix4 startTransform = new Matrix4(new Vector3(startPos.x, startPos.y, startPos.z), new Quaternion(), new Vector3(1, 1, 1));
            Matrix4 endTransform = new Matrix4(new Vector3(endPos.x, endPos.y, endPos.z), new Quaternion(), new Vector3(1, 1, 1));
            BulletSweepCallback callback = new BulletSweepCallback(collider, new org.terasology.math.geom.Vector3f(0, 1, 0), slopeFactor);
            callback.setCollisionFilterGroup(collider.getBroadphaseHandle().getCollisionFilterGroup());
            callback.setCollisionFilterMask(collider.getBroadphaseHandle().getCollisionFilterMask());
            collider.convexSweepTest((btConvexShape) (collider.getCollisionShape()), startTransform, endTransform, callback, allowedPenetration);
            return callback;
        }
    }
}
