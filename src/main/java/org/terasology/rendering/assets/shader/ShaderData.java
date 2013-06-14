package org.terasology.rendering.assets.shader;

import com.google.common.collect.ImmutableList;
import org.terasology.asset.AssetData;

import java.util.List;

/**
 * @author Immortius
 */
public class ShaderData implements AssetData {

    private String vertexProgram;
    private String fragmentProgram;
    private List<ShaderParameterMetadata> parameterMetadata;

    public ShaderData(String vertexProgram, String fragmentProgram, List<ShaderParameterMetadata> parameterMetadata) {
        this.vertexProgram = vertexProgram;
        this.fragmentProgram = fragmentProgram;
        this.parameterMetadata = ImmutableList.copyOf(parameterMetadata);
    }

    public String getVertexProgram() {
        return vertexProgram;
    }

    public String getFragmentProgram() {
        return fragmentProgram;
    }

    public List<ShaderParameterMetadata> getParameterMetadata() {
        return parameterMetadata;
    }
}
