package io.atlas.qa.core.data;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.atlas.qa.core.exception.AtlasException;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * Loads test data from JSON files on the classpath and hands it to TestNG
 * data providers as typed objects.
 *
 * <p>Typed records instead of {@code Object[]} rows: a test signature reads
 * {@code void checkout(Customer customer)} rather than
 * {@code void checkout(String a, String b, String c, String d)}, and a change in
 * the fixture fails at compile time instead of at row 37 of a spreadsheet.
 */
public final class JsonDataReader {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private JsonDataReader() {
    }

    public static <T> List<T> readList(String classpathResource, Class<T> type) {
        try (InputStream in = open(classpathResource)) {
            return MAPPER.readValue(in, MAPPER.getTypeFactory().constructCollectionType(List.class, type));
        } catch (IOException e) {
            throw new AtlasException("Unable to parse test data file " + classpathResource, e);
        }
    }

    public static <T> T read(String classpathResource, TypeReference<T> type) {
        try (InputStream in = open(classpathResource)) {
            return MAPPER.readValue(in, type);
        } catch (IOException e) {
            throw new AtlasException("Unable to parse test data file " + classpathResource, e);
        }
    }

    /** Wraps each element in its own row, ready for {@code @DataProvider}. */
    public static <T> Object[][] asRows(String classpathResource, Class<T> type) {
        List<T> items = readList(classpathResource, type);
        Object[][] rows = new Object[items.size()][1];
        for (int i = 0; i < items.size(); i++) {
            rows[i][0] = items.get(i);
        }
        return rows;
    }

    private static InputStream open(String classpathResource) {
        InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream(classpathResource);
        if (in == null) {
            throw new AtlasException("Test data file not found on the classpath: " + classpathResource);
        }
        return in;
    }
}
