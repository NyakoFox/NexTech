package gay.nyako.nextech.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.InventoryProvider;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class ItemPipeBlock extends AbstractPipeBlock {
    public ItemPipeBlock(Settings settings) {
        super(settings);
    }

    @Override
    protected MapCodec<? extends BlockWithEntity> getCodec() {
        return createCodec(ItemPipeBlock::new);
    }

    @Override
    public boolean connectsTo(World world, BlockPos pos) {
        // Connect to either blocks with inventories...
        if (world.getBlockState(pos).getBlock() instanceof InventoryProvider) return true;
        // ...or block entities with inventories.
        if (world.getBlockEntity(pos) instanceof Inventory) return true;
        // Otherwise, there's no way to put items in it, bail
        return false;
    }


    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new ItemPipeBlockEntity(pos, state);
    }
}
