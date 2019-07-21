package com.github.coleb1911.ghost2.database.repos;

import com.github.coleb1911.ghost2.commands.meta.ReflectiveAccess;
import com.github.coleb1911.ghost2.database.entities.GuildMeta;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@ReflectiveAccess
@Component
public interface GuildMetaRepository extends CrudRepository<GuildMeta, Long> {
    @Override
    @CachePut("guilds")
    <S extends GuildMeta> S save(S entity);

    @Override
    @Cacheable(value = "guilds", key = "#p0")
    Optional<GuildMeta> findById(Long id);

    @Override
    Iterable<GuildMeta> findAll();

    @Override
    long count();

    @Override
    @CacheEvict(value = "guilds", key = "#p0.id")
    void delete(GuildMeta guild);

    @Override
    boolean existsById(Long id);
}
