package org.assertj.vavr.api;

import io.vavr.control.Try;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;

class ClassLoadingStrategyFactory {

    private static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();
    private static final Method PRIVATE_LOOKUP_IN = Try.of(
        () -> MethodHandles.class.getMethod("privateLookupIn", Class.class, MethodHandles.Lookup.class)
    ).getOrElse((Method) null);

    static ClassLoadingStrategy<ClassLoader> classLoadingStrategy(Class<?> assertClass) {
        if (ClassInjector.UsingReflection.isAvailable()) {
            return ClassLoadingStrategy.Default.INJECTION;
        } else if (ClassInjector.UsingLookup.isAvailable() && PRIVATE_LOOKUP_IN != null) {
            try {
                return ClassLoadingStrategy.UsingLookup.of(PRIVATE_LOOKUP_IN.invoke(null, assertClass, LOOKUP));
            } catch (Exception e) {
                throw new IllegalStateException("Could not access package of " + assertClass, e);
            }
        } else {
            throw new IllegalStateException("No code generation strategy available");
        }
    }

    interface ClassLoadingStrategy<T> {
        class Default {
            public static final ClassLoadingStrategy<ClassLoader> INJECTION = new ClassLoadingStrategy<ClassLoader>() {
            };
        }
        class UsingLookup {
            public static ClassLoadingStrategy<ClassLoader> of(Object lookup) {
                return new ClassLoadingStrategy<ClassLoader>() {
                };
            }
        }
    }

    static class ClassInjector {
        static class UsingReflection {
            static boolean isAvailable() {
                return false;
            }
        }
        static class UsingLookup {
            static boolean isAvailable() {
                return false;
            }
        }
    }
}