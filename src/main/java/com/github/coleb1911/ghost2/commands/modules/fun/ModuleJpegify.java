package com.github.coleb1911.ghost2.commands.modules.fun;

import com.github.coleb1911.ghost2.commands.meta.CommandContext;
import com.github.coleb1911.ghost2.commands.meta.Module;
import com.github.coleb1911.ghost2.commands.meta.ModuleInfo;
import com.github.coleb1911.ghost2.commands.meta.ReflectiveAccess;
import org.pmw.tinylog.Logger;

import java.awt.*;
import java.awt.image.BufferedImage;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import javax.validation.constraints.NotNull;
import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Iterator;

public final class ModuleJpegify extends Module {

    private final String FAILED = "Image creation failed. Please try again.";
    private final String OUTPUT_FILE_NAME = "jpeg_compressed.jpg";

    @ReflectiveAccess
    public ModuleJpegify() {
        super(new ModuleInfo.Builder(ModuleJpegify.class)
                .withName("jpegify")
                .withAliases("jpeg", "jpg")
                .withDescription("Compress an image using jpeg file compression")
                .showTypingIndicator());
    }

    @Override
    @ReflectiveAccess
    public void invoke(@NotNull final CommandContext ctx) {
        String link = null;

        // Check to see if the image attached is from a link
        if(ctx.getAttachments().isEmpty()) {
            if(!ctx.getArgs().isEmpty()) {
                link = ctx.getArgs().get(0);
            }
        } else {
            link = ctx.getAttachments().get(0).getUrl();
        }

        if(link == null) {
            ctx.replyBlocking("There is no image attached.");
            return;
        }

        if(!isValidLink(link)) {
            ctx.replyBlocking("The link you provided is invalid.");
            return;
        }

        URL url = null;
        BufferedImage img = null;
        try {
            url = new URL(link);
            img = ImageIO.read(url);
        } catch (Exception e) {
            ctx.replyBlocking("You must provide a link or image.");
            return;
        }

        if(!isValidImage(img)) {
            ctx.replyBlocking("Image is invalid.");
            return;
        }

        // Prevents bogus colorspace error
        BufferedImage copy = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = copy.createGraphics();
        g2d.setColor(Color.WHITE); // Or what ever fill color you want...
        g2d.fillRect(0, 0, copy.getWidth(), copy.getHeight());
        g2d.drawImage(img, 0, 0, null);
        g2d.dispose();

        // Create image and return
        if(!createImage(copy, ctx)) return;

        try (BufferedInputStream resultImageStream = new BufferedInputStream(new FileInputStream(OUTPUT_FILE_NAME))) {
            ctx.replyBlocking(messageCreateSpec -> messageCreateSpec.addFile(OUTPUT_FILE_NAME, resultImageStream));
        } catch (IOException e) {
            ctx.replyBlocking(FAILED);
            Logger.error(e);
        }

        // Delete the file that was output
        File file = new File(OUTPUT_FILE_NAME);
        file.delete();
    }

    private boolean isValidLink(String url) {
        try {
            new URI(url);
            return true;
        } catch (URISyntaxException e) {
            return false;
        }
    }

    private boolean isValidImage(BufferedImage img) {
        if(img == null) return false;
        return img.getWidth() != 0 && img.getHeight() != 0;
    }

    private boolean createImage(BufferedImage img, @NotNull final CommandContext ctx) {
        // get all image writers for JPG format
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpg");

        if (!writers.hasNext()) ctx.replyBlocking(FAILED);

        File compressedImageFile = new File(OUTPUT_FILE_NAME);

        OutputStream os = null;
        try {
            os = new FileOutputStream(compressedImageFile);
        } catch (FileNotFoundException e) {
            ctx.replyBlocking(FAILED);
            Logger.error(e);
            return false;
        }

        ImageWriter writer = (ImageWriter) writers.next();
        ImageOutputStream ios = null;
        try {
            ios = ImageIO.createImageOutputStream(os);
        } catch (IOException e) {
            ctx.replyBlocking(FAILED);
            Logger.error(e);
            return false;
        }
        writer.setOutput(ios);
        ImageWriteParam param = writer.getDefaultWriteParam();

        // compress to the given quality
        param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        param.setCompressionQuality(0.00005f);

        // appends a complete image stream containing a single image and
        //associated stream and image metadata and thumbnails to the output
        try {
            writer.write(null, new IIOImage(img, null, null), param);
        } catch (IOException e) {
            ctx.replyBlocking(FAILED);
            Logger.error(e);
            return false;
        }

        try {
            os.close();
            ios.close();
        } catch (IOException e) {
            ctx.replyBlocking(FAILED);
            Logger.error(e);
            return false;
        }
        writer.dispose();
        return true;
    }
}
