package fr.gravani.eazzynject;

import fr.gravani.eazzynject.exceptions.ImplementationAmbiguityException;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

public class ImplementationsLink {
    @Getter
    private final Class<?> baseClass;
    @Getter
    private final Map<String, Class<?>> implementations = new HashMap<>();

    public ImplementationsLink(Class<?> baseClass, String tag, Class<?> firstImplementation) {
        this.baseClass = baseClass;
        implementations.put(tag, firstImplementation);
    }

    public void addImplementation(String tag, Class<?> implementation) throws ImplementationAmbiguityException {
        if(implementations.containsKey(tag)) {
            throw new ImplementationAmbiguityException("The tag " + tag + " already exists for the base class " + baseClass.getName());
        }
        implementations.put(tag, implementation);
    }
}
