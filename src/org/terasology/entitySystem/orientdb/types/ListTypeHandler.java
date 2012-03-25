package org.terasology.entitySystem.orientdb.types;

import com.google.common.collect.Lists;
import org.terasology.entitySystem.orientdb.ValueTypeHandler;

import java.util.List;
import java.util.Map;

/**
 * @author Immortius <immortius@gmail.com>
 */
public class ListTypeHandler implements ValueTypeHandler<List> {
    ValueTypeHandler contentsHandler;
    
    public ListTypeHandler(ValueTypeHandler contentsHandler) {
        this.contentsHandler = contentsHandler;
    }

    public Object serialize(List value) {
        if (value != null) {
            List result = Lists.newArrayListWithCapacity(value.size());
            for (Object item : value) {
                result.add(contentsHandler.serialize(item));
            }
            return result;
        }
        return null;
    }

    public List deserialize(Object value) {
        if (value != null && value instanceof List) {
            List valueList = (List) value;
            List result = Lists.newArrayListWithCapacity(valueList.size());
            for (Object item : valueList) {
                result.add(contentsHandler.deserialize(item));
            }
            return result;
        }
        return null;
    }

    public ValueTypeHandler getContentsHandler() {
        return contentsHandler;
    }
}
