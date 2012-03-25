package org.terasology.entitySystem.stubs;

import com.google.common.collect.Lists;
import org.terasology.entitySystem.AbstractComponent;
import org.terasology.entitySystem.EntityRef;

import java.util.List;

/**
 * @author Immortius <immortius@gmail.com>
 */
public class EntityRefComponent extends AbstractComponent {
    public EntityRef reference;
    public List<EntityRef> refs = Lists.newArrayList();
}
