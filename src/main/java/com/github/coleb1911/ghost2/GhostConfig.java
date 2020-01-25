package com.github.coleb1911.ghost2;

import org.aeonbits.owner.Accessible;
import org.aeonbits.owner.Config;
import org.aeonbits.owner.Config.LoadPolicy;
import org.aeonbits.owner.Config.LoadType;
import org.aeonbits.owner.Config.Sources;
import org.aeonbits.owner.Mutable;
import org.aeonbits.owner.Reloadable;

@LoadPolicy(LoadType.MERGE)
@Sources({"classpath:ghost.properties", "system:env"})
public interface GhostConfig extends Config, Accessible, Mutable, Reloadable {
    @Key("ghost.token")
    String token();

    @Key("ghost.operator-id")
    @DefaultValue("-1")
    Long operatorId();

    @Key("ghost.keys.w2g-api-key")
    @DefaultValue("ujww234232ewegwgwef4d")
    String w2gApiKey();
}