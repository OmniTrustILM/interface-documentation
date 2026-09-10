package com.otilm.openapi.codegen;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Handles file I/O operations for writing generated controller classes.
 */
public record GeneratedSourceWriter(String outputDirectory, String packageName) {

    /**
     * Writes the generated source code to a file in the appropriate package directory. Creates parent directories if
     * they don't exist.
     */
    public void writeImplementation(String className, String sourceCode) throws IOException {
        Path packagePath = Paths.get(outputDirectory, packageName.replace('.', File.separatorChar));
        Files.createDirectories(packagePath);

        Path javaFile = packagePath.resolve(className + ".java");
        Files.writeString(javaFile, sourceCode);
    }
}
