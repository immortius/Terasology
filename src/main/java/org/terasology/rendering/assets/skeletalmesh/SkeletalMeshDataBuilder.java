package org.terasology.rendering.assets.skeletalmesh;

import com.bulletphysics.linearmath.QuaternionUtil;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;

import javax.vecmath.Quat4f;
import javax.vecmath.Vector2f;
import javax.vecmath.Vector3f;
import java.util.List;
import java.util.Map;

/**
 * @author Immortius
 */
public class SkeletalMeshDataBuilder {

    private List<Bone> bones = Lists.newArrayList();
    private List<BoneWeight> weights = Lists.newArrayList();
    private List<Vector2f> uvs = Lists.newArrayList();
    private TIntList vertexStartWeights = new TIntArrayList();
    private TIntList vertexWeightCounts = new TIntArrayList();
    private TIntList indices;

    public SkeletalMeshDataBuilder() {

    }

    public SkeletalMeshDataBuilder addBone(Bone bone) {
        bones.add(bone);
        return this;
    }

    public SkeletalMeshDataBuilder addWeight(BoneWeight boneWeight) {
        weights.add(boneWeight);
        return this;
    }

    public void setVertexWeights(TIntList vertexStartWeight, TIntList vertexWeightCount) {
        this.vertexStartWeights.clear();
        this.vertexStartWeights.addAll(vertexStartWeight);
        this.vertexWeightCounts.clear();
        this.vertexWeightCounts.addAll(vertexWeightCount);
    }

    public void setUvs(List<Vector2f> uvs) {
        this.uvs.clear();
        this.uvs.addAll(uvs);
    }

    public void setIndices(TIntList indices) {
        this.indices.clear();
        this.indices.addAll(indices);
    }

    public SkeletalMeshData build() {
        int rootBones = 0;
        for (Bone bone : bones) {
            if (bone.getParent() == null) {
                rootBones++;
            }
        }

        if (rootBones == 0) {
            throw new IllegalStateException("Cannot create a skeleton with no root bones");
        } else if (rootBones > 1) {
            throw new IllegalStateException("Cannot create a skeleton with multiple root bones");
        }

        // TODO: More validation

        return new SkeletalMeshData(bones, weights, uvs, vertexStartWeights, vertexWeightCounts, indices);
    }




}
