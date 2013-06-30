package org.terasology.world.block.loader;

import org.terasology.asset.AbstractAsset;
import org.terasology.asset.AssetUri;

import java.awt.image.BufferedImage;

/**
 * @author Immortius
 */
public class TileImpl extends AbstractAsset<TileData> implements Tile  {

    private BufferedImage image;

    public TileImpl(AssetUri uri, TileData data) {
        super(uri);
        reload(data);
    }

    @Override
    public BufferedImage getImage() {
        return image;
    }

    @Override
    public void reload(TileData data) {
        image = data.getImage();
    }

    @Override
    public void dispose() {
        image = null;
    }

    @Override
    public boolean isDisposed() {
        return image == null;
    }
}
