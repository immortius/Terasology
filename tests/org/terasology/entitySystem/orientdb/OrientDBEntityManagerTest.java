package org.terasology.entitySystem.orientdb;

import com.orientechnologies.orient.core.Orient;
import com.orientechnologies.orient.core.db.graph.OGraphDatabase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.terasology.entitySystem.EntityManagerTest;
import org.terasology.entitySystem.EntityRef;
import org.terasology.entitySystem.stubs.IntegerComponent;
import org.terasology.entitySystem.stubs.StringComponent;

import static org.junit.Assert.assertEquals;

/**
 * @author Immortius <immortius@gmail.com>
 */
public class OrientDBEntityManagerTest extends EntityManagerTest {

    @Before
    public void setup() {
        OGraphDatabase db = new OGraphDatabase("memory:test");
        if (db.exists()) {
            db.delete();
        }
        entityManager = new OrientDBEntityManager("memory:test", "name", "password");
        entityManager.registerComponentType(StringComponent.class);
        entityManager.registerComponentType(IntegerComponent.class);
    }

    @After
    public void tearDown() {
        Orient.instance().shutdown();
    }

    @Test
    public void testNoChangeWithoutSave() {
        EntityRef entity = entityManager.create();
        entity.addComponent(new StringComponent("TEST"));
        StringComponent comp = entity.getComponent(StringComponent.class);
        comp.value = "WRONG";

        assertEquals("TEST", entity.getComponent(StringComponent.class).value);
    }

    @Test
    public void testChangedAfterSave() {
        EntityRef entity = entityManager.create();
        entity.addComponent(new StringComponent("TEST"));
        StringComponent comp = entity.getComponent(StringComponent.class);
        comp.value = "Right";
        entity.saveComponent(comp);

        assertEquals("Right", entity.getComponent(StringComponent.class).value);
    }

}
