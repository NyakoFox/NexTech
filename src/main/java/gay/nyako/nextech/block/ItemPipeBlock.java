package gay.nyako.nextech.block;

import com.mojang.serialization.MapCodec;
import gay.nyako.nextech.NexTechBlocks;
import gay.nyako.nextech.NexTechEntities;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.InventoryProvider;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
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

    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new ItemPipeBlockEntity(pos, state);
    }

    @Override
    protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        var result = super.onUse(state, world, pos, player, hit);

        if (result != ActionResult.PASS) {
            return result;
        }

        var pipeEntity = world.getBlockEntity(pos, NexTechEntities.ITEM_PIPE);

        if (pipeEntity.isEmpty()) {
            return ActionResult.PASS;
        }

        var extracting = pipeEntity.get().toggleExtracting();

        player.sendMessage(Text.literal("Switched pipe mode to " + (extracting ? "EXTRACT" : "INSERT")), true);

        return ActionResult.SUCCESS;
    }
}
