package org.terasology.entitySystem.orientdb;

import org.junit.Before;
import org.junit.Test;
import org.terasology.audio.Sound;
import org.terasology.entitySystem.orientdb.types.SoundTypeHandler;
import org.terasology.game.CoreRegistry;
import org.terasology.logic.manager.AudioManager;
import org.terasology.logic.manager.SoundManager;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Immortius <immortius@gmail.com>
 */
public class SoundTypeHandlerTest {

    private static final String SOUND_NAME = "Sound";
    Sound testSound;
    SoundManager audioManager;
    
    @Before
    public void setup() {
        testSound = mock(Sound.class);
        when(testSound.getName()).thenReturn(SOUND_NAME);
        audioManager = mock(SoundManager.class);
        when(audioManager.getSound(SOUND_NAME)).thenReturn(testSound);

    }
    
    @Test
    public void serializeToString() {
        SoundTypeHandler typeHandler = new SoundTypeHandler(audioManager);
        assertEquals(SOUND_NAME, typeHandler.serialize(testSound));
    }
    
    @Test
    public void deserializeToSound() {
        SoundTypeHandler typeHandler = new SoundTypeHandler(audioManager);
        assertEquals(testSound, typeHandler.deserialize(SOUND_NAME));
    }
}
