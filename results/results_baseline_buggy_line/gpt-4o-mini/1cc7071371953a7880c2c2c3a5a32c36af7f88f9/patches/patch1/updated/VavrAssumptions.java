/*
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 *
 * Copyright 2017-2022 the original author or authors.
 */
package org.assertj.vavr.api;

import io.vavr.Lazy;
import io.vavr.collection.Map;
import io.vavr.collection.Multimap;
import io.vavr.collection.Seq;
import io.vavr.collection.Set;
import io.vavr.control.Either;
import io.vavr.control.Option;
import io.vavr.control.Try;
import io.vavr.control.Validation;
import org.assertj.core.util.CheckReturnValue;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.Callable;

import static org.assertj.core.internal.bytebuddy.matcher.ElementMatchers.any;
import static org.assertj.core.util.Arrays.array;
import static org.assertj.vavr.api.ClassLoadingStrategyFactory.classLoadingStrategy;

public class VavrAssumptions {

    private static final org.assertj.core.internal.bytebuddy.ByteBuddy BYTE_BUDDY = new org.assertj.core.internal.bytebuddy.ByteBuddy().with(org.assertj.core.internal.bytebuddy.dynamic.scaffold.TypeValidation.DISABLED)
            .with(new org.assertj.core.internal.bytebuddy.implementation.auxiliary.AuxiliaryType.NamingStrategy.SuffixingRandom("Assertj$Assumptions"));

    private static final org.assertj.core.internal.bytebuddy.implementation.Implementation ASSUMPTION = org.assertj.core.internal.bytebuddy.implementation.MethodDelegation.to(AssumptionMethodInterceptor.class);

    private static final org.assertj.core.internal.bytebuddy.TypeCache<org.assertj.core.internal.bytebuddy.TypeCache.SimpleKey> CACHE = new org.assertj.core.internal.bytebuddy.TypeCache.WithInlineExpunction<>(org.assertj.core.internal.bytebuddy.TypeCache.Sort.SOFT);

    private static final class AssumptionMethodInterceptor {

        @org.assertj.core.internal.bytebuddy.implementation.bind.annotation.RuntimeType
        public static Object intercept(@org.assertj.core.internal.bytebuddy.implementation.bind.annotation.This AbstractVavrAssert<?, ?> assertion, @org.assertj.core.internal.bytebuddy.implementation.bind.annotation.SuperCall Callable<Object> proxy) throws Exception {
            try {
                Object result = proxy.call();
                if (result != assertion && result instanceof AbstractVavrAssert) {
                    final AbstractVavrAssert<?, ?> assumption = asAssumption((AbstractVavrAssert<?, ?>) result);
                    return assumption.withAssertionState(assertion);
                }
                return result;
            } catch (AssertionError e) {
                throw assumptionNotMet(e);
            }
        }
    }

    @CheckReturnValue
    @SuppressWarnings("unchecked")
    public static <LEFT, RIGHT> EitherAssert<LEFT, RIGHT> assumeThat(Either<LEFT, RIGHT> actual) {
        return asAssumption(EitherAssert.class, Either.class, actual);
    }

    @CheckReturnValue
    @SuppressWarnings("unchecked")
    public static <VALUE> LazyAssert<VALUE> assumeThat(Lazy<VALUE> actual) {
        return asAssumption(LazyAssert.class, Lazy.class, actual);
    }

    @CheckReturnValue
    @SuppressWarnings("unchecked")
    public static <K, V> MapAssert<K, V> assumeThat(Map<K, V> actual) {
        return asAssumption(MapAssert.class, Map.class, actual);
    }

    @CheckReturnValue
    @SuppressWarnings("unchecked")
    public static <K, V> MultimapAssert<K, V> assumeThat(Multimap<K, V> actual) {
        return asAssumption(MultimapAssert.class, Multimap.class, actual);
    }

    @CheckReturnValue
    @SuppressWarnings("unchecked")
    public static <VALUE> OptionAssert<VALUE> assumeThat(Option<VALUE> actual) {
        return asAssumption(OptionAssert.class, Option.class, actual);
    }

    @CheckReturnValue
    @SuppressWarnings("unchecked")
    public static <ELEMENT> SetAssert<ELEMENT> assumeThat(Set<ELEMENT> actual) {
        return asAssumption(SetAssert.class, Set.class, actual);
    }

    @CheckReturnValue
    @SuppressWarnings("unchecked")
    public static <ELEMENT> SeqAssert<ELEMENT> assumeThat(Seq<ELEMENT> actual) {
        return asAssumption(SeqAssert.class, Seq.class, actual);
    }

    @CheckReturnValue
    @SuppressWarnings("unchecked")
    public static <VALUE> TryAssert<VALUE> assumeThat(Try<VALUE> actual) {
        return asAssumption(TryAssert.class, Try.class, actual);
    }

    @CheckReturnValue
    @SuppressWarnings("unchecked")
    public static <INVALID, VALID> ValidationAssert<INVALID, VALID> assumeThat(Validation<INVALID, VALID> actual) {
        return asAssumption(ValidationAssert.class, Validation.class, actual);
    }

    private static <ASSERTION, ACTUAL> ASSERTION asAssumption(Class<ASSERTION> assertionType,
                                                              Class<ACTUAL> actualType,
                                                              Object actual) {
        return asAssumption(assertionType, array(actualType), array(actual));
    }

    private static <ASSERTION> ASSERTION asAssumption(Class<ASSERTION> assertionType,
                                                      Class<?>[] constructorTypes,
                                                      Object... constructorParams) {
        try {
            Class<? extends ASSERTION> type = createAssumptionClass(assertionType);
            Constructor<? extends ASSERTION> constructor = type.getConstructor(constructorTypes);
            return constructor.newInstance(constructorParams);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | InstantiationException e) {
            throw new RuntimeException("Cannot create assumption instance", e);
        }
    }

    @SuppressWarnings("unchecked")
    private static <ASSERTION> Class<? extends ASSERTION> createAssumptionClass(Class<ASSERTION> assertClass) {
        org.assertj.core.internal.bytebuddy.TypeCache.SimpleKey cacheKey = new org.assertj.core.internal.bytebuddy.TypeCache.SimpleKey(assertClass);
        return (Class<ASSERTION>) CACHE.findOrInsert(VavrAssumptions.class.getClassLoader(),
                cacheKey,
                () -> generateAssumptionClass(assertClass));
    }

    private static <ASSERTION> Class<? extends ASSERTION> generateAssumptionClass(Class<ASSERTION> assertionType) {
        return BYTE_BUDDY.subclass(assertionType)
                .method(any())
                .intercept(ASSUMPTION)
                .make()
                .load(VavrAssumptions.class.getClassLoader(), classLoadingStrategy(assertionType))
                .getLoaded();
    }

    private static RuntimeException assumptionNotMet(AssertionError assertionError) throws ReflectiveOperationException {
        Class<?> assumptionClass = getAssumptionClass("org.junit.AssumptionViolatedException");
        if (assumptionClass != null) return assumptionNotMet(assumptionClass, assertionError);

        assumptionClass = getAssumptionClass("org.opentest4j.TestAbortedException");
        if (assumptionClass != null) return assumptionNotMet(assumptionClass, assertionError);

        assumptionClass = getAssumptionClass("org.testng.SkipException");
        if (assumptionClass != null) return assumptionNotMet(assumptionClass, assertionError);

        throw new IllegalStateException("Assumptions require JUnit, opentest4j or TestNG on the classpath");
    }

    private static Class<?> getAssumptionClass(String className) {
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

    private static RuntimeException assumptionNotMet(Class<?> exceptionClass,
                                                     AssertionError e) throws ReflectiveOperationException {
        return (RuntimeException) exceptionClass.getConstructor(String.class, Throwable.class)
                .newInstance("assumption was not met due to: " + e.getMessage(), e);
    }

    private static AbstractVavrAssert<?, ?> asAssumption(AbstractVavrAssert<?, ?> assertion) {
        Object actual = assertion.actual();
        if (assertion instanceof LazyAssert) return asAssumption(LazyAssert.class, Lazy.class, actual);
        if (assertion instanceof EitherAssert) return asAssumption(EitherAssert.class, Either.class, actual);
        if (assertion instanceof MapAssert) return asAssumption(MapAssert.class, Map.class, actual);
        if (assertion instanceof OptionAssert) return asAssumption(OptionAssert.class, Option.class, actual);
        if (assertion instanceof SeqAssert) return asAssumption(SeqAssert.class, Seq.class, actual);
        if (assertion instanceof TryAssert) return asAssumption(TryAssert.class, Try.class, actual);
        if (assertion instanceof ValidationAssert) return asAssumption(ValidationAssert.class, Validation.class, actual);
        throw new IllegalArgumentException("Unsupported assumption creation for " + assertion.getClass());
    }

}