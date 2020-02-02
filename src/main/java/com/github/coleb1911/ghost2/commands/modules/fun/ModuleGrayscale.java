package com.github.coleb1911.ghost2.commands.modules.fun;

import com.github.coleb1911.ghost2.commands.meta.CommandContext;
import com.github.coleb1911.ghost2.commands.meta.Module;
import com.github.coleb1911.ghost2.commands.meta.ModuleInfo;
import com.github.coleb1911.ghost2.commands.meta.ReflectiveAccess;
import discord4j.core.object.entity.Attachment;
import discord4j.core.object.util.Permission;
import discord4j.core.object.util.PermissionSet;
import ij.IJ;
import ij.ImagePlus;
import ij.process.ImageConverter;

import javax.validation.constraints.NotNull;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.Set;


public final class ModuleGrayscale extends Module {

    //temp directory
    private final Path TEMP_DIR = Files.createTempDirectory("tmp");

    @ReflectiveAccess
    public ModuleGrayscale() throws IOException {
        super(new ModuleInfo.Builder(ModuleGrayscale.class)
                .withName("Grayscale")
                .withDescription("Convert an image to black and white.")
                .withAliases("gs", "gray")
                .withBotPermissions(PermissionSet.of(Permission.ATTACH_FILES))
                .withBotPermissions(PermissionSet.of(Permission.EMBED_LINKS))
        );
    }

    @Override
    public void invoke(@NotNull CommandContext ctx) {
        Set<Attachment> attachments = ctx.getMsgAttachments();
        if (attachments.isEmpty()) {
            ctx.replyBlocking("No image found. :(");
        }

        Iterator<Attachment> attachmentList = attachments.iterator();
        Attachment origImg = attachmentList.next();
        String origImgName = origImg.getFilename();

        if (!origImgName.endsWith("jpg") && !origImgName.endsWith("png") && !origImgName.endsWith("jpeg"))
            ctx.replyBlocking("Incorrect image format! Must be JP(E)G or PNG.");
        else {
            //ImageJ converts img to grayscale and saves in temp directory
            ImagePlus ijImg = new ImagePlus(origImg.getUrl());
            ijImg.getProcessor().convertToRGB();

            ImageConverter converter = new ImageConverter(ijImg);
            converter.convertToGray16();

            IJ.saveAs(ijImg, "JPEG", TEMP_DIR + "/newImg");
            String newUrl = TEMP_DIR + "/newImg.jpg";

            ctx.getChannel().createMessage(embedCreateSpec -> {
                try {
                    BufferedInputStream grayImgStream = new BufferedInputStream(new FileInputStream(newUrl));
                    embedCreateSpec.addFile(origImgName.substring(0, origImgName.length() - 4) + "_(Grayscale)" + ".jpg", grayImgStream);
                } catch (FileNotFoundException e) {
                    ctx.replyBlocking("There was an issue processing the image.");
                }
            }).subscribe();
        }
    }
}
