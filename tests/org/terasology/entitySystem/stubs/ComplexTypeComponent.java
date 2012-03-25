package org.terasology.entitySystem.stubs;

import com.google.common.collect.Lists;
import org.terasology.entitySystem.AbstractComponent;

import java.util.List;

/**
 * @author Immortius <immortius@gmail.com>
 */
public class ComplexTypeComponent extends AbstractComponent {

    public List<TestType> data = Lists.newArrayList();

    public enum TestEnum {
        On,
        Off
    }
    
    public TestEnum enumValue;
    
    public static class TestType {
        public String name;
        public int health = 3;
        public Integer maxHealth = 54;
    }
}
