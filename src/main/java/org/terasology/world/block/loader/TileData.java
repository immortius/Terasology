package org.terasology.world.block.loader;

import org.terasology.asset.AssetData;

import java.awt.image.BufferedImage;

/**
 * @author Immortius
 */
public class TileData implements AssetData {
    private BufferedImage image;

    public TileData(BufferedImage image) {
        this.image = image;
    }

    public BufferedImage getImage() {
        return image;
    }

}
