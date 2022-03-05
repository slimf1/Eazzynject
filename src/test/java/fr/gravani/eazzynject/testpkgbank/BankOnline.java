package fr.gravani.eazzynject.testpkgbank;

import fr.gravani.eazzynject.annotations.Inject;
import fr.gravani.eazzynject.annotations.Injectable;
import fr.gravani.eazzynject.annotations.Tag;
import lombok.Getter;

@Injectable
@Tag("BankOnline")
public class BankOnline implements Bank {
    @Inject
    @Getter
    private WebSite webSite;

    @Override
    public String print() {
        return "Bank online";
    }
}
