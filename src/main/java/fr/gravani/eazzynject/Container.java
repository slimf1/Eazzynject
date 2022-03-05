package fr.gravani.eazzynject;

import fr.gravani.eazzynject.annotations.Inject;
import fr.gravani.eazzynject.annotations.Injectable;
import fr.gravani.eazzynject.annotations.Tag;
import fr.gravani.eazzynject.exceptions.ClassLoaderNotFoundException;
import fr.gravani.eazzynject.exceptions.ImplementationAmbiguityException;
import fr.gravani.eazzynject.exceptions.ImplementationNotFoundException;
import fr.gravani.eazzynject.exceptions.NoDefaultConstructorException;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.*;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

public class Container {
    //*** Class attributes
    protected static final Map<Class<?>, Class<?>> dependencies = new HashMap<>();
    protected static final Map<Class<?>, Object> instanceCache = new HashMap<>(); // Use @Singleton annotation

    //*** Static methods
    public static final void initContainer(String packageName)
            throws ClassLoaderNotFoundException, IOException, ClassNotFoundException {
        Class[] allClassesInPackage = getClasses(packageName);
        Arrays.stream(allClassesInPackage)
                .filter(c -> c.isAnnotationPresent(Injectable.class) && !c.isInterface() && !Modifier.isAbstract(c.getModifiers()))
                .forEach(c -> {
                    registerSuperclassesInterfaces(c, c);
                });
    }

    private static void registerSuperclassesInterfaces(Class implementationClass, Class superClass) {
        Class cSuperClass = superClass.getSuperclass();
        Class[] interfaces = superClass.getInterfaces();

        if((cSuperClass == Object.class || cSuperClass == null) && interfaces.length == 0) {
            //*** No superclass and implements no interface
            registerMapping(implementationClass, implementationClass);
        }
        else {
            //*** Registering superclass
            if(cSuperClass != Object.class) {
                registerMapping(implementationClass, cSuperClass);
                registerSuperclassesInterfaces(implementationClass, cSuperClass);
            }

            //*** Registering interfaces
            for(Class curInterface : interfaces) {
                registerMapping(implementationClass, curInterface);
                registerSuperclassesInterfaces(implementationClass, curInterface);
            }
        }


    }

    private static final Class[] getClasses(String packageName) throws ClassLoaderNotFoundException, IOException, ClassNotFoundException {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        if(classLoader == null) {
            throw new ClassLoaderNotFoundException("ClassLoader not found for automatic package scanning.");
        }
        String filePath = packageName.replace('.', '/');
        Enumeration<URL> resources = classLoader.getResources(filePath);
        List<File> dirs = new ArrayList<>();
        while (resources.hasMoreElements()) {
            URL resource = resources.nextElement();
            dirs.add(new File(resource.getFile()));
        }
        ArrayList<Class> classes = new ArrayList<Class>();
        for (File directory : dirs) {
            classes.addAll(findClasses(directory, packageName));
        }
        return classes.toArray(new Class[classes.size()]);
    }

    private static List<Class> findClasses(File directory, String packageName) throws ClassNotFoundException {
        List<Class> classes = new ArrayList<Class>();
        if (!directory.exists()) {
            return classes;
        }
        File[] files = directory.listFiles();
        for (File file : files) {
            if (file.isDirectory()) {
                assert !file.getName().contains(".");
                classes.addAll(findClasses(file, packageName + "." + file.getName()));
            } else if (file.getName().endsWith(".class")) {
                classes.add(Class.forName(packageName + '.' + file.getName().substring(0, file.getName().length() - 6)));
            }
        }
        return classes;
    }

    //*** Instances methods
    public static <T> void registerMapping(Class<? extends T> child, Class<T> base) {
        dependencies.put(child, base);
    }

    public <T> T instantiate(Class<T> inter)
            throws ImplementationNotFoundException, NoDefaultConstructorException, ImplementationAmbiguityException {
        return instantiate(inter, null);
    }

    @SuppressWarnings("unchecked")
    private <T> T instantiate(Class<T> inter, String tag)
            throws ImplementationNotFoundException, NoDefaultConstructorException, ImplementationAmbiguityException {

        var implementation = getImplementationFromBase(inter, tag);

        if (instanceCache.containsKey(implementation)) {
            return (T)instanceCache.get(implementation);
        }

        var instance = injectIntoClass(implementation);
        instanceCache.put(implementation, instance);

        return (T)instance;
    }

    private <T> T injectIntoClass(Class<T> cls)
            throws NoDefaultConstructorException, ImplementationNotFoundException, ImplementationAmbiguityException {

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
            return null; // TODO : do more?
        }
    }

    private <T> void injectIntoFieldsAndSetter(Class<T> cls, Object instance)
            throws ImplementationNotFoundException, NoDefaultConstructorException, ImplementationAmbiguityException,
            ReflectiveOperationException {

        injectIntoFields(cls, instance);
        injectIntoSetters(cls, instance);
    }

    private <T> void injectIntoSetters(Class<T> cls, Object instance)
            throws ReflectiveOperationException, ImplementationNotFoundException, NoDefaultConstructorException,
            ImplementationAmbiguityException {

        for(Method method : cls.getDeclaredMethods()) {
            if (method.isAnnotationPresent(Inject.class)) {
                method.setAccessible(true);
                var tag = getTag(method);
                var parameters = getParameters(method.getParameterTypes(), tag);
                method.invoke(instance, parameters);
            }
        }
    }

    private <T> void injectIntoFields(Class<T> cls, Object instance)
            throws IllegalAccessException, ImplementationNotFoundException, NoDefaultConstructorException,
            ImplementationAmbiguityException {

        for(Field field : cls.getDeclaredFields()) {
            if (field.isAnnotationPresent(Inject.class)) {
                field.setAccessible(true);
                var tag = getTag(field);
                field.set(instance, instantiate(field.getType(), tag));
            }
        }
    }

    private <T> T injectIntoFieldsAndSetter(Class<T> cls)
            throws ReflectiveOperationException, ImplementationNotFoundException, NoDefaultConstructorException,
            ImplementationAmbiguityException {

        // If an injectable class doesn't have a constructor annotated with @Inject
        // we suppose that it has a default constructor (without parameters)
        var instance = cls.getDeclaredConstructor().newInstance();
        injectIntoFieldsAndSetter(cls, instance);
        return instance;
    }

    private Object[] getParameters(Class<?>[] parametersTypes, String tag)
            throws ImplementationNotFoundException, NoDefaultConstructorException, ImplementationAmbiguityException {

        var parameters = new ArrayList<>();
        for(var type : parametersTypes) {
            parameters.add(instantiate(type, tag));
        }
        return parameters.toArray();
    }

    private String getTag(AccessibleObject accessibleObject) {
        return accessibleObject.isAnnotationPresent(Tag.class)
                ? accessibleObject.getAnnotation(Tag.class).value() : null;
    }

    @SuppressWarnings("unchecked")
    private <T> T injectIntoConstructor(Constructor<?> constructor)
            throws ReflectiveOperationException, ImplementationNotFoundException, NoDefaultConstructorException,
            ImplementationAmbiguityException {

        var tag = getTag(constructor);
        var parameters = getParameters(constructor.getParameterTypes(), tag);
        return (T)constructor.newInstance(parameters);
    }

    private Class<?> getImplementationFromBase(Class<?> baseClass, String tag)
            throws ImplementationNotFoundException, ImplementationAmbiguityException {
        var implementations = dependencies
                .entrySet()
                .stream()
                .filter(item -> item.getValue().equals(baseClass))
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());

        if (implementations.isEmpty()) {
            throw new ImplementationNotFoundException(
                    String.format("Could not find any implementation for base type %s", baseClass.getName()));
        }

        if (implementations.size() == 1) {
            return implementations.stream().findFirst().get();
        }

        if (tag == null) {
            throw new ImplementationAmbiguityException(
                    String.format(
                            "Tag not found even though found %d different implementations for base type %s",
                            implementations.size(), baseClass.getName()));
        }

        var validImplementations = implementations
                .stream()
                .filter(implementation -> {
                    var implementationTag = implementation.isAnnotationPresent(Tag.class)
                            ? implementation.getAnnotation(Tag.class).value() : null;
                    if (implementationTag != null) {
                        return implementationTag.equals(tag);
                    }
                    return false;
                })
                .toList();

        if (validImplementations.isEmpty()) {
            throw new ImplementationNotFoundException(
                    String.format("Could not find any valid implementation for type %s", baseClass.getName()));
        } else if (validImplementations.size() >= 2) {
            throw new ImplementationAmbiguityException(
                    String.format("Found %s conflicting tags for type %s",
                            validImplementations.size(), baseClass.getName()));
        } else {
            return validImplementations.stream().findFirst().get();
        }
        // https://dev.to/jjbrt/how-to-create-your-own-dependency-injection-framework-in-java-4eaj
        // Le mettre dans le cr/readme
    }
}
