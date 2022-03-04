package fr.gravani.eazzynject;

import fr.gravani.eazzynject.annotations.Inject;
import fr.gravani.eazzynject.annotations.Injectable;
import fr.gravani.eazzynject.annotations.Tag;
import fr.gravani.eazzynject.exceptions.ImplementationAmbiguityException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class TagTest {
    Container container;

    interface Operator {
        int act(int a, int b);
    }

    @Injectable
    @Tag("add")
    static class AddOperator implements Operator {
        @Override
        public int act(int a, int b) {
            return a + b;
        }
    }

    @Injectable
    @Tag("subtract")
    static class SubtractOperator implements Operator {
        @Override
        public int act(int a, int b) {
            return a - b;
        }
    }

    @Injectable
    @Tag("multiply")
    static class MultiplyOperator implements Operator {
        @Override
        public int act(int a, int b) {
            return a * b;
        }
    }

    @Injectable
    static class DifferenceOfSquaresCalculator {
        @Inject
        @Tag("add")
        private Operator add;

        @Inject
        @Tag("subtract")
        private Operator subtract;

        @Inject
        @Tag("multiply")
        private Operator multiply;

        public int operation(int a, int b) {
            return multiply.act(add.act(a, b), subtract.act(a, b));
        }
    }

    @Injectable
    @Tag("add")
    static class AnotherAddOperator implements Operator {
        @Override
        public int act(int a, int b) {
            return a + b;
        }
    }

    @Injectable
    static class AdditionCalculator {
        @Inject
        @Tag("add")
        private Operator add;

        public int operation(int a, int b) {
            return add.act(a, b);
        }
    }

    @BeforeEach
    void setUpContainer() {
        container = new Container();
    }

    @Test
    void testTag() throws Exception {
        container.registerMapping(AddOperator.class, Operator.class);
        container.registerMapping(SubtractOperator.class, Operator.class);
        container.registerMapping(MultiplyOperator.class, Operator.class);
        container.registerMapping(DifferenceOfSquaresCalculator.class, DifferenceOfSquaresCalculator.class);

        var calculator = container.instantiate(DifferenceOfSquaresCalculator.class);
        assertEquals(calculator.operation(51, 39), 1080);
        assertEquals(calculator.operation(19, -9), 280);
    }

    @Test
    void testAmbiguousTag() {
        container.registerMapping(AddOperator.class, Operator.class);
        container.registerMapping(AnotherAddOperator.class, Operator.class);
        container.registerMapping(AdditionCalculator.class, AdditionCalculator.class);

        assertThrows(ImplementationAmbiguityException.class,
                () -> container.instantiate(AdditionCalculator.class));
    }
}
