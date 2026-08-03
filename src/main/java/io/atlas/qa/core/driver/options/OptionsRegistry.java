package io.atlas.qa.core.driver.options;

import io.atlas.qa.core.config.BrowserType;
import io.atlas.qa.core.exception.ConfigurationException;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Registry of the available {@link BrowserOptionsProvider} strategies, keyed by
 * browser. The map is built once and never mutated, so lookups are lock-free
 * even under heavy parallel execution.
 */
public final class OptionsRegistry {

    private static final Map<BrowserType, BrowserOptionsProvider> PROVIDERS = index(List.of(
            new ChromeOptionsProvider(),
            new FirefoxOptionsProvider(),
            new EdgeOptionsProvider(),
            new SafariOptionsProvider()));

    private OptionsRegistry() {
    }

    public static BrowserOptionsProvider providerFor(BrowserType browser) {
        BrowserOptionsProvider provider = PROVIDERS.get(browser);
        if (provider == null) {
            throw new ConfigurationException("No options provider registered for browser " + browser);
        }
        return provider;
    }

    private static Map<BrowserType, BrowserOptionsProvider> index(List<BrowserOptionsProvider> providers) {
        Map<BrowserType, BrowserOptionsProvider> map = new EnumMap<>(BrowserType.class);
        providers.forEach(provider -> map.put(provider.browser(), provider));
        return Map.copyOf(map);
    }
}
