package org.terasology.entitySystem.orientdbobject.serializers;

import com.google.common.collect.Lists;
import com.orientechnologies.orient.core.serialization.serializer.object.OObjectSerializer;
import org.terasology.entitySystem.orientdb.ValueTypeHandler;
import org.terasology.math.Vector3i;

import java.util.List;

/**
 * @author Immortius <immortius@gmail.com>
 */
public class Vector3iSerializer implements OObjectSerializer<Vector3i,List<Integer>> {

    public Object serializeFieldValue(Class<?> iClass, Vector3i iFieldValue) {
        return Lists.newArrayList(iFieldValue.x, iFieldValue.y, iFieldValue.z);
    }

    public Object unserializeFieldValue(Class<?> iClass, List<Integer> iFieldValue) {
        if (iFieldValue.size() == 3) {
            return new Vector3i(iFieldValue.get(0), iFieldValue.get(1), iFieldValue.get(2));
        }
        return null;
    }
}
