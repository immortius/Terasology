package org.terasology.rendering.assets.font;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import org.terasology.asset.AssetData;
import org.terasology.rendering.assets.texture.Texture;

import javax.vecmath.Point4i;
import javax.vecmath.Tuple4i;
import java.util.List;
import java.util.Map;

/**
 * @author Immortius
 */
public class FontData implements AssetData {

    private int lineHeight;
    private Map<Integer, FontCharacter> characters;

    public FontData(int lineHeight, Map<Integer, FontCharacter> characters) {
        this.lineHeight = lineHeight;
        this.characters = ImmutableMap.copyOf(characters);
    }

    public int getLineHeight() {
        return lineHeight;
    }

    public Iterable<Map.Entry<Integer, FontCharacter>> getCharacters() {
        return characters.entrySet();
    }

    public FontCharacter getCharacter(int index) {
        return characters.get(index);
    }

}
