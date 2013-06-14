package org.terasology.rendering.assets.material;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import org.terasology.asset.AssetData;
import org.terasology.rendering.assets.shader.Shader;
import org.terasology.rendering.assets.texture.Texture;

import java.util.Map;

/**
 * @author Immortius
 */
public class MaterialData implements AssetData {
    private Shader shader;
    Map<String, Texture> textures;
    Map<String, Float> floatParams;
    Map<String, float[]> floatArrayParams = Maps.newHashMap();
    Map<String, Integer> intParams = Maps.newHashMap();

    public MaterialData(Shader shader, Map<String, Texture> initialTextureParams, Map<String, Float> initialFloatParams, Map<String, float[]> initialFloatArrayParams, Map<String, Integer> initialIntParams) {
        this.shader = shader;
        this.textures = ImmutableMap.copyOf(initialTextureParams);
        this.floatParams = ImmutableMap.copyOf(initialFloatParams);
        this.floatArrayParams = ImmutableMap.copyOf(initialFloatArrayParams);
        this.intParams = ImmutableMap.copyOf(initialIntParams);
    }

    public Shader getShader() {
        return shader;
    }

    public Map<String, Texture> getTextures() {
        return textures;
    }

    public Map<String, Float> getFloatParams() {
        return floatParams;
    }

    public Map<String, float[]> getFloatArrayParams() {
        return floatArrayParams;
    }

    public Map<String, Integer> getIntegerParams() {
        return intParams;
    }
}
