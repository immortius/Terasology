package org.terasology.entitySystem.orientdb.types;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.terasology.entitySystem.orientdb.ValueTypeHandler;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Immortius <immortius@gmail.com>
 */
public class MappedContainerTypeHandler<T> implements ValueTypeHandler<T> {
    
    private static Logger logger = Logger.getLogger(MappedContainerTypeHandler.class.getName());

    private Class<T> clazz;
    private List<FieldInfo> fields = Lists.newArrayList();
    
    private class FieldInfo {
        public Field field;
        public ValueTypeHandler handler;

        private FieldInfo(Field field, ValueTypeHandler handler) {
            this.field = field;
            this.handler = handler;
        }
    }
    
    public MappedContainerTypeHandler(Class<T> clazz) {
        this.clazz = clazz;
    }

    public void addField(Field field, ValueTypeHandler handler) {
        fields.add(new FieldInfo(field, handler));
    }
    
    public Object serialize(T value) {
        if (value == null) return null;

        try {
            Map<String,Object> map = Maps.newHashMapWithExpectedSize(fields.size());
            for (FieldInfo fieldInfo : fields) {
                Object fieldValue = fieldInfo.field.get(value);
                Object serializedValue = fieldInfo.handler.serialize(fieldValue);
                map.put(fieldInfo.field.getName(), serializedValue);
            }
            return map;
        } catch (IllegalAccessException e) {
            logger.log(Level.SEVERE, "Unable to serialize field of " + value.getClass(), e);
        }
        return null;
    }

    public T deserialize(Object value) {
        if (value instanceof Map) {
            try {
                Map<String,Object> map = (Map<String,Object>) value;
                T result = clazz.newInstance();
                for (FieldInfo fieldInfo : fields) {
                    Object fieldValue = map.get(fieldInfo.field.getName());
                    if (fieldValue == null) continue;
                    Object deserializedValue = fieldInfo.handler.deserialize(fieldValue);
                    if (deserializedValue == null) continue;
                    fieldInfo.field.set(result, deserializedValue);
                }
                return result;
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Unable to deserialize " + value.getClass(), e);
            }
        }
        return null;
    }
}
