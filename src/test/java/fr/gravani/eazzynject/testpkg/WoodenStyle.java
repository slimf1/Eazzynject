package fr.gravani.eazzynject.testpkg;

import fr.gravani.eazzynject.annotations.Injectable;

@Injectable
public class WoodenStyle implements Style {
    @Override
    public String getStyle() {
        return "Wooden";
    }
}
