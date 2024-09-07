package com.ibarnstormer.projectomnipotence;

import com.ibarnstormer.projectomnipotence.config.ModConfig;
import com.ibarnstormer.projectomnipotence.network.payload.SyncSSDHDataPayload;
import com.ibarnstormer.projectomnipotence.utils.POCreativeTab;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.impl.networking.PayloadTypeRegistryImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main implements ModInitializer {
	public static final String MODID = "projectomnipotence";
	public static final Logger LOGGER = LoggerFactory.getLogger(MODID);
	public static ModConfig CONFIG = ModConfig.initConfig();
	public static final int CONFIG_VERSION = 1;


	@Override
	public void onInitialize() {
		LOGGER.info("Thank you for installing Project Omnipotence, enjoy!");

		POCreativeTab.init();

		// Network Payload registry
		PayloadTypeRegistry.playS2C().register(SyncSSDHDataPayload.ID, SyncSSDHDataPayload.CODEC);

		// Check config values
		if(CONFIG.luckLevelEntityGoal <= 0) {
			LOGGER.error("luckLevelEntityGoal has to be greater than 0, resetting to default value: 250.");
			CONFIG.luckLevelEntityGoal = 250;
		}

		if(CONFIG.totalLuckLevels < 0) {
			LOGGER.error("totalLuckLevels has to be greater than or equal to 0, resetting to default value: 3.");
			CONFIG.totalLuckLevels = 3;
		}

		if(CONFIG.invulnerabilityEntityGoal < 0) {
			LOGGER.error("invulnerabilityEntityGoal has to be greater than or equal to 0, resetting to default value: 1000");
			CONFIG.invulnerabilityEntityGoal = 1000;
		}

		if(CONFIG.flightEntityGoal < 0) {
			LOGGER.error("flightEntityGoal has to be greater than or equal to 0, resetting to default value: 10000");
			CONFIG.flightEntityGoal = 10000;
		}

	}
}
