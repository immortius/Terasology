package org.terasology.entitySystem.orientdbobject.serializers;

import com.google.common.collect.Lists;
import com.orientechnologies.orient.core.serialization.serializer.object.OObjectSerializer;

import javax.vecmath.Vector3f;
import java.util.List;

/**
 * @author Immortius <immortius@gmail.com>
 */
public class Vector3fSerializer implements OObjectSerializer<Vector3f, List<Float>> {

    public Object serializeFieldValue(Class<?> iClass, Vector3f iFieldValue) {
        return Lists.newArrayList(iFieldValue.x, iFieldValue.y, iFieldValue.z);
    }

    public Object unserializeFieldValue(Class<?> iClass, List<Float> iFieldValue) {
        if (iFieldValue.size() == 3) {
            return new Vector3f(iFieldValue.get(0), iFieldValue.get(1), iFieldValue.get(2));
        }
        return null;
    }

}
