package org.terasology.entitySystem.orientdb.types;

import com.google.common.collect.Lists;
import org.terasology.entitySystem.orientdb.ValueTypeHandler;

import javax.vecmath.Color4f;
import java.util.List;

/**
 * @author Immortius <immortius@gmail.com>
 */
public class Color4fTypeHandler implements ValueTypeHandler<Color4f> {

    public Object serialize(Color4f value) {
        if (value != null) {
            return Lists.newArrayList(value.x, value.y, value.z, value.w);
        }
        return null;
    }

    public Color4f deserialize(Object value) {
        if (value instanceof List) {
            List listValue = (List) value;
            if (listValue.size() == 4 && listValue.get(0) instanceof Float && listValue.get(1) instanceof Float && listValue.get(2) instanceof Float && listValue.get(3) instanceof Float) {
                return new Color4f((Float) listValue.get(0), (Float) listValue.get(1), (Float) listValue.get(2), (Float) listValue.get(3));
            }
        }
        return null;
    }
}
