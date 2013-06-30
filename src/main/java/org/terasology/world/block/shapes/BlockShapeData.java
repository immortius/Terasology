package org.terasology.world.block.shapes;

import com.bulletphysics.collision.shapes.CollisionShape;
import com.google.common.collect.Maps;
import org.terasology.asset.AssetData;
import org.terasology.math.Side;
import org.terasology.utilities.collection.EnumBooleanMap;
import org.terasology.world.block.BlockPart;

import javax.vecmath.Vector3f;
import java.util.EnumMap;

/**
 * @author Immortius
 */
public class BlockShapeData implements AssetData {
    private EnumMap<BlockPart, BlockMeshPart> meshParts = Maps.newEnumMap(BlockPart.class);
    private EnumBooleanMap<Side> fullSide = new EnumBooleanMap<>(Side.class);
    private CollisionShape collisionShape;
    private Vector3f collisionOffset = new Vector3f();
    private boolean yawSymmetric = false;
    private boolean pitchSymmetric = false;
    private boolean rollSymmetric = false;

    public BlockMeshPart getMeshPart(BlockPart part) {
        return meshParts.get(part);
    }

    /**
     * Sets the mesh to use for the given block part
     *
     * @param part
     * @param mesh
     */
    public void setMeshPart(BlockPart part, BlockMeshPart mesh) {
        meshParts.put(part, mesh);
    }

    public boolean isBlockingSide(Side side) {
        return fullSide.get(side);
    }

    /**
     * Sets whether the given side blocks the view of adjacent tiles (that is, it fills the side)
     *
     * @param side
     * @param blocking
     */
    public void setBlockingSide(Side side, boolean blocking) {
        fullSide.put(side, blocking);
    }

    public Vector3f getCollisionOffset() {
        return collisionOffset;
    }

    public void setCollisionOffset(Vector3f offset) {
        collisionOffset.set(offset);
    }

    public CollisionShape getCollisionShape() {
        return collisionShape;
    }

    public void setCollisionShape(CollisionShape shape) {
        collisionShape = shape;
    }

    public void setCollisionSymmetric(boolean collisionSymmetric) {
        yawSymmetric = collisionSymmetric;
        pitchSymmetric = collisionSymmetric;
        rollSymmetric = collisionSymmetric;
    }

    public boolean isRollSymmetric() {
        return rollSymmetric;
    }

    public boolean isPitchSymmetric() {
        return pitchSymmetric;
    }

    public boolean isYawSymmetric() {
        return yawSymmetric;
    }

    public void setYawSymmetric(boolean yawSymmetric) {
        this.yawSymmetric = yawSymmetric;
    }

    public void setPitchSymmetric(boolean pitchSymmetric) {
        this.pitchSymmetric = pitchSymmetric;
    }

    public void setRollSymmetric(boolean rollSymmetric) {
        this.rollSymmetric = rollSymmetric;
    }
}
