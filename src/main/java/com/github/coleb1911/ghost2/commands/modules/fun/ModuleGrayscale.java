package com.github.coleb1911.ghost2.commands.modules.fun;

import com.github.coleb1911.ghost2.commands.meta.ModuleInfo;
import com.github.coleb1911.ghost2.commands.meta.ReflectiveAccess;
import discord4j.rest.util.Permission;
import discord4j.rest.util.PermissionSet;
import ij.ImagePlus;
import ij.process.ImageConverter;

import java.io.IOException;


public final class ModuleGrayscale extends ImageManipulationModule {
    private static final String EFFECT_NAME = "grayscale";

    @ReflectiveAccess
    public ModuleGrayscale() throws IOException {
        super(new ModuleInfo.Builder(ModuleGrayscale.class)
                .withName(EFFECT_NAME)
                .withDescription("Convert an image to grayscale")
                .withAliases("gs", "gray")
                .withBotPermissions(PermissionSet.of(Permission.ATTACH_FILES))
                .withBotPermissions(PermissionSet.of(Permission.EMBED_LINKS))
        );
    }

    @Override
    protected void manipulate(ImagePlus image) {
        image.getProcessor().convertToRGB();
        ImageConverter converter = new ImageConverter(image);
        converter.convertToGray16();
    }

    @Override
    protected String effectName() {
        return "grayscale";
    }
}
