package org.dungeon.prototype.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Constraint(validatedBy = MultiConditionalNotNullValidator.class)
@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
public @interface MultiConditionalNotNull {
    String message() default "Field cannot be null when condition is met";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};

    Condition[] conditions();

    @Target({ ElementType.TYPE })
    @Retention(RetentionPolicy.RUNTIME)
    @interface Condition {
        String field();
        String conditionalField();
        String[] conditionalValues();
    }
}
