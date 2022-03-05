package fr.gravani.eazzynject;

import fr.gravani.eazzynject.annotations.Injectable;
import fr.gravani.eazzynject.exceptions.*;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

public class Scanner {

    private static final Container CONTAINER = new Container();

    //*** Static methods
    public static void initContainer(String packageName)
            throws ClassLoaderNotFoundException, IOException, ClassNotFoundException, ImplementationAmbiguityException {
        List<Class<?>> allClassesInPackage = getClasses(packageName);
        var injectableClasses =  allClassesInPackage
                .stream()
                .filter(c -> c.isAnnotationPresent(Injectable.class)
                        && !c.isInterface() && !Modifier.isAbstract(c.getModifiers()))
                .toList();

        for(var injectableClass : injectableClasses) {
            CONTAINER.registerMapping(injectableClass, injectableClass);
            registerSuperclassesInterfaces(injectableClass, injectableClass);
        }
    }

    public static <T> T getInstance(Class<T> type)
            throws ImplementationNotFoundException, NoDefaultConstructorException, ImplementationAmbiguityException,
            CyclicDependenciesException {
        return CONTAINER.instantiate(type);
    }

    private static void registerSuperclassesInterfaces(Class<?> implementationClass, Class<?> superClass) throws ImplementationAmbiguityException {
        Class<?> cSuperClass = superClass.getSuperclass();
        Class<?>[] interfaces = superClass.getInterfaces();

        //*** Registering superclass
        if(cSuperClass != Object.class && cSuperClass != null) {
            CONTAINER.registerMapping(implementationClass, cSuperClass);
            registerSuperclassesInterfaces(implementationClass, cSuperClass);
        }
        //*** Registering interfaces
        for(Class<?> curInterface : interfaces) {
            CONTAINER.registerMapping(implementationClass, curInterface);
            registerSuperclassesInterfaces(implementationClass, curInterface);
        }
    }

    private static List<Class<?>> getClasses(String packageName)
            throws ClassLoaderNotFoundException, IOException, ClassNotFoundException {

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
        List<Class<?>> classes = new ArrayList<>();
        for (File directory : dirs) {
            classes.addAll(findClasses(directory, packageName));
        }
        return classes;
    }

    private static List<Class<?>> findClasses(File directory, String packageName) throws ClassNotFoundException {
        List<Class<?>> classes = new ArrayList<>();
        if (!directory.exists()) {
            return classes;
        }
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    assert !file.getName().contains(".");
                    classes.addAll(findClasses(file, packageName + "." + file.getName()));
                } else if (file.getName().endsWith(".class")) {
                    classes.add(Class.forName(packageName + '.' + file.getName().substring(0, file.getName().length() - 6)));
                }
            }
        }
        return classes;
    }
}
