package com.github.coleb1911.ghost2.database.repos;

import com.github.coleb1911.ghost2.commands.meta.ReflectiveAccess;
import com.github.coleb1911.ghost2.database.entities.ApplicationMeta;
import org.springframework.data.repository.CrudRepository;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@ReflectiveAccess
public interface ApplicationMetaRepository extends CrudRepository<ApplicationMeta, String> {
    @Override
    @NonNull
    <S extends ApplicationMeta> S save(@NonNull S entity);

    @Override
    @NonNull
    Optional<ApplicationMeta> findById(@NonNull String s);

    default long getOperatorId() {
        Optional<ApplicationMeta> meta = findById("ghost2");
        if (meta.isEmpty()) return -1;
        return meta.get().getOperatorId();
    }
}