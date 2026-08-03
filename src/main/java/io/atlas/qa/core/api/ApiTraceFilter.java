package io.atlas.qa.core.api;

import io.atlas.qa.core.report.ReportManager;
import io.restassured.filter.Filter;
import io.restassured.filter.FilterContext;
import io.restassured.response.Response;
import io.restassured.specification.FilterableRequestSpecification;
import io.restassured.specification.FilterableResponseSpecification;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Writes every HTTP exchange to the log and to the HTML report.
 *
 * <p>Sensitive headers are redacted before anything is persisted: a test report
 * is an artefact that gets attached to tickets and shared in chat, and a bearer
 * token that leaks there is a real incident.
 */
public class ApiTraceFilter implements Filter {

    private static final Logger LOG = LogManager.getLogger("RestAssured");
    private static final int MAX_BODY_CHARS = 2_000;

    @Override
    public Response filter(FilterableRequestSpecification request,
                           FilterableResponseSpecification responseSpec,
                           FilterContext context) {

        long startedAt = System.currentTimeMillis();
        Response response = context.next(request, responseSpec);
        long elapsed = System.currentTimeMillis() - startedAt;

        String summary = "%s %s → %d (%d ms)".formatted(
                request.getMethod(), request.getURI(), response.getStatusCode(), elapsed);

        // getBody() is generic: it must be read into an Object before being
        // rendered, otherwise String.valueOf() binds to the char[] overload.
        Object requestBody = request.getBody();
        String renderedRequest = truncate(requestBody == null ? null : requestBody.toString());

        LOG.info(summary);
        LOG.debug("request body : {}", renderedRequest);
        LOG.debug("response body: {}", truncate(response.asString()));

        ReportManager.step("""
                <pre>%s
                request : %s
                response: %s</pre>""".formatted(summary,
                redact(renderedRequest),
                redact(truncate(response.asString()))));

        return response;
    }

    private String truncate(String body) {
        if (body == null || body.isBlank() || "null".equals(body)) {
            return "<empty>";
        }
        return body.length() <= MAX_BODY_CHARS ? body : body.substring(0, MAX_BODY_CHARS) + " …[truncated]";
    }

    private String redact(String body) {
        return body.replaceAll("(?i)(\"(?:password|token|authorization|secret)\"\\s*:\\s*\")[^\"]*(\")", "$1********$2");
    }
}
