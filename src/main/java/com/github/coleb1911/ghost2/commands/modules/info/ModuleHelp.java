package com.github.coleb1911.ghost2.commands.modules.info;

import com.github.coleb1911.ghost2.Ghost2Application;
import com.github.coleb1911.ghost2.commands.CommandRegistry;
import com.github.coleb1911.ghost2.commands.meta.CommandContext;
import com.github.coleb1911.ghost2.commands.meta.CommandType;
import com.github.coleb1911.ghost2.commands.meta.Module;
import com.github.coleb1911.ghost2.commands.meta.ModuleInfo;
import discord4j.core.DiscordClient;
import discord4j.core.object.entity.User;

import java.time.Instant;
import java.util.*;

// TODO: Add a text-only help list if the bot is unable to send embeds
// Apparently there's a way to disable embeds through permissions, but I can't find the related permission.
// I'd like to just get all of this new code pushed out instead of wasting my time digging around in
// Discord's awful API documentation for 2 hours just to fix one module.
public class ModuleHelp extends Module {
    public ModuleHelp() {
        super(new ModuleInfo.Builder(ModuleHelp.class)
                .withName("help")
                .withDescription("List commands or get help with a specific command"));
    }

    @Override
    public void invoke(CommandContext ctx) {
        DiscordClient client = Ghost2Application.getApplicationInstance().getClient();
        User self = client.getSelf().block();
        assert self != null;

        // Single-command help
        if (ctx.getArgs().size() > 0) {
            // Fetch & null-check CommandInfo
            ModuleInfo info = CommandRegistry.getInfo(ctx.getArgs().get(0));
            if (null == info) {
                ctx.reply(Module.REPLY_COMMAND_INVALID);
                return;
            }

            // Build and send embed
            ctx.getChannel().createMessage(messageSpec -> messageSpec.setEmbed(embedSpec -> {
                String aliasList;
                if (info.getAliases().length == 0) {
                    aliasList = "n/a";
                } else {
                    StringJoiner joiner = new StringJoiner(", ");
                    for (String alias : info.getAliases())
                        joiner.add(alias);
                    aliasList = joiner.toString();
                }

                embedSpec.setTitle("Command help");
                embedSpec.addField("Name", info.getName(), false);
                embedSpec.addField("Description", info.getDescription(), false);
                embedSpec.addField("Aliases", aliasList, false);
                embedSpec.addField("Category", info.getType().getFormattedName(), false);
            })).block();
            // Full command list
        } else {
            // Categorize all available modules
            Map<CommandType, List<ModuleInfo>> modules = new LinkedHashMap<>();
            for (CommandType type : CommandType.values()) modules.put(type, new ArrayList<>());
            for (ModuleInfo info : CommandRegistry.getAllInfo()) modules.get(info.getType()).add(info);

            // Build and send embed
            ctx.getChannel().createMessage(messageSpec -> messageSpec.setEmbed(embedSpec -> {
                embedSpec.setTitle("Help");
                embedSpec.setAuthor(self.getUsername(), null, self.getAvatarUrl());
                embedSpec.setFooter("See g!help <command> for help with specific commands", null);
                embedSpec.setTimestamp(Instant.now());

                for (Map.Entry<CommandType, List<ModuleInfo>> module : modules.entrySet()) {
                    String commandList;
                    if (module.getValue().isEmpty()) {
                        commandList = "No commands (...yet)";
                    } else {
                        StringJoiner joiner = new StringJoiner(", ");
                        for (ModuleInfo info : module.getValue())
                            joiner.add(String.format("`%s`", info.getName()));
                        commandList = joiner.toString();
                    }

                    CommandType type = module.getKey();
                    embedSpec.addField(type.getIcon() + " " + type.getFormattedName(), commandList, false);
                }
            })).block();
        }
    }

}
