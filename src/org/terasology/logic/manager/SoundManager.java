package org.terasology.logic.manager;

import org.terasology.audio.Sound;
import org.terasology.audio.SoundPool;
import org.terasology.audio.SoundSource;

/**
 * @author Immortius <immortius@gmail.com>
 */
public interface SoundManager {
    Sound getSound(String name);

    Sound getMusic(String name);

    /**
     * Initializes AudioManager
     */
    void initialize();

    /**
     * Update AudioManager sound sources
     * <p/>
     * Should be called in main game loop
     */
    void update();

    /**
     * Gracefully destroy audio subsystem
     */
    void destroy();

    SoundPool getSoundPool(String pool);

    SoundSource getSoundSource(String pool, String sound, int priority);

    SoundSource getSoundSource(String pool, Sound sound, int priority);

    void stopAllSounds();
}
