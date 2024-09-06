package gay.nyako.nextech.block;

import gay.nyako.nextech.NexTechEntities;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;

public class ItemPipeBlockEntity extends AbstractPipeBlockEntity {
    public ItemPipeBlockEntity(BlockPos pos, BlockState state) {
        super(NexTechEntities.ITEM_PIPE, pos, state);
    }
}
