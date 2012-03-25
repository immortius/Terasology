package org.terasology.entitySystem.orientdb.types;

import org.terasology.entitySystem.orientdb.ValueTypeHandler;
import org.terasology.model.blocks.BlockGroup;
import org.terasology.model.blocks.management.BlockManager;

/**
 * @author Immortius <immortius@gmail.com>
 */
public class BlockGroupTypeHandler implements ValueTypeHandler<BlockGroup> {

    public Object serialize(BlockGroup value) {
        if (value != null) {
            return value.getTitle();
        }
        return null;
    }

    public BlockGroup deserialize(Object value) {
        if (value != null) {
            return BlockManager.getInstance().getBlockGroup(value.toString());
        }
        return null;
    }
}
