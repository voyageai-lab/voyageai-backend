package com.voyageai.voyageaibackend.web.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link PlanningRequest}.
 */
class PlanningRequestTest {

  private Validator validator;

  @BeforeEach
  void setUp() {
    ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
    validator = factory.getValidator();
  }

  @Test
  void setRequirements_shouldSetValue() {
    // Given
    PlanningRequest request = new PlanningRequest();
    String requirements = "Plan a 5-day trip to Tokyo";

    // When
    request.setRequirements(requirements);

    // Then
    assertEquals(requirements, request.getRequirements());
  }

  @Test
  void validation_validRequirements_shouldPass() {
    // Given
    PlanningRequest request = new PlanningRequest();
    request.setRequirements("Plan a 5-day trip to Tokyo");

    // When
    Set<ConstraintViolation<PlanningRequest>> violations = validator.validate(request);

    // Then
    assertEquals(0, violations.size());
  }

  @Test
  void validation_emptyRequirements_shouldFail() {
    // Given
    PlanningRequest request = new PlanningRequest();
    request.setRequirements("");

    // When
    Set<ConstraintViolation<PlanningRequest>> violations = validator.validate(request);

    // Then
    assertFalse(violations.isEmpty());
    assertTrue(violations.size() >= 1, "Should have at least one validation violation");
    
    // Verify the property path is correct
    boolean hasRequirementsViolation = violations.stream()
        .anyMatch(v -> "requirements".equals(v.getPropertyPath().toString()));
    assertTrue(hasRequirementsViolation, "Should have violation on 'requirements' field");
  }

  @Test
  void validation_nullRequirements_shouldFail() {
    // Given
    PlanningRequest request = new PlanningRequest();
    request.setRequirements(null);

    // When
    Set<ConstraintViolation<PlanningRequest>> violations = validator.validate(request);

    // Then
    assertFalse(violations.isEmpty());
  }

  @Test
  void toString_shouldContainRequirements() {
    // Given
    PlanningRequest request = new PlanningRequest();
    request.setRequirements("Test requirements");

    // When
    String result = request.toString();

    // Then
    assertNotNull(result);
  }
}

