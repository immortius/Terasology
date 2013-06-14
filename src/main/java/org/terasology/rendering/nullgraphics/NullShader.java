package org.terasology.rendering.nullgraphics;

import com.google.common.collect.Maps;
import org.terasology.asset.AbstractAsset;
import org.terasology.asset.AssetUri;
import org.terasology.rendering.assets.shader.Shader;
import org.terasology.rendering.assets.shader.ShaderData;
import org.terasology.rendering.assets.shader.ShaderParameterMetadata;

import java.util.Map;

/**
 * @author Immortius
 */
public class NullShader extends AbstractAsset<ShaderData> implements Shader {

    private ShaderData data;

    private Map<String, ShaderParameterMetadata> parameterMap = Maps.newHashMap();

    public NullShader(AssetUri uri, ShaderData data) {
        super(uri);
        reload(data);
    }

    @Override
    public void recompile() {
    }

    @Override
    public ShaderParameterMetadata getParameter(String desc) {
        return parameterMap.get(desc);
    }

    @Override
    public Iterable<ShaderParameterMetadata> listParameters() {
        return data.getParameterMetadata();
    }

    @Override
    public void reload(ShaderData data) {
        this.data = data;
        parameterMap.clear();
        for (ShaderParameterMetadata param : data.getParameterMetadata()) {
            parameterMap.put(param.getName(), param);
        }
    }

    @Override
    public void dispose() {
        data = null;
    }

    @Override
    public boolean isDisposed() {
        return data == null;
    }
}
