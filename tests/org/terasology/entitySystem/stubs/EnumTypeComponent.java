package org.terasology.entitySystem.stubs;

import com.google.common.collect.Lists;
import org.terasology.entitySystem.AbstractComponent;

import java.util.List;

/**
 * @author Immortius <immortius@gmail.com>
 */
public class EnumTypeComponent extends AbstractComponent {

    public enum TestEnum {
        On,
        Off
    }
    
    public TestEnum enumValue;

}
