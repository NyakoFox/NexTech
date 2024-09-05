package gay.nyako.nextech.block;

import gay.nyako.nextech.NexTechEntities;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.jetbrains.annotations.Nullable;

public class PipeBlockEntity extends BlockEntity {
    private boolean[] connections = {false, false, false, false, false, false};

    public PipeBlockEntity(BlockPos pos, BlockState state) {
        super(NexTechEntities.PIPE, pos, state);
    }

    public void setConnections(boolean[] currentConnections) {
        this.connections = currentConnections;
    }

    @Override
    protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        boolean[] currentConnections = {false, false, false, false, false, false};
        for (int i = 0; i < currentConnections.length; i++)
        {
            Direction direction = Direction.byId(i);
            if (nbt.contains(direction.name()))
            {
                currentConnections[i] = nbt.getBoolean(direction.name());
            }
        }
        setConnections(currentConnections);

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
        for (int i = 0; i < connections.length; i++)
        {
            Direction direction = Direction.byId(i);
            nbt.putBoolean(direction.name(), connections[i]);
        }
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
