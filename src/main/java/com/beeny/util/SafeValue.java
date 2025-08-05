package com.beeny.util;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * Utility class providing null-safe access and defaulting helpers.
 * <p>
 * All methods are generic and operate on any reference type.
 */
public final class SafeValue {

    private SafeValue() {
        // Utility class; prevent instantiation
    }

    /**
     * Returns {@code value} if it is non-null; otherwise returns {@code defaultValue}.
     *
     * @param value        the value to test for null
     * @param defaultValue the value to return if {@code value} is null
     * @param <T>           the type of the values
     * @return {@code value} if non-null; otherwise {@code defaultValue}
     */
    public static <T> T getOrDefault(T value, T defaultValue) {
        return value != null ? value : defaultValue;
    }

    /**
     * Returns {@code value} if it is non-null; otherwise returns the result supplied by {@code defaultSupplier}.
     *
     * @param value            the value to test for null
     * @param defaultSupplier  supplier that produces the fallback value if {@code value} is null
     * @param <T>               the type of the values
     * @return {@code value} if non-null; otherwise the result of {@code defaultSupplier.get()}
     * @throws NullPointerException if {@code defaultSupplier} is null or produces a null result
     */
    public static <T> T getOrElse(T value, Supplier<? extends T> defaultSupplier) {
        if (value != null) {
            return value;
        }
        Objects.requireNonNull(defaultSupplier, "defaultSupplier must not be null");
        T supplied = defaultSupplier.get();
        return Objects.requireNonNull(supplied, "defaultSupplier.get() must not return null");
    }

    /**
     * Returns whether the provided {@code value} is non-null.
     *
     * @param value the value to test
     * @param <T>     the type of the value
     * @return {@code true} if {@code value} is non-null; {@code false} otherwise
     */
    public static <T> boolean isPresent(T value) {
        return value != null;
    }

    /**
     * Returns {@code value} if it is non-null; otherwise returns {@code defaultValue}.
     * <p>
     * This method mirrors {@link java.util.Objects#requireNonNullElse(Object, Object)} behavior:
     * neither {@code value} nor {@code defaultValue} is modified, and if {@code value} is null then
     * {@code defaultValue} must be non-null.
     *
     * @param value        the possibly-null value
     * @param defaultValue the non-null value to return if {@code value} is null
     * @param <T>           the type of the values
     * @return {@code value} if non-null; otherwise {@code defaultValue}
     * @throws NullPointerException if {@code value} is null and {@code defaultValue} is null
     */
    public static <T> T requireNonNullElse(T value, T defaultValue) {
        return value != null ? value : Objects.requireNonNull(defaultValue, "defaultValue must not be null");
        // Equivalent to Objects.requireNonNullElse in modern JDKs, but kept here for convenience/consistency.
    }

    /**
     * Returns {@code value} if it is non-null; otherwise returns the result supplied by {@code defaultSupplier}.
     * <p>
     * This method mirrors {@link java.util.Objects#requireNonNullElseGet(Object, Supplier)} behavior:
     * if {@code value} is null, the {@code defaultSupplier} is invoked and must not be null nor supply null.
     *
     * @param value           the possibly-null value
     * @param defaultSupplier the supplier of the value to use if {@code value} is null
     * @param <T>              the type of the values
     * @return {@code value} if non-null; otherwise the non-null result of {@code defaultSupplier.get()}
     * @throws NullPointerException if {@code defaultSupplier} is null or supplies a null value
     */
    public static <T> T requireNonNullElseGet(T value, Supplier<? extends T> defaultSupplier) {
        if (value != null) {
            return value;
        }
        Objects.requireNonNull(defaultSupplier, "defaultSupplier must not be null");
        T supplied = defaultSupplier.get();
        return Objects.requireNonNull(supplied, "defaultSupplier.get() must not return null");
    }
}