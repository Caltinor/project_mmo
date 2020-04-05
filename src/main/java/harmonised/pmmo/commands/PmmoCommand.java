package harmonised.pmmo.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.command.ISuggestionProvider;
import net.minecraft.entity.player.ServerPlayerEntity;

public class PmmoCommand
{
    public static void register( CommandDispatcher<CommandSource> dispatch )
    {
        String[] suggestCommand = new String[2];
        suggestCommand[0] = "set";
        suggestCommand[1] = "clear";
//        suggestCommand[2] = "levelAtXp";
//        suggestCommand[3] = "xpAtLevel";

        String[] suggestSkill = new String[14];
        suggestSkill[0] = "Mining";
        suggestSkill[1] = "Building";
        suggestSkill[2] = "Excavation";
        suggestSkill[3] = "Woodcutting";
        suggestSkill[4] = "Farming";
        suggestSkill[5] = "Agility";
        suggestSkill[6] = "Endurance";
        suggestSkill[7] = "Combat";
        suggestSkill[8] = "Archery";
        suggestSkill[9] = "Repairing";
        suggestSkill[10] = "Flying";
        suggestSkill[11] = "Swimming";
        suggestSkill[12] = "Fishing";
        suggestSkill[13] = "Crafting";

//        String[] suggestClear = new String[1];
//        suggestClear[0] = "iagreetothetermsandconditions";

        LiteralArgumentBuilder<CommandSource> builder = Commands.literal("pmmo")
                .requires( src -> src.hasPermissionLevel(4 ) )
                .requires( src -> src.getEntity() instanceof ServerPlayerEntity )
                .then( Commands.argument("command", StringArgumentType.word() )
                .suggests( ( ctx, theBuilder ) -> ISuggestionProvider.suggest( suggestCommand, theBuilder ) )
                .executes( CommandClear::execute ) );

        builder.then( Commands.argument("set", StringArgumentType.word() )
                .then( Commands.argument("skill", StringArgumentType.word() )
                .suggests( ( ctx, theBuilder ) -> ISuggestionProvider.suggest( suggestSkill, theBuilder ) )
                .then( Commands.argument("request", StringArgumentType.word() )
                .executes( CommandSet::execute ) ) ) );

        builder.then( Commands.argument("clear", StringArgumentType.word() )
                .executes( CommandClear::execute )
                .then( Commands.argument("request", StringArgumentType.word() )
//                .suggests( ( ctx, theBuilder ) -> ISuggestionProvider.suggest( suggestClear, theBuilder ) )
                .suggests( ( ctx, theBuilder ) -> ISuggestionProvider.suggest( suggestSkill, theBuilder ) )
                .executes( CommandClear::execute ) ) );

//        builder.then( Commands.argument("levelAtXp", StringArgumentType.word() )
//                .then( Commands.argument("request", StringArgumentType.word() )
//                .executes( CommandLevelAtXp::execute ) ) );
//
//        builder.then( Commands.argument("xpAtLevel", StringArgumentType.word() )
//                .then( Commands.argument("request", StringArgumentType.word() )
//                .executes( CommandXpAtLevel::execute ) ) );

        dispatch.register( builder );
    }
}
