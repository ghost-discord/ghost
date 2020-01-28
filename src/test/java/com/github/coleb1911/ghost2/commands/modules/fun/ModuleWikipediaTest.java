package com.github.coleb1911.ghost2.commands.modules.fun;

import com.github.coleb1911.ghost2.commands.meta.CommandContext;
import com.github.coleb1911.ghost2.utility.RestUtils;
import com.github.tomakehurst.wiremock.WireMockServer;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.core.spec.MessageCreateSpec;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.function.Consumer;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.*;

public class ModuleWikipediaTest {
    WireMockServer wireMockServer;

    @BeforeEach
    public void setup() {
        wireMockServer = new WireMockServer(8090);
        wireMockServer.start();
        setupStub();
    }

    private void setupStub() {
        wireMockServer.stubFor(get(urlPathEqualTo("/w/api.php"))
            .withQueryParam("srsearch", equalTo("Azmec"))
            .withQueryParam( "action", equalTo("query"))
            .willReturn(aResponse().withHeader("Content-Type", "application/json")
            .withStatus(200)
            .withBodyFile("json/azmec.json")));
        wireMockServer.stubFor(get(urlPathEqualTo("/w/api.php"))
            .withQueryParam("srsearch", equalTo("Nelson Mandela"))
            .withQueryParam( "action", equalTo("query"))
            .willReturn(aResponse().withHeader("Content-Type", "application/json")
            .withStatus(200)
            .withBodyFile("json/nelson-mandela.json")));

    }

    @AfterEach
    public void teardown() {
        wireMockServer.stop();
    }

    @Test
    public void testWikiNeslonMandela() {
        final RestTemplate restTemplate = RestUtils.defaultRestTemplate();
        ModuleWikipedia wikiModule = new ModuleWikipedia(wireMockServer.baseUrl(), restTemplate);
        CommandContext mockContext = mock(CommandContext.class, RETURNS_DEEP_STUBS);
        when(mockContext.getArgs()).thenReturn(List.of("Nelson Mandela"));
        ArgumentCaptor<Consumer<? super MessageCreateSpec>> messageCreatorCaptor = ArgumentCaptor.forClass(Consumer.class);
        wikiModule.invoke(mockContext);
        verify(mockContext.getChannel()).createMessage(messageCreatorCaptor.capture());

        MessageCreateSpec mockCreateSpec = mock(MessageCreateSpec.class);
        ArgumentCaptor<Consumer<? super EmbedCreateSpec>> embedCreatorCaptor = ArgumentCaptor.forClass(Consumer.class);
        messageCreatorCaptor.getValue().accept(mockCreateSpec);
        verify(mockCreateSpec).setEmbed(embedCreatorCaptor.capture());
    }

    @Test
    public void testSuggestion() {
        final RestTemplate template = RestUtils.defaultRestTemplate();
        ModuleWikipedia wikiModule = new ModuleWikipedia(wireMockServer.baseUrl(), template);
        CommandContext mockContext = mock(CommandContext.class, RETURNS_DEEP_STUBS);
        when(mockContext.getArgs()).thenReturn(List.of("Azmec"));
        wikiModule.invoke(mockContext);

        ArgumentCaptor<String> reply = ArgumentCaptor.forClass(String.class);
        verify(mockContext).replyBlocking(reply.capture());
        assertEquals("Did you mean \"aztecs\"?", reply.getValue());


    }
}
