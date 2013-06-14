package org.terasology.rendering.nullgraphics;

import org.terasology.asset.AbstractAsset;
import org.terasology.asset.AssetUri;
import org.terasology.rendering.assets.material.Material;
import org.terasology.rendering.assets.material.MaterialData;
import org.terasology.rendering.assets.texture.Texture;

import java.nio.FloatBuffer;

/**
 * @author Immortius
 */
public class NullMaterial extends AbstractAsset<MaterialData> implements Material {

    public NullMaterial(AssetUri uri, MaterialData data) {
        super(uri);
    }

    @Override
    public void reload(MaterialData data) {
    }

    @Override
    public void dispose() {
    }

    @Override
    public boolean isDisposed() {
        return false;
    }

    @Override
    public void setFloat(String desc, float f) {
    }

    @Override
    public void setFloat2(String desc, float f1, float f2) {
    }

    @Override
    public void setFloat3(String desc, float f1, float f2, float f3) {
    }

    @Override
    public void setFloat4(String desc, float f1, float f2, float f3, float f4) {
    }

    @Override
    public void setInt(String desc, int i) {
    }

    @Override
    public void setFloat1(String desc, FloatBuffer buffer) {
    }

    @Override
    public void setFloat2(String desc, FloatBuffer buffer) {
    }

    @Override
    public void setFloat3(String desc, FloatBuffer buffer) {
    }

    @Override
    public void setFloat4(String desc, FloatBuffer buffer) {
    }

    @Override
    public void setTexture(String desc, Texture texture) {
    }


}
