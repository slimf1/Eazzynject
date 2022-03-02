package fr.gravani.eazzynject;

import fr.gravani.eazzynject.annotations.Injectable;

import java.security.PrivilegedAction;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Map;

public class Container {
    private final Map<Class<?>, Class<?>> dependencies = new HashMap<>();
    private final Map<Class<?>, ?> instanceCache = new HashMap<>(); // Toujours en créer des nouvelles

    public <T> void registerMapping(Class<T> base, Class<? extends T> child) {
        dependencies.put(base, child);
    }

    public <T> Class<? extends T> getMapping(Class<T> cls) {
        if (!dependencies.containsKey(cls)) {
            throw new IllegalArgumentException("Key error"); // TODO: Exc custom?
        }
        return dependencies.get(cls).asSubclass(cls);
    }

    public <T> T instantiate(Class<T> inter) {
        return null;
    }
}
