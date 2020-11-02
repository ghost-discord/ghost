package com.github.coleb1911.ghost2.commands.modules.fun;

import com.github.coleb1911.ghost2.commands.meta.CommandContext;
import com.github.coleb1911.ghost2.commands.meta.Module;
import com.github.coleb1911.ghost2.commands.meta.ModuleInfo;
import com.github.coleb1911.ghost2.commands.meta.ReflectiveAccess;
import org.springframework.stereotype.Service;

import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * @author chicago4550
 */



@Service
public final class ModuleCalculator extends Module {

  @ReflectiveAccess
  public ModuleCalculator(){
    super(new ModuleInfo.Builder(ModuleCalculator.class)
            .withName("Calculator")
            .withAliases("calc")
            .withDescription("Complete a 2-number calculation with either addition, subtraction, multiplication, or division"));

  }

    /**
     * Constructs a new Module. Should only be used by subclasses of Module.
     *
     * @param info ModuleInfo.Builder with the subclass' metadata
     */
    protected ModuleCalculator(ModuleInfo.Builder info) {
        super(info);
    }

    @Override
    @ReflectiveAccess
    public void invoke(@NotNull CommandContext ctx) {


        List<String> Args = ctx.getArgs();

        if (Args.size() != 3) {
            ctx.getChannel().createMessage("this calculation is not in the correct format, please follow the format of (number calculation number)");
        }

        boolean incorrectInput = false;
        for (int i = 0; i < Args.size(); i++) {
            if (i % 2 == 0 && !incorrectInput) {
                try {
                    double f = Double.parseDouble(Args.get(i));
                } catch (Exception e) {
                    ctx.getChannel().createMessage("You entered an invalid number, please try this command again with a valid number");
                    incorrectInput = true;
                }
            }

        }

        double result = 0;
        double num1 = Double.parseDouble((String) Args.get(0));
        double num2 = Double.parseDouble((String) Args.get(2));

        if(Args.get(1).compareTo("+") == 0){
            result = num1 + num2;
        }
        else if(Args.get(1).compareTo("-") == 0){
            result = num1 - num2;
        }
        else if(Args.get(1).compareTo("*") == 0){
            result = num1 * num2;
        }
        else if(Args.get(1).compareTo("/") == 0){
            result = num1 / num2;
        }
        ctx.getChannel().createMessage("result is " + result);
    }

/*
    //@ReflectiveAccess
  public void involk(@NotNull final CommandContext ctx) {


        List<String> Args = ctx.getArgs();

        if (Args.size() != 3) {
            ctx.getChannel().createMessage("this calculation is not in the correct format, please follow the format of (number calculation number)");
        }

        boolean incorrectInput = false;
        for (int i = 0; i < Args.size(); i++) {
            if (i % 2 == 0 && !incorrectInput) {
                try {
                    double f = Double.parseDouble(Args.get(i));
                } catch (Exception e) {
                    ctx.getChannel().createMessage("You entered an invalid number, please try this command again with a valid number");
                    incorrectInput = true;
                }
            }

        }

        double result = 0;
        double num1 = Double.parseDouble((String) Args.get(0));
        double num2 = Double.parseDouble((String) Args.get(2));

        if(Args.get(1).compareTo("+") == 0){
            result = num1 + num2;
        }
        else if(Args.get(1).compareTo("-") == 0){
            result = num1 - num2;
        }
        else if(Args.get(1).compareTo("*") == 0){
            result = num1 * num2;
        }
        else if(Args.get(1).compareTo("/") == 0){
            result = num1 / num2;
        }
        ctx.getChannel().createMessage("result is " + result);
  }

//    @Override
//    public void invoke(@NotNull CommandContext ctx) {

//    }
*/

}
