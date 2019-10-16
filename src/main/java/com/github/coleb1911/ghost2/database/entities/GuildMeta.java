package com.github.coleb1911.ghost2.database.entities;

import discord4j.core.object.entity.Guild;
import discord4j.core.object.util.Snowflake;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;


/**
 * Represents a Guild in ghost2's database.
 */
@Entity
@Table(name = "GUILD_META")
public class GuildMeta {
    public static final int PREFIX_LENGTH = 6;
    private static final String DEFAULT_PREFIX = "g!";
    @Id
    @Column(name = "ID", unique = true, nullable = false)
    private Long id;

    /**
     * Defaults to {@value DEFAULT_PREFIX}.
     */
    @Column(name = "PREFIX", nullable = false, length = PREFIX_LENGTH)
    private String prefix = DEFAULT_PREFIX;

    @Column(name = "AUTOROLE")
    private Long autoRoleId;

    /**
     * Defaults to {@code false}.
     */
    @Column(name = "AUTOROLE_ENABLED", nullable = false)
    private Boolean autoRoleEnabled = false;

    /**
     * Defaults to {@code false}.
     */
    @Column(name = "AUTOROLE_CONFIRMATION_ENABLED", nullable = false)
    private Boolean autoRoleConfirmationEnabled = false;

    // Hibernate requires a default constructor; fields are set with the setters instead of constructor
    public GuildMeta() {
    }

    /**
     * Constructs a new GuildMeta.<br>
     * See field JavaDocs for default field values.
     *
     * @param id Guild ID
     */
    public GuildMeta(long id) {
        this.id = id;
    }

    /**
     * Constructs a new GuildMeta.<br>
     * See field JavaDocs for default field values.
     *
     * @param id Guild ID
     */
    public GuildMeta(Snowflake id) {
        this.id = id.asLong();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public Long getAutoRoleId() {
        return autoRoleId;
    }

    public void setAutoRoleId(Long autoRoleId) {
        this.autoRoleId = autoRoleId;
    }

    public Boolean getAutoRoleEnabled() {
        return autoRoleEnabled;
    }

    public void setAutoRoleEnabled(Boolean autoRoleEnabled) {
        this.autoRoleEnabled = autoRoleEnabled;
    }

    public Boolean getAutoRoleConfirmationEnabled() {
        return autoRoleConfirmationEnabled;
    }

    public void setAutoRoleConfirmationEnabled(Boolean autoRoleConfirmationEnabled) {
        this.autoRoleConfirmationEnabled = autoRoleConfirmationEnabled;
    }

    @Override
    public boolean equals(Object other) {
        return (other instanceof GuildMeta || other instanceof Guild) && hashCode() == other.hashCode();
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}