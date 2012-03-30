package org.terasology.entitySystem.orientdbobject.serializers;

import com.orientechnologies.orient.core.serialization.serializer.object.OObjectSerializer;
import org.terasology.audio.Sound;
import org.terasology.entitySystem.orientdb.ValueTypeHandler;
import org.terasology.logic.manager.SoundManager;

/**
 * @author Immortius <immortius@gmail.com>
 */
public class SoundSerializer implements OObjectSerializer<Sound,String> {

    private SoundManager soundManager;

    public SoundSerializer(SoundManager soundManager) {
        this.soundManager = soundManager;
    }

    public Object serializeFieldValue(Class<?> iClass, Sound iFieldValue) {
        return iFieldValue.getName();
    }

    public Object unserializeFieldValue(Class<?> iClass, String iFieldValue) {
        return soundManager.getSound(iFieldValue);
    }
}
