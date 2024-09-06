package gay.nyako.nextech;

import gay.nyako.nextech.block.ItemPipeBlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class NexTechEntities {
    public static final BlockEntityType<ItemPipeBlockEntity> ITEM_PIPE = register(
            "item_pipe",
            BlockEntityType.Builder.create(ItemPipeBlockEntity::new, NexTechBlocks.ITEM_PIPE).build()
    );

    public static <T extends BlockEntityType<?>> T register(String path, T blockEntityType) {
        return Registry.register(Registries.BLOCK_ENTITY_TYPE, Identifier.of("nextech", path), blockEntityType);
    }

    public static void register() {
    }
}
