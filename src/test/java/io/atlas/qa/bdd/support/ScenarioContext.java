package io.atlas.qa.bdd.support;

import io.atlas.qa.core.exception.AtlasException;
import io.atlas.qa.domain.Customer;

import java.util.HashMap;
import java.util.Map;

/**
 * State shared between the step classes of a single scenario.
 *
 * <p>PicoContainer builds one instance per scenario and injects it into every
 * step class that declares it in its constructor. That is what makes it safe to
 * run scenarios in parallel: no static field, no leakage from one scenario to
 * the next, and steps that stay small because they do not have to carry the
 * whole state in their arguments.
 */
public class ScenarioContext {

    private final Map<String, Object> values = new HashMap<>();
    private Customer customer;

    public void put(String key, Object value) {
        values.put(key, value);
    }

    @SuppressWarnings("unchecked")
    public <T> T get(String key, Class<T> type) {
        Object value = values.get(key);
        if (value == null) {
            throw new AtlasException("Nothing was stored in the scenario context under '%s'".formatted(key));
        }
        if (!type.isInstance(value)) {
            throw new AtlasException("'%s' holds a %s, not a %s"
                    .formatted(key, value.getClass().getSimpleName(), type.getSimpleName()));
        }
        return (T) value;
    }

    public boolean has(String key) {
        return values.containsKey(key);
    }

    public Customer customer() {
        if (customer == null) {
            customer = Customer.random();
        }
        return customer;
    }
}
