package fr.gravani.eazzynject;

import fr.gravani.eazzynject.annotations.Inject;
import fr.gravani.eazzynject.exceptions.CyclicDependenciesException;
import fr.gravani.eazzynject.exceptions.ImplementationAmbiguityException;
import fr.gravani.eazzynject.exceptions.ImplementationNotFoundException;
import fr.gravani.eazzynject.exceptions.NoDefaultConstructorException;
import lombok.Getter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ContainerSetterInjectionTest {
    Container container;

    interface HttpService {
        String getString();
    }

    interface NewsService {
        HttpService getHttpService();
    }

    static class MySuperUselessService {
        public String getValue() {
            return "super useless";
        }
    }

    static class MyUselessService {
        private MySuperUselessService mySuperUselessService;

        @Inject
        public void setMySuperUselessService(MySuperUselessService mySuperUselessService) {
            this.mySuperUselessService = mySuperUselessService;
        }

        public String getValue() {
            return "useless "+ mySuperUselessService.getValue();
        }
    }

    static class MyOtherUselessService {
        public String getValue() {
            return "other useless";
        }
    }

    static class RssNewsService implements NewsService {

        private HttpService httpService;

        private MyUselessService myUselessService;

        @Inject
        public void setHttpService(HttpService httpService) {
            this.httpService = httpService;
        }

        @Inject
        public void setMyUselessService(MyUselessService myUselessService) {
            this.myUselessService = myUselessService;
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

    static class DarkWebHttpService implements HttpService {

        @Getter
        private MyOtherUselessService myOtherUselessService;

        private MyUselessService myUselessService;

        @Inject
        void setMyOtherUselessService(MyOtherUselessService myOtherUselessService) {
            this.myOtherUselessService = myOtherUselessService;
        }

        @Inject
        public void setMyUselessService(MyUselessService myUselessService) {
            this.myUselessService = myUselessService;
        }

        @Override
        public String getString() {
            return "hi " + myUselessService.getValue() + " " + myOtherUselessService.getValue();
        }
    }

    interface UnimplementedInterface {
        void method();
    }

    static class MyService {
        @Getter
        private UnimplementedInterface unimplementedInterface;

        @Inject
        public void setUnimplementedInterface(UnimplementedInterface unimplementedInterface) {
            this.unimplementedInterface = unimplementedInterface;
        }
    }

    static class EpicService {
        @Getter
        private final String name;

        public EpicService(String name) {
            this.name = name;
        }
    }

    static class CycleA {
        private CycleB cycleB;

        @Inject
        public void setCycleB(CycleB cycleB) {
            this.cycleB = cycleB;
        }
    }

    static class CycleB {
        private CycleC cycleC;

        @Inject
        public void setCycleC(CycleC cycleC) {
            this.cycleC = cycleC;
        }
    }

    static class CycleC {
        private CycleA cycleA;

        @Inject
        public void setCycleA(CycleA cycleA) {
            this.cycleA = cycleA;
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
