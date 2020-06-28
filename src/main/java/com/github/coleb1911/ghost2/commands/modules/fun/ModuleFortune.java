package com.github.coleb1911.ghost2.commands.modules.fun;

import com.github.coleb1911.ghost2.commands.meta.CommandContext;
import com.github.coleb1911.ghost2.commands.meta.Module;
import com.github.coleb1911.ghost2.commands.meta.ModuleInfo;
import com.github.coleb1911.ghost2.commands.meta.ReflectiveAccess;

import javax.validation.constraints.NotNull;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.Random;
import java.util.Scanner;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


/** This class provides a module for a fortune cookie command, pulled from an API with different categories available
 * @author John Allison | john123allison@gmail.com
 */
public final class ModuleFortune extends Module {
    private static final Random random = new Random();
    private final String API_URL = " http://yerkee.com/api/fortune/";
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
            categorySelected  = ctx.getArgs().get(0);
            if (Arrays.asList(FORTUNE_CATEGORIES).contains(categorySelected)) {
                category = categorySelected;
            } else {
                // This looks weird because had to apply the fencepost problem to format this correctly
                String reply = "Invalid argument. List of available categories: " + FORTUNE_CATEGORIES[0] + " ";
                for(int i = 1; i < FORTUNE_CATEGORIES.length - 1; i++) {
                    reply += ", " + FORTUNE_CATEGORIES[i];
                }
                ctx.replyBlocking(reply);
                return;
            }
        } else {
            int index = random.nextInt(FORTUNE_CATEGORIES.length - 1);
            category = FORTUNE_CATEGORIES[index];
        }

        // make a url and cast it as a HttpURLConnection
        String idURL = API_URL + category;
        URL url = new URL(idURL);
        HttpURLConnection conn = (HttpURLConnection)url.openConnection();

        conn.setRequestMethod("GET");
        conn.connect();

        int responseCode = conn.getResponseCode();

        // read response
        String json = "";
        if(responseCode != 200) {
            throw new RuntimeException("Response Code" + responseCode);
        } else {
            Scanner scanner = new Scanner(url.openStream());
            while(scanner.hasNext()) {
                json += scanner.nextLine();
            }

            scanner.close();

            // parse JSON
            try {
                JSONObject obj = new JSONObject(json);
                String fortune = (String) obj.get("fortune");
                ctx.replyBlocking(fortune);
            } catch (JSONException e) {
                System.out.println("JSON parsing Exception");
            }
        }
    }
}
