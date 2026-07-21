package com.warmthdawn.appliedpackaging.integration.recipe;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;

/** Public-method compatibility rules for Thermal recipe wrappers and consumable input counts. */
final class ThermalRecipeSemantics {
    private ThermalRecipeSemantics() {
    }

    static Object unwrap(Object recipe) {
        if (!isThermal(recipe)) {
            return recipe;
        }
        try {
            Method valueMethod = recipe.getClass().getMethod("value");
            Object value = valueMethod.invoke(recipe);
            return value != null && value != recipe ? value : recipe;
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return recipe;
        }
    }

    static InputCounts consumableInputCounts(Object recipe) {
        if (!isThermal(recipe)) {
            return null;
        }
        try {
            return new InputCounts(
                    collectionSize(recipe, "getInputItems"),
                    collectionSize(recipe, "getInputFluids"));
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return null;
        }
    }

    private static boolean isThermal(Object recipe) {
        return recipe != null && recipe.getClass().getName().startsWith("cofh.thermal.");
    }

    private static int collectionSize(Object target, String methodName) throws ReflectiveOperationException {
        Method method;
        try {
            method = target.getClass().getMethod(methodName);
        } catch (NoSuchMethodException ignored) {
            return 0;
        }
        try {
            Object value = method.invoke(target);
            return value instanceof Collection<?> collection ? collection.size() : 0;
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw e;
        }
    }

    record InputCounts(int items, int fluids) {
    }
}
