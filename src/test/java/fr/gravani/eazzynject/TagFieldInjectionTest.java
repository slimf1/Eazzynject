package fr.gravani.eazzynject;

import fr.gravani.eazzynject.annotations.Inject;
import fr.gravani.eazzynject.annotations.Injectable;
import fr.gravani.eazzynject.annotations.Tag;
import fr.gravani.eazzynject.exceptions.ImplementationAmbiguityException;
import fr.gravani.eazzynject.exceptions.ImplementationNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TagFieldInjectionTest {
    Container container;

    interface Operator {
        int act(int a, int b);
    }

    @Tag("add")
    static class AddOperator implements Operator {
        @Override
        public int act(int a, int b) {
            return a + b;
        }
    }

    @Tag("subtract")
    static class SubtractOperator implements Operator {
        @Override
        public int act(int a, int b) {
            return a - b;
        }
    }

    @Tag("multiply")
    static class MultiplyOperator implements Operator {
        @Override
        public int act(int a, int b) {
            return a * b;
        }
    }

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

    @Tag("add")
    static class AnotherAddOperator implements Operator {
        @Override
        public int act(int a, int b) {
            return a + b;
        }
    }

    static class AdditionCalculator {
        @Inject
        @Tag("add")
        private Operator add;

        public int operation(int a, int b) {
            return add.act(a, b);
        }
    }

    interface Bank {
        String getName();
    }

    static class CreditAgricool implements Bank {
        @Override
        public String getName() {
            return "Credit Agricool";
        }
    }

    static class FortuneBank implements Bank {
        @Override
        public String getName() {
            return "Fortune Bank";
        }
    }

    static class MyAccount {
        @Inject
        private Bank bank;
    }

    static class MyCalculator {
        @Inject
        @Tag("average")
        private Operator averageOperator;
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
    void testAmbiguousTag() throws Exception {
        container.registerMapping(AddOperator.class, Operator.class);
        assertThrows(ImplementationAmbiguityException.class,
                () -> container.registerMapping(AnotherAddOperator.class, Operator.class));
        container.registerMapping(AdditionCalculator.class, AdditionCalculator.class);

        assertNotNull(container.instantiate(AdditionCalculator.class));
    }

    @Test
    void testAmbiguousImplementationWithoutTag() throws Exception {
        container.registerMapping(CreditAgricool.class, Bank.class);
        assertThrows(ImplementationAmbiguityException.class,
                () -> container.registerMapping(FortuneBank.class, Bank.class));
        container.registerMapping(MyAccount.class, MyAccount.class);

        assertNotNull(container.instantiate(MyAccount.class));
    }

    @Test
    void testNonExistingTag() throws Exception {
        container.registerMapping(AddOperator.class, Operator.class);
        container.registerMapping(SubtractOperator.class, Operator.class);
        container.registerMapping(MultiplyOperator.class, Operator.class);
        container.registerMapping(MyCalculator.class, MyCalculator.class);

        assertThrows(ImplementationNotFoundException.class,
                () -> container.instantiate(MyCalculator.class));
    }

    @Test
    void testInstantiateSpecificTag() throws Exception {
        container.registerMapping(AddOperator.class, Operator.class);
        container.registerMapping(SubtractOperator.class, Operator.class);
        container.registerMapping(MultiplyOperator.class, Operator.class);
        container.registerMapping(MyCalculator.class, MyCalculator.class);

        Operator operator = container.instantiate(Operator.class, "add");
        assertTrue(operator instanceof AddOperator);
    }
}
