package gay.nyako.nextech.mixin;

import gay.nyako.nextech.access.WorldAccess;
import gay.nyako.nextech.network.PipeNetworkManager;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(World.class)
public class WorldMixin implements WorldAccess {
    @Unique
    private PipeNetworkManager pipeNetwork;

    @Override
    public PipeNetworkManager nexTech$getPipeNetworkManager() {
        if (pipeNetwork == null) {
            pipeNetwork = new PipeNetworkManager((World)((Object) this));
        }
        return pipeNetwork;
    }
}
