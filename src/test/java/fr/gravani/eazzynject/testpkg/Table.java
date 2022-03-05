package fr.gravani.eazzynject.testpkg;

import fr.gravani.eazzynject.annotations.Inject;
import fr.gravani.eazzynject.annotations.Injectable;

@Injectable
public class Table implements Furniture {

    @Inject
    private Style style;

    @Override
    public String getName() {
        return String.format("Table with %s style", style.getStyle());
    }
}
