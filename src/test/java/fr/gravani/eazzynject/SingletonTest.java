package fr.gravani.eazzynject;

import fr.gravani.eazzynject.annotations.Singleton;
import fr.gravani.eazzynject.annotations.Tag;
import lombok.Getter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class SingletonTest {
    Container container;

    abstract static class Counter {
        @Getter
        private int state = 0;
        int count() {
            return state++;
        }
    }

    @Singleton
    @Tag("global")
    static class GlobalCounter extends Counter {}

    @Tag("local")
    static class LocalCounter extends Counter {}

    @BeforeEach
    void setUpContainer() {
        container = new Container();
    }

    @Test
    void testSingleton() throws Exception {
        container.registerMapping(GlobalCounter.class, Counter.class);
        container.registerMapping(LocalCounter.class, Counter.class);

        var globalCounter1 = container.instantiate(Counter.class, "global");
        assertEquals(globalCounter1.count(), 0);
        assertEquals(globalCounter1.count(), 1);
        assertEquals(globalCounter1.count(), 2);
        var localCounter1 = container.instantiate(Counter.class, "local");
        assertEquals(localCounter1.count(), 0);
        assertEquals(localCounter1.count(), 1);
        assertEquals(localCounter1.count(), 2);

        var globalCounter2 = container.instantiate(Counter.class, "global");
        assertSame(globalCounter1, globalCounter2);
        assertEquals(globalCounter2.count(), 3);
        assertEquals(globalCounter2.count(), 4);
        assertEquals(globalCounter2.count(), 5);

        var localCounter2 = container.instantiate(Counter.class, "local");
        assertNotSame(localCounter1, localCounter2);
        assertEquals(localCounter2.count(), 0);
        assertEquals(localCounter2.count(), 1);
        assertEquals(localCounter2.count(), 2);
    }
}
