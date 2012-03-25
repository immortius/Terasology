package org.terasology.entitySystem.orientdb.types;

import com.google.common.collect.Lists;
import org.terasology.entitySystem.orientdb.ValueTypeHandler;
import org.terasology.math.Vector3i;

import java.util.List;

/**
 * @author Immortius <immortius@gmail.com>
 */
public class Vector3iTypeHandler implements ValueTypeHandler<Vector3i> {
    public Object serialize(Vector3i value) {
        if (value != null) {
            return Lists.newArrayList(value.x, value.y, value.z);
        }
        return null;
    }

    public Vector3i deserialize(Object value) {
        if (value != null && value instanceof List) {
            List listValue = (List) value;
            if (listValue.size() == 3 && listValue.get(0) instanceof Integer && listValue.get(1) instanceof Integer && listValue.get(2) instanceof Integer) {
                return new Vector3i((Integer) listValue.get(0), (Integer) listValue.get(1), (Integer) listValue.get(2));
            }
        }
        return null;
    }
}
