package com.github.coleb1911.ghost2.commands.modules.fun;

import com.github.coleb1911.ghost2.commands.meta.ModuleInfo;
import com.github.coleb1911.ghost2.commands.meta.ReflectiveAccess;
import discord4j.rest.util.Permission;
import discord4j.rest.util.PermissionSet;
import ij.ImagePlus;
import ij.plugin.filter.GaussianBlur;

import java.io.IOException;


public final class ModuleBlur extends ImageManipulationModule {
    private static final String EFFECT_NAME = "blur";
    private static final double SIGMA_TO_SIZE_RELATION = 0.02;

    @ReflectiveAccess
    public ModuleBlur() throws IOException {
        super(new ModuleInfo.Builder(ModuleBlur.class)
                .withName(EFFECT_NAME)
                .withDescription("Blur image")
                .withAliases("blur", "blr")
                .withBotPermissions(PermissionSet.of(Permission.ATTACH_FILES))
                .withBotPermissions(PermissionSet.of(Permission.EMBED_LINKS))
        );
    }

    @Override
    protected void manipulate(ImagePlus image) {
        int longestSide = Math.max(image.getWidth(), image.getHeight());
        new GaussianBlur().blurGaussian(image.getProcessor(), longestSide * SIGMA_TO_SIZE_RELATION);
    }

    @Override
    protected String effectName() {
        return EFFECT_NAME;
    }
}
