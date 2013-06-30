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
    private Map<String, Texture> textures = Maps.newHashMap();
    private Map<String, Float> floatParams = Maps.newHashMap();
    private Map<String, float[]> floatArrayParams = Maps.newHashMap();
    private Map<String, Integer> intParams = Maps.newHashMap();

    public MaterialData(Shader shader) {
        this.shader = shader;
    }

    public Shader getShader() {
        return shader;
    }

    public Map<String, Texture> getTextures() {
        return textures;
    }

    public void setParam(String parmName, Texture value) {
        textures.put(parmName, value);
    }

    public Map<String, Float> getFloatParams() {
        return floatParams;
    }

    public void setParam(String parmName, float value) {
        floatParams.put(parmName, value);
    }

    public Map<String, float[]> getFloatArrayParams() {
        return floatArrayParams;
    }

    public void setParam(String parmName, float[] value) {
        floatArrayParams.put(parmName, value);
    }

    public Map<String, Integer> getIntegerParams() {
        return intParams;
    }

    public void setParam(String parmName, int value) {
        intParams.put(parmName, value);
    }

    public void setTextureParams(Map<String, Texture> textures) {
        this.textures.clear();
        textures.putAll(textures);
    }

    public void setFloatParams(Map<String, Float> floatParams) {
        this.floatParams.clear();
        this.floatParams.putAll(floatParams);
    }

    public void setFloatArrayParams(Map<String, float[]> floatArrayParams) {
        this.floatArrayParams.clear();
        this.floatArrayParams.putAll(floatArrayParams);
    }

    public void setIntParams(Map<String, Integer> intParams) {
        this.intParams.clear();
        this.intParams.putAll(intParams);
    }
}
