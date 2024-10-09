package gay.nyako.nextech.network.node;

import gay.nyako.nextech.block.AbstractPipeBlockEntity;
import gay.nyako.nextech.network.ConnectionMode;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

public interface PipeNetworkNode extends PipeNetworkNodeProvider {
    /**
     * @return The {@link BlockPos} of the relevant pipe (not the endpoint).
     */
    BlockPos getPipePos();

    /**
     * @return Whether the node represents an input/output for the network (e.g. inventories, machines).
     */
    boolean isEndpoint();

    /**
     * @return The side of the pipe this endpoint is connected to, or null if this node is not an endpoint.
     */
    Direction getPipeSide();

    /**
     * @return Whether the connected {@link gay.nyako.nextech.block.AbstractPipeBlockEntity}
     *  should receive ticks for this node, for things like extraction.
     *  Only affects endpoints.
     */
    boolean shouldTick();

    /**
     * @return Whether the node should connect IN, OUT, or BOTH.
     *  Only affects endpoints.
     */
    ConnectionMode getConnectionMode();

    /**
     * @return The {@link BlockPos} of the endpoint or pipe.
     */
    default BlockPos getBlockPos() {
        var direction = getPipeSide();

        if (direction != null) {
            return getPipePos().offset(direction);
        } else {
            return getPipePos();
        }
    }

    default AbstractPipeBlockEntity getPipeBlockEntity(World world) {
        return (AbstractPipeBlockEntity) world.getBlockEntity(getPipePos());
    }

    @Override
    default PipeNetworkNode getPipeNetworkNode() {
        return this;
    }
}
