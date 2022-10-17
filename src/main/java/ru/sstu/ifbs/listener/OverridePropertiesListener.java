package ru.sstu.ifbs.listener;

import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.io.File;
import java.lang.reflect.Field;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

@Order
public class OverridePropertiesListener implements ApplicationListener<ApplicationEnvironmentPreparedEvent> {
    private static final Logger logger = Logger.getLogger(OverridePropertiesListener.class.getName());
    private static final String SECURE_PROPERTY_ENABLED_NAME = "secure-property.enabled";
    private static final String SECURE_PROPERTY_ENABLED_VALUE = "true";
    private static final String SECURE_PROPERTY_PREFIX_NAME = "secure-property.prefix";
    private static final String SECURE_PROPERTY_PREFIX_DEFAULT_VALUE = "<*>";
    private static final String SECURE_PROPERTY_PREFIX_NAME_TEMPLATE = "${" + SECURE_PROPERTY_PREFIX_NAME + "}";

    private boolean customPrefixUsed = false;
    private String propertyPrefix;

    public void onApplicationEvent(ApplicationEnvironmentPreparedEvent event) {
        ConfigurableEnvironment environment = event.getEnvironment();
        setUpProperties(environment);
        MutablePropertySources propertySources = environment.getPropertySources();
        Iterator<PropertySource<?>> propertyIterator = propertySources.iterator();
        while (propertyIterator.hasNext()) {
            PropertySource<?> propertySource = propertyIterator.next();
            if (propertySource.containsProperty(SECURE_PROPERTY_ENABLED_NAME)) {
                final Object objValue = propertySource.getProperty(SECURE_PROPERTY_ENABLED_NAME);
                if (objValue instanceof String) {
                    String value = (String) objValue;
                    if (SECURE_PROPERTY_ENABLED_VALUE.equals(value)) {
                        Object objSource = propertySource.getSource();
                        if (objSource instanceof Map) {
                            Map<String, Object> mapSource = (Map<String, Object>) objSource;
                            try {
                                if (!isModifiable(mapSource)) {
                                    Field innerMapField = mapSource.getClass().getDeclaredField("m");
                                    innerMapField.setAccessible(true);
                                    Map<String, Object> innerMap = (Map<String, Object>) innerMapField.get(mapSource);
                                    overrideValuesIfNeed(innerMap);
                                    innerMapField.setAccessible(false);
                                } else {
                                    overrideValuesIfNeed(mapSource);
                                }
                            } catch (Exception e) {
                                logger.log(Level.WARNING, e.getMessage());
                            }
                        }
                    }
                }
            }
        }
    }

    private boolean isModifiable(Map<String, Object> map) {
        try {
            map.replace(SECURE_PROPERTY_ENABLED_NAME, SECURE_PROPERTY_ENABLED_VALUE);
            return true;
        } catch (UnsupportedOperationException e) {
            return false;
        }
    }

    private void overrideValuesIfNeed(Map<String, Object> map) throws URISyntaxException, FileNotFoundException {
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String propertyValue = entry.getValue().toString();
            if ((propertyValue.startsWith(getPropertyPrefix()) || (propertyValue.startsWith(SECURE_PROPERTY_PREFIX_NAME_TEMPLATE) && customPrefixUsed))
                    && (propertyValue.length() > getPropertyPrefix().length())) {
                String propertyName = entry.getKey();
                String propertyPath = deletePrefix(propertyValue);
                Path absolutePath = getAbsolutePath(propertyPath);
                File file = new File(absolutePath.toUri());
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file)))) {
                    boolean reading = true;
                    while (reading) {
                        String line = reader.readLine();
                        if (line == null) {
                            break;
                        }
                        if (line.startsWith(propertyName)) {
                            String newPropertyValue = line.substring(propertyName.length() + 1);
                            if (!newPropertyValue.isBlank()) {
                                entry.setValue(newPropertyValue);
                            }
                            reading = false;
                        }
                    }
                } catch (Exception e) {
                    logger.log(Level.WARNING, e.getMessage());
                }
            }
        }
    }

    private String getPropertyPrefix() {
        return propertyPrefix;
    }

    private void setPropertyPrefix(String prefix) {
        if (prefix == null) {
            throw new NullPointerException();
        }
        this.propertyPrefix = prefix;
    }

    private void setUpProperties(final ConfigurableEnvironment environment) {
        if (environment.containsProperty(SECURE_PROPERTY_PREFIX_NAME)) {
            setPropertyPrefix(environment.getProperty(SECURE_PROPERTY_PREFIX_NAME));
            customPrefixUsed = true;
        } else {
            setPropertyPrefix(SECURE_PROPERTY_PREFIX_DEFAULT_VALUE);
        }
    }

    private String deletePrefix(String s) {
        if (s.startsWith(getPropertyPrefix())) {
            return s.substring(getPropertyPrefix().length());
        } else if (s.startsWith(SECURE_PROPERTY_PREFIX_NAME_TEMPLATE)) {
            return s.substring(SECURE_PROPERTY_PREFIX_NAME_TEMPLATE.length());
        } else {
            return s;
        }
    }

    private Path getAbsolutePath(String s) throws URISyntaxException, FileNotFoundException {
        if (s.startsWith("classpath:")) {
            String stringPath = Path.of(ClassLoader.getSystemResource("").toURI()).toString();
            return Paths.get(stringPath, s.substring("classpath:".length()));
        } else {
            File file = new File(s);
            if (file.exists()) {
                return file.toPath().toAbsolutePath().normalize();
            }
            throw new FileNotFoundException("File " + s + " not found");
        }
    }
}
