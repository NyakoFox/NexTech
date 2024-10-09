package gay.nyako.nextech.network;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.HashMap;

public class Connections {
    HashMap<Direction, Connection> connections;

    public Connections() {
        connections = new HashMap<>();
    }

    public boolean hasConnection(Direction direction)
    {
        return connections.containsKey(direction);
    }

    public boolean hasConnection(Direction direction, boolean isPipe) {
        return hasConnection(direction) && getConnection(direction).isPipe == isPipe;
    }

    public Connection getConnection(Direction direction) {
        return connections.get(direction);
    }

    public void addConnection(Direction direction, Connection connection)
    {
        connections.put(direction, connection);
    }

    public NbtCompound write()
    {
        var root = new NbtCompound();
        for (Direction direction : connections.keySet())
        {
            Connection connection = connections.get(direction);
            root.put(direction.getName(), connection.write());
        }
        return root;
    }

    public void read(NbtCompound root)
    {
        connections.clear();
        for (String key : root.getKeys())
        {
            var connection = root.getCompound(key);
            var direction = switch(key) {
                case "north" -> Direction.NORTH;
                case "south" -> Direction.SOUTH;
                case "east" -> Direction.EAST;
                case "west" -> Direction.WEST;
                case "up" -> Direction.UP;
                case "down" -> Direction.DOWN;
                default -> Direction.NORTH;
            };
            addConnection(direction, Connection.read(connection));
        }
    }

    public static class Connection {
        public BlockPos position;
        public boolean isPipe;

        public Connection(BlockPos position, boolean isPipe)
        {
            this.position = position;
            this.isPipe = isPipe;
        }

        public NbtCompound write()
        {
            NbtCompound root = new NbtCompound();
            root.putInt("x", position.getX());
            root.putInt("y", position.getY());
            root.putInt("z", position.getZ());
            root.putBoolean("pipe", isPipe);
            return root;
        }

        public static Connection read(NbtCompound root)
        {
            return new Connection(new BlockPos(
                    root.getInt("x"),
                    root.getInt("y"),
                    root.getInt("z")
            ), root.getBoolean("pipe"));
        }
    }
}
