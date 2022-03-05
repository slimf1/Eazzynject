package fr.gravani.eazzynject.testpkgbank;

import fr.gravani.eazzynject.annotations.Inject;
import fr.gravani.eazzynject.annotations.Injectable;
import fr.gravani.eazzynject.annotations.Tag;
import lombok.Getter;

@Injectable
@Tag("CreditAgricool")
public class CreditAgricool extends BankOnline {

    @Inject
    @Getter
    private DABService dabService;

    @Override
    public String print() {
        return super.print() + " CreditAgricool";
    }
}
