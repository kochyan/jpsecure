package ru.sstu.ifbs.decrypt.util;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Set;

public interface DecryptConstants {
    Set<String> IGNORED_SOURCES = Set.of("configurationProperties", "systemProperties", "random");
    String HIGH_PRIORITY_SOURCE = "systemEnvironment";
    String DECRYPT_KEY_PROPERTY_NAME = "decryptKey";
    String DECRYPT_ALGORITHM_PROPERTY_NAME = "decryptAlgorithm";
    String CUSTOM_DECRYPTOR_PROPERTY_NAME = "decrypt.custom.decryptor.class";
    String DECRYPT_MARKER = "<dec> ";
    Charset ISO_CHARSET = StandardCharsets.ISO_8859_1;
}
