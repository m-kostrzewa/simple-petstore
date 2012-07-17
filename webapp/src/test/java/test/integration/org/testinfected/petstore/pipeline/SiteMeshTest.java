package test.integration.org.testinfected.petstore.pipeline;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.States;
import org.jmock.integration.junit4.JMock;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.simpleframework.http.Request;
import org.simpleframework.http.Response;
import org.simpleframework.http.Status;
import org.testinfected.petstore.Server;
import org.testinfected.petstore.decoration.Decorator;
import org.testinfected.petstore.util.RequestMatcher;
import org.testinfected.petstore.decoration.Selector;
import org.testinfected.petstore.pipeline.MiddlewareStack;
import org.testinfected.petstore.pipeline.SiteMesh;
import test.support.org.testinfected.petstore.web.HttpRequest;
import test.support.org.testinfected.petstore.web.HttpResponse;
import test.support.org.testinfected.petstore.web.StaticResponse;

import java.io.IOException;

import static org.hamcrest.CoreMatchers.equalTo;
import static test.support.org.testinfected.petstore.web.StaticResponse.respondWith;

@RunWith(JMock.class)
public class SiteMeshTest {
    Mockery context = new JUnit4Mockery();
    Selector selector = context.mock(Selector.class);
    RequestMatcher matcher = context.mock(RequestMatcher.class);
    Decorator decorator = context.mock(Decorator.class);
    States response = context.states("response");

    SiteMesh siteMesh = new SiteMesh(selector);

    Server server = new Server(9999);
    String originalPage = "plain page";
    StaticResponse app = respondWith(Status.OK, originalPage);
    HttpRequest request = HttpRequest.aRequest().to(server);

    @Before public void
    startServer() throws IOException {
        context.checking(new Expectations() {{
            allowing(selector).select(with(any(Response.class))); will(returnValue(true)); when(response.is("selected"));
        }});
        response.become("selected");

        server.run(new MiddlewareStack() {{
            use(siteMesh);
            run(app);
        }});
    }

    @After public void
    stopServer() throws Exception {
        server.shutdown();
    }

    @Test public void
    doesNotAffectPageWhenNoDecoratorIsRegistered() throws IOException {
        HttpResponse response = request.get("/plain/page");
        response.assertHasContent(equalTo(originalPage));
    }

    @Test public void
    doesNotAffectPageIfRequestIsNotMatched() throws IOException {
        siteMesh.map(matcher, decorator);
        context.checking(new Expectations() {{
            atLeast(1).of(matcher).matches(with(any(Request.class))); will(returnValue(false));
        }});

        HttpResponse response = request.get("/plain/page");
        response.assertHasContent(equalTo(originalPage));
    }

    @Test public void
    decoratesContentWhenRequestMatchesRegisteredDecorator() throws IOException {
        final String decoratedPage = "decorated page";
        context.checking(new Expectations() {{
            oneOf(decorator).decorate(with(originalPage)); will(returnValue(decoratedPage));
        }});
        siteMesh.map("/decorated", decorator);

        HttpResponse response = request.get("/decorated/page");
        response.assertHasContent(equalTo(decoratedPage));
    }

    @Test public void
    doesNotAffectPageWhenResponseIsNotSelected() throws IOException {
        response.become("not selected");
        context.checking(new Expectations() {{
            oneOf(selector).select(with(any(Response.class))); will(returnValue(false));
        }});
        siteMesh.map("/decorated", new StaticDecorator("decorated page"));

        HttpResponse response = request.get("/decorated/page");
        response.assertHasContent(equalTo(originalPage));
    }

    @Test public void
    appliesDecoratorsInReverseOrderOfAddition() throws IOException {
        siteMesh.map("/decorated", new StaticDecorator("first decorator"));
        siteMesh.map("/decorated/page", new StaticDecorator("last decorator"));

        HttpResponse response = request.get("/decorated/page");
        response.assertHasContent(equalTo("last decorator"));
    }

    private class StaticDecorator implements Decorator {
        private final String decoration;

        public StaticDecorator(String decoratedContent) {
            this.decoration = decoratedContent;
        }

        public String decorate(String content) {
            return decoration;
        }
    }
}