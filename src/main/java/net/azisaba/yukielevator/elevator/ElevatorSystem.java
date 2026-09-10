package net.azisaba.yukielevator.elevator;

import java.util.stream.IntStream;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.util.Vector;

import net.azisaba.yukielevator.config.ElevatorConfig;

public class ElevatorSystem {

    private final Material baseType;
    private final int height;

    public ElevatorSystem(ElevatorConfig config) {
        baseType = config.getBaseBlockType();
        height = config.getElevatorHeight();
    }

    public Material getBaseType() {
        return baseType;
    }

    public int getHeight() {
        return height;
    }

    public boolean isSafe(Block block) {
        if ( block == null ) {
            return false;
        }
        if ( !block.isPassable() ) {
            return false;
        }
        Material type = block.getType();
        return type != Material.LAVA
                && type != Material.FIRE
                && type != Material.SOUL_FIRE
                && type != Material.CACTUS
                && type != Material.SWEET_BERRY_BUSH;
    }

    public boolean isFloor(Block baseFrom) {
        if ( baseFrom == null || baseFrom.getType() != baseType ) {
            return false;
        }
        return IntStream.range(1, height).allMatch(distance -> isSafe(baseFrom.getRelative(BlockFace.UP, distance)));
    }

    public Block tryFindFloor(Block baseFrom, BlockFace face) {
        if ( baseFrom == null || (face != BlockFace.UP && face != BlockFace.DOWN) ) {
            return null;
        }

        int step = (face == BlockFace.UP) ? 1 : -1;
        int minY = baseFrom.getWorld().getMinHeight();
        int maxY = baseFrom.getWorld().getMaxHeight();

        int startY = baseFrom.getY() + step * height;
        for ( int y = startY; y >= minY && y < maxY; y += step ) {
            Block target = baseFrom.getWorld().getBlockAt(baseFrom.getX(), y, baseFrom.getZ());
            if ( isFloor(target) ) {
                return target;
            }
        }
        return null;
    }

    public Location calculatePlayerTo(Location playerFrom, Block baseFrom, Block baseTo) {
        Location playerTo = baseTo.getRelative(BlockFace.UP).getLocation();

        Location xzDiff = playerFrom.clone().subtract(baseFrom.getLocation());
        xzDiff.setY(0);
        playerTo.add(xzDiff);

        Vector direction = playerFrom.getDirection();
        playerTo.setDirection(direction);

        return playerTo;
    }
}
