package com.github.coleb1911.ghost2.commands.modules.fun;

import com.github.coleb1911.ghost2.commands.meta.CommandContext;
import com.github.coleb1911.ghost2.commands.meta.Module;
import com.github.coleb1911.ghost2.commands.meta.ModuleInfo;

import javax.validation.constraints.NotNull;
import java.util.Random;

public final class Module8Ball extends Module {

    private final String[] OUTCOMES = {
            "It is certain.",
            "It is decidedly so.",
            "Without a doubt.",
            "Yes - Definitely",
            "You may rely on it.",
            "As I see it, yes.",
            "Most Likely",
            "Outlook good.",
            "Yes.",
            "Signs point to yes.",
            "Reply hazy, try again.",
            "Ask again later.",
            "Better not tell you now.",
            "Cannot predict now.",
            "Concentrate and ask again.",
            "Don't count on it.",
            "My reply is no.",
            "My sources say no.",
            "Outlook not so good.",
            "Very doubtful."
    };

    public Module8Ball() {
        super(new ModuleInfo.Builder(Module8Ball.class)
                .withName("8ball")
                .withDescription("Play the magic 8 ball game."));
    }

    @Override
    public void invoke(@NotNull CommandContext ctx) {
        // See if a question was asked
        try {
            ctx.getArgs().get(0);
        } catch(Exception e) {
            ctx.reply("You must ask a yes or no question use the `8ball` command");
            return;
        }
        ctx.reply("Magic 8 ball says: " + getRandomElement(OUTCOMES));
    }

    public String getRandomElement(String[] list) {
        Random rand = new Random();
        return list[rand.nextInt(list.length)];
    }
}
