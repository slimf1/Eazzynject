package fr.gravani.eazzynject;

import fr.gravani.eazzynject.exceptions.ImplementationNotFoundException;
import fr.gravani.eazzynject.testpkg.Furniture;
import fr.gravani.eazzynject.testpkg.Style;
import fr.gravani.eazzynject.testpkg.Table;
import fr.gravani.eazzynject.testpkg.WoodenStyle;
import fr.gravani.eazzynject.testpkgbank.*;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class EazzynjectTest {
    @Test
    void testPackageScanner() throws Exception {
        Eazzynject.initContainer("fr.gravani.eazzynject.testpkg");

        Table table1 = Eazzynject.getInstance(Table.class);
        assertEquals(table1.getName(), "Table with Wooden style");

        Furniture table2 = Eazzynject.getInstance(Furniture.class);
        assertEquals(table2.getName(), "Table with Wooden style");

        WoodenStyle style1 = Eazzynject.getInstance(WoodenStyle.class);
        assertEquals(style1.getStyle(), "Wooden");

        Style style2 = Eazzynject.getInstance(Style.class);
        assertEquals(style2.getStyle(), "Wooden");
    }

    @Test
    void testPackageScannerBank() throws Exception {
        Eazzynject.initContainer(fr.gravani.eazzynject.testpkgbank.Bank.class);

        Bank b1 = Eazzynject.getInstance(Bank.class, "Fortunement");
        assertNotNull(b1);
        assertTrue(b1 instanceof Fortunement);

        Bank b2 = Eazzynject.getInstance(Bank.class, "CreditAgricool");
        assertNotNull(b2);
        assertTrue(b2 instanceof CreditAgricool);
        assertNotNull(((CreditAgricool) b2).getDabService());


        Bank b3 = Eazzynject.getInstance(Bank.class, "BankOnline");
        assertNotNull(b3);
        assertTrue(b3 instanceof BankOnline);
        assertNotNull(((BankOnline)b3).getWebSite());

        assertThrows(ImplementationNotFoundException.class,
                () -> Eazzynject.getInstance(Bank.class, "FakeBank"));
    }
}
