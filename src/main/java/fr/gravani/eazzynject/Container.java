package fr.gravani.eazzynject;

import fr.gravani.eazzynject.annotations.Inject;
import fr.gravani.eazzynject.exceptions.ImplementationNotFoundException;
import fr.gravani.eazzynject.exceptions.NoDefaultConstructorException;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class Container {
    private final Map<Class<?>, Class<?>> dependencies = new HashMap<>();
    private final Map<Class<?>, Object> instanceCache = new HashMap<>(); // Toujours en créer des nouvelles

    public <T> void registerMapping(Class<? extends T> child, Class<T> base) {
        dependencies.put(child, base);
    }

    @SuppressWarnings("unchecked")
    public <T> T instantiate(Class<T> inter) {
        Class<?> implementation = null;
        try {
            implementation = getImplementationFromBase(inter);
        } catch (ImplementationNotFoundException e) {
            e.printStackTrace();
            return null;
        }

        if (instanceCache.containsKey(implementation)) {
            return (T)instanceCache.get(implementation);
        }

        Object instance = null;
        try {
            instance = injectIntoClass(implementation);
            instanceCache.put(implementation, instance);
        } catch (NoDefaultConstructorException e) {
            e.printStackTrace();
        }

        return (T)instance;
    }

    public <T> T injectIntoClass(Class<T> cls) throws NoDefaultConstructorException {
        try {
            var injectableConstructors = Arrays.stream(cls.getConstructors())
                    .filter(c -> c.isAnnotationPresent(Inject.class))
                    .toList();

            if (!injectableConstructors.isEmpty()) {
                return injectIntoConstructor(injectableConstructors.stream().findFirst().get());
            } else {
                return injectIntoFields(cls);
            }
        } catch (NoSuchFieldException e) {
            throw new NoDefaultConstructorException(
                    String.format("Could not find a default constructor or an " +
                            "injectable constructor for the injectable class %s", cls.getName()));
        } catch (ReflectiveOperationException e) {
            e.printStackTrace();
            return null; // TODO : faire plus?
        }
    }

    private <T> T injectIntoFields(Class<T> cls) throws ReflectiveOperationException {
        // Dans le cas où la classe n'a pas de constructeur avec @Inject
        // on suppose qu'elle a un constructeur par défaut rpésent
        var instance = cls.getDeclaredConstructor().newInstance();
        for(Field field : cls.getDeclaredFields()) {
            if (field.isAnnotationPresent(Inject.class)) {
                field.setAccessible(true);
                //field.set(instance, injectIntoClass(getImplementationFromBase(field.getType())));
                field.set(instance, instantiate(field.getType()));
            }
        }
        return instance;
    }

    @SuppressWarnings("unchecked")
    private <T> T injectIntoConstructor(Constructor<?> constructor) throws ReflectiveOperationException {
        var parameters = Arrays.stream(constructor.getParameterTypes())
                .map(this::instantiate)
                //.map(t -> injectIntoClass(getImplementationFromBase(t)))
                .toArray();
        return (T)constructor.newInstance(parameters);
    }

    // TODO: passer par un tag
    private Class<?> getImplementationFromBase(Class<?> baseClass) throws ImplementationNotFoundException {
        var implementations = dependencies
                .entrySet()
                .stream()
                .filter(item -> item.getValue().equals(baseClass))
                .collect(Collectors.toSet());

        if (implementations.isEmpty()) {
            throw new ImplementationNotFoundException(
                    String.format("Could not find any implementation for base class %s", baseClass.getName()));
        }

        if (implementations.size() == 1) {
            return implementations.stream().findFirst().get().getKey();
        }

        // Là gérer le tag: cf
        // https://dev.to/jjbrt/how-to-create-your-own-dependency-injection-framework-in-java-4eaj
        // Le mettre dans le cr/readme
        return implementations.stream().findFirst().get().getKey();
    }
}
