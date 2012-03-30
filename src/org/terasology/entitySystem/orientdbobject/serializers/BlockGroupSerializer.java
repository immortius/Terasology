package org.terasology.entitySystem.orientdbobject.serializers;

import com.orientechnologies.orient.core.serialization.serializer.object.OObjectSerializer;
import org.terasology.entitySystem.orientdb.ValueTypeHandler;
import org.terasology.model.blocks.BlockGroup;
import org.terasology.model.blocks.management.BlockManager;

/**
 * @author Immortius <immortius@gmail.com>
 */
public class BlockGroupSerializer implements OObjectSerializer<BlockGroup,String> {

    public Object serializeFieldValue(Class<?> iClass, BlockGroup iFieldValue) {
        return iFieldValue.getTitle();
    }

    public Object unserializeFieldValue(Class<?> iClass, String iFieldValue) {
        return BlockManager.getInstance().getBlockGroup(iFieldValue.toString());
    }
}
