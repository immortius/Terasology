package org.terasology.rendering.nullgraphics;

import org.terasology.asset.AbstractAsset;
import org.terasology.asset.AssetUri;
import org.terasology.rendering.assets.texture.Texture;
import org.terasology.rendering.assets.texture.TextureData;

/**
 * @author Immortius
 */
public class NullTexture extends AbstractAsset<TextureData> implements Texture {

    private FilterMode filterMode;
    private WrapMode wrapMode;
    private int height;
    private int width;

    public NullTexture(AssetUri uri, TextureData data) {
        super(uri);
        reload(data);
    }

    @Override
    public int getWidth() {
        return width;
    }

    @Override
    public int getHeight() {
        return height;
    }

    @Override
    public WrapMode getWrapMode() {
        return wrapMode;
    }

    @Override
    public FilterMode getFilterMode() {
        return filterMode;
    }

    @Override
    public void reload(TextureData data) {
        this.filterMode = data.getFilterMode();
        this.wrapMode = data.getWrapMode();
        this.height = data.getHeight();
        this.width = data.getWidth();
    }

    @Override
    public void dispose() {
    }

    @Override
    public boolean isDisposed() {
        return false;
    }
}
