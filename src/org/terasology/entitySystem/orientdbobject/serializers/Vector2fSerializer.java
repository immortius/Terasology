package org.terasology.entitySystem.orientdbobject.serializers;

import com.google.common.collect.Lists;
import com.orientechnologies.orient.core.serialization.serializer.object.OObjectSerializer;
import org.terasology.entitySystem.orientdb.ValueTypeHandler;

import javax.vecmath.Vector2f;
import java.util.List;

/**
 * @author Immortius <immortius@gmail.com>
 */
public class Vector2fSerializer implements OObjectSerializer<Vector2f, List<Float>> {

    public Object serializeFieldValue(Class<?> iClass, Vector2f iFieldValue) {
        return Lists.newArrayList(iFieldValue.x, iFieldValue.y);
    }

    public Object unserializeFieldValue(Class<?> iClass, List<Float> iFieldValue) {
        if (iFieldValue.size() == 2) {
            return new Vector2f(iFieldValue.get(0), iFieldValue.get(1));
        }
        return null;
    }
}
