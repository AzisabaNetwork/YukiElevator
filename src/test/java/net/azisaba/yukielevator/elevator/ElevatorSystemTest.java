package net.azisaba.yukielevator.elevator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import net.azisaba.yukielevator.config.ElevatorConfig;

class ElevatorSystemTest {

    private ElevatorConfig config;
    private ElevatorSystem system;

    @BeforeEach
    void setUp() {
        config = mock(ElevatorConfig.class);
        when(config.getBaseBlockType()).thenReturn(Material.DIAMOND_BLOCK);
        when(config.getElevatorHeight()).thenReturn(3);
        system = new ElevatorSystem(config);
    }

    @Test
    void testIsSafe() {
        Block air = mock(Block.class);
        when(air.isPassable()).thenReturn(true);
        when(air.getType()).thenReturn(Material.AIR);
        assertTrue(system.isSafe(air));

        Block stone = mock(Block.class);
        when(stone.isPassable()).thenReturn(false);
        when(stone.getType()).thenReturn(Material.STONE);
        assertFalse(system.isSafe(stone));

        Block lava = mock(Block.class);
        when(lava.isPassable()).thenReturn(true);
        when(lava.getType()).thenReturn(Material.LAVA);
        assertFalse(system.isSafe(lava));

        assertFalse(system.isSafe(null));
    }

    @Test
    void testIsFloor() {
        Block base = mock(Block.class);
        when(base.getType()).thenReturn(Material.DIAMOND_BLOCK);

        Block up1 = mock(Block.class);
        when(up1.isPassable()).thenReturn(true);
        when(up1.getType()).thenReturn(Material.AIR);

        Block up2 = mock(Block.class);
        when(up2.isPassable()).thenReturn(true);
        when(up2.getType()).thenReturn(Material.AIR);

        when(base.getRelative(BlockFace.UP, 1)).thenReturn(up1);
        when(base.getRelative(BlockFace.UP, 2)).thenReturn(up2);

        assertTrue(system.isFloor(base));

        // When head is blocked
        when(up2.isPassable()).thenReturn(false);
        when(up2.getType()).thenReturn(Material.STONE);
        assertFalse(system.isFloor(base));
    }

    @Test
    void testTryFindFloorWithObstacles() {
        World world = mock(World.class);
        when(world.getMinHeight()).thenReturn(-64);
        when(world.getMaxHeight()).thenReturn(320);

        Map<Integer, Block> column = new HashMap<>();

        for ( int y = -64; y < 320; y++ ) {
            Block b = mock(Block.class);
            when(b.getWorld()).thenReturn(world);
            when(b.getX()).thenReturn(0);
            when(b.getY()).thenReturn(y);
            when(b.getZ()).thenReturn(0);
            when(b.getType()).thenReturn(Material.AIR);
            when(b.isPassable()).thenReturn(true);
            when(b.getRelative(any(BlockFace.class), anyInt())).thenAnswer(invocation -> {
                BlockFace face = invocation.getArgument(0);
                int dist = invocation.getArgument(1);
                int targetY = ((Block) invocation.getMock()).getY() + (face == BlockFace.UP ? dist : -dist);
                return column.get(targetY);
            });
            column.put(y, b);
        }

        when(world.getBlockAt(anyInt(), anyInt(), anyInt())).thenAnswer(invocation -> {
            int y = invocation.getArgument(1);
            return column.get(y);
        });

        // Floor 1 at Y = 10 (Diamond Block)
        Block f1 = column.get(10);
        when(f1.getType()).thenReturn(Material.DIAMOND_BLOCK);
        when(f1.isPassable()).thenReturn(false);

        // Ceiling/Floor at Y = 14, 15 (Stone blocks between floors)
        when(column.get(14).getType()).thenReturn(Material.STONE);
        when(column.get(14).isPassable()).thenReturn(false);
        when(column.get(15).getType()).thenReturn(Material.STONE);
        when(column.get(15).isPassable()).thenReturn(false);

        // Floor 2 at Y = 20 (Diamond Block)
        Block f2 = column.get(20);
        when(f2.getType()).thenReturn(Material.DIAMOND_BLOCK);
        when(f2.isPassable()).thenReturn(false);

        // Upward search from Floor 1 should be blocked by stone blocks at Y=14,15!
        Block foundUp = system.tryFindFloor(f1, BlockFace.UP);
        assertNull(foundUp);

        // Downward search from Floor 2 should be blocked by stone blocks at Y=15,14!
        Block foundDown = system.tryFindFloor(f2, BlockFace.DOWN);
        assertNull(foundDown);
    }

    @Test
    void testTryFindFloorWithoutObstacles() {
        World world = mock(World.class);
        when(world.getMinHeight()).thenReturn(-64);
        when(world.getMaxHeight()).thenReturn(320);

        Map<Integer, Block> column = new HashMap<>();
        for ( int y = -64; y < 320; y++ ) {
            Block b = mock(Block.class);
            when(b.getWorld()).thenReturn(world);
            when(b.getX()).thenReturn(0);
            when(b.getY()).thenReturn(y);
            when(b.getZ()).thenReturn(0);
            when(b.getType()).thenReturn(Material.AIR);
            when(b.isPassable()).thenReturn(true);
            when(b.getRelative(any(BlockFace.class), anyInt())).thenAnswer(invocation -> {
                BlockFace face = invocation.getArgument(0);
                int dist = invocation.getArgument(1);
                int targetY = ((Block) invocation.getMock()).getY() + (face == BlockFace.UP ? dist : -dist);
                return column.get(targetY);
            });
            column.put(y, b);
        }

        when(world.getBlockAt(anyInt(), anyInt(), anyInt())).thenAnswer(invocation -> column.get(invocation.<Integer>getArgument(1)));

        // Floor 1 at Y = 10 (Diamond Block)
        Block f1 = column.get(10);
        when(f1.getType()).thenReturn(Material.DIAMOND_BLOCK);
        when(f1.isPassable()).thenReturn(false);

        // Floor 2 at Y = 20 (Diamond Block)
        Block f2 = column.get(20);
        when(f2.getType()).thenReturn(Material.DIAMOND_BLOCK);
        when(f2.isPassable()).thenReturn(false);

        // Upward search from Floor 1 should find Floor 2
        Block foundUp = system.tryFindFloor(f1, BlockFace.UP);
        assertNotNull(foundUp);
        assertEquals(20, foundUp.getY());

        // Downward search from Floor 2 should find Floor 1
        Block foundDown = system.tryFindFloor(f2, BlockFace.DOWN);
        assertNotNull(foundDown);
        assertEquals(10, foundDown.getY());
    }

    @Test
    void testOneWayElevatorWithObstacleUnderUpperFloor() {
        World world = mock(World.class);
        when(world.getMinHeight()).thenReturn(-64);
        when(world.getMaxHeight()).thenReturn(320);

        Map<Integer, Block> column = new HashMap<>();
        for ( int y = -64; y < 320; y++ ) {
            Block b = mock(Block.class);
            when(b.getWorld()).thenReturn(world);
            when(b.getX()).thenReturn(0);
            when(b.getY()).thenReturn(y);
            when(b.getZ()).thenReturn(0);
            when(b.getType()).thenReturn(Material.AIR);
            when(b.isPassable()).thenReturn(true);
            when(b.getRelative(any(BlockFace.class), anyInt())).thenAnswer(invocation -> {
                BlockFace face = invocation.getArgument(0);
                int dist = invocation.getArgument(1);
                int targetY = ((Block) invocation.getMock()).getY() + (face == BlockFace.UP ? dist : -dist);
                return column.get(targetY);
            });
            column.put(y, b);
        }

        when(world.getBlockAt(anyInt(), anyInt(), anyInt())).thenAnswer(invocation -> column.get(invocation.<Integer>getArgument(1)));

        // Floor 1 at Y = 10 (Diamond Block)
        Block f1 = column.get(10);
        when(f1.getType()).thenReturn(Material.DIAMOND_BLOCK);
        when(f1.isPassable()).thenReturn(false);

        // Obstacles (e.g. Red Concrete / Stone) placed at bottom 2 blocks of Floor 2: Y = 13, 14
        when(column.get(13).getType()).thenReturn(Material.RED_CONCRETE);
        when(column.get(13).isPassable()).thenReturn(false);
        when(column.get(14).getType()).thenReturn(Material.RED_CONCRETE);
        when(column.get(14).isPassable()).thenReturn(false);

        // Floor 2 at Y = 15 (Diamond Block)
        Block f2 = column.get(15);
        when(f2.getType()).thenReturn(Material.DIAMOND_BLOCK);
        when(f2.isPassable()).thenReturn(false);

        // Upward search from Floor 1 should be blocked by red concrete at Y = 13
        Block foundUp = system.tryFindFloor(f1, BlockFace.UP);
        assertNull(foundUp);

        // Downward search from Floor 2 skips Y=14, 13 (within height offset) and should successfully find Floor 1!
        Block foundDown = system.tryFindFloor(f2, BlockFace.DOWN);
        assertNotNull(foundDown);
        assertEquals(10, foundDown.getY());
    }

    @Test
    void testTryFindFloorInNegativeY() {
        World world = mock(World.class);
        when(world.getMinHeight()).thenReturn(-64);
        when(world.getMaxHeight()).thenReturn(320);

        Map<Integer, Block> column = new HashMap<>();
        for ( int y = -64; y < 320; y++ ) {
            Block b = mock(Block.class);
            when(b.getWorld()).thenReturn(world);
            when(b.getX()).thenReturn(0);
            when(b.getY()).thenReturn(y);
            when(b.getZ()).thenReturn(0);
            when(b.getType()).thenReturn(Material.AIR);
            when(b.isPassable()).thenReturn(true);
            when(b.getRelative(any(BlockFace.class), anyInt())).thenAnswer(invocation -> {
                BlockFace face = invocation.getArgument(0);
                int dist = invocation.getArgument(1);
                int targetY = ((Block) invocation.getMock()).getY() + (face == BlockFace.UP ? dist : -dist);
                return column.get(targetY);
            });
            column.put(y, b);
        }

        when(world.getBlockAt(anyInt(), anyInt(), anyInt())).thenAnswer(invocation -> column.get(invocation.<Integer>getArgument(1)));

        // Deepslate floor at Y = -40
        Block fNeg40 = column.get(-40);
        when(fNeg40.getType()).thenReturn(Material.DIAMOND_BLOCK);
        when(fNeg40.isPassable()).thenReturn(false);

        // Floor at Y = -10
        Block fNeg10 = column.get(-10);
        when(fNeg10.getType()).thenReturn(Material.DIAMOND_BLOCK);
        when(fNeg10.isPassable()).thenReturn(false);

        Block foundUp = system.tryFindFloor(fNeg40, BlockFace.UP);
        assertNotNull(foundUp);
        assertEquals(-10, foundUp.getY());

        Block foundDown = system.tryFindFloor(fNeg10, BlockFace.DOWN);
        assertNotNull(foundDown);
        assertEquals(-40, foundDown.getY());
    }
}
