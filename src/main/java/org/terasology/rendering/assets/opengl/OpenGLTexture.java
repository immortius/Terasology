/*
 * Copyright 2013 Moving Blocks
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.terasology.rendering.assets.opengl;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terasology.asset.AbstractAsset;
import org.terasology.asset.AssetUri;
import org.terasology.rendering.assets.texture.Texture;
import org.terasology.rendering.assets.texture.TextureData;

import java.nio.ByteBuffer;

import static org.lwjgl.opengl.GL11.*;

/**
 *
 */
public class OpenGLTexture extends AbstractAsset<TextureData> implements Texture {

    private static final Logger logger = LoggerFactory.getLogger(OpenGLTexture.class);

    private int id = 0;
    private int width;
    private int height;
    private WrapMode wrapMode = WrapMode.Clamp;
    private FilterMode filterMode = FilterMode.Nearest;
    private ByteBuffer[] data;

    public OpenGLTexture(AssetUri uri, TextureData data) {
        super(uri);

        reload(data);
    }

    @Override
    public void reload(TextureData data) {
        dispose();
        this.width = data.getWidth();
        this.height = data.getHeight();
        this.wrapMode = data.getWrapMode();
        this.filterMode = data.getFilterMode();

        id = glGenTextures();
        logger.debug("Bound texture '{}' - {}", getURI(), id);
        glBindTexture(GL11.GL_TEXTURE_2D, id);

        glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, wrapMode.getGLMode());
        glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, wrapMode.getGLMode());
        GL11.glTexParameteri(GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, filterMode.getGlMinFilter());
        GL11.glTexParameteri(GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, filterMode.getGlMagFilter());
        GL11.glPixelStorei(GL11.GL_UNPACK_ALIGNMENT, 4);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL12.GL_TEXTURE_MAX_LEVEL, data.getBuffers().length - 1);

        for (int i = 0; i < data.getBuffers().length; i++) {
            GL11.glTexImage2D(GL11.GL_TEXTURE_2D, i, GL11.GL_RGBA, width >> i, height >> i, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, data.getBuffers()[i]);
        }
    }

    @Override
    public void dispose() {
        if (id != 0) {
            glDeleteTextures(id);
            id = 0;
        }
    }

    @Override
    public boolean isDisposed() {
        return id == 0;
    }

    public int getId() {
        return id;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public WrapMode getWrapMode() {
        return wrapMode;
    }

    public FilterMode getFilterMode() {
        return filterMode;
    }
}
