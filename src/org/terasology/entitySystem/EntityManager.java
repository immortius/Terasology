package org.terasology.entitySystem;

import org.terasology.components.LocationComponent;
import org.terasology.components.MeshComponent;

import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.Map;

/**
 * @author Immortius <immortius@gmail.com>
 */
public interface EntityManager {

    void registerComponentType(Class<? extends Component> componentclass);
    void dispose();

    /**
     * @return A references to a new, unused entity
     */
    EntityRef create();

    /**
     * @param componentClass
     * @return The number of entities with this component class
     */
    long getComponentCount(Class<? extends Component> componentClass);
    
    <T extends Component> Iterable<Map.Entry<EntityRef,T>> iterateComponents(Class<T> componentClass);
    Iterable<EntityRef> iteratorEntities(Class<? extends Component> ...  componentClasses);

    EventSystem getEventSystem();
    void setEventSystem(EventSystem systen);
}
