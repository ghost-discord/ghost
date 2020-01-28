package com.github.coleb1911.ghost2.commands.modules.utility;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.coleb1911.ghost2.commands.meta.CommandContext;
import com.github.coleb1911.ghost2.commands.meta.Module;
import com.github.coleb1911.ghost2.commands.meta.ModuleInfo;
import com.github.coleb1911.ghost2.commands.meta.ReflectiveAccess;
import com.github.coleb1911.ghost2.utility.RestUtils;
import discord4j.core.spec.EmbedCreateSpec;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;
import reactor.core.publisher.Mono;

import javax.annotation.Generated;
import javax.validation.constraints.NotNull;
import java.net.URI;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

/*
 * Page about dictionary API's: https://medium.com/@martin.breuss/finding-a-useful-dictionary-api-52084a01503d
 * Example response: [{"word":"paradigm","score":133798,"defs":["n\tthe generally accepted perspective of a particular discipline at a given time","n\tsystematic arrangement of all the inflected forms of a word","n\ta standard or typical example","n\tthe class of all items that can be substituted into the same position (or slot) in a grammatical sentence (are in paradigmatic relation with one another)"]}]
 */

/**
 * @author LeMikaelF
 */
public final class ModuleDefine extends Module {
    private static final String REPLY_MISSING_WORD = "Please enter a word to define.";
    private static final String REPLY_SERVER_ERROR = "The dictionary API is currently unavailable. Please try again later.";
    private static final String REQUEST_TEMPLATE = "https://api.datamuse.com/words?sp=%s&max=1&md=d";

    private final RestTemplate restTemplate = RestUtils.defaultRestTemplate();

    @ReflectiveAccess
    public ModuleDefine() {
        super(new ModuleInfo.Builder(ModuleDefine.class)
                .withName("define")
                .withDescription("Define a word in English"));
    }


    @Override
    @ReflectiveAccess
    public void invoke(@NotNull final CommandContext ctx) {
        if (ctx.getArgs().size() < 1) {
            ctx.getChannel().createMessage(REPLY_MISSING_WORD).block();
            return;
        }

        String word = ctx.getArgs().get(0);
        if ("".equals(word)) return;

        final Consumer<EmbedCreateSpec> specConsumer;
        try {
            specConsumer = createDefinitionEmbed(word);
        } catch (HttpServerErrorException e) {
            ctx.replyBlocking(REPLY_SERVER_ERROR + "(" + e.getStatusCode().toString() + ")");
            return;
        }

        if (specConsumer == null) {
            ctx.replyBlocking("No definition found for " + word + ".");
        } else {
            ctx.getChannel().createEmbed(specConsumer).block();
        }
    }


    /**
     * Uses the DataMuse REST API to define a word.
     *
     * @param word The word to define
     * @return An Embed consumer containing up to 5 definitions of the requested word.
     */

    private Consumer<EmbedCreateSpec> createDefinitionEmbed(String word) {
        /*
         * REST API parameters:
         * sp=XXXX      <- Spelled like (will favor exact spelling, but will return similar words; required by API)
         * max=1        <- Limit response to 1 result
         * md=d         <- Metadata type: definition
         */
        String url = REQUEST_TEMPLATE.replaceAll("%s", word);

        // Redundant type parameter is necessary to circumvent bug  (fix backported to OpenJDK 11.0.4,
        // but Travis CI uses 11.0.2)
        @SuppressWarnings("Convert2Diamond")
        ResponseEntity<List<ResponseObject>> responseEntity = restTemplate.exchange(
                URI.create(url),
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<ResponseObject>>() {
                }
        );

        return Mono.justOrEmpty(responseEntity.getBody())
                .filter(Predicate.not(List::isEmpty))
                .map(objects -> objects.get(0))
                .map(this::formatDefinitionEmbed)
                .block();
    }

    /**
     * Creates a {@link Consumer} that will properly format an embed with the description.
     *
     * @param responseObject The object created from the JSON request.
     * @return A properly formatted embed (max 5 definitions, one field per definition).
     */
    private Consumer<EmbedCreateSpec> formatDefinitionEmbed(ResponseObject responseObject) {
        return (embedSpec) -> {
            embedSpec.setTitle("\uD83D\uDCD6 Definition of \"" + responseObject.getWord() + "\"");
            List<String> defs = responseObject.getDefs();

            for (int i = 0; i < defs.size(); i++) {
                // Break off part-of-speech
                String[] def = defs.get(i).split("\t");

                if (i >= 5) break;

                String formattedDef = "> **" + def[0] + ".** " + def[1];
                embedSpec.addField(String.valueOf(i + 1), formattedDef, false);
            }

            embedSpec.setFooter("Source: datamuse.com", null);
            embedSpec.setThumbnail("https://www.datamuse.com/api/datamuse-logo-rgb.png");
        };
    }

    /**
     * A POJO representing the response from the REST API
     */
    @Generated("com.robohorse.robopojogenerator")
    private static class ResponseObject {

        @JsonProperty("defs")
        private List<String> defs;

        @JsonProperty("word")
        private String word;

        public void setDefs(List<String> defs) {
            this.defs = defs;
        }

        public List<String> getDefs() {
            return defs;
        }

        public void setWord(String word) {
            this.word = word;
        }

        public String getWord() {
            return word;
        }

        @Override
        public String toString() {
            return "ResponseObject{" +
                    "defs = '" + defs + '\'' +
                    ",word = '" + word + '\'' +
                    "}";
        }
    }
}
