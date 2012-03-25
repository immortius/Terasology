package org.terasology.entitySystem.orientdb;

import com.google.common.collect.Lists;
import com.orientechnologies.orient.core.Orient;
import com.orientechnologies.orient.core.db.graph.OGraphDatabase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.terasology.entitySystem.EntityManagerTest;
import org.terasology.entitySystem.EntityRef;
import org.terasology.entitySystem.orientdb.types.Vector3fTypeHandler;
import org.terasology.entitySystem.stubs.*;

import javax.vecmath.Vector3f;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

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
        OrientDBEntityManager orientdbManager = new OrientDBEntityManager("memory:test", "name", "password");
        entityManager = orientdbManager;
        orientdbManager.registerValueTypeHandler(Vector3f.class, new Vector3fTypeHandler());
        entityManager.registerComponentType(StringComponent.class);
        entityManager.registerComponentType(IntegerComponent.class);
        entityManager.registerComponentType(Vector3fComponent.class);
        entityManager.registerComponentType(Vector3fListComponent.class);
        entityManager.registerComponentType(ComplexTypeComponent.class);
        entityManager.registerComponentType(EntityRefComponent.class);

        // We disable caches to better test for things that would fail on reloading the database.
        orientdbManager.setCacheEnabled(false);
    }

    @After
    public void tearDown() {
        Orient.instance().shutdown();
    }

    @Test
    public void entityRemovedWhenDestroyed() {
        EntityRef entity = entityManager.create();

        StringComponent comp = new StringComponent();
        entity.addComponent(comp);
        entity.destroy();

        assertFalse(entity.exists());
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

    @Test
    public void testSerializeVector3() throws Exception {
        EntityRef entity = entityManager.create();
        Vector3fComponent original = new Vector3fComponent();
        original.setValue(new Vector3f(1,2,3));
        entity.addComponent(original);

        Vector3fComponent restored = entity.getComponent(Vector3fComponent.class);
        assertEquals(original, restored);
    }
    
    @Test
    public void testSerializeVector3fList() throws Exception {

        EntityRef entity = entityManager.create();
        Vector3fListComponent original = new Vector3fListComponent();
        original.positions.add(new Vector3f(1, 2, 3));
        original.positions.add(new Vector3f(4,5,6));
        entity.addComponent(original);

        Vector3fListComponent restored = entity.getComponent(Vector3fListComponent.class);
        assertEquals(original, restored);
        
    }
    
    @Test
    public void testSerializeMappedContainer() {
        EntityRef entity = entityManager.create();

        ComplexTypeComponent comp = new ComplexTypeComponent();
        ComplexTypeComponent.TestType data = new ComplexTypeComponent.TestType();
        data.name = "Fred";
        comp.data.add(data);
        entity.addComponent(comp);
        
        comp = entity.getComponent(ComplexTypeComponent.class);
        assertEquals(1, comp.data.size());
        assertEquals("Fred", comp.data.get(0).name);
    }

    @Test
    public void testSerializeEnum() {
        EntityRef entity = entityManager.create();

        ComplexTypeComponent comp = new ComplexTypeComponent();
        comp.enumValue = ComplexTypeComponent.TestEnum.Off;
        entity.addComponent(comp);

        comp = entity.getComponent(ComplexTypeComponent.class);
        assertEquals(ComplexTypeComponent.TestEnum.Off, comp.enumValue);
    }

    @Test
    public void testNullHandling() {
        EntityRef entity = entityManager.create();

        ComplexTypeComponent comp = new ComplexTypeComponent();
        comp.enumValue = ComplexTypeComponent.TestEnum.Off;
        entity.addComponent(comp);
        comp.data = null;
        entity.saveComponent(comp);

        comp = entity.getComponent(ComplexTypeComponent.class);
        assertEquals(Lists.newArrayList(), comp.data);
    }

    @Test
    public void entityRefSerialization() {
        EntityRef mainEntity = entityManager.create();
        EntityRef referencedEntity = entityManager.create();
        
        EntityRefComponent original = new EntityRefComponent();
        original.reference = referencedEntity;

        mainEntity.addComponent(original);

        EntityRefComponent retrieved = mainEntity.getComponent(EntityRefComponent.class);
        assertEquals(original.reference, retrieved.reference);
        assertEquals(original.refs, retrieved.refs);
    }

    @Test
    public void entityRefListSerialization() {
        EntityRef mainEntity = entityManager.create();
        EntityRef referencedEntity1 = entityManager.create();
        EntityRef referencedEntity2 = entityManager.create();

        EntityRefComponent original = new EntityRefComponent();
        original.refs.add(referencedEntity1);
        original.refs.add(referencedEntity2);

        mainEntity.addComponent(original);

        EntityRefComponent retrieved = mainEntity.getComponent(EntityRefComponent.class);
        assertEquals(original.refs, retrieved.refs);
    }

    @Test
    public void entityRefRemoval() {
        EntityRef mainEntity = entityManager.create();
        EntityRef referencedEntity1 = entityManager.create();

        EntityRefComponent original = new EntityRefComponent();
        original.reference = referencedEntity1;
        mainEntity.addComponent(original);

        referencedEntity1.destroy();

        EntityRefComponent retrieved = mainEntity.getComponent(EntityRefComponent.class);
        assertEquals(null, retrieved.reference);
    }

}
