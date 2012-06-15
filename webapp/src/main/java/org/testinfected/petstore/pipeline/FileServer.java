package org.testinfected.petstore.pipeline;

import org.simpleframework.http.Request;
import org.simpleframework.http.Response;
import org.simpleframework.http.Status;
import org.testinfected.petstore.Handler;
import org.testinfected.petstore.Resource;
import org.testinfected.petstore.ResourceLoader;
import org.testinfected.petstore.ResourceNotFoundException;
import org.testinfected.petstore.util.Charsets;
import org.testinfected.petstore.util.Streams;

import java.io.IOException;
import java.io.InputStream;

public class FileServer implements Handler {

    private final ResourceLoader resourceLoader;

    public FileServer(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    public void handle(Request request, Response response) throws Exception {
        try {
            renderFile(request, response);
        } catch (ResourceNotFoundException e) {
            renderNotFound(e, response);
        }
    }

    private void renderFile(Request request, Response response) throws IOException {
        Resource resource = resourceLoader.load(fileName(request));
        response.set("Content-Type", resource.mimeType());
        response.setDate("Last-Modified", resource.lastModified());
        InputStream file = resource.open();
        try {
            Streams.copy(file, response.getOutputStream(resource.contentLength()));
        } finally {
            Streams.close(file);
        }
    }

    private String fileName(Request request) {
        return request.getPath().getPath();
    }

    private void renderNotFound(ResourceNotFoundException notFound, Response response) throws IOException {
        response.reset();
        response.setCode(Status.NOT_FOUND.getCode());
        response.setText(Status.NOT_FOUND.getDescription());
        String body = "Not found: " + notFound.getResource();
        byte[] bytes = body.getBytes(Charsets.ISO_8859_1);
        response.set("Content-Type", "text/plain");
        response.getOutputStream(bytes.length).write(bytes);
    }
}