package gay.nyako.nextech;

import gay.nyako.nextech.block.PipeBlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class NexTechEntities {
    public static final BlockEntityType<PipeBlockEntity> PIPE = register(
            "pipe",
            BlockEntityType.Builder.create(PipeBlockEntity::new, NexTechBlocks.PIPE).build()
    );

    public static <T extends BlockEntityType<?>> T register(String path, T blockEntityType) {
        return Registry.register(Registries.BLOCK_ENTITY_TYPE, Identifier.of("nextech", path), blockEntityType);
    }

    public static void register() {
    }
}
