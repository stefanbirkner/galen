package com.galenframework.junit;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;

import static java.util.concurrent.Executors.newCachedThreadPool;
import static org.apache.commons.io.IOUtils.write;

/**
 * Created by stefan on 22.12.15.
 */
public class TestServer {
    private static final int NO_DELAY = 0;
    private static final int PORT = 13728;
    private static final int SYSTEM_DEFAULT_BACKLOG = 0;
    private static HttpServer SERVER;


    public static void main(String[] args) throws IOException, InterruptedException {
        InetSocketAddress address = new InetSocketAddress(PORT);
        SERVER = HttpServer.create(address, SYSTEM_DEFAULT_BACKLOG);
        SERVER.createContext("/", new SingleFileHandler());
        SERVER.setExecutor(newCachedThreadPool());
        SERVER.start();
        Thread.sleep(10_000_000);
        SERVER.stop(NO_DELAY);
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
