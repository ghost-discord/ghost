package com.github.coleb1911.ghost2.commands.modules.info;

import com.github.coleb1911.ghost2.Ghost2Application;
import com.github.coleb1911.ghost2.commands.CommandRegistry;
import com.github.coleb1911.ghost2.commands.meta.CommandContext;
import com.github.coleb1911.ghost2.commands.meta.CommandType;
import com.github.coleb1911.ghost2.commands.meta.Module;
import com.github.coleb1911.ghost2.commands.meta.ModuleInfo;
import com.github.coleb1911.ghost2.commands.meta.ReflectiveAccess;

import javax.validation.constraints.NotNull;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

public final class ModuleHelp extends Module {
    @ReflectiveAccess
    public ModuleHelp() {
        super(new ModuleInfo.Builder(ModuleHelp.class)
                .withName("help")
                .withDescription("List commands or get help with a specific command"));
    }

    @Override
    public void invoke(@NotNull final CommandContext ctx) {
        CommandRegistry registry = Ghost2Application.getApplicationInstance().getDispatcher().getRegistry();

        // Single-command help
        if (ctx.getArgs().size() > 0) {
            // Fetch & null-check CommandInfo
            ModuleInfo info = registry.getInfo(ctx.getArgs().get(0));
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
                    for (String alias : info.getAliases()) {
                        joiner.add(alias);
                    }
                    aliasList = joiner.toString();
                }

                embedSpec.setTitle("Command help");
                embedSpec.addField("Name", info.getName(), false);
                embedSpec.addField("Description", info.getDescription(), false);
                embedSpec.addField("Aliases", aliasList, false);
                embedSpec.addField("Category", info.getType().getIcon() + info.getType().getFormattedName(), false);
            })).block();
        } else { // Full command list
            // Categorize all available modules
            Map<CommandType, List<ModuleInfo>> modules = new LinkedHashMap<>();

            for (CommandType type : CommandType.values()) {
                modules.put(type, new ArrayList<>());
            }

            for (ModuleInfo info : registry.getAllInfo()) {
                modules.get(info.getType()).add(info);
            }

            // Build and send embed
            ctx.getChannel().createMessage(messageSpec -> messageSpec.setEmbed(embedSpec -> {
                embedSpec.setTitle("Help");
                embedSpec.setAuthor(ctx.getSelf().getUsername(), null, ctx.getSelf().getAvatarUrl());
                embedSpec.setFooter("See g!help <command> for help with specific commands", null);
                embedSpec.setTimestamp(Instant.now());

                for (Map.Entry<CommandType, List<ModuleInfo>> module : modules.entrySet()) {
                    String commandList;
                    if (module.getValue().isEmpty()) {
                        commandList = "No commands (...yet)";
                    } else {
                        StringJoiner joiner = new StringJoiner(", ");
                        for (ModuleInfo info : module.getValue()) {
                            joiner.add("`" + info.getName() + "`");
                        }
                        commandList = joiner.toString();
                    }

                    CommandType type = module.getKey();
                    embedSpec.addField(type.getIcon() + " " + type.getFormattedName(), commandList, false);
                }
            })).block();
        }
    }

}
