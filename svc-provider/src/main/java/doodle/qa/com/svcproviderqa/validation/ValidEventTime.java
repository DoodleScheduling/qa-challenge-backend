package doodle.qa.com.svcproviderqa.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Validation annotation to ensure that an event's start time is before its end time. */
@Documented
@Constraint(validatedBy = ValidEventTimeValidator.class)
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidEventTime {
  String message() default "End time must be after start time";

  Class<?>[] groups() default {};

  Class<? extends Payload>[] payload() default {};
}
