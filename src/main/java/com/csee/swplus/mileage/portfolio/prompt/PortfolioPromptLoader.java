package com.csee.swplus.mileage.portfolio.prompt;

import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Loads UTF-8 prompt fragments from {@code classpath:prompts/}. Cached after first read.
 */
@Component
public class PortfolioPromptLoader {

    private static final String ROOT = "prompts/";

    private final ConcurrentHashMap<String, String> cache = new ConcurrentHashMap<>();

    /**
     * @param relativePath e.g. {@code cv/head.txt} or {@code prompts/cv/head.txt}
     */
    public String load(String relativePath) {
        String path = normalize(relativePath);
        return cache.computeIfAbsent(path, this::readClasspathUtf8);
    }

    /** Default blue portfolio stylesheet (same as server-side HTML export; multiline + @import fonts). */
    public String defaultBlueCss() {
        return load("shared/default-blue-style.css").trim();
    }

    private static String normalize(String relativePath) {
        if (relativePath == null || relativePath.isEmpty()) {
            throw new IllegalArgumentException("prompt path required");
        }
        String p = relativePath.replace('\\', '/').trim();
        if (p.startsWith("/")) {
            p = p.substring(1);
        }
        return p.startsWith(ROOT) ? p : ROOT + p;
    }

    private String readClasspathUtf8(String classpathPath) {
        ClassLoader cl = PortfolioPromptLoader.class.getClassLoader();
        try (InputStream in = cl.getResourceAsStream(classpathPath)) {
            if (in == null) {
                throw new IllegalStateException("Missing classpath resource: " + classpathPath);
            }
            ByteArrayOutputStream buf = new ByteArrayOutputStream();
            byte[] b = new byte[8192];
            int n;
            while ((n = in.read(b)) != -1) {
                buf.write(b, 0, n);
            }
            return new String(buf.toByteArray(), StandardCharsets.UTF_8).replace("\r\n", "\n");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
