package doodle.qa.com.svcproviderqa.validation;

import doodle.qa.com.svcproviderqa.dto.EventDto;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.time.LocalDateTime;

/**
 * Validator for the {@link ValidEventTime} annotation. Ensures that an event's start time is before
 * its end time.
 */
public class ValidEventTimeValidator implements ConstraintValidator<ValidEventTime, EventDto> {

  @Override
  public void initialize(ValidEventTime constraintAnnotation) {
    // No initialization needed
  }

  @Override
  public boolean isValid(EventDto eventDto, ConstraintValidatorContext context) {
    if (eventDto == null) {
      return true; // Null objects are validated by @NotNull
    }

    LocalDateTime startTime = eventDto.getStartTime();
    LocalDateTime endTime = eventDto.getEndTime();

    // If either time is null, skip this validation as @NotNull will handle it
    if (startTime == null || endTime == null) {
      return true;
    }

    // Validate that start time is before end time
    return startTime.isBefore(endTime);
  }
}
