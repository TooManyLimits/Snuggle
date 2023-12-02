package util;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;
import java.util.function.Consumer;

public class IOUtil {

    public static String getResource(String path) {
        try(var stream = Thread.currentThread().getContextClassLoader().getResourceAsStream(path)) {
            if (stream == null) throw new IOException();
            return new String(stream.readAllBytes());
        } catch (IOException e) {
            throw new IllegalArgumentException("Could not find resource at path \"" + path + "\".", e);
        }
    }

    public static void applyRecursive(Path start, Consumer<Path> forEach) {
        File file = start.toFile();
        if (file.isDirectory()) {
            for (File innerfile : Objects.requireNonNull(file.listFiles()))
                applyRecursive(innerfile.toPath(), forEach);
        } else {
            forEach.accept(start);
        }
    }

}
