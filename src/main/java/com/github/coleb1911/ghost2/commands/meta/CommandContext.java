package com.github.coleb1911.ghost2.commands.meta;

import discord4j.core.DiscordClient;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.MessageChannel;
import discord4j.core.object.entity.Role;
import discord4j.core.object.entity.User;
import discord4j.core.spec.EmbedCreateSpec;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

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

    /**
     * Reply to a command.
     *
     * @param text Reply message content
     * @return {@linkplain Mono} containing the reply {@linkplain Message}
     */
    public Mono<Message> reply(String text) {
        return channel.createMessage(text);
    }

    /**
     * Reply to a command via direct message.
     *
     * @param text Reply message content
     * @return {@linkplain Mono} containing the reply {@linkplain Message}
     */
    public Mono<Message> replyDirect(String text) {
        return invoker.getPrivateChannel()
                .flatMap(ch -> ch.createMessage(text));
    }

    /**
     * Reply to a command with an embed.
     *
     * @param consumer Embed spec consumer
     * @return {@linkplain Mono} containing the reply {@linkplain Message}
     * @see <a href="https://github.com/Discord4J/Discord4J/wiki/Specs">Specs</a>
     */
    public Mono<Message> replyEmbed(Consumer<EmbedCreateSpec> consumer) {
        return channel.createEmbed(consumer);
    }

    /**
     * Reply to a command. Blocks until finished.
     *
     * @param text Reply message content
     * @return The reply {@linkplain Message}
     */
    public Message replyBlocking(String text) {
        return reply(text).block();
    }

    /**
     * Reply to a command via direct message. Blocks until finished.
     *
     * @param text Reply message content
     * @return The reply {@linkplain Message}
     */
    public Message replyDirectBlocking(String text) {
        return replyDirect(text).block();
    }

    /**
     * Reply to a command with an embed. Blocks until finished.
     *
     * @param consumer Embed spec consumer
     * @return The reply {@linkplain Message}
     * @see <a href="https://github.com/Discord4J/Discord4J/wiki/Specs">Specs</a>
     */
    public Message replyEmbedBlocking(Consumer<EmbedCreateSpec> consumer) {
        return replyEmbed(consumer).block();
    }

    private List<String> extractArgs(Message message) {
        String content = message.getContent().orElse("");
        // Arrays.asList returns an immutable list implementation, so we need to wrap it in an actual ArrayList
        return new ArrayList<>(Arrays.asList(content.split("\\p{javaSpaceChar}")));
    }
}
