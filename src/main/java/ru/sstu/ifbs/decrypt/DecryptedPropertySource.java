package ru.sstu.ifbs.decrypt;

import org.springframework.core.env.PropertySource;

import java.util.Map;

class DecryptedPropertySource extends PropertySource<Map<String, Object>> {

    DecryptedPropertySource(String name, Map<String, Object> source) {
        super(name, source);
    }

    @Override
    public Object getProperty(String name) {
        return source.getOrDefault(name, null);
    }
}
