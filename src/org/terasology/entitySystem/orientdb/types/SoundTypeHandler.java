package org.terasology.entitySystem.orientdb.types;

import org.terasology.audio.Sound;
import org.terasology.entitySystem.orientdb.ValueTypeHandler;
import org.terasology.logic.manager.SoundManager;

/**
 * @author Immortius <immortius@gmail.com>
 */
public class SoundTypeHandler implements ValueTypeHandler<Sound> {

    private SoundManager soundManager;

    public SoundTypeHandler(SoundManager soundManager) {
        this.soundManager = soundManager;
    }

    public Object serialize(Sound value) {
        if (value != null) {
            return value.getName();
        }
        return null;
    }

    public Sound deserialize(Object value) {
        if (value instanceof String) {
            return soundManager.getSound((String) value);
        }
        return null;
    }
}
