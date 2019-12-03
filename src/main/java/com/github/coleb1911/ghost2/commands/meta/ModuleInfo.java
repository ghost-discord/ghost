package com.github.coleb1911.ghost2.commands.meta;

import discord4j.core.object.util.PermissionSet;
import org.apache.commons.lang3.StringUtils;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Contains metadata for a Module.
 * <p>
 * The metadata includes all of the following:
 * <ul>
 * <li>Command name</li>
 * <li>Command description</li>
 * <li>Required permissions for the bot</li>
 * <li>Required permissions for the user</li>
 * <li>{@linkplain CommandType Command type}</li>
 * <li>Command aliases</li>
 * </ul>
 */
public final class ModuleInfo {
    private final String name;
    private final String description;
    private final PermissionSet botPermissions;
    private final PermissionSet userPermissions;
    private final CommandType type;
    private final List<String> aliases;
    private final boolean showTypingIndicator;

    private ModuleInfo(String name,
                       String description,
                       PermissionSet botPermissions,
                       PermissionSet userPermissions,
                       CommandType type,
                       String[] aliases,
                       boolean showTypingIndicator) {
        this.name = name.toLowerCase();
        this.description = description;
        this.botPermissions = botPermissions;
        this.userPermissions = userPermissions;
        this.type = type;
        this.aliases = Arrays.stream(aliases).map(String::toLowerCase).collect(Collectors.toUnmodifiableList());
        this.showTypingIndicator = showTypingIndicator;
    }

    /**
     * @return Command name
     */
    public String getName() {
        return name;
    }

    /**
     * @return Command description
     */
    public String getDescription() {
        return description;
    }

    /**
     * @return Bot permissions required to invoke the command
     */
    public PermissionSet getBotPermissions() {
        return botPermissions;
    }

    /**
     * @return User permissions required to invoke the command
     */
    public PermissionSet getUserPermissions() {
        return userPermissions;
    }

    /**
     * @return Command type
     */
    public CommandType getType() {
        return type;
    }

    /**
     * @return Immutable list of aliases
     */
    public List<String> getAliases() {
        return aliases;
    }

    /**
     * @return Whether or not this command should show a typing indicator during execution
     */
    public boolean shouldType() {
        return showTypingIndicator;
    }

    /**
     * The builder class for ModuleInfo.
     * <p>
     * Every {@link Module} subclass must construct themselves with a valid ModuleInfo in order to be considered a valid command.
     * The minimum criteria for a valid ModuleInfo.Builder are as follows:
     * <ul>
     * <li>{@code name} is not null and contains at least one non-whitespace character.</li>
     * <li>{@code description} is not null and contains at least one non-whitespace character.</li>
     * <li>
     * {@link CommandType type} is not null. Since ModuleInfo.Builder obtains the command type
     * reflectively via the {@link CommandPackage @CommandPackage} annotation, this means two things:<br>
     * 1. The Module must be located in a sub-package of {@link com.github.coleb1911.ghost2.commands.modules modules}<br>
     * 2. The Module must pass a valid class (i.e. itself) to the ModuleInfo.Builder constructor.
     * </li>
     * </ul>
     * <p>
     * The other fields are set to an empty value automatically by the Builder constructor and do not have to be
     * provided by the Module. In essence, you only need to call the {@linkplain Builder#Builder constructor},
     * {@link Builder#withName withName}, and {@link Builder#withDescription withDescription}.
     */
    public static class Builder {
        @NotNull private final Class<? extends Module> moduleClass;
        @NotNull private final CommandType type;
        @NotBlank private String name;
        @NotBlank private String description;
        @NotNull private PermissionSet botPermissions;
        @NotNull private PermissionSet userPermissions;
        @NotNull private String[] aliases;
        @NotNull private boolean showTypingIndicator = false;

        /**
         * Constructs a new CommandInfo builder.<br>
         * This builder should only be utilized by a {@link Module} to provide its own metadata.
         *
         * @param moduleClass Actual class of the Module calling the constructor. <b>Cannot be null.</b> Used to get
         *                    {@link CommandType} from the Module's package.
         */
        public Builder(@NotNull Class<? extends Module> moduleClass) {
            this.moduleClass = moduleClass;
            this.name = "";
            this.description = "";
            this.botPermissions = PermissionSet.none();
            this.userPermissions = PermissionSet.none();
            this.aliases = new String[0];

            CommandPackage packageAnnotation = moduleClass.getPackage().getAnnotation(CommandPackage.class);
            if (packageAnnotation == null)
                throw new IllegalArgumentException("Module class passed to #ModuleInfo.Builder() was incorrect or class is not in a valid package");
            this.type = packageAnnotation.value();
        }

        /**
         * Sets the command name to the given value.
         *
         * @param name Command name. Must be not-null and contain at least one non-whitespace character.
         * @return this Builder
         */
        public Builder withName(@NotBlank String name) {
            this.name = name;
            return this;
        }

        /**
         * Sets the command description to the given value.
         *
         * @param description Command description. Must be not-null and contain at least one non-whitespace character.
         * @return this Builder
         */
        public Builder withDescription(@NotBlank String description) {
            this.description = description;
            return this;
        }

        /**
         * Sets the required bot permissions to the given PermissionSet.
         *
         * @param botPermissions Required bot permissions
         * @return this Builder
         */
        public Builder withBotPermissions(@NotNull PermissionSet botPermissions) {
            this.botPermissions = botPermissions;
            return this;
        }

        /**
         * Sets the required user permissions to the given PermissionSet.
         *
         * @param userPermissions Required user permissions. <b>Cannot be null.</b>
         * @return this Builder
         */
        public Builder withUserPermissions(@NotNull PermissionSet userPermissions) {
            this.userPermissions = userPermissions;
            return this;
        }

        /**
         * Sets the required user and bot permissions to the given PermissionSet.
         *
         * @param permissions Required permissions. <b>Cannot be null.</b>
         * @return this Builder
         */
        public Builder withPermissions(@NotNull PermissionSet permissions) {
            this.botPermissions = permissions;
            this.userPermissions = permissions;
            return this;
        }

        /**
         * Sets the command aliases to the given values.
         *
         * @param aliases Command aliases. <b>Cannot be null.</b>
         * @return this Builder
         */
        public Builder withAliases(@NotNull String... aliases) {
            this.aliases = aliases;
            return this;
        }

        /**
         * Make this Module show a typing indicator while it is executing. Good for
         * providing feedback during long-running commands.
         * <p>
         * Note that because of the way the Discord API handles typing indicators,
         * your command should send a message when it is done in order to hide the
         * indicator. If not, it'll persist for at least 10 seconds before it disappears.
         *
         * @return this Builder
         */
        public Builder showTypingIndicator() {
            this.showTypingIndicator = true;
            return this;
        }

        /**
         * Builds the {@code CommandInfo}.
         *
         * @return A {@code CommandInfo} object built from the parameters provided to this Builder
         */
        ModuleInfo build() {
            checkValid();
            return new ModuleInfo(name, description, botPermissions, userPermissions, type, aliases, showTypingIndicator);
        }

        /**
         * Ensures all the fields in this Builder are populated correctly.
         *
         * @throws InvalidModuleException upon encountering an incorrectly populated field
         */
        private void checkValid() throws InvalidModuleException {
            Set<InvalidModuleException.Reason> reasons = new LinkedHashSet<>();

            // Check all fields
            if (StringUtils.isBlank(name)) reasons.add(InvalidModuleException.Reason.INVALID_NAME);

            if (StringUtils.isBlank(description)) reasons.add(InvalidModuleException.Reason.INVALID_DESCRIPTION);

            if (null == botPermissions) reasons.add(InvalidModuleException.Reason.INVALID_BOT_PERMISSIONS);

            if (null == userPermissions) reasons.add(InvalidModuleException.Reason.INVALID_USER_PERMISSIONS);

            if (null == type) reasons.add(InvalidModuleException.Reason.INVALID_TYPE);

            if (null == aliases) reasons.add(InvalidModuleException.Reason.INVALID_ALIASES);

            // Throw if any invalid fields encountered
            if (!reasons.isEmpty()) {
                throw new InvalidModuleException(moduleClass, reasons);
            }
        }
    }
}