package org.terasology.world;

import org.junit.Before;
import org.junit.Test;
import org.terasology.TerasologyTestingEnvironment;
import org.terasology.engine.CoreRegistry;
import org.terasology.math.Vector3i;
import org.terasology.world.block.Block;
import org.terasology.world.block.BlockUri;
import org.terasology.world.block.family.BlockFamilyFactoryRegistry;
import org.terasology.world.block.family.SymmetricFamily;
import org.terasology.world.block.management.BlockManager;
import org.terasology.world.block.management.BlockManagerImpl;
import org.terasology.world.chunks.Chunk;

import static org.junit.Assert.assertEquals;


public class ChunkTest extends TerasologyTestingEnvironment {

    private Chunk chunk;
    private BlockManagerImpl blockManager;

    @Before
    public void setup() throws Exception {
        super.setup();
        blockManager = (BlockManagerImpl)CoreRegistry.get(BlockManager.class);
        chunk = new Chunk(new Vector3i(0, 0, 0));
    }

    @Test
    public void testChangeBlock() {

        blockManager.addBlockFamily(new SymmetricFamily(new BlockUri("some:uri"), new Block()), false);
        Block block = blockManager.getBlock("some:uri");
        chunk.setBlock(new Vector3i(1, 2, 3), block);
        assertEquals(block, chunk.getBlock(new Vector3i(1, 2, 3)));
    }

}

