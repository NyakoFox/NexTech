package gay.nyako.nextech.network;

import gay.nyako.nextech.access.WorldAccess;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;

import java.math.BigInteger;
import java.util.HashMap;

public class PipeNetworkManager {
    public static PipeNetworkManager getInstance(World world) {
        return ((WorldAccess) world).nexTech$getPipeNetworkManager();
    }

    private final World world;
    private final HashMap<Identifier, PipeNetwork> pipeNetworks;

    private BigInteger tickCount = BigInteger.ZERO;

    public PipeNetworkManager(World world) {
        this.world = world;
        this.pipeNetworks = new HashMap<>(3);
    }

    public void tick() {
        tickCount = tickCount.add(BigInteger.ONE);

        for (var network : pipeNetworks.values()) {
            network.tick(tickCount);
        }
    }

    public PipeNetwork getPipeNetwork(Identifier networkId) {
        return pipeNetworks.computeIfAbsent(networkId, k -> new PipeNetwork(networkId, world));
    }
}
