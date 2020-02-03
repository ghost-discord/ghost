package com.github.coleb1911.ghost2.commands.modules.fun;

import com.github.coleb1911.ghost2.commands.meta.CommandContext;
import com.github.coleb1911.ghost2.commands.meta.Module;
import com.github.coleb1911.ghost2.commands.meta.ModuleInfo;
import com.github.coleb1911.ghost2.commands.meta.ReflectiveAccess;
import discord4j.core.object.util.Permission;
import discord4j.core.object.util.PermissionSet;
import ij.IJ;
import ij.ImagePlus;
import ij.process.ImageConverter;
import org.pmw.tinylog.Logger;

import javax.validation.constraints.NotNull;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;


public final class ModuleGrayscale extends Module {
    //temp directory
    private final Path TEMP_DIR = Files.createTempDirectory("tmp");

    @ReflectiveAccess
    public ModuleGrayscale() throws IOException {
        super(new ModuleInfo.Builder(ModuleGrayscale.class)
                .withName("grayscale")
                .withDescription("Convert an image to grayscale")
                .withAliases("gs", "gray")
                .withBotPermissions(PermissionSet.of(Permission.ATTACH_FILES))
                .withBotPermissions(PermissionSet.of(Permission.EMBED_LINKS))
        );
    }

    @Override
    public void invoke(@NotNull CommandContext ctx) {
        // Get image URL from attachment/argument
        String imageUrl;
        if (ctx.getAttachments().isEmpty()) {
            if (ctx.getArgs().isEmpty()) {
                ctx.replyBlocking("You didn't attach an image.");
                return;
            }
            imageUrl = ctx.getArgs().get(0);
        } else imageUrl = ctx.getAttachments().get(0).getUrl();

        // Validate URL
        try {
            new URI(imageUrl);
        } catch (URISyntaxException e) {
            ctx.replyBlocking("Invalid URL.");
        }

        // Validate image
        ImagePlus image = new ImagePlus(imageUrl);
        if (image.getWidth() == 0 && image.getHeight() == 0) {
            ctx.replyBlocking("Invalid image. Supported formats are TIFF, GIF and JPEG.");
            return;
        }

        // Convert image to grayscale
        image.getProcessor().convertToRGB();
        ImageConverter converter = new ImageConverter(image);
        converter.convertToGray16();

        // Generate new filename
        final String grayImgName = imageUrl.substring(imageUrl.lastIndexOf("/") + 1, imageUrl.lastIndexOf(".")) + "_grayscale.jpg";

        // Save output to temp directory
        final String outputPath = TEMP_DIR.resolve(grayImgName).toString();
        IJ.saveAs(image, "JPEG", outputPath);

        // Open stream
        final BufferedInputStream grayImgStream;
        try {
            grayImgStream = new BufferedInputStream(new FileInputStream(outputPath));
        } catch (FileNotFoundException e) {
            Logger.error(e);
            return;
        }

        // Reply with processed image
        ctx.replyBlocking(messageCreateSpec -> messageCreateSpec.addFile(grayImgName, grayImgStream));
    }
}
