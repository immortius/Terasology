package org.terasology.entitySystem.orientdbobject.serializers;

import com.google.common.collect.Lists;
import com.orientechnologies.orient.core.serialization.serializer.object.OObjectSerializer;
import org.terasology.entitySystem.orientdb.ValueTypeHandler;

import javax.vecmath.Color4f;
import java.util.List;

/**
 * @author Immortius <immortius@gmail.com>
 */
public class Color4fSerializer implements OObjectSerializer<Color4f,List<Float>> {

    public Object serializeFieldValue(Class<?> iClass, Color4f iFieldValue) {
        return Lists.newArrayList(iFieldValue.x, iFieldValue.y, iFieldValue.z, iFieldValue.w);
    }

    public Object unserializeFieldValue(Class<?> iClass, List<Float> iFieldValue) {
        if (iFieldValue.size() == 4) {
            return new Color4f(iFieldValue.get(0), iFieldValue.get(1), iFieldValue.get(2), iFieldValue.get(3));
        }
        return null;
    }
}
