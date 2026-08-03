package io.atlas.qa.base;

import io.atlas.qa.core.api.ApiClient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.annotations.BeforeClass;

/**
 * Lifecycle shared by every API test.
 *
 * <p>No browser is started here: an API test that pays the cost of a WebDriver
 * session it never uses is the most common form of waste in a hybrid suite.
 */
public abstract class BaseApiTest {

    protected final Logger log = LogManager.getLogger(getClass());
    protected ApiClient api;

    @BeforeClass(alwaysRun = true)
    public void prepareClient() {
        api = ApiClient.forApplication();
    }
}
