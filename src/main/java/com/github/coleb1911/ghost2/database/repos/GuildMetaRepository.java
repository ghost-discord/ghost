package com.github.coleb1911.ghost2.database.repos;

import com.github.coleb1911.ghost2.database.entities.GuildMeta;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface GuildMetaRepository extends CrudRepository<GuildMeta, Long> {
    @Override
    <S extends GuildMeta> S save(S entity);

    @Override
    @Cacheable(value = "guilds", key = "#p0")
    Optional<GuildMeta> findById(Long id);

    @Override
    @Cacheable("guilds")
    Iterable<GuildMeta> findAll();

    @Override
    long count();

    @Override
    @CacheEvict(value = "guilds", key = "#p0.id")
    void delete(GuildMeta guild);

    @Override
    @Cacheable("guilds")
    boolean existsById(Long id);
}
