package de.hilling.junit.cdi.annotations;

import de.hilling.junit.cdi.scope.TestScoped;
import org.immutables.value.Value;

import javax.enterprise.inject.Alternative;
import javax.enterprise.inject.Stereotype;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import javax.enterprise.util.AnnotationLiteral;

/**
 * Use this annotation to mark Alternatives that should globally replace
 * production implementations.
 * <p>
 *     These services cannot be disabled or enabled on a per test basis
 *     because the container is only started once.
 * </p>
 */
@Alternative
@TestScoped
@Stereotype
@Target({ElementType.TYPE, ElementType.FIELD, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Value.Immutable
public @interface GlobalTestImplementation {
    class PriorityLiteral extends AnnotationLiteral<Priority> implements Priority {
        private static final long serialVersionUID = 1L;

        public int value() {
            return 100;
        }
    }
}