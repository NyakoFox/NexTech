package gay.nyako.nextech.block;

import gay.nyako.nextech.NexTech;
import gay.nyako.nextech.NexTechEntities;
import gay.nyako.nextech.network.PipeNetwork;
import gay.nyako.nextech.network.ConnectionMode;
import gay.nyako.nextech.network.node.EndpointPipeNetworkNode;
import gay.nyako.nextech.network.node.PipeNetworkNode;
import gay.nyako.nextech.network.node.SimplePipeNetworkNode;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.block.BlockState;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.math.BigInteger;
import java.util.ArrayList;

public class ItemPipeBlockEntity extends AbstractPipeBlockEntity {
    // Arbitrary testing values until we have pipe tiers / options
    private static long EXTRACT_RATE = 10; // ticks/second
    private static long EXTRACT_AMOUNT = 1; // items/tick

    private final PipeNetworkNode pipeNetworkNode;

    private boolean extracting;

    public ItemPipeBlockEntity(BlockPos pos, BlockState state) {
        super(NexTechEntities.ITEM_PIPE, pos, state);

        pipeNetworkNode = new SimplePipeNetworkNode(pos);

        extracting = false;
    }

    @Override
    public boolean connectsTo(Direction side, BlockPos targetPos) {
        // // Connect to either blocks with inventories...
        // if (world.getBlockState(targetPos).getBlock() instanceof InventoryProvider) return true;
        // // ...or block entities with inventories.
        // if (blockEntity instanceof Inventory) return true;
        // // Otherwise, there's no way to put items in it, bail
        // return false;
        var storage = ItemStorage.SIDED.find(world, targetPos, side);
        return storage != null && (storage.supportsInsertion() || (extracting && storage.supportsExtraction()));
    }

    @Override
    public void tickNetwork(PipeNetwork network, PipeNetworkNode node, Direction side, BigInteger tickCount) {
        if (!extracting) {
            return;
        }

        if (!tickCount.mod(BigInteger.valueOf(EXTRACT_RATE)).equals(BigInteger.ZERO)) {
            return;
        }

        NexTech.LOGGER.info("Ticking extraction at [{}] {}", pos.toShortString(), side.toString());

        var targetPos = pos.offset(side);

        var inputStorage = ItemStorage.SIDED.find(world, targetPos, side.getOpposite());

        if (inputStorage == null || !inputStorage.supportsExtraction()) {
            return;
        }

        var outputStorages = new ArrayList<Storage<ItemVariant>>();

        var networkDestinations = network.getDestinations(node);

        for (var networkDestination : networkDestinations) {
            var destNode = networkDestination.node();

            var outputStorage = ItemStorage.SIDED.find(world, destNode.getBlockPos(), destNode.getPipeSide().getOpposite());

            if (outputStorage != null && outputStorage.supportsInsertion()) {
                outputStorages.add(outputStorage);
            }
        }

        if (outputStorages.isEmpty()) {
            return;
        }

        long maxExtract = EXTRACT_AMOUNT;

        try (var transaction = Transaction.openOuter()) {
            for (var view : inputStorage.nonEmptyViews()) {
                if (maxExtract <= 0) {
                    break;
                }

                var resource = view.getResource();

                long toExtract;

                try (var testExtraction = transaction.openNested()) {
                    toExtract = view.extract(resource, Math.min(view.getAmount(), maxExtract), testExtraction);
                }

                long totalExtracted = 0;

                for (var outputStorage : outputStorages) {
                    if (toExtract <= 0) {
                        break;
                    }

                    var inserted = outputStorage.insert(resource, toExtract, transaction);

                    if (inserted > 0) {
                        view.extract(resource, inserted, transaction);

                        toExtract -= inserted;
                        totalExtracted += inserted;

                        transaction.commit();
                    }
                }

                maxExtract -= totalExtracted;
            }
        }
    }

    @Override
    public Identifier getPipeNetworkId() {
        return PipeNetwork.ITEMS;
    }

    @Override
    public PipeNetworkNode getConnectedPipeNetworkNode(Direction side, BlockPos targetPos) {
        var connectionMode = extracting ? ConnectionMode.OUT : ConnectionMode.BOTH;
        var shouldTick = extracting;

        return new EndpointPipeNetworkNode(pos, side, connectionMode, shouldTick);
    }

    @Override
    public PipeNetworkNode getPipeNetworkNode() {
        return pipeNetworkNode;
    }

    @Override
    protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        if (nbt.contains("extracting")) {
            this.extracting = nbt.getBoolean("extracting");
        }

        super.readNbt(nbt, registryLookup);
    }

    @Override
    protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        nbt.putBoolean("extracting", this.extracting);

        super.writeNbt(nbt, registryLookup);
    }

    public boolean toggleExtracting() {
        extracting = !extracting;

        updateConnections();

        return extracting;
    }
}
