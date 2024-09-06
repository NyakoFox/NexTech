package gay.nyako.nextech;

import gay.nyako.nextech.block.ItemPipeBlock;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class NexTechBlocks {
    public static final Block ITEM_PIPE = register("item_pipe", new ItemPipeBlock(AbstractBlock.Settings.create().requiresTool().luminance(x -> 15)));

    public static Block register(String id, Block block) {
        return Registry.register(Registries.BLOCK, Identifier.of("nextech", id), block);
    }

    public static void register() {

    }
}
