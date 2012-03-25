package org.terasology.entitySystem.stubs;

import com.google.common.collect.Lists;
import org.terasology.entitySystem.AbstractComponent;

import javax.vecmath.Vector3f;
import java.util.List;

/**
 * @author Immortius <immortius@gmail.com>
 */
public final class Vector3fListComponent extends AbstractComponent {
    public List<Vector3f> positions = Lists.newArrayList();

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Vector3fListComponent that = (Vector3fListComponent) o;

        if (positions != null ? !positions.equals(that.positions) : that.positions != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return positions != null ? positions.hashCode() : 0;
    }
}
