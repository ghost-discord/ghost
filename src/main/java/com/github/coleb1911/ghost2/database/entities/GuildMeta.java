package com.github.coleb1911.ghost2.database.entities;

import discord4j.core.object.entity.Guild;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "GUILD_META")
public class GuildMeta {
    public static final String DEFAULT_PREFIX = "g!";

    @Id
    private long id;

    @Column(name = "PREFIX", nullable = false)
    private String prefix = DEFAULT_PREFIX;

    public GuildMeta() {
    }

    public GuildMeta(long id, String prefix) {
        this.id = id;
        this.prefix = prefix;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    @Override
    public boolean equals(Object other) {
        return (other instanceof GuildMeta || other instanceof Guild) && hashCode() == other.hashCode();
    }

    @Override
    public int hashCode() {
        return Long.hashCode(id);
    }
}