package ru.sstu.ifbs.example;

import org.springframework.core.env.PropertySource;
import ru.sstu.ifbs.propertyloader.PropertyLoader;

import java.util.List;

public class CustomPropertyLoader implements PropertyLoader {
    @Override
    public List<PropertySource<?>> loadPropertySources() {
        return null;
    }
}
