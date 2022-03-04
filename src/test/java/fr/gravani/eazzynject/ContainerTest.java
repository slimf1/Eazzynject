package fr.gravani.eazzynject;

import fr.gravani.eazzynject.annotations.Inject;
import fr.gravani.eazzynject.annotations.Injectable;
import lombok.Getter;
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
    static class MySuperUselessService {
        public String getValue() {
            return "super useless";
        }
    }

    @Injectable
    static class MyUselessService {
        @Inject
        private MySuperUselessService mySuperUselessService;

        public String getValue() {
            return "useless "+ mySuperUselessService.getValue();
        }
    }

    @Injectable
    static class MyOtherUselessService {
        public String getValue() {
            return "other useless";
        }
    }

    @Injectable
    static class RssNewsService implements NewsService {

        private final HttpService httpService;

        @Inject
        private MyUselessService myUselessService;

        @Inject
        public RssNewsService(HttpService httpService) {
            this.httpService = httpService;
        }

        @Override
        public HttpService getHttpService() {
            return httpService;
        }

        @Override
        public String toString() {
            return "RssNewsService: "+ myUselessService.getValue();
        }
    }

    @Injectable
    static class DarkWebHttpService implements HttpService {

        @Getter
        private MyOtherUselessService myOtherUselessService;

        @Inject
        private MyUselessService myUselessService;

        @Inject
        void setMyOtherUselessService(MyOtherUselessService myOtherUselessService) {
            this.myOtherUselessService = myOtherUselessService;
        }

        @Override
        public String getString() {
            return "hi " + myUselessService.getValue() + " " + myOtherUselessService.getValue();
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
        container.registerMapping(MyOtherUselessService.class, MyOtherUselessService.class);
        container.registerMapping(MySuperUselessService.class, MySuperUselessService.class);

        var newsService = container.instantiate(NewsService.class);
        assertTrue(newsService.getHttpService() instanceof DarkWebHttpService);
        assertEquals(newsService.getHttpService().getString(), "hi useless super useless other useless");
        assertEquals(newsService.toString(), "RssNewsService: useless super useless");
    }
}
