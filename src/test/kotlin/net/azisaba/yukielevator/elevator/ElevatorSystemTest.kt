package net.azisaba.yukielevator.elevator

import net.azisaba.yukielevator.config.ElevatorConfig
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.World
import org.bukkit.block.Block
import org.bukkit.block.BlockFace
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.util.UUID

class ElevatorSystemTest {

    private lateinit var system: ElevatorSystem

    @BeforeEach
    fun setUp() {
        val config = mock(ElevatorConfig::class.java)
        `when`(config.baseBlockType).thenReturn(Material.DIAMOND_BLOCK)
        `when`(config.elevatorHeight).thenReturn(3)
        system = ElevatorSystem(config)
    }

    /** x=0, z=0 の縦一列 (すべて空気) を作る */
    private class Column {
        val world: World = mock(World::class.java)
        private val blocks = HashMap<Int, Block>()

        init {
            `when`(world.minHeight).thenReturn(-64)
            `when`(world.maxHeight).thenReturn(320)
            for (y in -64 until 320) {
                val b = mock(Block::class.java)
                `when`(b.world).thenReturn(world)
                `when`(b.x).thenReturn(0)
                `when`(b.y).thenReturn(y)
                `when`(b.z).thenReturn(0)
                `when`(b.type).thenReturn(Material.AIR)
                `when`(b.isPassable).thenReturn(true)
                `when`(b.getRelative(any(BlockFace::class.java), anyInt())).thenAnswer {
                    val face = it.getArgument<BlockFace>(0)
                    val dist = it.getArgument<Int>(1)
                    blocks[y + if (face == BlockFace.UP) dist else -dist]
                }
                blocks[y] = b
            }
            `when`(world.getBlockAt(anyInt(), anyInt(), anyInt())).thenAnswer { blocks[it.getArgument<Int>(1)] }
        }

        operator fun get(y: Int): Block = blocks.getValue(y)

        fun set(y: Int, type: Material) {
            `when`(this[y].type).thenReturn(type)
            `when`(this[y].isPassable).thenReturn(false)
        }
    }

    @Test
    fun testIsSafe() {
        val air = mock(Block::class.java)
        `when`(air.isPassable).thenReturn(true)
        `when`(air.type).thenReturn(Material.AIR)
        assertTrue(system.isSafe(air))

        val stone = mock(Block::class.java)
        `when`(stone.isPassable).thenReturn(false)
        `when`(stone.type).thenReturn(Material.STONE)
        assertFalse(system.isSafe(stone))

        val lava = mock(Block::class.java)
        `when`(lava.isPassable).thenReturn(true)
        `when`(lava.type).thenReturn(Material.LAVA)
        assertFalse(system.isSafe(lava))

        assertFalse(system.isSafe(null))
    }

    @Test
    fun testIsFloor() {
        val column = Column()
        column.set(10, Material.DIAMOND_BLOCK)
        assertTrue(system.isFloor(column[10]))

        // 頭の位置が塞がっている
        column.set(12, Material.STONE)
        assertFalse(system.isFloor(column[10]))
    }

    @Test
    fun testTryFindFloorWithObstacles() {
        val column = Column()
        column.set(10, Material.DIAMOND_BLOCK)
        column.set(14, Material.STONE)
        column.set(15, Material.STONE)
        column.set(20, Material.DIAMOND_BLOCK)

        assertNull(system.tryFindFloor(column[10], BlockFace.UP))
        assertNull(system.tryFindFloor(column[20], BlockFace.DOWN))
    }

    @Test
    fun testTryFindFloorWithoutObstacles() {
        val column = Column()
        column.set(10, Material.DIAMOND_BLOCK)
        column.set(20, Material.DIAMOND_BLOCK)

        assertEquals(20, system.tryFindFloor(column[10], BlockFace.UP)?.y)
        assertEquals(10, system.tryFindFloor(column[20], BlockFace.DOWN)?.y)
    }

    @Test
    fun testOneWayElevatorWithObstacleUnderUpperFloor() {
        val column = Column()
        column.set(10, Material.DIAMOND_BLOCK)
        column.set(13, Material.RED_CONCRETE)
        column.set(14, Material.RED_CONCRETE)
        column.set(15, Material.DIAMOND_BLOCK)

        assertNull(system.tryFindFloor(column[10], BlockFace.UP))

        val foundDown = system.tryFindFloor(column[15], BlockFace.DOWN)
        assertNotNull(foundDown)
        assertEquals(10, foundDown!!.y)
    }

    @Test
    fun testStackedFloorsMoveOneFloorAtATime() {
        val column = Column()
        listOf(0, 5, 10, 15).forEach { column.set(it, Material.DIAMOND_BLOCK) }

        assertEquals(10, system.tryFindFloor(column[15], BlockFace.DOWN)?.y)
        assertEquals(5, system.tryFindFloor(column[10], BlockFace.DOWN)?.y)
        assertEquals(10, system.tryFindFloor(column[5], BlockFace.UP)?.y)
    }

    @Test
    fun testTryFindFloorInNegativeY() {
        val column = Column()
        column.set(-40, Material.DIAMOND_BLOCK)
        column.set(-10, Material.DIAMOND_BLOCK)

        assertEquals(-10, system.tryFindFloor(column[-40], BlockFace.UP)?.y)
        assertEquals(-40, system.tryFindFloor(column[-10], BlockFace.DOWN)?.y)
    }

    @Test
    fun testIsStandingOn() {
        val column = Column()
        val base = column[10]

        assertTrue(system.isStandingOn(Location(column.world, 0.5, 11.0, 0.5), base))
        // ジャンプ中など、床から浮いている
        assertFalse(system.isStandingOn(Location(column.world, 0.5, 11.5, 0.5), base))
        // 別の列
        assertFalse(system.isStandingOn(Location(column.world, 1.5, 11.0, 0.5), base))
    }

    @Test
    fun testCooldownIsSharedBetweenDirections() {
        var now = 1_000L
        val cooldown = ElevatorCooldown(500L) { now }
        val uuid = UUID.randomUUID()

        assertFalse(cooldown.isCoolingDown(uuid))
        cooldown.mark(uuid) // 下降した
        now += 100
        assertTrue(cooldown.isCoolingDown(uuid)) // 直後のジャンプ判定は無視される
        now += 400
        assertFalse(cooldown.isCoolingDown(uuid))

        cooldown.mark(uuid)
        cooldown.clear(uuid)
        assertFalse(cooldown.isCoolingDown(uuid))
    }
}
