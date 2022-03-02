package fr.gravani.eazzynject;

import fr.gravani.eazzynject.annotations.Inject;
import fr.gravani.eazzynject.annotations.Injectable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class ContainerTest {
    Container container;

    @Injectable
    interface HttpService {
        String getString();
    }

    @Injectable
    interface NewsService {
        HttpService getHttpService();
    }

    static class RssNewsService implements NewsService {
        private final HttpService httpService;

        @Inject
        public RssNewsService(HttpService httpService) {
            this.httpService = httpService;
        }

        @Override
        public HttpService getHttpService() {
            return httpService;
        }
    }

    static class DarkWebHttpService implements HttpService {
        @Override
        public String getString() {
            return "hi";
        }
    }

    @BeforeEach
    void setUpContainer() {
        container = new Container();
    }

    @Test
    void testSimpleInjection() {
        container.registerMapping(HttpService.class, DarkWebHttpService.class);
        container.registerMapping(NewsService.class, RssNewsService.class);

        var newsService = container.instantiate(NewsService.class);
        assertTrue(newsService.getHttpService() instanceof DarkWebHttpService);
    }
}
