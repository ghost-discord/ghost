package com.github.coleb1911.ghost2.commands.modules.info;

import com.github.coleb1911.ghost2.commands.CommandRegistry;
import com.github.coleb1911.ghost2.commands.meta.CommandContext;
import com.github.coleb1911.ghost2.commands.meta.CommandType;
import com.github.coleb1911.ghost2.commands.meta.Module;
import com.github.coleb1911.ghost2.commands.meta.ModuleInfo;
import com.github.coleb1911.ghost2.commands.meta.ReflectiveAccess;
import org.springframework.beans.factory.annotation.Autowired;

import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.stream.Collectors;

/**
 * @author cbryant02
 * @author LeMikaelF
 */
public final class ModuleHelp extends Module {

    @Autowired private CommandRegistry registry;

    @ReflectiveAccess
    public ModuleHelp() {
        super(new ModuleInfo.Builder(ModuleHelp.class)
                .withName("help")
                .withDescription("List commands or get help with a specific command"));
    }

    @Override
    public void invoke(@NotNull final CommandContext ctx) {
        if (ctx.getArgs().size() > 0) {
            singleCommandHelp(ctx);
        } else {
            fullCommandList(ctx);
        }
    }

    /**
     * Sends a message detailing a single command, or an error message if the command is invalid.
     *
     * @param ctx The {@code CommandContext} to publish on.
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
     * Sends a message with a formatted embed enumerating all the commands in their respective {@code CommandType}.
     *
     * @param ctx The {@code CommandContext} to publish on.
     */
    private void fullCommandList(@NotNull CommandContext ctx) {
        ctx.getChannel().createMessage(messageSpec -> messageSpec.setEmbed(embedSpec -> {
            embedSpec.setTitle("Help");
            embedSpec.setFooter("See help <command> for help with specific commands", null);

            for (Map.Entry<CommandType, List<ModuleInfo>> module : categorizeModules().entrySet()) {
                List<String> names = module.getValue().stream()
                        .map(ModuleInfo::getName)
                        .collect(Collectors.toList());

                CommandType type = module.getKey();
                String commandList = getFormattedListString("`", "`, `", "`", names)
                        .orElse("No commands (...yet)");

                embedSpec.addField(type.getIcon() + " " + type.getFormattedName(), commandList, false);
            }
        })).block();
    }


    /**
     * Takes a list of {@link String Strings}, and if it's empty, returns an empty {@code String}. If not, returns a
     * {@code String} beginning with {@code before}, delimits the values with {@code between}, and appends
     * {@code after} at the end.
     *
     * @param prefix    A {@code String} to prepend to the result.
     * @param delimiter A {@code String} to insert between the list elements (ex.: ", ").
     * @param suffix    A {@code String} to append to the result.
     * @param values    The values to be displayed.
     * @return A formatted string
     */
    private Optional<String> getFormattedListString(String prefix, String delimiter, String suffix, List<String> values) {
        if (values.isEmpty()) return Optional.empty();

        StringJoiner joiner = new StringJoiner(delimiter, prefix, suffix);
        for (String value : values) {
            joiner.add(value);
        }

        return Optional.of(joiner.toString());
    }

    /**
     * Categorizes all available modules.
     *
     * @return A map of all available sorts of {@link CommandType} and their corresponding {@link ModuleInfo}.
     */
    private Map<CommandType, List<ModuleInfo>> categorizeModules() {
        Map<CommandType, List<ModuleInfo>> modules = new LinkedHashMap<>();

        for (CommandType type : CommandType.values()) {
            modules.put(type, new ArrayList<>());
        }
        for (ModuleInfo info : registry.getAllInfo()) {
            modules.get(info.getType()).add(info);
        }

        return modules;
    }
}
