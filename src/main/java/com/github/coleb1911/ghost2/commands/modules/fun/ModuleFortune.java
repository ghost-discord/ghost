package com.github.coleb1911.ghost2.commands.modules.fun;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.coleb1911.ghost2.commands.meta.CommandContext;
import com.github.coleb1911.ghost2.commands.meta.Module;
import com.github.coleb1911.ghost2.commands.meta.ModuleInfo;
import com.github.coleb1911.ghost2.commands.meta.ReflectiveAccess;
import com.github.coleb1911.ghost2.utility.RestUtils;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import javax.validation.constraints.NotNull;
import java.util.Arrays;
import java.util.Random;


/**
 * This class provides a module for a fortune cookie command, pulled from an API with different categories available
 *
 * @author John Allison | john123allison@gmail.com
 */
public final class ModuleFortune extends Module {
    private static final Random random = new Random();
    private static final RestTemplate TEMPLATE = RestUtils.defaultRestTemplate();
    private static final String REPLY_FETCH_ERROR = "Error trying to retrieve fortune.";

    private final String API_URL = "http://yerkee.com/api/fortune/";
    private final String[] FORTUNE_CATEGORIES = {"all", "bible", "computers", "cookie", "definitions", "miscellaneous",
            "people", "platitudes", "politics", "science", "wisdom"};

    @ReflectiveAccess
    public ModuleFortune() {
        super(new ModuleInfo.Builder(ModuleFortune.class)
                .withName("fortune")
                .withDescription("Fetch a free fortune!"));
    }

    @Override
    @ReflectiveAccess
    public void invoke(@NotNull CommandContext ctx) {
        String category;
        String categorySelected;

        // check if args are provided - if not, assign a random category to the fortune
        // if an invalid arg is provided, return a help message
        if (!ctx.getArgs().isEmpty()) {
            categorySelected = ctx.getArgs().get(0);
            if (Arrays.asList(FORTUNE_CATEGORIES).contains(categorySelected)) {
                category = categorySelected;
            } else {
                // This looks weird because had to apply the fencepost problem to format this correctly
                String reply = "Invalid argument. List of available categories: " + FORTUNE_CATEGORIES[0] + " ";
                for (int i = 1; i < FORTUNE_CATEGORIES.length - 1; i++) {
                    reply += ", " + FORTUNE_CATEGORIES[i];
                }
                ctx.replyBlocking(reply);
                return;
            }
        } else {
            int index = random.nextInt(FORTUNE_CATEGORIES.length - 1);
            category = FORTUNE_CATEGORIES[index];
        }

        try {
            final ModuleFortune.Fortune fortune = TEMPLATE.getForObject(API_URL + category,
                    ModuleFortune.Fortune.class);
            if (fortune == null) {
                ctx.replyBlocking(REPLY_FETCH_ERROR);
                return;
            } else {
                ctx.replyBlocking(fortune.getFortune());
            }
        } catch (HttpStatusCodeException exception) {
            ctx.replyBlocking(REPLY_FETCH_ERROR);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class Fortune {
        @JsonProperty("fortune")
        private String fortune;

        String getFortune() {
            return this.fortune;
        }
    }
}

