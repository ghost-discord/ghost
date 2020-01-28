package com.github.coleb1911.ghost2.commands.modules.utility;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.coleb1911.ghost2.References;
import com.github.coleb1911.ghost2.commands.meta.CommandContext;
import com.github.coleb1911.ghost2.commands.meta.Module;
import com.github.coleb1911.ghost2.commands.meta.ModuleInfo;
import com.github.coleb1911.ghost2.commands.meta.ReflectiveAccess;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClientBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import javax.validation.constraints.NotNull;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class uses the IBM Watson translator API. It requires an API key (available through https://www.ibm.com/watson/services/language-translator/), and is free up to 1 million characters (over 300 pages) per month.
 * <p>
 * It reads from two properties in ghost.properties:
 * - ibmwatsontranslator.key is the API Key.
 * - ibmwatsontranslator.url is the endpoint URL, which can be found in the same page as the API Key.
 */
public final class ModuleTranslate extends Module {
    private static final String REPLY_INVALID_INPUT = "Invalid input. See help for more details.";
    private static final String REPLY_UNCONFIGURED = "Translate module is unconfigured.";

    //Since IBM Watson only takes 2-letter language codes, perform basic validation.
    private static final String DEFAULT_LANG_CODE =
            Locale.getDefault().getISO3Language().length() == 2 ? Locale.getDefault().getISO3Language() : "en";
    private static final String DESCRIPTION = "Translates a word or an expression.\n" +
            "**Ex**.: \"fr ¡Buenas noches! from:es\" will translate \"¡Buenas noches!\" from Spanish to French.\n" +
            "If no \"from\" language is specified, " +
            Locale.forLanguageTag(DEFAULT_LANG_CODE).getDisplayName() + " will be used.\n\n" +
            "**Sample language codes**\n" +
            "French: fr\n" +
            "Spanish: es\n" +
            "English: en\n" +
            "German: de\n" +
            "Russian: ru\n";

    private static final String API_URL = References.getConfig().watsonApiUrl();
    private static final String API_KEY = References.getConfig().watsonApiKey();

    @ReflectiveAccess
    public ModuleTranslate() {
        super(new ModuleInfo.Builder(ModuleTranslate.class)
                .withName("translate")
                .withDescription(DESCRIPTION));
    }

    @Override
    @ReflectiveAccess
    public void invoke(@NotNull CommandContext ctx) {
        if (StringUtils.isBlank(API_URL) || StringUtils.isBlank(API_KEY)) {
            ctx.replyBlocking(REPLY_UNCONFIGURED);
            return;
        }

        String command = String.join(" ", ctx.getArgs());

        //These two are mandatory; throw a runtime exception if we can't extract them.
        String toLang = extractToLang(command).orElseGet(postInvalidInputMessageAndThrow(ctx));
        String phrase = extractPhrase(command).orElseGet(postInvalidInputMessageAndThrow(ctx));

        String fromLang = extractFromLang(command).orElse(DEFAULT_LANG_CODE);

        Optional<String> translation = sendTranslationRequest(toLang, fromLang, phrase);
        translation.ifPresentOrElse(
                s -> postTranslationMessage(ctx, fromLang, toLang, phrase, s),
                () -> postNoTranslationMessage(ctx, phrase));
    }

    private Supplier<String> postInvalidInputMessageAndThrow(@NotNull CommandContext ctx) {
        return () -> {
            postInvalidInputMessage(ctx);
            throw new IllegalArgumentException();
        };
    }

    private void postInvalidInputMessage(CommandContext ctx) {
        ctx.getChannel().createMessage(REPLY_INVALID_INPUT).block();
    }

    private void postNoTranslationMessage(CommandContext ctx, String phrase) {
        ctx.getChannel().createMessage("No translations were found for “" + phrase + "”.").block();
    }

    private void postTranslationMessage(CommandContext ctx, String from, String to, String phrase, String translation) {
        String fromLangDisplay = Locale.forLanguageTag(from).getDisplayName();
        String toLangDisplay = Locale.forLanguageTag(to).getDisplayName();
        String desc = "Translate from " + fromLangDisplay + ": " + phrase + "\n" +
                "Translation in " + toLangDisplay + ": " + translation;

        ctx.getChannel().createEmbed(embedCreateSpec -> {
            embedCreateSpec.setTitle("\uD83D\uDCD6" + "Translator");
            embedCreateSpec.setDescription(desc);
            embedCreateSpec.setFooter("Taken from IBM Watson Translator", null);
        }).block();
    }

    private Optional<String> sendTranslationRequest(String toLang, String fromLang, String phrase) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        Map<String, String> map = new HashMap<>();
        map.put("text", phrase);
        map.put("model_id", fromLang + "-" + toLang);

        CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        credentialsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials("apikey", API_KEY));
        HttpClient client = HttpClientBuilder
                .create()
                .setDefaultCredentialsProvider(credentialsProvider)
                .build();

        HttpComponentsClientHttpRequestFactory clientHttpRequestFactory
                = new HttpComponentsClientHttpRequestFactory();
        clientHttpRequestFactory.setHttpClient(client);
        RestTemplate restTemplate = new RestTemplate(clientHttpRequestFactory);
        ResponseEntity<TranslatorResponse> entity = restTemplate.exchange(API_URL,
                HttpMethod.POST, new HttpEntity<>(map, headers), TranslatorResponse.class);
        if (entity.getBody() != null && entity.getBody().getTranslations() != null &&
                entity.getBody().getTranslations().get(0) != null) {
            return Optional.of(entity.getBody().getTranslations().get(0).getTranslation());
        } else {
            return Optional.empty();
        }
    }

    private Optional<String> extractPhrase(String command) {
        String phrasePattern = "([a-z]{2})((?!\\s*$).*?)(?: from:[a-z]{2}|$)";
        Pattern pattern = Pattern.compile(phrasePattern);
        Matcher matcher = pattern.matcher(command);

        //This conditional is not redundant because of the side effects or matcher#matches.
        if (matcher.matches() && !"".equals(matcher.group(2).strip())) {
            return Optional.of(matcher.group(2).strip());
        } else {
            return Optional.empty();
        }
    }

    private Optional<String> extractFromLang(String command) {
        //Check for language to translate from
        String fromLangPattern = "([a-z]{2})\\s(.+)(\\sfrom:([a-z]{2}))";
        Pattern pattern = Pattern.compile(fromLangPattern);
        Matcher matcher = pattern.matcher(command);

        //This conditional is not redundant because of the side effects or matcher#matches.
        if (matcher.matches()) {
            return Optional.ofNullable(matcher.group(4));
        } else {
            return Optional.empty();
        }
    }

    private Optional<String> extractToLang(String command) {
        String toLangPattern = "([a-z]{2})\\s.*";
        Pattern pattern = Pattern.compile(toLangPattern);
        Matcher matcher = pattern.matcher(command);

        //This conditional is not redundant because of the side effects or matcher#matches.
        if (matcher.matches()) {
            return Optional.ofNullable(matcher.group(1));
        } else {
            return Optional.empty();
        }

    }


    private static class TranslatorResponse {

        @JsonProperty("translations")
        private List<TranslationsItem> translations;

        List<TranslationsItem> getTranslations() {
            return translations;
        }
    }

    private static class TranslationsItem {

        @JsonProperty("translation")
        private String translation;

        String getTranslation() {
            return translation;
        }
    }
}
