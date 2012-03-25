package org.terasology.entitySystem.orientdb.types;

import com.orientechnologies.orient.core.metadata.schema.OType;
import org.terasology.entitySystem.orientdb.ValueTypeHandler;

/**
 * @author Immortius <immortius@gmail.com>
 */
public class EnumTypeHandler<T extends Enum> implements ValueTypeHandler<T> {
    private Class<T> enumType;
    
    public EnumTypeHandler(Class<T> enumType) {
        this.enumType = enumType;
    }
    
    public Object serialize(T value) {
        if (value != null) {
            return value.toString();
        }
        return null;
    }

    public T deserialize(Object value) {
        if (value instanceof String) {
            return enumType.cast(Enum.valueOf(enumType, value.toString()));
        }
        return null;
    }
}
