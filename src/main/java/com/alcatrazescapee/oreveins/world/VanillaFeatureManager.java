/*
 * Part of the Realistic Ore Veins Mod by AlcatrazEscapee
 * Work under Copyright. See the project LICENSE.md for details.
 */

package com.alcatrazescapee.oreveins.world;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import net.minecraft.block.BlockState;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.GenerationStage;
import net.minecraft.world.gen.GenerationStage.Decoration;
import net.minecraft.world.gen.feature.*;
import net.minecraftforge.event.world.BiomeLoadingEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import com.alcatrazescapee.oreveins.Config;

/**
 * Manages the removal and replacement of vanilla world gen features
 * Called on config reload
 *
 * @author AlcatrazEscapee
 */
public class VanillaFeatureManager
{
    private static final Map<ResourceLocation, List<Supplier<ConfiguredFeature<?, ?>>>> DISABLED_FEATURES = new HashMap<>(100);

    private static Set<BlockState> disabledBlockStates;
    private static boolean disableAll;

    public static void onConfigReloading() {
        disabledBlockStates = Config.COMMON.disabledBlockStates();
        disableAll = Config.COMMON.noOres.get();
    }

    /* Old [1.15.x] stuff. It's not called manually anymore, we have an event for it
    public static void onConfigReloading()
    {
        disabledBlockStates = Config.COMMON.disabledBlockStates();
        disableAll = Config.COMMON.noOres.get();

        ForgeRegistries.BIOMES.forEach(biome -> {
            List<ConfiguredFeature<?, ?>> features = DISABLED_FEATURES.computeIfAbsent(biome, key -> new ArrayList<>());

            List<ConfiguredFeature<?, ?>> toReAdd = features.stream().filter(x -> !shouldDisable(x)).collect(Collectors.toList());
            List<ConfiguredFeature<?, ?>> toRemove = biome.getFeatures(GenerationStage.Decoration.UNDERGROUND_ORES).stream().filter(VanillaFeatureManager::shouldDisable).collect(Collectors.toList());

            features.addAll(toRemove);
            features.removeAll(toReAdd);

            List<ConfiguredFeature<?, ?>> currentFeatures = biome.getFeatures(GenerationStage.Decoration.UNDERGROUND_ORES);
            currentFeatures.addAll(toReAdd);
            currentFeatures.removeAll(toRemove);
        });
    }
     */

    // Priority low so we can catch everything
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onBiomeLoadingLate(final BiomeLoadingEvent biomeLoadingEvent) {
        if (biomeLoadingEvent.getName() == null)
            throw new RuntimeException("BiomeLoadingEvent has null name! FIXME! [Regards, CosmicDan]");

        // get cached values, or create empty list if no cached value
        final List<Supplier<ConfiguredFeature<?, ?>>> featuresToDisable = DISABLED_FEATURES.computeIfAbsent(biomeLoadingEvent.getName(), (ResourceLocation key) -> new ArrayList<>(10));

        // build a list of things to keep (not disabled) (cache-based)
        // [CosmicDan] I think this comes first so it re-enables things that were previously disabled...?
        final List<Supplier<ConfiguredFeature<?, ?>>> toReAdd = new ArrayList<>(featuresToDisable.size());
        for (final Supplier<ConfiguredFeature<?, ?>> feature : featuresToDisable) {
            if (!shouldDisable(feature.get()))
                toReAdd.add(feature);
        }

        // build a list of things to disable
        final List<Supplier<ConfiguredFeature<?, ?>>> toRemove = new ArrayList<>(featuresToDisable.size());
        for (final Supplier<ConfiguredFeature<?, ?>> feature : biomeLoadingEvent.getGeneration().getFeatures(Decoration.UNDERGROUND_ORES)) {
            if (shouldDisable(feature.get()))
                toRemove.add(feature);
        }

        // update cache (featuresToDisable) so next runs are faster
        featuresToDisable.addAll(toRemove);
        featuresToDisable.removeAll(toReAdd);

        // now actually add new features, then remove disabled ones for this biome
        final List<Supplier<ConfiguredFeature<?, ?>>> currentFeatures = biomeLoadingEvent.getGeneration().getFeatures(Decoration.UNDERGROUND_ORES);
        currentFeatures.addAll(toReAdd);
        currentFeatures.removeAll(toRemove);
    }

    private static boolean shouldDisable(ConfiguredFeature<?, ?> feature)
    {
        if (feature.config instanceof DecoratedFeatureConfig)
        {
            Feature<?> oreFeature = ((DecoratedFeatureConfig) feature.config).feature.get().feature;
            if (oreFeature == Feature.ORE || oreFeature == Feature.EMERALD_ORE)
            {
                IFeatureConfig featureConfig = ((DecoratedFeatureConfig) feature.config).feature.get().config;
                if (featureConfig instanceof OreFeatureConfig)
                {
                    OreFeatureConfig oreConfig = (OreFeatureConfig) featureConfig;
                    return disableAll || disabledBlockStates.contains(oreConfig.state);
                }
            }
        }
        return false;
    }
}
