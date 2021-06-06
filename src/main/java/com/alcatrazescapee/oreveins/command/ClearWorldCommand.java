/*
 * Part of the Realistic Ore Veins Mod by AlcatrazEscapee
 * Work under Copyright. See the project LICENSE.md for details.
 */


package com.alcatrazescapee.oreveins.command;

import java.util.HashSet;
import java.util.Set;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;

import com.alcatrazescapee.oreveins.world.vein.VeinManager;
import com.alcatrazescapee.oreveins.world.vein.VeinType;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.world.server.ServerWorld;

import static com.alcatrazescapee.oreveins.OreVeins.MOD_ID;

public final class ClearWorldCommand
{
    private static final Set<BlockState> VEIN_STATES = new HashSet<>();

    public static void resetVeinStates()
    {
        VeinManager.INSTANCE.getVeins().stream().map(VeinType::getOreStates).forEach(VEIN_STATES::addAll);
    }

    public static void register(CommandDispatcher<CommandSource> dispatcher)
    {
        dispatcher.register(
            Commands.literal("clearworld").requires(source -> source.hasPermission(2))
                .then(Commands.argument("radius", IntegerArgumentType.integer(1, 250))
                    .executes(cmd -> clearWorld(cmd.getSource(), IntegerArgumentType.getInteger(cmd, "radius")))));
    }

    private static int clearWorld(CommandSource source, int radius)
    {
        final ServerWorld world = source.getLevel();
        final BlockPos center = new BlockPos(source.getPosition());
        final BlockState air = Blocks.AIR.defaultBlockState();

        for (BlockPos pos : BlockPos.Mutable.betweenClosed(center.offset(-radius, 255 - center.getY(), -radius), center.offset(radius, -center.getY(), radius)))
        {
            if (!VEIN_STATES.contains(world.getBlockState(pos)))
            {
                world.setBlock(pos, air, 2 | 16);
            }
        }

        source.sendSuccess(new TranslationTextComponent(MOD_ID + ".command.clear_world_done"), true);
        return 1;
    }
}
