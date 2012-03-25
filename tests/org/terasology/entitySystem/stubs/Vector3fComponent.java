package org.terasology.entitySystem.stubs;

import org.terasology.entitySystem.AbstractComponent;

import javax.vecmath.Vector3f;

/**
 * @author Immortius <immortius@gmail.com>
 */
public class Vector3fComponent extends AbstractComponent {
    private Vector3f value = new Vector3f();

    public Vector3f getValue() {
        return value;
    }

    public void setValue(Vector3f value) {
        this.value.set(value);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Vector3fComponent that = (Vector3fComponent) o;

        if (value != null ? !value.equals(that.value) : that.value != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return value != null ? value.hashCode() : 0;
    }

    public String toString() {
        return "Vector3fComponent[" + value + "]";
    }
}
