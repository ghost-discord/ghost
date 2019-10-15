package com.github.coleb1911.ghost2.commands.modules.fun;

import com.github.coleb1911.ghost2.commands.meta.CommandContext;
import com.github.coleb1911.ghost2.commands.meta.Module;
import com.github.coleb1911.ghost2.commands.meta.ModuleInfo;
import com.github.coleb1911.ghost2.commands.meta.ReflectiveAccess;
import discord4j.core.object.VoiceState;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.TextChannel;
import discord4j.core.object.entity.User;
import discord4j.core.object.presence.Status;
import discord4j.core.object.util.Permission;

import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

public final class ModuleRandomUser extends Module {

    @ReflectiveAccess
    public ModuleRandomUser() {
        super(new ModuleInfo.Builder(ModuleRandomUser.class)
                .withName("randomuser")
                .withDescription("Return a random user")
                .withAliases("ruser"));
    }

    @Override
    public void invoke(@NotNull CommandContext ctx) {
        try{
            VoiceState callerState = ctx.getInvoker().getVoiceState().block();
            List<User> users = new ArrayList<>();
            // If the invoker is currently in a voice channel
            if(callerState !=null){
                users = callerState
                        .getChannel().block()
                        .getVoiceStates().collectList().block()
                        .stream().map(e -> e.getUser().block())
                        .collect(Collectors.toList());
                // If not, return a random user online in the current text channel
            } else{
                List<User> allUsers = ctx.getClient().getUsers().collectList().block();
                for(User user : allUsers){
                    Member member = user.asMember(ctx.getGuild().getId()).block();
                    if(!user.isBot() && !member.getPresence().block().getStatus().equals(Status.OFFLINE) && ((TextChannel)ctx.getChannel()).getEffectivePermissions(user.getId()).block().asEnumSet().contains(Permission.VIEW_CHANNEL))
                        users.add(user);
                }
            }
            if(!users.isEmpty()){
                Random r = new Random();
                User randomPick = users.get(r.nextInt(users.size()));
                ctx.reply("The random user is "+randomPick.getUsername());
            }else ctx.reply("There are no users to pick from");

        } catch(NullPointerException e){
            ctx.reply("Error when fetching information on the channel users");
        }

    }
}
