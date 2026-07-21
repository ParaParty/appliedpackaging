package com.warmthdawn.appliedpackaging.integration.recipe;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/** Conservative semantic checks for recipe viewers that do not expose chance metadata in their slot API. */
public final class RecipeTransferSemantics {
    private static final String RANDOM_OUTPUT = "gui.appliedpackaging.jei_transfer.random_output";
    private static final String UNSUPPORTED = "gui.appliedpackaging.jei_transfer.unsupported";
    private static final List<String> NON_CONSUMING_RECIPE_CLASSES = List.of(
            "com.buuz135.industrial.recipe.LaserDrillOreRecipe",
            "com.buuz135.industrial.recipe.LaserDrillFluidRecipe",
            "com.buuz135.industrial.plugin.jei.machineproduce.MachineProduceWrapper");

    private RecipeTransferSemantics() {
    }

    /** Returns a user-facing rejection key, or {@code null} when the known semantics are deterministic. */
    public static String rejectionKey(Object recipe) {
        if (recipe == null) {
            return null;
        }
        String className = recipe.getClass().getName();
        if (NON_CONSUMING_RECIPE_CLASSES.contains(className)
                || className.contains("OrechidRecipe")
                || className.contains("HeatPropertiesRecipe")) {
            return UNSUPPORTED;
        }
        try {
            if (className.startsWith("cofh.thermal.")) {
                Object value = readOptionalMember(recipe, "value", "value");
                if (value != null && value != recipe) {
                    recipe = value;
                    className = recipe.getClass().getName();
                }
            }
            if (className.startsWith("cofh.thermal.")) {
                Object chances = readOptionalMember(recipe, "getOutputItemChances", "outputItemChances");
                if (chances != null && containsNonIntegralThermalChance(chances)) {
                    return RANDOM_OUTPUT;
                }
            }
            if (hasTypeName(recipe.getClass(), "mekanism.api.recipes.SawmillRecipe")) {
                Number chance = asNumber(readOptionalMember(recipe, "getSecondaryChance", "secondaryChance"));
                if (chance != null && !isZeroOrOne(chance.doubleValue())) {
                    return RANDOM_OUTPUT;
                }
            }
            if (hasTypeName(
                    recipe.getClass(),
                    "me.desht.pneumaticcraft.common.recipes.machine.ExplosionCraftingRecipeImpl")) {
                Number lossRate = asNumber(readOptionalMember(recipe, "getLossRate", "lossRate"));
                if (lossRate != null && lossRate.doubleValue() != 0.0d) {
                    return RANDOM_OUTPUT;
                }
            }
            if (hasTypeName(recipe.getClass(), "com.enderio.machines.common.recipe.SagMillingRecipe")) {
                Object bonusType = readOptionalMember(recipe, "getBonusType", "bonusType");
                if (bonusType != null && !"NONE".equals(bonusType.toString())) {
                    return RANDOM_OUTPUT;
                }
            }
            Object keepNbt = readOptionalMember(recipe, "isKeepNbtOfReagent", "keepNbtOfReagent");
            if (Boolean.TRUE.equals(keepNbt)) {
                return UNSUPPORTED;
            }
            if (containsRandomOutputDefinition(recipe, new IdentityHashMap<>(), 0)) {
                return RANDOM_OUTPUT;
            }
            return null;
        } catch (ReflectiveOperationException | RuntimeException e) {
            return UNSUPPORTED;
        }
    }

    private static boolean containsRandomOutputDefinition(
            Object value,
            Map<Object, Boolean> visited,
            int depth) throws ReflectiveOperationException {
        if (value == null || depth > 3 || isLeaf(value) || visited.put(value, Boolean.TRUE) != null) {
            return false;
        }
        if (value instanceof Map<?, ?> map) {
            for (Object entryValue : map.values()) {
                if (containsRandomOutputDefinition(entryValue, visited, depth + 1)) {
                    return true;
                }
            }
            return false;
        }
        if (value instanceof Iterable<?> iterable) {
            for (Object element : iterable) {
                if (isRandomOutputElement(element)
                        || containsRandomOutputDefinition(element, visited, depth + 1)) {
                    return true;
                }
            }
            return false;
        }
        if (value.getClass().isArray()) {
            for (int index = 0; index < Array.getLength(value); index++) {
                Object element = Array.get(value, index);
                if (isRandomOutputElement(element)
                        || containsRandomOutputDefinition(element, visited, depth + 1)) {
                    return true;
                }
            }
            return false;
        }

        for (String member : List.of(
                "getRollableResults",
                "getOutputs",
                "getSecondaryOutputs",
                "getResultPool",
                "outputs",
                "secondaryOutputs",
                "resultPool")) {
            Object container = readOptionalMember(value, member, member);
            if (container != null && containsRandomOutputDefinition(container, visited, depth + 1)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isRandomOutputElement(Object value) throws ReflectiveOperationException {
        if (value == null) {
            return false;
        }
        Number chance = asNumber(readOptionalMember(value, "getChance", "chance"));
        if (chance == null) {
            chance = asNumber(readOptionalMember(value, "chance", "chance"));
        }
        if (chance == null) {
            chance = asNumber(readOptionalMember(value, "getProbability", "probability"));
        }
        if (chance == null) {
            chance = asNumber(readOptionalMember(value, "probability", "probability"));
        }
        if (chance != null && Double.compare(chance.doubleValue(), 1.0d) != 0) {
            return true;
        }
        Number maxRange = asNumber(readOptionalMember(value, "getMaxRange", "maxRange"));
        if (maxRange == null) {
            maxRange = asNumber(readOptionalMember(value, "maxRange", "maxRange"));
        }
        return maxRange != null && maxRange.longValue() != 1L;
    }

    private static boolean hasTypeName(Class<?> type, String expectedName) {
        for (Class<?> cursor = type; cursor != null; cursor = cursor.getSuperclass()) {
            if (cursor.getName().equals(expectedName)) {
                return true;
            }
            for (Class<?> implemented : cursor.getInterfaces()) {
                if (hasTypeName(implemented, expectedName)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean containsNonIntegralThermalChance(Object value) {
        if (value instanceof Collection<?> collection) {
            for (Object element : collection) {
                Number number = asNumber(element);
                if (number != null && !isPositiveInteger(number.doubleValue())) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean isPositiveInteger(double value) {
        return Double.isFinite(value) && value >= 1.0d && value == Math.rint(value);
    }

    private static boolean isZeroOrOne(double value) {
        return Double.compare(value, 0.0d) == 0 || Double.compare(value, 1.0d) == 0;
    }

    private static Number asNumber(Object value) {
        return value instanceof Number number ? number : null;
    }

    private static boolean isLeaf(Object value) {
        Class<?> type = value.getClass();
        return type.isPrimitive()
                || value instanceof Number
                || value instanceof CharSequence
                || value instanceof Boolean
                || value instanceof Enum<?>;
    }

    private static Object readOptionalMember(Object target, String methodName, String fieldName)
            throws ReflectiveOperationException {
        Method method = findPublicMethod(target.getClass(), methodName);
        if (method != null) {
            try {
                return method.invoke(target);
            } catch (InvocationTargetException e) {
                Throwable cause = e.getCause();
                if (cause instanceof RuntimeException runtimeException) {
                    throw runtimeException;
                }
                throw e;
            }
        }
        Field field = findPublicField(target.getClass(), fieldName);
        return field == null ? null : field.get(target);
    }

    private static Method findPublicMethod(Class<?> type, String name) {
        for (Method method : type.getMethods()) {
            if (Modifier.isPublic(method.getModifiers())
                    && method.getParameterCount() == 0
                    && method.getName().equals(name)) {
                return method;
            }
        }
        return null;
    }

    private static Field findPublicField(Class<?> type, String name) {
        try {
            Field field = type.getField(name);
            return Modifier.isPublic(field.getModifiers()) ? field : null;
        } catch (NoSuchFieldException ignored) {
            return null;
        }
    }
}
