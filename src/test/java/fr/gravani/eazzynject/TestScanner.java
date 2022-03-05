package fr.gravani.eazzynject;

import fr.gravani.eazzynject.testpkg.Furniture;
import fr.gravani.eazzynject.testpkg.Table;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestScanner {
    @Test
    void testPackageScanner() throws Exception {
        Scanner.initContainer("fr.gravani.eazzynject.testpkg");

        Table table1 = Scanner.getInstance(Table.class);
        assertEquals(table1.getName(), "Table with Wooden style");

        Furniture table2 = Scanner.getInstance(Furniture.class);
        assertEquals(table2.getName(), "Table with Wooden style");
    }
}
