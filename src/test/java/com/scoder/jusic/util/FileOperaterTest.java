package com.scoder.jusic.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FileOperaterTest {

    @TempDir
    Path tempDirectory;

    @Test
    void readsAndWritesWithoutChangingTheLegacyContentShape() throws Exception {
        Path file = tempDirectory.resolve("playlist.txt");
        String content = "first\nsecond\nthird";

        FileOperater.writefileinfo(content, file.toString());

        assertEquals(content, new String(Files.readAllBytes(file), Charset.defaultCharset()));
        assertEquals("firstsecondthird", FileOperater.commonReadFile(file.toFile()));
    }
}
