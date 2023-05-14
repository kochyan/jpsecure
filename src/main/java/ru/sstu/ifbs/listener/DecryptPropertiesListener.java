package ru.sstu.ifbs.listener;

import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.security.Key;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static java.nio.charset.StandardCharsets.ISO_8859_1;

public class DecryptPropertiesListener implements ApplicationListener<ApplicationEnvironmentPreparedEvent> {
    private static final Set<String> IGNORED_SOURCES = Set.of("configurationProperties", "systemProperties", "random");
    private static final String DECRYPT_MARKER = "<dec> ";

    private static final String HIGH_PRIORITY_SOURCE = "systemEnvironment";
    private static final String DECRYPT_KEY_PROPERTY_NAME = "decryptKey";
    private static final String DECRYPT_ALGORITHM_PROPERTY_NAME = "decryptAlgorithm";

    private String highPriorityKey = null;
    private String highPriorityAlgorithm = null;

    public void onApplicationEvent(ApplicationEnvironmentPreparedEvent event) {
        ConfigurableEnvironment environment = event.getEnvironment();
        MutablePropertySources propertySources = environment.getPropertySources();
        propertySources.stream().forEach(propertySource -> {
            final String sourceName = propertySource.getName();
            if (IGNORED_SOURCES.contains(sourceName)) {
                return;
            }
            final Object source = propertySource.getSource();
            if (source instanceof Map) {
                final Map<String, Object> propertyMap = (Map<String, Object>) source;
                if (sourceName.equals(HIGH_PRIORITY_SOURCE)) {
                    highPriorityKey = propertyMap.getOrDefault(DECRYPT_KEY_PROPERTY_NAME, "").toString();
                    highPriorityAlgorithm = propertyMap.getOrDefault(DECRYPT_ALGORITHM_PROPERTY_NAME, "").toString();
                }
                final Set<Map.Entry<String, Object>> propertyEntries = propertyMap.entrySet();
                final List<Map.Entry<String, Object>> encryptedProperties = propertyEntries.stream()
                        .filter(propertyEntry -> propertyEntry.getValue().toString().startsWith(DECRYPT_MARKER))
                        .collect(Collectors.toList());
                if (encryptedProperties.isEmpty()) {
                    return;
                }
                final List<Map.Entry<String, String>> decryptedProperties = encryptedProperties.stream().map(prop -> {
                    try {
                        final byte[] encryptedProperty = prop.getValue().toString().substring(DECRYPT_MARKER.length()).getBytes(ISO_8859_1);
                        final Key key = new SecretKeySpec(highPriorityKey.getBytes(ISO_8859_1), highPriorityAlgorithm);
                        final Cipher cipher = Cipher.getInstance(highPriorityAlgorithm);

                        cipher.init(Cipher.DECRYPT_MODE, key);
                        return Map.entry(prop.getKey(), new String(cipher.doFinal(encryptedProperty), ISO_8859_1));
                    } catch (Exception e) {
                        throw new RuntimeException("Got unexpected error during decryption: " + e.getMessage());
                    }
                }).collect(Collectors.toList());

                if (decryptedProperties.isEmpty()) {
                    return;
                }

                final Map<String, Object> decryptedMap = new HashMap<>(propertyMap);
                decryptedProperties.forEach(decryptedEntry -> decryptedMap.put(decryptedEntry.getKey(), decryptedEntry.getValue()));
                propertySources.replace(sourceName, new DecryptedPropertySource(sourceName, Collections.unmodifiableMap(decryptedMap)));
            }
        });
    }

    static class DecryptedPropertySource extends PropertySource<Map<String, Object>> {

        public DecryptedPropertySource(String name, Map<String, Object> source) {
            super(name, source);
        }

        @Override
        public Object getProperty(String name) {
            return source.getOrDefault(name, null);
        }
    }
}
