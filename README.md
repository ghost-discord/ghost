![ghost2](https://github.com/cbryant02/cbryant02.github.io/raw/master/media/ghost2_banner.png)

# ghost2
[ghost](https://github.com/cbryant02/ghost)'s spritual successor, featuring magical Spring JPA-powered database code, a super-lightweight embedded H2 database, more flexible command modules, Discord4J 3, and approximately 85.4% less spaghetti.

Pull requests are still welcome. JavaDocs are not written yet, but the codebase isn't yet large, complicated, or spaghettified. With some knowledge of Spring, you'll find your way around fine.

## Configuring ghost2
ghost2 reads configuration values from a property file. Provide `ghost.properties` somewhere on the classpath, and in it, only the following value:

| key         | value     |
|-------------|-----------|
| ghost.token | Bot token |

You can obtain a bot token from [Discord's developer site](https://discordapp.com/developers/). See [this guide](https://github.com/reactiflux/discord-irc/wiki/Creating-a-discord-bot-&-getting-a-token) if you need help.

## Running
Launch with Spring Boot via the `bootRun` Gradle task. Unlike its younger sibling, ghost2 will automatically set up the database for you with Spring and Hibernate.

Public single-jar builds will be available when ghost2 has more functionality implemented.