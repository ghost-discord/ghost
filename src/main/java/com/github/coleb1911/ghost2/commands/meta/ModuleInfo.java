package com.github.coleb1911.ghost2.commands.meta;

import discord4j.core.object.util.PermissionSet;

/**
 * Contains metadata for a command module; everything needed to invoke it and/or display help for it
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

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public PermissionSet getBotPermissions() {
        return botPermissions;
    }

    public PermissionSet getUserPermissions() {
        return userPermissions;
    }

    public CommandType getType() {
        return type;
    }

    public String[] getAliases() {
        return aliases;
    }

    public static class Builder {
        private static final String ERROR_INVALID = "Invalid CommandInfo object; must provide at least name and description.";

        private String name;
        private String description;
        private PermissionSet botPermissions;
        private PermissionSet userPermissions;
        private CommandType type;
        private String[] aliases;

        /**
         * Constructs a new CommandInfo builder.
         * <br/>
         * This builder should only be utilized by a {@link Module} to provide its own metadata.
         *
         * @param self Actual class of the Module calling the constructor. Used to get {@link CommandType} from the Module's package.
         */
        public Builder(Class<? extends Module> self) {
            name = "";
            description = "";
            botPermissions = PermissionSet.none();
            userPermissions = PermissionSet.none();
            type = self.getPackage().getAnnotation(CommandPackage.class).value();
            aliases = new String[0];
        }

        /**
         * Sets the command name to the given value.
         *
         * @param name Command name
         * @return this Builder
         */
        public Builder withName(String name) {
            this.name = name;
            return this;
        }

        /**
         * Sets the command description to the given value.
         *
         * @param description Command description
         * @return this Builder
         */
        public Builder withDescription(String description) {
            this.description = description;
            return this;
        }

        /**
         * Sets the required bot permissions to the given PermissionSet.
         *
         * @param botPermissions Required bot permissions
         * @return this Builder
         */
        public Builder withBotPermissions(PermissionSet botPermissions) {
            this.botPermissions = botPermissions;
            return this;
        }

        /**
         * Sets the required user permissions to the given PermissionSet.
         *
         * @param userPermissions Required user permissions
         * @return this Builder
         */
        public Builder withUserPermissions(PermissionSet userPermissions) {
            this.userPermissions = userPermissions;
            return this;
        }

        /**
         * Sets the command aliases to the given values.
         *
         * @param aliases Command aliases
         * @return this Builder
         */
        public Builder withAliases(String[] aliases) {
            this.aliases = aliases;
            return this;
        }

        /**
         * Builds the {@code CommandInfo}.
         *
         * @return A {@code CommandInfo} object built from the parameters provided to this Builder
         */
        ModuleInfo build() {
            if (!checkValid()) throw new IllegalStateException(ERROR_INVALID);
            return new ModuleInfo(name, description, botPermissions, userPermissions, type, aliases);
        }

        /**
         * Ensures all the necessary parameters in this Builder are populated
         *
         * @return Whether or not all the necessary parameters in this Builder are populated
         */
        private boolean checkValid() {
            return !name.isEmpty() &&
                    !description.isEmpty() &&
                    type != null;
        }
    }
}