package com.github.coleb1911.ghost2.commands.modules.info;

import com.github.coleb1911.ghost2.Ghost2Application;
import com.github.coleb1911.ghost2.commands.CommandRegistry;
import com.github.coleb1911.ghost2.commands.meta.Module;
import com.github.coleb1911.ghost2.commands.meta.*;

import javax.validation.constraints.NotNull;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

public final class ModuleHelp extends Module {

    private CommandRegistry registry;

    @ReflectiveAccess
    public ModuleHelp() {
        super(new ModuleInfo.Builder(ModuleHelp.class)
                .withName("help")
                .withDescription("List commands or get help with a specific command"));
    }

    @Override
    public void invoke(@NotNull final CommandContext ctx) {
        //We cannot initialize the command registry earlier.
        initializeRegistry();

        if (ctx.getArgs().size() > 0) {
            singleCommandHelp(ctx);
        } else {
            fullCommandList(ctx);
        }
    }

    /**
     * Sends a message detailing a single command, or an error message if the command is invalid.
     *
     * @param ctx The `CommandContext` to publish on.
     */
    private void singleCommandHelp(@NotNull CommandContext ctx) {
        // Fetch & null-check CommandInfo
        ModuleInfo info = registry.getInfo(ctx.getArgs().get(0));
        if (null == info) {
            ctx.reply(Module.REPLY_COMMAND_INVALID);
            return;
        }

        // Build and send embed
        ctx.getChannel().createMessage(messageSpec -> messageSpec.setEmbed(embedSpec -> {
            String aliasesString = getFormattedListString("", ", ", "", info.getAliases())
                    .orElse("n/a");

            embedSpec.setTitle("Command help");
            embedSpec.addField("Name", info.getName(), false);
            embedSpec.addField("Description", info.getDescription(), false);
            embedSpec.addField("Aliases", aliasesString, false);
            embedSpec.addField("Category", info.getType().getIcon() + info.getType().getFormattedName(), false);
        })).block();
    }


    /**
     * Sends a message with a formatted embed enumerating all the commands in their respective `CommandType`.
     *
     * @param ctx The `CommandContext` to publish on.
     */
    private void fullCommandList(@NotNull CommandContext ctx) {

        // Build and send embed
        ctx.getChannel().createMessage(messageSpec -> messageSpec.setEmbed(embedSpec -> {
            embedSpec.setTitle("Help");
            embedSpec.setAuthor(ctx.getSelf().getUsername(), null, ctx.getSelf().getAvatarUrl());
            embedSpec.setFooter("See g!help <command> for help with specific commands", null);
            embedSpec.setTimestamp(Instant.now());

            for (Map.Entry<CommandType, List<ModuleInfo>> module : getCommandTypeToModuleInfoMap().entrySet()) {
                List<String> names = module.getValue().stream().map(ModuleInfo::getName).collect(Collectors.toList());
                String commandList = getFormattedListString("`", "`, `", "`", names)
                        .orElse("No commands (...yet)");

                CommandType type = module.getKey();
                embedSpec.addField(type.getIcon() + " " + type.getFormattedName(), commandList, false);
            }
        })).block();
    }



    /**
     * Takes a list of `String`, and if they're empty, returns an empty `String`. If not, returns a `String` beginning
     * with the `before` `String`, intercalates the values with the `between` `String`, and appends the `after` `String`
     * at the end.
     *
     * @param before  A `String` to prepend to the result.
     * @param between A `String` to intercalate between the list elements (ex.: ", ").
     * @param after   A `String` to append to the result.
     * @param values  The values to be displayed.
     * @return A formatted string
     */
    private Optional<String> getFormattedListString(String before, String between, String after, List<String> values) {
        if (values.isEmpty()) return Optional.empty();
        StringJoiner joiner = new StringJoiner(between);
        for (String value : values) {
            joiner.add(value);
        }

        return Optional.of(before + joiner + after);
    }

    /**
     * Categorizes all available modules.
     *
     * @return A map of all available sorts of `CommandType` and their corresponding `ModuleInfo`.
     */
    private Map<CommandType, List<ModuleInfo>> getCommandTypeToModuleInfoMap() {
        Map<CommandType, List<ModuleInfo>> modules = new LinkedHashMap<>();

        for (CommandType type : CommandType.values()) {
            modules.put(type, new ArrayList<>());
        }
        for (ModuleInfo info : registry.getAllInfo()) {
            modules.get(info.getType()).add(info);
        }

        return modules;
    }

    /**
     * Initializes the `CommandRegistry` the first time it is called.
     */
    private void initializeRegistry() {
        if (registry == null) {
            registry = Ghost2Application.getApplicationInstance().getDispatcher().getRegistry();
        }
    }
}
