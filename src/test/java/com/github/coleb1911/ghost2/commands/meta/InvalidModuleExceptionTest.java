package com.github.coleb1911.ghost2.commands.meta;

import com.github.coleb1911.ghost2.commands.modules.moderation.ModuleKick;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class InvalidModuleExceptionTest {

    @Test
    @ReflectiveAccess
    void exceptionConstructor() {

        InvalidModuleException ime = new InvalidModuleException(ModuleKick.class, InvalidModuleException.Reason.EXCEPTION_IN_CONSTRUCTOR);
        Assertions.assertEquals("Module subclass com.github.coleb1911.ghost2.commands.modules.moderation.ModuleKick is written incorrectly. Refer to the wiki for help. (Reason(s): Module threw an exception in its constructor. )",
                ime.getMessage());
    }

}
