package io.github.deltatango.pgjson.util;

import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class FileUtilTest {

    FileUtil fileUtil = new FileUtil();

    @Test
    void readBytesAsStringFromFile() throws IOException {
        String fileContent = fileUtil.readBytesAsStringFromFile("src/test/resources/testFile");
        assertEquals("VGVzdA==", fileContent);
    }

    @Test
    void readStringFromFile() throws IOException {
        String fileContent = fileUtil.readStringFromFile("src/test/resources/testFile");
        assertEquals("Test", fileContent);
    }
}