package cli;

import builtin_types.BuiltinTypes;
import exceptions.compile_time.CompilationException;
import util.CompileAll;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class SnuggleCli {
    public static void main(String[] args) throws CompilationException, IOException {
        String inputFileName = args[0];
        File inputFile = new File(inputFileName);
        Map<String, String> srcMap;
        if (inputFile.isDirectory()) {
            srcMap = search(inputFile.toPath());
        } else {
            srcMap = getSingle(inputFile);
        }
        CompileAll.compileAllToJar(
                new File("output.jar"),
                BuiltinTypes.standard(),
                srcMap
        );
    }

    public static Map<String, String> search(Path dir) throws IOException {
        Map<String, String> output = new HashMap<>();
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(dir)) {
            for (Path child : ds) {
                if (Files.isDirectory(child)) {
                    Map<String, String> children = search(child);
                    for (Map.Entry<String, String> c : children.entrySet()) {
                        output.put(child.getFileName().toString()+"/"+c.getKey(), c.getValue());
                    }
                } else if (child.getFileName().toString().endsWith(".txt")) {
                    output.put(
                            child.getFileName().toString().replaceAll("\\.txt$", ""),
                            getFileText(new File(child.toUri()))
                    );
                }
            }
        }
        return output;
    }

    public static Map<String, String> getSingle(File file) throws IOException {
        return Map.of(
                "main",
                getFileText(file)
        );
    }

    public static String getFileText(File file) throws IOException {
        FileInputStream fis = new FileInputStream(file);
        byte[] bytes = fis.readAllBytes();
        String src = new String(bytes, StandardCharsets.UTF_8);
        fis.close();
        return src;
    }
}
