package fabricish;

import fabricish.block.FabricIshBlocks;
import fabricish.item.FabricIshItems;
import fabricish.server.ServerAdmin;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;

import net.minecraft.resources.Identifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FabricIshMod implements ModInitializer {
	public static final String MOD_ID = "fabric-ish";

	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		LOGGER.info("Hello Fabric-ish world!");

        FabricIshBlocks.initialize();
        FabricIshItems.register();

		// Whitelist/ops sync and pack-update restarts, dedicated servers only.
		ServerLifecycleEvents.SERVER_STARTED.register(server -> {
			if (server.isDedicatedServer()) {
				ServerAdmin.start(server);
			}
		});
		ServerLifecycleEvents.SERVER_STOPPING.register(server -> ServerAdmin.stop());
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> ServerAdmin.afterStop());
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}
