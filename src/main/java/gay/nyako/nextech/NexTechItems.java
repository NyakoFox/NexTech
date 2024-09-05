package gay.nyako.nextech;

import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class NexTechItems {
    public static final Item PIPE = register("pipe", new BlockItem(NexTechBlocks.PIPE, new Item.Settings()));

    public static Item register(String id, Item item) {
        return Registry.register(Registries.ITEM, Identifier.of("nextech", id), item);
    }

    public static void register() {

    }
}
