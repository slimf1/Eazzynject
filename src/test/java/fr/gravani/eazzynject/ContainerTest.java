package fr.gravani.eazzynject;

import fr.gravani.eazzynject.annotations.Inject;
import fr.gravani.eazzynject.annotations.Injectable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ContainerTest {
    Container container;

    interface HttpService {
        String getString();
    }

    interface NewsService {
        HttpService getHttpService();
    }

    @Injectable
    static class MyUselessService {
        public String getValue() {
            return "useless";
        }
    }

    @Injectable
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

    @Injectable
    static class DarkWebHttpService implements HttpService {
        @Inject
        private MyUselessService myUselessService;

        @Override
        public String getString() {
            return "hi "+ myUselessService.getValue();
        }
    }

    @BeforeEach
    void setUpContainer() {
        container = new Container();
    }

    @Test
    void testSimpleInjection() throws Exception {
        // tout ça => le remplacer avec scan de injectable (get la classe au dessus)
        container.registerMapping(DarkWebHttpService.class, HttpService.class);
        container.registerMapping(RssNewsService.class, NewsService.class);
        container.registerMapping(MyUselessService.class, MyUselessService.class);

        var newsService = container.instantiate(NewsService.class);
        assertTrue(newsService.getHttpService() instanceof DarkWebHttpService);
        assertEquals(newsService.getHttpService().getString(), "hi useless");
    }
}
