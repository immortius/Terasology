package org.terasology.entitySystem.orientdb;

import com.google.common.collect.Lists;
import org.terasology.entitySystem.Component;
import org.terasology.entitySystem.EntityRef;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

/**
 * @author Immortius <immortius@gmail.com>
 */
public class ComponentTypeInfo {

    String name;
    List<Field> fields = Lists.newArrayList();
    
    public ComponentTypeInfo(Class<Component> component) {
        name = component.getSimpleName();
        for (Field field : component.getDeclaredFields()) {
            field.setAccessible(true);
            fields.add(field);
        }
    }
}
