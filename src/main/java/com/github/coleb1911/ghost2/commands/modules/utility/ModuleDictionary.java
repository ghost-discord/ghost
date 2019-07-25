package com.github.coleb1911.ghost2.commands.modules.utility;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.coleb1911.ghost2.commands.meta.CommandContext;
import com.github.coleb1911.ghost2.commands.meta.Module;
import com.github.coleb1911.ghost2.commands.meta.ModuleInfo;
import com.github.coleb1911.ghost2.commands.meta.ReflectiveAccess;
import discord4j.core.spec.EmbedCreateSpec;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Generated;
import javax.validation.constraints.NotNull;
import java.net.URI;
import java.util.List;
import java.util.StringJoiner;
import java.util.function.Consumer;

/*

Page about dictionary API's: https://medium.com/@martin.breuss/finding-a-useful-dictionary-api-52084a01503d
Example response: [{"word":"paradigm","score":133798,"defs":["n\tthe generally accepted perspective of a particular discipline at a given time","n\tsystematic arrangement of all the inflected forms of a word","n\ta standard or typical example","n\tthe class of all items that can be substituted into the same position (or slot) in a grammatical sentence (are in paradigmatic relation with one another)"]}]
 */

//TODO See if I dragged in too many dependencies (Tomcat etc.) with Springboot web.
public final class ModuleDictionary extends Module {
    final private String TOO_MANY_WORDS_ERROR_STRING = "I can only define one word at a time. Please retry.";
    final private String CALLED_WITH_ZERO_WORDS_ERROR_STRING = "Please enter a word to define.";

    private RestTemplate restTemplate = new RestTemplate();

    @ReflectiveAccess
    public ModuleDictionary() {
        super(new ModuleInfo.Builder(ModuleDictionary.class)
                .withName("define")
                .withDescription("Define a word in English")
        );
    }


    @Override
    public void invoke(@NotNull CommandContext ctx) {
        if (ctx.getArgs().size() > 1) {
            ctx.getChannel().createMessage(TOO_MANY_WORDS_ERROR_STRING).block();
        } else if (ctx.getArgs().size() == 0) {
            ctx.getChannel().createMessage(CALLED_WITH_ZERO_WORDS_ERROR_STRING).block();
        }

        String word = ctx.getArgs().get(0);
        if ("".equals(word)) return;

        Consumer<EmbedCreateSpec> specConsumer = getDefinitionREST(word);
        ctx.getChannel().createMessage(messageCreateSpec -> messageCreateSpec.setEmbed(specConsumer)).block();
    }


    /**
     * Uses the datamuse REST API to define a word.
     *
     * @param word The word to define
     * @return A formatted String containing one or multiple definitions of the requested word.
     * May be on multiple lines; guaranteed to be no more than 100 words.
     */

    private Consumer<EmbedCreateSpec> getDefinitionREST(String word) {
        Consumer<EmbedCreateSpec> voidConsumer = embedCreateSpec -> {};

        /*
        REST Api parameters:
        sp=XXXX      <- Spelled like (will favor exact spelling, but will return similar words; required by API)
        max=1        <- Limit response to 1 result
        md=d         <- Metadata type: definition
         */
        String urlTemplate = "https://api.datamuse.com/words?sp=%s&max=1&md=d";
        String url = urlTemplate.replaceAll("%s", word);

        //Redundant type parameter is necessary to circumvent bug JDK-8212586 (fix backported to OpenJDK 11.0.4,
        //but Travis CI uses 11.0.2)
        @SuppressWarnings("Convert2Diamond")
        ResponseEntity<List<ResponseObject>> responseEntity = restTemplate.exchange(URI.create(url), HttpMethod.GET, null, new ParameterizedTypeReference<List<ResponseObject>>() {});
        List<ResponseObject> responseObjects = responseEntity.getBody();
        if (responseObjects == null || responseObjects.size() == 0) return voidConsumer;
        ResponseObject responseObject = responseObjects.get(0);
        if (responseObject == null) {
            return voidConsumer;
        }

        return getEmbedConsumer(responseObject);
    }

    /**
     * Creates a Consumer that will properly format an embed with the description.
     * For internal use, should only be called by getDefinitionREST.
     *
     * @param responseObject The object created from the JSON request.
     * @return A properly formatted embed (max 150 words, new lines on every definition).
     */
    private Consumer<EmbedCreateSpec> getEmbedConsumer(ResponseObject responseObject) {
        return (embedCreateSpec) -> {
            int wordCount = 0;

            embedCreateSpec.setTitle("\uD83D\uDCD6" + "Definition of “" + responseObject.getWord() + "”");
            List<String> defs = responseObject.getDefs();
            StringJoiner joiner = new StringJoiner("\n");

            for (int i = 0; i < defs.size(); i++) {
                String def = defs.get(i);
                wordCount += def.split(" ").length;
                if (wordCount >= 150) break;
                joiner.add("**" + (i + 1) + ": " + "**" + def);
            }

            embedCreateSpec.setDescription(joiner.toString());
            //Discord API does not support markdown in footers
            embedCreateSpec.setFooter("Taken from Datamuse.com", null);
        };
    }

    /**
     * A POJO representing the response from the Rest API
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
            return
                    "ResponseObject{" +
                            "defs = '" + defs + '\'' +
                            ",word = '" + word + '\'' +
                            "}";
        }
    }
}
