package com.udframework.bdd.util;

import java.io.File;
import java.net.URL;
import java.util.function.ToIntFunction;

public class ClassUtil {

    public static Class<?> findClassOnClassPath (ToIntFunction<Class<?>> tester, String packageName) throws ClassNotFoundException {
        File packageDir = getPackageDir(packageName);
        File[] files = packageDir.listFiles();

        if (files != null) {
            for (File file : files) {
                String root = packageName.equals("") ? "" : packageName + ".";
                if (file.isDirectory()) {
                    Class<?> c = findClassOnClassPath(tester, root + file.getName());
                    if (c != null) return c;
                }
                else if (file.getName().endsWith(".class")) {
                    String className = root + file.getName().substring(0, file.getName().length() - 6);
                    Class<?> clazz = Class.forName(className);
                    if (tester.applyAsInt(clazz) == 0) {
                        return clazz;
                    }
                }
            }
        }
        return null;
    }

    public static File getPackageDir (String packageName) throws ClassNotFoundException {
        String packageDirName = packageName.replace('.', '/');
        URL url = Thread.currentThread().getContextClassLoader().getResource(packageDirName);
        if (url != null) {
            return new File(url.getFile());
        }
        throw new ClassNotFoundException("package not found");
    }

    public static Object createInstance(Class<?> clazz) throws ReflectiveOperationException {
        try {
            return clazz.getConstructor().newInstance();
        }
        catch (NoSuchMethodException e) {
            throw new NoSuchMethodException("No default constructor found for " + clazz.getName());
        }
    }
}
