package fr.gravani.eazzynject.testpkgbank;

import fr.gravani.eazzynject.annotations.Injectable;
import fr.gravani.eazzynject.annotations.Tag;

@Injectable
@Tag("Fortunement")
public class Fortunement extends BankOnline {
    @Override
    public String print() {
        return super.print() + " Fortunement";
    }
}
