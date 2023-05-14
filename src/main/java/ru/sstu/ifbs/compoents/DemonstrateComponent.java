package ru.sstu.ifbs.compoents;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.logging.Level;
import java.util.logging.Logger;

@Component
public class DemonstrateComponent {
    private final static Logger logger = Logger.getLogger(DemonstrateComponent.class.getName());

    @Value("${example1}")
    private String firstValue;
    @Value("${example2}")
    private String secondValue;
    @Value("${example3}")
    private String thirdValue;

    @PostConstruct
    public void init() {
        logger.log(Level.INFO, "firstValue: " + firstValue);
        logger.log(Level.INFO, "secondValue: " + secondValue);
        logger.log(Level.INFO, "thirdValue: " + thirdValue);
    }
}
