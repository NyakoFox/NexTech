package gay.nyako.nextech.network.node;

import gay.nyako.nextech.network.ConnectionMode;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.Objects;

public class SimplePipeNetworkNode implements PipeNetworkNode {
    private final BlockPos pos;

    /**
     * A basic pipe network node, used in non-ticking pipes (e.g. energy pipes).
     * @param pos The {@link BlockPos} of the relevant pipe (not the endpoint).
     * @param endpoint Whether the node represents an input/output for the network (e.g. inventories, machines).
     * @param side The side of the pipe this endpoint is connected to, or null if this node is not an endpoint.
     */

    /**
     * A simple node for pipe connections in a pipe network.
     * @param pos The {@link BlockPos} of the pipe.
     */
    public SimplePipeNetworkNode(BlockPos pos) {
        this.pos = pos;
    }

    @Override
    public BlockPos getPipePos() {
        return pos;
    }

    @Override
    public boolean isEndpoint() {
        return false;
    }

    @Override
    public Direction getPipeSide() {
        return null;
    }

    @Override
    public boolean shouldTick() {
        return false;
    }

    @Override
    public ConnectionMode getConnectionMode() {
        return ConnectionMode.BOTH;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (object == null || getClass() != object.getClass()) return false;
        SimplePipeNetworkNode that = (SimplePipeNetworkNode) object;
        return Objects.equals(pos, that.pos);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(pos);
    }
}
