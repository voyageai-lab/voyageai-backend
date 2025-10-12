package com.voyageai.voyageaibackend.exception;

/**
 * Exception thrown when a requested resource is not found.
 * Typically results in HTTP 404 status.
 */
public class ResourceNotFoundException extends BusinessException {

  /**
   * Constructs a new ResourceNotFoundException.
   *
   * @param message the detail message
   */
  public ResourceNotFoundException(String message) {
    super(message, "RESOURCE_NOT_FOUND", 404);
  }

  /**
   * Constructs a new ResourceNotFoundException for a specific resource type and ID.
   *
   * @param resourceType the type of resource (e.g., "User", "TravelPlan")
   * @param resourceId the ID of the resource
   */
  public ResourceNotFoundException(String resourceType, Object resourceId) {
    super(resourceType + " not found with id: " + resourceId, "RESOURCE_NOT_FOUND", 404);
  }
}

