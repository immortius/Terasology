package org.terasology.entitySystem.stubs;

import org.terasology.entitySystem.AbstractComponent;
import org.terasology.persistence.interfaces.StorageReader;
import org.terasology.persistence.interfaces.StorageWriter;

/**
 * @author Immortius <immortius@gmail.com>
 */
public final class IntegerComponent extends AbstractComponent {
    private int value;
    
    public IntegerComponent() {}
    
    public IntegerComponent(int value) {
        this.value = value;
    }
    
    public int getValue() {
        return value;
    }

    public void setValue(int newValue) {
        value = newValue;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        IntegerComponent that = (IntegerComponent) o;

        if (value != that.value) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return value;
    }
}
