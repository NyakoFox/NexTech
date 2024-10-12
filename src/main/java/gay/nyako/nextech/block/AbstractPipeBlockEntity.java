package gay.nyako.nextech.block;

import gay.nyako.nextech.network.Connections;
import gay.nyako.nextech.network.PipeNetwork;
import gay.nyako.nextech.network.ConnectionMode;
import gay.nyako.nextech.network.node.PipeNetworkNode;
import gay.nyako.nextech.network.node.PipeNetworkNodeProvider;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.jetbrains.annotations.Nullable;

import java.math.BigInteger;

public abstract class AbstractPipeBlockEntity extends BlockEntity implements PipeNetworkNodeProvider {
    private Connections connections = new Connections();

    private boolean initializedConnections = false;

    public AbstractPipeBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public void setConnections(Connections connections) {
        if (!world.isClient) {
            // Update connections in the network
            var network = getPipeNetwork();

            network.removeNode(this);

            for (Direction direction : Direction.values()) {
                var targetPos = pos.offset(direction);
                var targetBlockEntity = world.getBlockEntity(targetPos);

                if (connections.hasConnection(direction)) {
                    var connection = connections.getConnection(direction);

                    PipeNetworkNode connectedNode = null;

                    if (connection.isPipe) {
                        var blockEntity = world.getBlockEntity(targetPos);

                        if (blockEntity instanceof PipeNetworkNodeProvider nodeProvider) {
                            connectedNode = nodeProvider.getPipeNetworkNode();
                        }
                    } else {
                        connectedNode = getConnectedPipeNetworkNode(direction, targetPos);
                    }

                    if (connectedNode == null) {
                        continue;
                    }

                    network.addConnection(this, connectedNode);
                }
            }
        }

        this.connections = connections;
        markDirty();
    }

    public boolean isSideConnected(Direction side) {
        return this.connections.hasConnection(side);
    }

    public Connections.Connection getConnection(Direction side) {
        return this.connections.getConnection(side);
    }

    public abstract boolean connectsTo(Direction side, BlockPos targetPos);

    public abstract void tickNetwork(PipeNetwork network, PipeNetworkNode node, Direction side, BigInteger tickCount);

    public abstract Identifier getPipeNetworkId();

    public abstract PipeNetworkNode getConnectedPipeNetworkNode(Direction side, BlockPos targetPos);

    public PipeNetwork getPipeNetwork() {
        return PipeNetwork.getInstance(getPipeNetworkId(), world);
    }

    public boolean initializedConnections() {
        return initializedConnections;
    }

    public void updateConnections() {
        var connections = new Connections();

        // Calculate new connections
        for (Direction direction : Direction.values()) {
            var targetPos = pos.offset(direction);
            var targetBlockEntity = world.getBlockEntity(targetPos);

            if (targetBlockEntity instanceof AbstractPipeBlockEntity pipeBlockEntity) {
                if (pipeBlockEntity.getPipeNetworkId() == getPipeNetworkId()) {
                    connections.addConnection(direction, new Connections.Connection(targetPos, true));
                }
                continue;
            }

            if (connectsTo(direction, targetPos)) {
                connections.addConnection(direction, new Connections.Connection(targetPos, false));
            }
        }

        initializedConnections = true;

        setConnections(connections);
    }

    @Override
    protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        if (nbt.contains("connections")) {
            this.connections.read(nbt.getCompound("connections"));
        }

        if (world != null && world.isClient) {
            world.updateListeners(pos, null, null, 0);
        }
        if (world != null)
        {
            markDirty();
        }
    }

    @Override
    protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        nbt.put("connections", this.connections.write());
    }

    @Override
    public @Nullable Object getRenderData() {
        return connections;
    }

    @Override
    public Packet<ClientPlayPacketListener> toUpdatePacket() {
        return BlockEntityUpdateS2CPacket.create(this);
    }

    @Override
    public NbtCompound toInitialChunkDataNbt(RegistryWrapper.WrapperLookup registryLookup) {
        return createNbt(registryLookup);
    }

    @Override
    public void markDirty() {
        super.markDirty();
        if (!world.isClient()) {
            sync();
        }
    }

    public void sync() {
        if (world.isClient()) {
            System.out.println("don't run sync() on the client!!!!!!! what are u doing!!!!!");
            return;
        }
        ((ServerWorld) world).getChunkManager().markForUpdate(getPos());
    }
}
