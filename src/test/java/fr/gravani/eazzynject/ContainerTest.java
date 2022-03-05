package fr.gravani.eazzynject;

import fr.gravani.eazzynject.annotations.Inject;
import fr.gravani.eazzynject.annotations.Injectable;
import fr.gravani.eazzynject.exceptions.*;
import lombok.Getter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

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

    interface UnimplementedInterface {
        void method();
    }

    @Injectable
    static class MyService {
        @Getter
        private final UnimplementedInterface unimplementedInterface;

        @Inject
        public MyService(UnimplementedInterface unimplementedInterface) {
            this.unimplementedInterface = unimplementedInterface;
        }
    }

    @Injectable
    static class EpicService {
        @Getter
        private final String name;

        public EpicService(String name) {
            this.name = name;
        }
    }

    @Injectable // TODO: enlever les injectable des classes de test qui n'en ont pas besoin
    static class CycleA {
        @Inject
        private CycleB cycleB;
    }

    @Injectable
    static class CycleB {
        @Inject
        private CycleC cycleC;
    }

    @Injectable
    static class CycleC {
        @Inject
        private CycleA cycleA;
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

    @Test
    void testTypeWithoutImplementation() throws ImplementationAmbiguityException {
        container.registerMapping(MyService.class, MyService.class);

        assertThrows(ImplementationNotFoundException.class,
                () -> container.instantiate(MyService.class));
    }

    @Test
    void testInjectableWithoutInjectableDefaultConstructor() throws ImplementationAmbiguityException {
        container.registerMapping(EpicService.class, EpicService.class);

        assertThrows(NoDefaultConstructorException.class,
                () -> container.instantiate(EpicService.class));
    }

    @Test
    void testCircularDependencies() throws ImplementationAmbiguityException {
        container.registerMapping(CycleA.class, CycleA.class);
        container.registerMapping(CycleB.class, CycleB.class);
        container.registerMapping(CycleC.class, CycleC.class);

        var exception = assertThrows(CyclicDependenciesException.class,
                () -> container.instantiate(CycleA.class));
        assertTrue(exception.getMessage().contains("CycleA"));
        assertTrue(exception.getMessage().contains("CycleB"));
        assertTrue(exception.getMessage().contains("CycleC"));
    }
}
