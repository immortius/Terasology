package org.terasology.rendering.assets.font;

import com.google.common.collect.Maps;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import org.terasology.rendering.assets.texture.Texture;

import java.util.Map;

/**
 * @author Immortius
 */
public class FontDataBuilder {

    private int lineHeight;
    private TIntObjectMap<Texture> pages = new TIntObjectHashMap<>();
    private Map<Integer, FontCharacter> characters = Maps.newHashMap();

    private int characterId;
    private int characterX;
    private int characterY;
    private int characterWidth;
    private int characterHeight;
    private int characterXOffset;
    private int characterYOffset;
    private int characterXAdvance;
    private int characterPage;

    public FontDataBuilder() {
    }


    public FontData build() {
        return new FontData(lineHeight, characters);
    }

    public void setLineHeight(int lineHeight) {
        this.lineHeight = lineHeight;
    }

    public void addPage(int pageId, Texture texture) {
        pages.put(pageId, texture);
    }

    public FontDataBuilder startCharacter(int characterId) {
        this.characterId = characterId;
        return this;
    }

    public FontDataBuilder setCharacterX(int characterX) {
        this.characterX = characterX;
        return this;
    }

    public FontDataBuilder setCharacterY(int characterY) {
        this.characterY = characterY;
        return this;
    }

    public FontDataBuilder setCharacterWidth(int characterWidth) {
        this.characterWidth = characterWidth;
        return this;
    }

    public FontDataBuilder setCharacterHeight(int characterHeight) {
        this.characterHeight = characterHeight;
        return this;
    }

    public FontDataBuilder setCharacterXOffset(int characterXOffset) {
        this.characterXOffset = characterXOffset;
        return this;
    }

    public FontDataBuilder setCharacterYOffset(int characterYOffset) {
        this.characterYOffset = characterYOffset;
        return this;
    }

    public FontDataBuilder setCharacterXAdvance(int characterXAdvance) {
        this.characterXAdvance = characterXAdvance;
        return this;
    }

    public FontDataBuilder setCharacterPage(int characterPage) {
        this.characterPage = characterPage;
        if (pages.get(characterPage) == null) {
            throw new IllegalArgumentException("Invalid font - character on missing page '" + characterPage + "'");
        }
        return this;
    }

    public FontDataBuilder endCharacter() {
        Texture page = pages.get(characterPage);
        FontCharacter character = new FontCharacter(((float) characterX / page.getWidth()), ((float) characterY / page.getHeight()), characterWidth, characterHeight, characterXOffset, characterYOffset, characterXAdvance, page);
        characters.put(characterId, character);
        return this;
    }

}
