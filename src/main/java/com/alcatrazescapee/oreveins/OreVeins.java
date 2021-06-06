/*
 * Part of the Realistic Ore Veins Mod by AlcatrazEscapee
 * Work under Copyright. See the project LICENSE.md for details.
 */

package com.alcatrazescapee.oreveins;

import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.WorldGenRegistries;
import net.minecraft.world.gen.GenerationStage.Decoration;
import net.minecraftforge.event.world.BiomeLoadingEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import net.minecraft.world.gen.feature.ConfiguredFeature;
import net.minecraft.world.gen.feature.NoFeatureConfig;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

import com.alcatrazescapee.oreveins.world.ModFeatures;
import com.alcatrazescapee.oreveins.world.VanillaFeatureManager;

import static com.alcatrazescapee.oreveins.OreVeins.MOD_ID;

@Mod(MOD_ID)
public class OreVeins
{
    public static final String MOD_ID = "oreveins";

    private static ConfiguredFeature<?, ?> oreveinsfeature;

    private static final Logger LOGGER = LogManager.getLogger();

    public OreVeins()
    {
        LOGGER.debug("Constructing");

        // Setup config
        Config.register();

        // Register event handlers
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        modEventBus.register(this);
        ModFeatures.FEATURES.register(modEventBus);

        MinecraftForge.EVENT_BUS.register(ForgeEventHandler.INSTANCE);
    }

    @SubscribeEvent
    //@SuppressWarnings("deprecation")
    public void setup(final FMLCommonSetupEvent event)
    {
        LOGGER.debug("Setup");

        /**
         * ORE_SILVER_CONFIG = Registry.register(WorldGenRegistries.CONFIGURED_FEATURE, "ore_silver",
         *             Feature.ORE.withConfiguration(
         *                 new OreFeatureConfig(
         *                     OreFeatureConfig.FillerBlockType.BASE_STONE_OVERWORLD,
         *                     ModBlocks.SILVER_ORE.get().getDefaultState(), 9)
         *             ).range(64).square().func_242731_b(20)
         *         );
         *
         */

        //// expanded and adapted example
        /*
        ConfiguredFeature<OreFeatureConfig, ?> configuredOre = Feature.ORE.configured(new OreFeatureConfig(
                OreFeatureConfig.FillerBlockType.NATURAL_STONE,
                Blocks.STONE.defaultBlockState(), 9)
        );
        configuredOre.range(64).squared();
        // nb: "oreveins" is a ResourceLocation, was "ore_silver" in example
        feature = Registry.register(WorldGenRegistries.CONFIGURED_FEATURE, "oreveins", configuredOre);


         */
        //// real attempt
        ConfiguredFeature<NoFeatureConfig, ?> configuredOreVeins = ModFeatures.VEINS.get().configured(new NoFeatureConfig());
        //noinspection AssignmentToStaticFieldFromInstanceMethod
        oreveinsfeature = Registry.register(WorldGenRegistries.CONFIGURED_FEATURE, "oreveins", configuredOreVeins);

        VanillaFeatureManager.onConfigReloading();

        /*
         * Old (pre 1.16.x) stuff for reference.
         * Also doesn't need thread handling now (pretty sure), since feature addition is fired on its own event
         *
        // World Gen - needs to be ran on main thread to avoid concurrency errors with multiple mods trying to do the same ore generation modifications.
        // Forge fix your stuff and either make not deprecate it or add an alternative.
        DeferredWorkQueue.runLater(() -> {
            ForgeRegistries.BIOMES.forEach(new Consumer<Biome>() {
                @Override
                public void accept(final Biome biome) {
                    ConfiguredFeature<?, ?> feature =
                            ModFeatures.VEINS.get().withConfiguration(new NoFeatureConfig())
                                    .withPlacement(
                                            Placement.NOPE.configure(
                                                    IPlacementConfig.NO_PLACEMENT_CONFIG
                                            )
                                    );
                    biome.addFeature(GenerationStage.Decoration.UNDERGROUND_ORES, feature);
                }
            });
            VanillaFeatureManager.onConfigReloading();
        });
         */
    }

    // Priority high so other mods can modify if required
    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onBiomeLoadingEarly(final BiomeLoadingEvent biomeLoadingEvent) {
        /* From example at https://forums.minecraftforge.net/topic/94945-1164how-to-generate-ores/
        if(biome.getCategory() == Biome.Category.NETHER || biome.getCategory() == Biome.Category.THEEND) return;

        biome.getGeneration().getFeatures(GenerationStage.Decoration.UNDERGROUND_ORES)
                .add(() -> ModFeatures.ORE_SILVER_CONFIG);
         */

        //// new 1.16.x attempt [adapted from FMLCommonSetupEvent above]
        // add VeinsFeature "hook". Hook as in, it is added to all biomes - and a later event dynamically adds and
        // We do filtering in a later event [NORMAL priority].
        biomeLoadingEvent.getGeneration().getFeatures(Decoration.UNDERGROUND_ORES).add(() -> oreveinsfeature);
    }

    @SubscribeEvent
    public void onLoadConfig(final ModConfig.Reloading event)
    {
        LOGGER.debug("Reloading config - reevaluating vanilla ore vein settings");
        if (event.getConfig().getType() == ModConfig.Type.SERVER)
        {
            VanillaFeatureManager.onConfigReloading();
        }
    }
}
