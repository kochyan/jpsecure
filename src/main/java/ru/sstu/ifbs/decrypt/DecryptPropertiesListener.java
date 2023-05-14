package ru.sstu.ifbs.decrypt;

import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MutablePropertySources;
import ru.sstu.ifbs.decrypt.decryptors.PropertyDecryptor;
import ru.sstu.ifbs.decrypt.decryptors.impl.DefaultPropertyDecryptor;

import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static org.apache.logging.log4j.util.Strings.isBlank;
import static org.apache.logging.log4j.util.Strings.isNotBlank;
import static ru.sstu.ifbs.decrypt.util.DecryptConstants.*;

public class DecryptPropertiesListener implements ApplicationListener<ApplicationEnvironmentPreparedEvent> {
    private static final Logger logger = Logger.getLogger(DecryptPropertiesListener.class.getName());
    private String secretKey = null;
    private String algorithm = null;
    private String decryptorClassname = null;
    private PropertyDecryptor decryptor = null;

    public void onApplicationEvent(ApplicationEnvironmentPreparedEvent event) {
        ConfigurableEnvironment environment = event.getEnvironment();
        decryptorClassname = environment.getProperty(CUSTOM_DECRYPTOR_PROPERTY_NAME, "");
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
                    initKeyAndAlgorithm(propertyMap);
                    initDecryptor();
                }
                final List<Map.Entry<String, Object>> encryptedProperties = findEncryptedProperties(propertyMap.entrySet());
                if (encryptedProperties.isEmpty()) {
                    return;
                }
                final List<Map.Entry<String, String>> decryptedProperties = decryptor.decryptProperties(encryptedProperties);
                final Map<String, Object> decryptedMap = mergeDecryptedProperties(decryptedProperties, propertyMap);
                propertySources.replace(sourceName, new DecryptedPropertySource(sourceName, decryptedMap));
            } else {
                throw new RuntimeException("Unsupported property source type");
            }
        });
    }

    private Map<String, Object> mergeDecryptedProperties(List<Map.Entry<String, String>> decryptedProperties, Map<String, Object> encryptedProperties) {
        final Map<String, Object> decryptedMap = new HashMap<>(encryptedProperties);
        decryptedProperties.forEach(decryptedEntry -> decryptedMap.put(decryptedEntry.getKey(), decryptedEntry.getValue()));
        return Collections.unmodifiableMap(decryptedMap);
    }


    private List<Map.Entry<String, Object>> findEncryptedProperties(Set<Map.Entry<String, Object>> properties) {
        return properties.stream()
                .filter(propertyEntry -> propertyEntry.getValue().toString().startsWith(DECRYPT_MARKER))
                .collect(Collectors.toList());
    }

    private void initKeyAndAlgorithm(final Map<String, Object> properties) {
        secretKey = properties.getOrDefault(DECRYPT_KEY_PROPERTY_NAME, "").toString();
        algorithm = properties.getOrDefault(DECRYPT_ALGORITHM_PROPERTY_NAME, "").toString();
        if (isBlank(secretKey) || isBlank(algorithm)) {
            throw new RuntimeException("Decryption key or algorithm is not specified");
        }
    }

    private void initDecryptor() {
        if (isNotBlank(decryptorClassname)) {
            try {
                decryptor = createInstance();
            } catch (Exception e) {
                throw new RuntimeException("Error during creating custom decryptor: " + e);
            }
        } else {
            decryptor = new DefaultPropertyDecryptor(secretKey, algorithm);
        }
    }

    private PropertyDecryptor createInstance() {
        try {
            return (PropertyDecryptor) Class.forName(decryptorClassname)
                    .getDeclaredConstructor(String.class, String.class)
                    .newInstance(secretKey, algorithm);
        } catch (Exception e) {
            logger.warning("Can`t create new instance using constructor (String key, String algorithm), error: " + e.getMessage());
        }
        try {
            return (PropertyDecryptor) Class.forName(decryptorClassname)
                    .getDeclaredConstructor()
                    .newInstance();
        } catch (Exception e) {
            logger.warning("Can`t create new instance using empty constructor, error: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }
}
