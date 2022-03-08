package fr.gravani.eazzynject;

import fr.gravani.eazzynject.annotations.Inject;
import fr.gravani.eazzynject.annotations.Singleton;
import fr.gravani.eazzynject.annotations.Tag;
import fr.gravani.eazzynject.exceptions.CyclicDependenciesException;
import fr.gravani.eazzynject.exceptions.ImplementationAmbiguityException;
import fr.gravani.eazzynject.exceptions.ImplementationNotFoundException;
import fr.gravani.eazzynject.exceptions.NoDefaultConstructorException;

import java.lang.reflect.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class Container {
    private static final int MAX_RECURSIVE_INJECTIONS = 32;

    private final Dependencies dependencies = new Dependencies();
    private final Map<Class<?>, Object> instanceCache = new HashMap<>();
    private final Map<Class<?>, Integer> injectionCounter = new HashMap<>();

    public void registerMapping(Class<?> child, Class<?> base) throws ImplementationAmbiguityException {
        String tag = child.isAnnotationPresent(Tag.class)
                ? child.getAnnotation(Tag.class).value() : null;
        dependencies.put(base, child, tag);
    }

    public <T> T instantiate(Class<T> type)
            throws ImplementationNotFoundException, NoDefaultConstructorException, ImplementationAmbiguityException,
            CyclicDependenciesException {
        return instantiate(type, null);
    }

    public <T> T instantiate(Class<T> type, String tag)
            throws ImplementationNotFoundException, NoDefaultConstructorException, ImplementationAmbiguityException,
            CyclicDependenciesException {
        injectionCounter.clear();
        return instantiateByTag(type, tag);
    }

    @SuppressWarnings("unchecked")
    private <T> T instantiateByTag(Class<T> inter, String tag)
            throws ImplementationNotFoundException, NoDefaultConstructorException, ImplementationAmbiguityException,
            CyclicDependenciesException {

        var implementation = getImplementationFromBase(inter, tag);
        if (injectionCounter.containsKey(implementation)) {
            injectionCounter.put(implementation, injectionCounter.get(implementation) + 1);
        } else {
            injectionCounter.put(implementation, 1);
        }

        if (injectionCounter.get(implementation) > MAX_RECURSIVE_INJECTIONS) {
            var classesInCycle = injectionCounter
                    .entrySet()
                    .stream()
                    .filter(entry -> entry.getValue() >= MAX_RECURSIVE_INJECTIONS - 1)
                    .map(entry -> entry.getKey().getName())
                    .sorted()
                    .toList();
            throw new CyclicDependenciesException(
                    String.format("Found circular dependencies with classes: %s",
                            String.join(",", classesInCycle)));
        }

        var isSingleton = implementation.isAnnotationPresent(Singleton.class);
        if (isSingleton && instanceCache.containsKey(implementation)) {
            return (T)instanceCache.get(implementation);
        }

        var instance = injectIntoClass(implementation);
        if (isSingleton) {
            instanceCache.put(implementation, instance);
        }
        return (T)instance;
    }

    private <T> T injectIntoClass(Class<T> cls)
            throws NoDefaultConstructorException, ImplementationNotFoundException, ImplementationAmbiguityException,
            CyclicDependenciesException {

        try {
            var injectableConstructors = Arrays.stream(cls.getDeclaredConstructors())
                    .filter(c -> c.isAnnotationPresent(Inject.class))
                    .toList();

            T instance = null;
            if (!injectableConstructors.isEmpty()) {
                instance = injectIntoConstructor(injectableConstructors.stream().findFirst().get());
            }

            if (instance == null) {
                instance = injectIntoFieldsAndSetter(cls);
            } else {
                injectIntoFieldsAndSetter(cls, instance);
            }

            return instance;
        } catch (NoSuchMethodException e) {
            throw new NoDefaultConstructorException(
                    String.format("Could not find a default constructor or an " +
                            "injectable constructor for the injectable class %s", cls.getName()));
        } catch (ReflectiveOperationException e) {
            e.printStackTrace();
            return null;
        }
    }

    private <T> void injectIntoFieldsAndSetter(Class<T> cls, Object instance)
            throws ImplementationNotFoundException, NoDefaultConstructorException, ImplementationAmbiguityException,
            ReflectiveOperationException, CyclicDependenciesException {

        injectIntoFields(cls, instance);
        injectIntoSetters(cls, instance);
    }

    private <T> void injectIntoSetters(Class<T> cls, Object instance)
            throws ReflectiveOperationException, ImplementationNotFoundException, NoDefaultConstructorException,
            ImplementationAmbiguityException, CyclicDependenciesException {

        for(Method method : cls.getDeclaredMethods()) {
            if (method.isAnnotationPresent(Inject.class)) {
                method.setAccessible(true);
                var tag = getTag(method);
                var parameters = getParameters(method.getParameters(), tag);
                method.invoke(instance, parameters);
            }
        }
    }

    private <T> void injectIntoFields(Class<T> cls, Object instance)
            throws IllegalAccessException, ImplementationNotFoundException, NoDefaultConstructorException,
            ImplementationAmbiguityException, CyclicDependenciesException {

        for(Field field : cls.getDeclaredFields()) {
            if (field.isAnnotationPresent(Inject.class)) {
                field.setAccessible(true);
                var tag = getTag(field);
                field.set(instance, instantiateByTag(field.getType(), tag));
            }
        }
    }

    private <T> T injectIntoFieldsAndSetter(Class<T> cls)
            throws ReflectiveOperationException, ImplementationNotFoundException, NoDefaultConstructorException,
            ImplementationAmbiguityException, CyclicDependenciesException {

        // If an injectable class doesn't have a constructor annotated with @Inject
        // we suppose that it has a default constructor (without parameters)
        var instance = cls.getDeclaredConstructor().newInstance();
        injectIntoFieldsAndSetter(cls, instance);
        return instance;
    }

    private Object[] getParameters(Parameter[] parameters, String constructorTag)
            throws ImplementationNotFoundException, NoDefaultConstructorException, ImplementationAmbiguityException,
            CyclicDependenciesException {

        var parametersOutput = new ArrayList<>();
        for(var parameter : parameters) {
            var type = parameter.getType();
            var parameterTag = parameter.isAnnotationPresent(Tag.class) ? parameter.getAnnotation(Tag.class).value() : null;
            var appliedTag = constructorTag == null ? parameterTag : constructorTag;
            parametersOutput.add(instantiateByTag(type, appliedTag));
        }
        return parametersOutput.toArray();
    }

    private static String getTag(AccessibleObject accessibleObject) {
        return accessibleObject.isAnnotationPresent(Tag.class)
                ? accessibleObject.getAnnotation(Tag.class).value() : null;
    }

    @SuppressWarnings("unchecked")
    private <T> T injectIntoConstructor(Constructor<?> constructor)
            throws ReflectiveOperationException, ImplementationNotFoundException, NoDefaultConstructorException,
            ImplementationAmbiguityException, CyclicDependenciesException {

        var tag = getTag(constructor);
        var parameters = getParameters(constructor.getParameters(), tag);
        return (T)constructor.newInstance(parameters);
    }

    private Class<?> getImplementationFromBase(Class<?> baseClass, String tag)
            throws ImplementationNotFoundException, ImplementationAmbiguityException {

        return dependencies.findImplementationFromBaseClass(baseClass, tag);
    }
}
