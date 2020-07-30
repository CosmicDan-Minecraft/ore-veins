/*
 * Part of the Realistic Ore Veins Mod by AlcatrazEscapee
 * Work under Copyright. See the project LICENSE.md for details.
 */

package com.alcatrazescapee.oreveins.world.vein;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.function.Predicate;
import java.util.function.Supplier;
import javax.annotation.Nullable;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;
import net.minecraft.block.BlockState;
import net.minecraft.util.JSONUtils;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.dimension.DimensionType;

import com.alcatrazescapee.oreveins.Config;
import com.alcatrazescapee.oreveins.util.collections.IWeightedList;
import com.alcatrazescapee.oreveins.world.rule.DistanceRule;
import com.alcatrazescapee.oreveins.world.rule.IBiomeRule;
import com.alcatrazescapee.oreveins.world.rule.IDimensionRule;
import com.alcatrazescapee.oreveins.world.rule.IRule;

public abstract class VeinType<V extends Vein<?>>
{
    protected final int verticalSize;
    protected final int horizontalSize;
    protected final float density;

    private final int count;
    private final int rarity;
    private final int minY;
    private final int maxY;

    private final IBiomeRule biomeRule;
    private final IDimensionRule dimensions;
    private final Predicate<BlockPos> originDistance;
    private final List<IRule> rules;
    private final IWeightedList<Indicator> indicator;

    protected VeinType(JsonObject json, JsonDeserializationContext context) throws JsonParseException
    {
        count = JSONUtils.getInt(json, "count", 1);
        if (count <= 0)
        {
            throw new JsonParseException("Count must be > 0.");
        }
        rarity = JSONUtils.getInt(json, "rarity", 10);
        if (rarity <= 0)
        {
            throw new JsonParseException("Count must be > 0.");
        }
        minY = JSONUtils.getInt(json, "min_y", 16);
        maxY = JSONUtils.getInt(json, "max_y", 64);
        if (minY < 0 || maxY > 256 || minY > maxY)
        {
            throw new JsonParseException("Min Y and Max Y must be within [0, 256], and Min Y must be <= Max Y.");
        }
        verticalSize = JSONUtils.getInt(json, "vertical_size", 8);
        if (verticalSize <= 0)
        {
            throw new JsonParseException("Vertical Size must be > 0.");
        }
        horizontalSize = JSONUtils.getInt(json, "horizontal_size", 15);
        if (horizontalSize <= 0)
        {
            throw new JsonParseException("Horizontal Size must be > 0.");
        }
        density = JSONUtils.getInt(json, "density", 20);
        if (density <= 0)
        {
            throw new JsonParseException("Density must be > 0.");
        }

        biomeRule = json.has("biomes") ? context.deserialize(json.get("biomes"), IBiomeRule.class) : IBiomeRule.DEFAULT;
        dimensions = json.has("dimensions") ? context.deserialize(json.get("dimensions"), IDimensionRule.class) : IDimensionRule.DEFAULT;
        originDistance = json.has("origin_distance") ? context.deserialize(json.get("origin_distance"), DistanceRule.class) : DistanceRule.DEFAULT;
        rules = json.has("rules") ? context.deserialize(json.get("rules"), new TypeToken<List<IRule>>() {}.getType()) : Collections.emptyList();
        indicator = json.has("indicator") ? context.deserialize(json.get("indicator"), new TypeToken<IWeightedList<Indicator>>() {}.getType()) : IWeightedList.empty();
    }

    /**
     * Gets the state to generate at a point.
     * Handled by {@link VeinType} using a weighted list
     *
     * @return A block state
     */
    public abstract BlockState getStateToGenerate(V vein, BlockPos pos, Random random);

    /**
     * Gets all possible ore states spawned by this vein.
     * Used for command vein searching / world stripping
     *
     * @return a collection of block states
     */
    public abstract Collection<BlockState> getOreStates();

    /**
     * Gets an indicator for this vein type
     *
     * @param random A random to use to select an indicator
     * @return An Indicator if it exists, or null if not
     */
    @Nullable
    public Indicator getIndicator(Random random)
    {
        return indicator != null ? indicator.get(random) : null;
    }

    /**
     * If the vein can generate on the previous state
     *
     * @param world The world
     * @param pos   The position to generate at
     * @return if the vein can generate
     */
    public boolean canGenerateAt(IBlockReader world, BlockPos pos)
    {
        if (rules != null)
        {
            for (IRule rule : rules)
            {
                if (!rule.test(world, pos))
                {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Is the vein in range of a vertical column with specific offsets
     * This should be a simple check for optimization purposes
     *
     * @param vein    The vein instance
     * @param xOffset The x offset
     * @param zOffset The y offset
     * @return if the vein can generate any blocks in this column
     */
    public boolean inRange(V vein, int xOffset, int zOffset)
    {
        return xOffset * xOffset + zOffset * zOffset < horizontalSize * horizontalSize;
    }

    /**
     * Is the vein valid at a specific origin position?
     * Returning false here stops the entire generation of the vein
     */
    public boolean isValidPos(BlockPos pos)
    {
        // Only thing that uses this right now is an origin distance test
        return originDistance.test(pos);
    }

    /**
     * Check if the dimension is valid for this vein
     *
     * @param dimension a dimension
     * @return true if the dimension is valid
     */
    public boolean matchesDimension(DimensionType dimension)
    {
        return dimensions.test(dimension);
    }

    /**
     * Check if the biome is valid for this vein
     *
     * @param biome a biome
     * @return true if the biome is valid
     */
    public boolean matchesBiome(Supplier<Biome> biome)
    {
        // This is here to avoid querying for the biome in the case we don't need it
        return biomeRule == IBiomeRule.DEFAULT || biomeRule.test(biome.get());
    }

    /**
     * Gets the min Y which this vein can spawn at
     *
     * @return a Y position
     */
    public int getMinY()
    {
        return minY;
    }

    /**
     * Gets the max Y which this vein can spawn at
     *
     * @return a Y position
     */
    public int getMaxY()
    {
        return maxY;
    }

    /**
     * Gets the number of rolls for a chunk
     *
     * @return a number in [1...]
     */
    public int getCount()
    {
        return count;
    }

    /**
     * Gets the rarity of this vein in a chunk
     *
     * @return a number in [1...]
     */
    public int getRarity()
    {
        return rarity;
    }

    /**
     * Gets the max chunk radius that this vein needs to check
     *
     * @return a radius in chunks
     */
    public int getChunkRadius()
    {
        return 1 + (horizontalSize >> 4);
    }

    @Override
    public String toString()
    {
        return String.format("[%s: Count: %d, Rarity: %d, Y: %d - %d, Size: %d / %d, Density: %2.2f", VeinManager.INSTANCE.getName(this), count, rarity, minY, maxY, horizontalSize, verticalSize, density);
    }

    /**
     * Gets the chance to generate at a specific location
     *
     * @param vein the vein instance
     * @param pos  the position
     * @return a chance: 0 = 0% chance, 1 = 100% chance
     */
    public abstract float getChanceToGenerate(V vein, BlockPos pos);

    /**
     * Creates veins for this type for a given chunk position and random.
     * This is called after rarity + chance rolls are done.
     */
    public abstract void createVeins(List<Vein<?>> veins, int chunkX, int chunkZ, Random random);

    protected final BlockPos defaultStartPos(int chunkX, int chunkZ, Random rand)
    {
        int spawnRange = maxY - minY, minRange = minY;
        if (Config.COMMON.avoidVeinCutoffs.get())
        {
            if (verticalSize * 2 < spawnRange)
            {
                spawnRange -= verticalSize * 2;
                minRange += verticalSize;
            }
            else
            {
                minRange = minY + (maxY - minY) / 2;
                spawnRange = 1;
            }
        }
        return new BlockPos(chunkX * 16 + rand.nextInt(16), minRange + rand.nextInt(spawnRange), chunkZ * 16 + rand.nextInt(16));
    }
}
