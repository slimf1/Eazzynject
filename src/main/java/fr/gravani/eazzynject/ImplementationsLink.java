package fr.gravani.eazzynject;

import fr.gravani.eazzynject.exceptions.ImplementationAmbiguityException;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
public class ImplementationsLink {
    private Class<?> baseClass;
    private Map<String, Class<?>> implementations;

    public ImplementationsLink(Class<?> baseClass, String tag, Class<?> firstImplementation) {
        this.baseClass = baseClass;
        implementations = new HashMap<>();
        implementations.put(tag, firstImplementation);
    }

    public void addImplementation(String tag, Class implementation) throws ImplementationAmbiguityException {
        if(implementations.containsKey(tag)) {
            throw new ImplementationAmbiguityException("The tag " + tag + " already exists for the base class " + baseClass.getName());
        }
        implementations.put(tag, implementation);
    }
}
