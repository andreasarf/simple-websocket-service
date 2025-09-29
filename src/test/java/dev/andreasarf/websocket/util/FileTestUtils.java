package dev.andreasarf.websocket.util;

import lombok.experimental.UtilityClass;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.FileCopyUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;

@UtilityClass
public class FileTestUtils {

    public static InputStream readFile(final String fileName) throws IOException {
        return new ClassPathResource(fileName).getInputStream();
    }

    public static InputStreamReader readFileIntoReader(final String fileName) throws IOException {
        return new InputStreamReader(new ClassPathResource(fileName).getInputStream(), StandardCharsets.UTF_8);
    }

    public static String readFileToString(final String fileName) throws IOException {
        try (final Reader reader = readFileIntoReader(fileName)) {
            return FileCopyUtils.copyToString(reader);
        }
    }

    public static byte[] readFileToByteArray(final String fileName) throws IOException {
        return FileCopyUtils.copyToByteArray(readFile(fileName));
    }
}
