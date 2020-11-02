package com.github.coleb1911.ghost2.commands.modules.fun;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.coleb1911.ghost2.commands.meta.CommandContext;
import com.github.coleb1911.ghost2.commands.meta.Module;
import com.github.coleb1911.ghost2.commands.meta.ModuleInfo;
import com.github.coleb1911.ghost2.commands.meta.ReflectiveAccess;
import com.github.coleb1911.ghost2.utility.RestUtils;
import discord4j.core.object.util.Permission;
import discord4j.core.object.util.PermissionSet;
import org.jsoup.Jsoup;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

/**
 * @author chicago4550
 */



@Service
public final class ModuleCalculatorTest extends Module {

  @ReflectiveAccess
  public ModuleUrbanDictionary(){
    super(new ModuleInfo.Builder(ModuleCalculatorTest.class)

        .WithName("calculator")
        .withAliases("calc")
        .withDescription("Complete a 2-number calculation with either addition, subtraction, multiplication, or division")
        .withBotPermissions());

  }
  @Override
  @ReflectiveAccess
  public void involk(@NotNull final CommandContext ctx) {

        String[] testNewStrings = ctx;

        if (testNewStrings.length != 3) {
            ctx.getChannel().createMessage("this calculation is not in the correct format, please follow the format of (number calculation number)")
        }

        Boolean testBoolean = false;
        for (int i = 0; i < testNewStrings.length; i++) {
            if (i % 2 == 0 && testBoolean == false) {
                try {
                    float f = Float.parseFloat(testNewStrings[i]);
                    testStrings.add(String.valueOf(f));
                } catch (Exception e) {
                    ctx.getChannel().createMessage("You entered an invalid number, please try this command again with a valid number");
                    testBoolean = true;
                }
            }
            else if (testBoolean == false && i != testNewStrings.length - 1) {
                testStrings.add(testNewStrings[i]);
            }

        }
        testStrings.add(testNewStrings[testNewStrings.length - 1]);

        float result = 0;
        float num1 = Float.parseFloat((String) testStrings.get(0));
        float num2 = Float.parseFloat((String) testStrings.get(2));

        if(testStrings.get(1).compareTo("+") == 0){
            result = ad(num1, num2);
        }
        else if(testStrings.get(1).compareTo("-") == 0){
            result = sub(num1, num2);
        }
        else if(testStrings.get(1).compareTo("*") == 0){
            result = mult(num1, num2);
        }
        else if(testStrings.get(1).compareTo("/") == 0){
            result = div(num1, num2);
        }
        ctx.getChannel().createMessage("result is " + result);
  }
}
