/*
 * Part of the Realistic Ore Veins Mod by AlcatrazEscapee
 * Work under Copyright. See the project LICENSE.md for details.
 */

package com.alcatrazescapee.oreveins.world.rule;

import java.util.Objects;
import java.util.function.Predicate;

import net.minecraft.util.ResourceLocation;

import com.alcatrazescapee.oreveins.util.json.PredicateDeserializer;
import net.minecraft.util.registry.DynamicRegistries;
import net.minecraft.world.DimensionType;

@FunctionalInterface
public interface IDimensionRule extends Predicate<DimensionType>
{
    IDimensionRule DEFAULT = (DimensionType dim) -> dim == DimensionType.DEFAULT_OVERWORLD;

    class Deserializer extends PredicateDeserializer<DimensionType, IDimensionRule>
    {
        public static final Deserializer INSTANCE = new Deserializer();

        private Deserializer()
        {
            super(IDimensionRule.class, "dimensions");
        }

        @Override
        protected IDimensionRule createSingleRule(final String nameIn)
        {
            //final ResourceLocation typeName = new ResourceLocation(nameIn);
            //return type -> typeName.equals(type.getRegistryName());
            // TODO: test this. Just make it dump the string names to console...?
            final DimensionType typeIn = DynamicRegistries.builtin().dimensionTypes().get(new ResourceLocation(nameIn));
            return (DimensionType type) -> Objects.equals(typeIn, type);
        }

        @Override
        protected IDimensionRule createPredicate(Predicate<DimensionType> predicate)
        {
            return predicate::test;
        }
    }
}
