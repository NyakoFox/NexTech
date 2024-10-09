package gay.nyako.nextech;

import gay.nyako.nextech.block.AbstractPipeBlockEntity;
import gay.nyako.nextech.network.PipeNetworkManager;
import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerBlockEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NexTech implements ModInitializer {
	public static final String MOD_ID = "nextech";

	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		// This code runs as soon as Minecraft is in a mod-load-ready state.
		// However, some things (like resources) may still be uninitialized.
		// Proceed with mild caution.

		LOGGER.info("Hello Fabric world!");

		NexTechBlocks.register();
		NexTechItems.register();
		NexTechEntities.register();

		ServerTickEvents.START_WORLD_TICK.register(world -> {
			PipeNetworkManager.getInstance(world).tick();
		});

		ServerBlockEntityEvents.BLOCK_ENTITY_LOAD.register((blockEntity, world) -> {
			if (blockEntity instanceof AbstractPipeBlockEntity pipeBlockEntity && !pipeBlockEntity.initializedConnections()) {
				pipeBlockEntity.getPipeNetwork().queueForLoad(pipeBlockEntity);
			}
		});
	}
}