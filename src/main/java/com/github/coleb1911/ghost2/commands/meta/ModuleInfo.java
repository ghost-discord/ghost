package com.github.coleb1911.ghost2.commands.meta;

import discord4j.core.object.util.PermissionSet;
import org.apache.commons.lang3.StringUtils;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;

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
public class ModuleInfo {
    private final String name;
    private final String description;
    private final PermissionSet botPermissions;
    private final PermissionSet userPermissions;
    private final CommandType type;
    private final String[] aliases;

    private ModuleInfo(String name, String description, PermissionSet botPermissions, PermissionSet userPermissions, CommandType type, String[] aliases) {
        this.name = name;
        this.description = description;
        this.botPermissions = botPermissions;
        this.userPermissions = userPermissions;
        this.type = type;
        this.aliases = aliases;
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
     * @return Command aliases
     */
    public String[] getAliases() {
        return aliases;
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
        @NotNull private Class<? extends Module> moduleClass;
        @NotBlank
        private String name;
        @NotBlank
        private String description;
        @NotNull
        private PermissionSet botPermissions;
        @NotNull
        private PermissionSet userPermissions;
        @NotNull
        private CommandType type;
        @NotNull
        private String[] aliases;

        /**
         * Constructs a new CommandInfo builder.<br>
         * This builder should only be utilized by a {@link Module} to provide its own metadata.
         *
         * @param moduleClass Actual class of the Module calling the constructor. <b>Must be not-null.</b> Used to get
         *                    {@link CommandType} from the Module's package.
         */
        public Builder(@NotNull Class<? extends Module> moduleClass) {
            this.moduleClass = moduleClass;
            this.name = "";
            this.description = "";
            this.botPermissions = PermissionSet.none();
            this.userPermissions = PermissionSet.none();
            this.type = moduleClass.getPackage().getAnnotation(CommandPackage.class).value();
            this.aliases = new String[0];
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
         * @param userPermissions Required user permissions. Must be not-null.
         * @return this Builder
         */
        public Builder withUserPermissions(@NotNull PermissionSet userPermissions) {
            this.userPermissions = userPermissions;
            return this;
        }

        /**
         * Sets the command aliases to the given values.
         *
         * @param aliases Command aliases. Must be not-null.
         * @return this Builder
         */
        public Builder withAliases(@NotNull String... aliases) {
            this.aliases = aliases;
            return this;
        }

        /**
         * Builds the {@code CommandInfo}.
         *
         * @return A {@code CommandInfo} object built from the parameters provided to this Builder
         */
        ModuleInfo build() {
            checkValid();
            return new ModuleInfo(name, description, botPermissions, userPermissions, type, aliases);
        }

        /**
         * Ensures all the fields in this Builder are populated correctly.
         *
         * @throws InvalidModuleException upon encountering an incorrectly populated field
         */
        private void checkValid() throws InvalidModuleException {
            List<InvalidModuleException.Reason> reasons = new ArrayList<>();

            // Check all fields
            if (StringUtils.isBlank(name)) reasons.add(InvalidModuleException.Reason.INVALID_NAME);

            if (StringUtils.isBlank(description)) reasons.add(InvalidModuleException.Reason.INVALID_DESCRIPTION);

            if (null == botPermissions) reasons.add(InvalidModuleException.Reason.INVALID_BOT_PERMISSIONS);

            if (null == userPermissions) reasons.add(InvalidModuleException.Reason.INVALID_USER_PERMISSIONS);

            if (null == type) reasons.add(InvalidModuleException.Reason.INVALID_TYPE);

            if (null == aliases) reasons.add(InvalidModuleException.Reason.INVALID_ALIASES);

            // Throw if any invalid fields encountered
            if (!reasons.isEmpty()) {
                throw new InvalidModuleException(moduleClass, reasons.toArray(new InvalidModuleException.Reason[0]));
            }
        }
    }
}