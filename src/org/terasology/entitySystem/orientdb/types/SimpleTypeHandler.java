package org.terasology.entitySystem.orientdb.types;

import org.terasology.entitySystem.orientdb.ValueTypeHandler;

/**
 * @author Immortius <immortius@gmail.com>
 */
public class SimpleTypeHandler implements ValueTypeHandler {

    public Object serialize(Object value) {
        return value;
    }

    public Object deserialize(Object value) {
        return value;
    }
}
