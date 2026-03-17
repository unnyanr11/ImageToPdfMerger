package com.example;

import java.nio.file.Path;
import java.util.stream.Stream;

public class ImageUtils {

    public static boolean isImageFile(Path path) {
        if (!java.nio.file.Files.isRegularFile(path)) return false;
        String name = path.getFileName().toString().toLowerCase();
        return name.endsWith(".jpg")
                || name.endsWith(".jpeg")
                || name.endsWith(".png")
                || name.endsWith(".bmp")
                || name.endsWith(".gif")
                || name.endsWith(".tiff")
                || name.endsWith(".tif");
    }

    public static Stream<Path> imageFilesIn(Path dir) {
        try {
            return java.nio.file.Files.list(dir)
                    .filter(ImageUtils::isImageFile);
        } catch (Exception e) {
            e.printStackTrace();
            return Stream.empty();
        }
    }
}
