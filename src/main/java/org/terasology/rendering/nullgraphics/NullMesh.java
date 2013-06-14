package org.terasology.rendering.nullgraphics;

import gnu.trove.list.TFloatList;
import org.terasology.asset.AbstractAsset;
import org.terasology.asset.AssetUri;
import org.terasology.math.AABB;
import org.terasology.rendering.assets.mesh.Mesh;
import org.terasology.rendering.assets.mesh.MeshData;

/**
 * @author Immortius
 */
public class NullMesh extends AbstractAsset<MeshData> implements Mesh {

    private MeshData data;
    private AABB aabb;

    public NullMesh(AssetUri uri, MeshData data) {
        super(uri);
        reload(data);
    }

    @Override
    public AABB getAABB() {
        return aabb;
    }

    @Override
    public TFloatList getVertices() {
        return data.getVertices();
    }

    @Override
    public void reload(MeshData data) {
        this.data = data;
        this.aabb = AABB.createEncompasing(data.getVertices());
    }

    @Override
    public void dispose() {
        this.data = null;
    }

    @Override
    public boolean isDisposed() {
        return data == null;
    }
}
