package org.terasology.entitySystem.orientdb;

import com.orientechnologies.orient.core.id.ORID;
import org.terasology.entitySystem.Component;
import org.terasology.entitySystem.EntityManager;
import org.terasology.entitySystem.EntityRef;
import org.terasology.entitySystem.Event;

/**
 * @author Immortius <immortius@gmail.com>
 */
public class OrientDBEntityRef implements EntityRef{
    ORID id;
    OrientDBEntityManager entityManager;

    OrientDBEntityRef(OrientDBEntityManager manager, ORID id) {
        this.id = id;
        this.entityManager = manager;
    }

    public Object getId() {
        return id;
    }

    public <T extends Component> T getComponent(Class<T> componentClass) {
        return entityManager.getComponent(id, componentClass);
    }

    public <T extends Component> T addComponent(T component) {
        return entityManager.addComponent(id, component);
    }

    public void removeComponent(Class<? extends Component> componentClass) {
        entityManager.removeComponent(id, componentClass);
    }

    public void saveComponent(Component component) {
        entityManager.saveComponent(id, component);
    }

    public Iterable<Component> iterateComponents() {
        return entityManager.iterateComponents(id);
    }

    public void destroy() {
        entityManager.destroy(id);
    }

    public void send(Event event) {
        entityManager.getEventSystem().send(this, event);
    }

    public boolean hasComponent(Class<? extends Component> component) {
        return entityManager.hasComponent(id, component);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        OrientDBEntityRef that = (OrientDBEntityRef) o;

        if (!id.equals(that.id)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return id.toString();
    }
}
