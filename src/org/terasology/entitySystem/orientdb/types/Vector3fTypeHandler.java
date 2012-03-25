package org.terasology.entitySystem.orientdb.types;

import com.google.common.collect.Lists;
import org.terasology.entitySystem.orientdb.ValueTypeHandler;

import javax.vecmath.Vector3f;
import java.util.List;

/**
 * @author Immortius <immortius@gmail.com>
 */
public class Vector3fTypeHandler implements ValueTypeHandler<Vector3f> {

    public Object serialize(Vector3f value) {
        if (value != null) {
            return Lists.newArrayList(value.x, value.y, value.z);
        }
        return null;
    }

    public Vector3f deserialize(Object value) {
        if (value instanceof List) {
            List list = (List) value;
            if (list.size() == 3 && list.get(0) instanceof Float && list.get(1) instanceof Float && list.get(2) instanceof Float) {
                return new Vector3f((Float) list.get(0), (Float) list.get(1), (Float) list.get(2));
            }
        }
        return null;
    }
}
