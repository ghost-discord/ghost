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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Contains relevant information for when a command is invoked.
 */
public class CommandContext {
    private final DiscordClient client;     // Client that received the message event (i.e. us)
    private final Message message;          // Message that triggered the command
    private final Guild guild;              // Guild the message was sent in
    private final MessageChannel channel;   // Channel the message was sent in
    private final Member invoker;           // User that invoked the command (as a member of the guild)
    private final Member self;              // The bot user (as a member of the guild)
    private final List<String> args;        // Arguments passed to the command (split according to whitespace)
    private final List<User> userMentions;  // Users mentioned in the message
    private final List<Role> roleMentions;  // Roles mentioned in the message

    public CommandContext(MessageCreateEvent event) {
        client = event.getClient();
        message = event.getMessage();
        guild = event.getGuild().blockOptional().orElseThrow();
        channel = message.getChannel().blockOptional().orElseThrow();
        invoker = event.getMember().orElseThrow();
        self = event.getClient().getSelfId().map(guild::getMemberById).map(Mono::block).orElseThrow();
        args = extractArgs(message);
        userMentions = message.getUserMentions().collectList().blockOptional().orElseThrow();
        roleMentions = message.getRoleMentions().collectList().blockOptional().orElseThrow();
    }

    public DiscordClient getClient() {
        return client;
    }

    public Message getMessage() {
        return message;
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

    public List<String> getArgs() {
        return Collections.unmodifiableList(args);
    }

    public List<User> getUserMentions() {
        return Collections.unmodifiableList(userMentions);
    }

    public List<Role> getRoleMentions() {
        return Collections.unmodifiableList(roleMentions);
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

    private static List<String> extractArgs(Message message) {
        String[] components = message.getContent()
                .map(msg -> msg.split("\\p{javaSpaceChar}"))
                .orElse(new String[0]);
        return Arrays.stream(components)
                .skip(1)
                .collect(Collectors.toList());
    }
}
