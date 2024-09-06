package gay.nyako.nextech.block;

import gay.nyako.nextech.network.Connections;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.jetbrains.annotations.Nullable;

public abstract class AbstractPipeBlockEntity extends BlockEntity {
    private Connections connections = new Connections();

    public AbstractPipeBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public void setConnections(Connections currentConnections) {
        this.connections = currentConnections;
    }

    public boolean isSideConnected(Direction direction)
    {
        return this.connections.hasConnection(direction);
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
