package com.github.coleb1911.ghost2;

import org.aeonbits.owner.Accessible;
import org.aeonbits.owner.Config;
import org.aeonbits.owner.Config.Sources;
import org.aeonbits.owner.Mutable;
import org.aeonbits.owner.Reloadable;

import java.util.Base64;

@Sources("classpath:ghost.properties")
public interface GhostConfig extends Config, Accessible, Mutable, Reloadable {
    @Key("ghost.token")
    String token();

    @Key("ghost.operatorid")
    @DefaultValue("-1")
    Long operatorId();

    @Key("ghost.w2g_api_key")
    @DefaultValue("ujww234232ewegwgwef4d")
    String w2g_api_key();

}