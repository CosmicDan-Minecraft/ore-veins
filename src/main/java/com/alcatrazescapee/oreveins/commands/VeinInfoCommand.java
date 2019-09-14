/*
 * Part of the Realistic Ore Veins Mod by AlcatrazEscapee
 * Work under Copyright. See the project LICENSE.md for details.
 */

package com.alcatrazescapee.oreveins.commands;

import javax.annotation.ParametersAreNonnullByDefault;

import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TranslationTextComponent;

import com.alcatrazescapee.oreveins.world.vein.VeinManager;
import com.alcatrazescapee.oreveins.world.vein.VeinType;
import com.mojang.brigadier.CommandDispatcher;

import static com.alcatrazescapee.oreveins.OreVeins.MOD_ID;

@ParametersAreNonnullByDefault
public final class VeinInfoCommand
{
    public static void register(CommandDispatcher<CommandSource> dispatcher)
    {
        dispatcher.register(
                Commands.literal("veininfo").requires(source -> source.hasPermissionLevel(2))
                        .then(Commands.argument("type", new VeinTypeArgument())
                                .executes(cmd -> veinInfo(cmd.getSource(), VeinTypeArgument.getVein(cmd, "type")))
                        ));
    }

    private static int veinInfo(CommandSource source, ResourceLocation veinName)
    {
        source.sendFeedback(new TranslationTextComponent(MOD_ID + ".tooltip.registered_veins"), true);

        // Search for veins that match a type
        final VeinType<?> type = VeinManager.INSTANCE.getVein(veinName);
        if (type == null)
        {
            source.sendErrorMessage(new TranslationTextComponent(MOD_ID + ".tooltip.unknown_vein", veinName));
        }
        else
        {
            source.sendFeedback(new StringTextComponent("> Vein Type: " + type.toString()), true);
        }
        return 1;
    }
}
