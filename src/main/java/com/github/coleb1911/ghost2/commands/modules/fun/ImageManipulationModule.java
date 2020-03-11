package com.github.coleb1911.ghost2.commands.modules.fun;

import com.github.coleb1911.ghost2.commands.meta.CommandContext;
import com.github.coleb1911.ghost2.commands.meta.Module;
import com.github.coleb1911.ghost2.commands.meta.ModuleInfo;
import ij.IJ;
import ij.ImagePlus;
import org.pmw.tinylog.Logger;

import javax.validation.constraints.NotNull;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

public abstract class ImageManipulationModule extends Module {
    private final Path TEMP_DIR = Files.createTempDirectory("tmp");

    public ImageManipulationModule(ModuleInfo.Builder info) throws IOException {
        super(info);
    }

    @Override
    public void invoke(@NotNull CommandContext ctx) {
        String link = parseImageLink(ctx);

        if (Objects.isNull(link)) {
            ctx.replyBlocking("You didn't attach an image.");
            return;
        }

        if (!isValidUrl(link)) {
            ctx.replyBlocking("Invalid URL.");
            return;
        }

        ImagePlus image = new ImagePlus(link);

        if (!isValidImage(image)) {
            ctx.replyBlocking("Invalid image. Supported formats are TIFF, GIF and JPEG.");
            return;
        }

        manipulate(image);

        String resultImageFilename = createTmpFilename(link);
        String outputPath = saveImageTo(image, resultImageFilename);

        try (BufferedInputStream resultImageStream = new BufferedInputStream(new FileInputStream(outputPath))) {
            ctx.replyBlocking(messageCreateSpec -> messageCreateSpec.addFile(resultImageFilename, resultImageStream));
        } catch (IOException e) {
            Logger.error(e);
        }
    }

    private String saveImageTo(ImagePlus image, String filePath) {
        final String outputPath = TEMP_DIR.resolve(filePath).toString();
        IJ.saveAs(image, "JPEG", outputPath);
        return outputPath;
    }

    private String createTmpFilename(String link) {
        return link.substring(link.lastIndexOf("/") + 1, link.lastIndexOf(".")) + effectName() + ".jpg";
    }

    private String parseImageLink(@NotNull CommandContext ctx) {
        String imageUrl = null;

        if (ctx.getAttachments().isEmpty()) {
            if (!ctx.getArgs().isEmpty()) {
                imageUrl = ctx.getArgs().get(0);
            }
        } else {
            imageUrl = ctx.getAttachments().get(0).getUrl();
        }

        return imageUrl;
    }

    private boolean isValidUrl(String url) {
        try {
            new URI(url);
            return true;
        } catch (URISyntaxException e) {
            return false;
        }
    }

    private boolean isValidImage(ImagePlus image) {
        return image.getWidth() == 0 && image.getHeight() == 0;
    }

    protected abstract void manipulate(ImagePlus image);

    protected abstract String effectName();
}
