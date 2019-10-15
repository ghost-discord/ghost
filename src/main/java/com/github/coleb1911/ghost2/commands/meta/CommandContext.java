package com.github.coleb1911.ghost2.commands.meta;

import discord4j.core.DiscordClient;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.VoiceState;
import discord4j.core.object.entity.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Contains relevant information for when a command is invoked.
 */
public class CommandContext {
    private final Guild guild;
    private final MessageChannel channel;
    private final Member invoker;
    private final Member self;
    private final Message message;
    private final List<String> args;
    private final String trigger;
    private final DiscordClient client;
    private final List<User> userMentions;
    private final List<Role> roleMentions;

    public CommandContext(MessageCreateEvent event) {
        this.guild = Objects.requireNonNull(event.getGuild().block());
        this.channel = event.getMessage().getChannel().block();
        this.invoker = event.getMember().orElse(null);
        this.self = Objects.requireNonNull(event.getClient().getSelf().block()).asMember(guild.getId()).block();
        this.message = event.getMessage();
        this.args = extractArgs(message);
        this.trigger = args.remove(0);
        this.client = event.getClient();
        this.userMentions = event.getMessage().getUserMentions().collectList().block();
        this.roleMentions = event.getMessage().getRoleMentions().collectList().block();
    }

    public Guild getGuild() {
        return guild;
    }

    public MessageChannel getChannel() {
        return channel;
    }

    public Member getInvoker() {
        return invoker;
    }

    public Member getSelf() {
        return self;
    }

    public Message getMessage() {
        return message;
    }

    public List<String> getArgs() {
        return args;
    }

    public String getTrigger() {
        return trigger;
    }

    public DiscordClient getClient() {
        return client;
    }

    public List<User> getUserMentions() {
        return userMentions;
    }

    public List<Role> getRoleMentions() {
        return roleMentions;
    }

    public void reply(String message) {
        channel.createMessage(message).subscribe();
    }

    public void replyDirect(String message) {
        invoker.getPrivateChannel().map(ch -> ch.createMessage(message).subscribe()).subscribe();
    }

    private List<String> extractArgs(Message message) {
        String content = message.getContent().orElse("").toLowerCase();
        // Arrays.asList returns an immutable list implementation, so we need to wrap it in an actual ArrayList
        return new ArrayList<>(Arrays.asList(content.split("\\p{javaSpaceChar}")));
    }
}
