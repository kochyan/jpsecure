package ru.sstu.ifbs.listener;

import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;
import ru.sstu.ifbs.decrypt.decryptors.PropertyDecryptor;
import ru.sstu.ifbs.decrypt.decryptors.impl.DefaultPropertyDecryptor;
import ru.sstu.ifbs.propertyloader.PropertyLoader;

import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static org.apache.logging.log4j.util.Strings.isBlank;
import static org.apache.logging.log4j.util.Strings.isNotBlank;
import static ru.sstu.ifbs.util.DecryptConstants.*;

public class ApplicationPreparedEventListener implements ApplicationListener<ApplicationEnvironmentPreparedEvent> {
    private static final Logger logger = Logger.getLogger(ApplicationPreparedEventListener.class.getName());
    private String secretKey = null;
    private String algorithm = null;
    private String decryptorClassname = null;
    private PropertyDecryptor decryptor = null;

    @Override
    public void onApplicationEvent(ApplicationEnvironmentPreparedEvent event) {
        initLoaderAndLoadCustomProperties(event);
        ConfigurableEnvironment environment = event.getEnvironment();
        decryptorClassname = environment.getProperty(CUSTOM_DECRYPTOR_PROPERTY_NAME, "");
        MutablePropertySources propertySources = environment.getPropertySources();
        propertySources.stream().forEach(propertySource -> {
            Optional<DecryptedPropertySource> decryptedSource = decryptPropertySource(propertySource);
            decryptedSource.ifPresent(decryptedPropertySource -> propertySources.replace(propertySource.getName(), decryptedPropertySource));
        });
    }

    private void initLoaderAndLoadCustomProperties(ApplicationEnvironmentPreparedEvent event) {
        String loaderClassname = event.getEnvironment().getProperty(CUSTOM_LOADER_PROPERTY_NAME);
        if (isBlank(loaderClassname)) {
            return;
        }
        PropertyLoader propertyLoader = null;
        try {
            propertyLoader = (PropertyLoader) Class.forName(loaderClassname).newInstance();
        } catch (Exception e) {
            logger.warning("Error during creating custom decryptor: " + e);
            return;
        }

        propertyLoader.loadPropertySources().forEach(event.getEnvironment().getPropertySources()::addLast);
    }

    private Optional<DecryptedPropertySource> decryptPropertySource(PropertySource<?> propertySource) {
        final String sourceName = propertySource.getName();
        if (IGNORED_SOURCES.contains(sourceName)) {
            return Optional.empty();
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
                return Optional.empty();
            }
            final List<Map.Entry<String, String>> decryptedProperties = decryptor.decryptProperties(encryptedProperties);
            final Map<String, Object> decryptedMap = mergeDecryptedProperties(decryptedProperties, propertyMap);
            return Optional.of(new DecryptedPropertySource(sourceName, decryptedMap));
        } else {
            logger.warning("Unsupported property source: " + source.getClass().getName());
            return Optional.empty();
        }
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

    static class DecryptedPropertySource extends PropertySource<Map<String, Object>> {

        DecryptedPropertySource(String name, Map<String, Object> source) {
            super(name, source);
        }

        @Override
        public Object getProperty(String name) {
            return source.getOrDefault(name, null);
        }
    }
}