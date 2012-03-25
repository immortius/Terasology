package org.terasology.entitySystem.orientdb.types;

import com.google.common.collect.Lists;
import org.terasology.entitySystem.orientdb.ValueTypeHandler;

import javax.vecmath.Vector2f;
import java.util.List;

/**
 * @author Immortius <immortius@gmail.com>
 */
public class Vector2fTypeHandler implements ValueTypeHandler<Vector2f> {

    public Object serialize(Vector2f value) {
        if (value != null) {
            return Lists.newArrayList(value.x, value.y);
        }
        return null;
    }

    public Vector2f deserialize(Object value) {
        if (value instanceof List) {
            List list = (List) value;
            if (list.size() == 2 && list.get(0) instanceof Float && list.get(1) instanceof Float) {
                return new Vector2f((Float) list.get(0), (Float) list.get(1));
            }
        }
        return null;
    }
}
