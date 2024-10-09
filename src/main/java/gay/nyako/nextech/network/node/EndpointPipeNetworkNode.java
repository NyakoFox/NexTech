package gay.nyako.nextech.network.node;

import gay.nyako.nextech.network.ConnectionMode;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.Objects;

public class EndpointPipeNetworkNode implements PipeNetworkNode {
    private final BlockPos pipePos;
    private final Direction side;
    private final ConnectionMode connectionMode;
    private final boolean shouldTick;

    /**
     * An endpoint node of a pipe network, used for insertion and extraction.
     * @param pipePos The {@link BlockPos} of the connected pipe (not the endpoint).
     * @param side The side of the pipe this endpoint is connected to.
     * @param connectionMode Whether pipes should transfer into the endpoint, out of the endpoint, or both.
     * @param shouldTick Whether the connected {@link gay.nyako.nextech.block.AbstractPipeBlockEntity}
     *   should receive ticks for this node, for things like extraction.
     */
    public EndpointPipeNetworkNode(BlockPos pipePos, Direction side, ConnectionMode connectionMode, boolean shouldTick) {
        this.pipePos = pipePos;
        this.side = side;
        this.connectionMode = connectionMode;
        this.shouldTick = shouldTick;
    }

    @Override
    public BlockPos getPipePos() {
        return pipePos;
    }

    @Override
    public boolean isEndpoint() {
        return true;
    }

    @Override
    public Direction getPipeSide() {
        return side;
    }

    @Override
    public boolean shouldTick() {
        return shouldTick;
    }

    @Override
    public ConnectionMode getConnectionMode() {
        return connectionMode;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (object == null || getClass() != object.getClass()) return false;
        EndpointPipeNetworkNode that = (EndpointPipeNetworkNode) object;
        return Objects.equals(pipePos, that.pipePos) && side == that.side;
    }

    @Override
    public int hashCode() {
        return Objects.hash(pipePos, side);
    }
}
