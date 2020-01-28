package com.github.coleb1911.ghost2.database.repos;

import com.github.coleb1911.ghost2.commands.meta.ReflectiveAccess;
import com.github.coleb1911.ghost2.database.entities.GuildMeta;
import discord4j.core.object.util.Snowflake;
import org.springframework.data.repository.CrudRepository;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@ReflectiveAccess
public interface GuildMetaRepository extends CrudRepository<GuildMeta, Long> {
    @Override
    @NonNull
    <S extends GuildMeta> S save(@NonNull S entity);

    @Override
    @NonNull
    Optional<GuildMeta> findById(@NonNull Long id);

    @NonNull
    default Optional<GuildMeta> findById(@NonNull Snowflake id) {
        return findById(id.asLong());
    }

    @Override
    @NonNull
    Iterable<GuildMeta> findAll();

    @Override
    long count();

    @Override
    void delete(@NonNull GuildMeta guild);

    @Override
    boolean existsById(@NonNull Long id);
}
