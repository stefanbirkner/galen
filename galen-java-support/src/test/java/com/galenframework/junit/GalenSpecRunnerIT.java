package com.galenframework.junit;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.hamcrest.Matcher;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ErrorCollector;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.junit.runner.RunWith;
import org.junit.runner.notification.Failure;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;

import static java.util.concurrent.Executors.newCachedThreadPool;
import static org.apache.commons.io.IOUtils.write;
import static org.hamcrest.Matchers.*;

public class GalenSpecRunnerIT {
    private static final int NO_DELAY = 0;
    private static final int PORT = 13728;
    private static final int SYSTEM_DEFAULT_BACKLOG = 0;
    private static HttpServer SERVER;

    @BeforeClass
    public static void startServer() throws IOException {
        InetSocketAddress address = new InetSocketAddress(PORT);
        SERVER = HttpServer.create(address, SYSTEM_DEFAULT_BACKLOG);
        SERVER.createContext("/", new SingleFileHandler());
        SERVER.setExecutor(newCachedThreadPool());
        SERVER.start();
    }

    @AfterClass
    public static void stopServer() {
        SERVER.stop(NO_DELAY);
    }

    @Rule
    public final ErrorCollector collector = new ErrorCollector();

    @RunWith(GalenSpecRunner.class)
    @Size(width = 640, height = 480)
    @Spec("/com/galenframework/junit/homepage_small.gspec")
    @Url("http://127.0.0.1:" + PORT + "/")
    public static class ValidSpec {
    }

    @Test
    public void shouldBeSuccessfulForValidSpec() {
        Result result = runTest(ValidSpec.class);
        //We use an error collector because running a test for each assertion takes too much time.
        collector.checkThat("is successful", result.wasSuccessful(), is(true));
        collector.checkThat("has no failures", result.getFailures(), is(empty()));
        collector.checkThat("has a test for each spec", result.getRunCount(), is(4));
    }

    @RunWith(GalenSpecRunner.class)
    @Size(width = 640, height = 480)
    @Spec("/com/galenframework/junit/inapplicable.gspec")
    @Url("http://127.0.0.1:" + PORT + "/")
    public static class InapplicableSpec {
    }

    @Test
    public void shouldFailForInapplicableSpec() {
        Result result = runTest(InapplicableSpec.class);
        //We use an error collector because running a test for each assertion takes too much time.
        collector.checkThat("is not successful", result.wasSuccessful(), is(false));
        collector.checkThat("has failures", result.getFailures(), hasSize(2));
        collector.checkThat("has only assertion errors", result.getFailures(),
                not(hasFailureWithException(not(instanceOf(AssertionError.class)))));
        collector.checkThat("describes failure", result.getFailures(),
                hasFailureWithException(hasProperty("message", equalTo(
                        "[\"first_paragraph\" width is 400px but it should be less than 10px]"))));
        collector.checkThat("has a test for each spec", result.getRunCount(), is(3));
    }

    @RunWith(GalenSpecRunner.class)
    @Include("variantA")
    @Size(width = 640, height = 480)
    @Spec("/com/galenframework/junit/tag.gspec")
    @Url("http://127.0.0.1:" + PORT + "/")
    public static class ExcludeTag {
    }

    @Test
    public void shouldNotRunTestsForSectionsThatAreExcluded() {
        Result result = runTest(ExcludeTag.class);
        collector.checkThat("has only tests for not excluded sections", result.getRunCount(), is(3));
    }

    @RunWith(GalenSpecRunner.class)
    @Include("variantA")
    @Exclude("variantB")
    @Size(width = 640, height = 480)
    @Spec("/com/galenframework/junit/tag.gspec")
    @Url("http://127.0.0.1:" + PORT + "/")
    public static class IncludeTag {
    }

    @Test
    public void shouldOnlyRunTestsForSectionsThatAreIncluded() {
        Result result = runTest(IncludeTag.class);
        collector.checkThat("has only tests for included sections", result.getRunCount(), is(2));
    }

    @RunWith(GalenSpecRunner.class)
    @Spec("/com/galenframework/junit/homepage_small.gspec")
    @Url("http://127.0.0.1:" + PORT + "/")
    public static class NoSizeAnnotation {
    }

    @Test
    public void shouldProvideHelpfulMessageIfSizeAnnotationsAreMissing() {
        Result result = runTest(com.galenframework.junit.GalenSpecRunnerIT.NoSizeAnnotation.class);
        //We use an error collector because running a test for each assertion takes too much time.
        collector.checkThat("is successful", result.wasSuccessful(), is(false));
        collector.checkThat("has failure", result.getFailures(), hasSize(1));
        collector.checkThat("describes failure", result.getFailures(),
                hasFailureWithException(hasProperty("message",
                        equalTo("The annotation @Size is missing."))));
    }

    @RunWith(GalenSpecRunner.class)
    @Size(width = 640, height = 480)
    @Url("http://127.0.0.1:" + PORT + "/")
    public static class NoSpecAnnotation {
    }

    @Test
    public void shouldProvideHelpfulMessageIfSpecAnnotationIsMissing() {
        Result result = runTest(NoSpecAnnotation.class);
        //We use an error collector because running a test for each assertion takes too much time.
        collector.checkThat("is successful", result.wasSuccessful(), is(false));
        collector.checkThat("has failure", result.getFailures(), hasSize(1));
        collector.checkThat("describes failure", result.getFailures(),
                hasFailureWithException(hasProperty("message",
                        equalTo("The annotation @Spec is missing."))));
    }

    @RunWith(GalenSpecRunner.class)
    @Size(width = 640, height = 480)
    @Spec("/com/galenframework/junit/homepage_small.gspec")
    public static class NoUrlAnnotation {
    }

    @Test
    public void shouldProvideHelpfulMessageIfUrlAnnotationIsMissing() {
        Result result = runTest(NoUrlAnnotation.class);
        //We use an error collector because running a test for each assertion takes too much time.
        collector.checkThat("is successful", result.wasSuccessful(), is(false));
        collector.checkThat("has failure", result.getFailures(), hasSize(1));
        collector.checkThat("describes failure", result.getFailures(),
                hasFailureWithException(hasProperty("message",
                        equalTo("The annotation @Url is missing."))));
    }

    private Matcher<Iterable<? super Failure>> hasFailureWithException(Matcher<?> matcher) {
        return hasItem(hasProperty("exception", matcher));
    }

    private Result runTest(Class<?> test) {
        return JUnitCore.runClasses(test);
    }

    private static class SingleFileHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange httpExchange) throws IOException {
            Headers responseHeaders = httpExchange.getResponseHeaders();
            responseHeaders.set("Content-Type", "text/html");
            httpExchange.sendResponseHeaders(200, 0);
            String bodyAsText =
                    "<!DOCTYPE html>\n" +
                            "<html>\n" +
                            "<head>\n" +
                            "</head>\n" +
                            "<body>\n" +
                            "<p id=\"p1\" style=\"width:400px;float:left;\">First paragraph.</p>\n" +
                            "<p id=\"p2\">Second paragraph.</p>\n" +
                            "</body>\n" +
                            "</html>";
            OutputStream responseBody = httpExchange.getResponseBody();
            write(bodyAsText, responseBody);
            responseBody.close();
        }
    }
}