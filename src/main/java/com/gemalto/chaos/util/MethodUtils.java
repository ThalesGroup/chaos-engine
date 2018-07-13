package com.gemalto.chaos.util;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MethodUtils {
    private MethodUtils () {
    }

    public static List<Method> getMethodsWithAnnotation (Class<?> clazz, Class<? extends Annotation> annotation) {
        final List<Method> methods = new ArrayList<>();
        while (clazz != Object.class) {
            List<Method> allMethods = Arrays.asList(clazz.getDeclaredMethods());
            allMethods.stream().filter(method -> method.isAnnotationPresent(annotation)).forEach(methods::add);
            clazz = clazz.getSuperclass();
        }
        return methods;
    }
}
