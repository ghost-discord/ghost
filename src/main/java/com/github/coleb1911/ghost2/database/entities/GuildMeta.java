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
    @Column(name = "ID", unique = true, nullable = false)
    private Long id;

    @Column(name = "PREFIX", nullable = false)
    private String prefix = DEFAULT_PREFIX;

    // Hibernate requires a default constructor; fields are set with the setters instead of constructor
    public GuildMeta() {
    }

    public GuildMeta(long id, String prefix) {
        this.id = id;
        this.prefix = prefix;
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

    @Override
    public boolean equals(Object other) {
        return (other instanceof GuildMeta || other instanceof Guild) && hashCode() == other.hashCode();
    }

    @Override
    public int hashCode() {
        return Long.hashCode(id);
    }
}