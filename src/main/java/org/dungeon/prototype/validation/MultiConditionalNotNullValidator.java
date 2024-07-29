package org.dungeon.prototype.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.dungeon.prototype.model.document.room.RoomContentDocument;

import java.lang.reflect.Field;
import java.util.Arrays;

public class MultiConditionalNotNullValidator implements ConstraintValidator<MultiConditionalNotNull, RoomContentDocument> {
    private MultiConditionalNotNull.Condition[] conditions;

    @Override
    public void initialize(MultiConditionalNotNull constraintAnnotation) {
        this.conditions = constraintAnnotation.conditions();
    }

    @Override
    public boolean isValid(RoomContentDocument value, ConstraintValidatorContext context) {
        try {
            for (MultiConditionalNotNull.Condition condition : conditions) {
                Field conditionalField = value.getClass().getDeclaredField(condition.conditionalField());
                conditionalField.setAccessible(true);
                Object conditionalFieldValue = conditionalField.get(value);

                if (conditionalFieldValue != null && Arrays.asList(condition.conditionalValues()).contains(conditionalFieldValue.toString())) {
                    Field field = value.getClass().getDeclaredField(condition.field());
                    field.setAccessible(true);
                    Object fieldValue = field.get(value);

                    if (fieldValue == null) {
                        context.disableDefaultConstraintViolation();
                        context.buildConstraintViolationWithTemplate(context.getDefaultConstraintMessageTemplate())
                                .addPropertyNode(condition.field())
                                .addConstraintViolation();
                        return false;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }
}
