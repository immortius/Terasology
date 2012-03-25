package org.terasology.entitySystem.orientdb;

/**
 * @author Immortius <immortius@gmail.com>
 */
public interface ValueTypeHandler<T> {

    public Object serialize(T value);
    
    public T deserialize(Object value);
    
}
