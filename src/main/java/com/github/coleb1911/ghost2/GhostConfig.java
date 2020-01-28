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

    // Available only on request, no documentation
    // https://community.watch2gether.com
    @Key("ghost.keys.watch2gether")
    @DefaultValue("ujww234232ewegwgwef4d")
    String w2gApiKey();

    // https://developers.google.com/youtube/v3
    @Key("ghost.keys.youtube")
    String youtubeApiKey();

    // https://www.ibm.com/watson/services/language-translator/
    @Key("ghost.keys.watson")
    String watsonApiKey();

    @Key("ghost.keys.watson-url")
    String watsonApiUrl();
}