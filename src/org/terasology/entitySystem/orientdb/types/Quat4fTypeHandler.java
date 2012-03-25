package org.terasology.entitySystem.orientdb.types;

import com.google.common.collect.Lists;
import org.terasology.entitySystem.orientdb.ValueTypeHandler;

import javax.vecmath.Color4f;
import javax.vecmath.Quat4f;
import java.util.List;

/**
 * @author Immortius <immortius@gmail.com>
 */
public class Quat4fTypeHandler implements ValueTypeHandler<Quat4f> {

    public Object serialize(Quat4f value) {
        if (value != null) {
            return Lists.newArrayList(value.x, value.y, value.z, value.w);
        }
        return null;
    }

    public Quat4f deserialize(Object value) {
        if (value instanceof List) {
            List listValue = (List) value;
            if (listValue.size() == 4 && listValue.get(0) instanceof Float && listValue.get(1) instanceof Float && listValue.get(2) instanceof Float && listValue.get(3) instanceof Float) {
                return new Quat4f((Float) listValue.get(0), (Float) listValue.get(1), (Float) listValue.get(2), (Float) listValue.get(3));
            }
        }
        return null;
    }
}
