package com.proyectoestadistico.i18n;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.Properties;

public final class UiMessages {

    private static final Properties props = new Properties();
    private static boolean english;

    static {
        setEnglish(false);
    }

    private UiMessages() {}

    public static void setEnglish(boolean en) {
        english = en;
        props.clear();
        String suffix = en ? "en" : "es";
        String path = "/com/proyectoestadistico/i18n/messages_" + suffix + ".properties";
        try (InputStream in = UiMessages.class.getResourceAsStream(path)) {
            if (in == null) {
                throw new IllegalStateException("Missing resource: " + path);
            }
            props.load(new InputStreamReader(in, StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    public static boolean isEnglish() {
        return english;
    }

    public static String get(String key) {
        return props.getProperty(key, key);
    }

    public static String get(String key, Object... args) {
        String pattern = get(key);
        return MessageFormat.format(pattern, args);
    }
}
