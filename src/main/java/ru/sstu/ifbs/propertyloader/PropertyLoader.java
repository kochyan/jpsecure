package ru.sstu.ifbs.propertyloader;

import org.springframework.core.env.PropertySource;

import java.util.List;

public interface PropertyLoader {
    List<PropertySource<?>> loadPropertySources();
}
