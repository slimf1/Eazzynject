package fr.gravani.eazzynject;

import fr.gravani.eazzynject.testpkg.Table;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestScanner {
    @Test
    void testPackageScanner() throws Exception {
        Scanner.initContainer("fr.gravani.eazzynject.testpkg");
        Table table = Scanner.getInstance(Table.class);
        assertEquals(table.getName(), "Table with Wooden style");
    }
}
