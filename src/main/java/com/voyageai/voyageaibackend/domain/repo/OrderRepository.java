package com.voyageai.voyageaibackend.domain.repo;

import com.voyageai.voyageaibackend.domain.entity.Order;
import com.voyageai.voyageaibackend.domain.entity.Order.OrderStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository interface for managing {@link Order} entities.
 * Extends JpaRepository to provide standard CRUD operations and
 * supports custom query methods based on method names.
 * 
 * <p>Note: Method names use underscores (e.g., findByUser_Id) to access nested properties
 * in Spring Data JPA. This is a Spring Data convention and checkstyle warnings are suppressed.
 */
@Repository
@SuppressWarnings("checkstyle:MethodName")
public interface OrderRepository extends JpaRepository<Order, Long> {

  /**
   * Finds all orders placed by a specific user.
   *
   * @param userId the ID of the user
   * @return a list of orders belonging to the user
   */
  List<Order> findByUser_Id(Long userId);

  /**
   * Finds an order by its unique ID and the user who placed it.
   *
   * @param orderId the ID of the order
   * @param userId the ID of the user
   * @return an Optional containing the order if found, or empty otherwise
   */
  Optional<Order> findByIdAndUser_Id(Long orderId, Long userId);

  /**
   * Finds all orders with a specific status.
   *
   * @param status the status of the orders to find
   * @return a list of orders matching the given status
   */
  List<Order> findByStatus(OrderStatus status);

  /**
   * Finds all orders for a specific user with a given status.
   *
   * @param userId the ID of the user
   * @param status the status of the orders to find
   * @return a list of orders matching the user and status
   */
  List<Order> findByUser_IdAndStatus(Long userId, OrderStatus status);

  /**
   * Checks if an order exists for a given user and travel plan ID.
   *
   * @param userId the ID of the user
   * @param travelPlanId the DynamoDB travel plan ID
   * @return true if an order exists, false otherwise
   */
  boolean existsByUser_IdAndTravelPlanId(Long userId, String travelPlanId);

  /**
   * Finds an order by its associated DynamoDB travel plan ID.
   *
   * @param travelPlanId the DynamoDB travel plan ID
   * @return an Optional containing the order if found, or empty otherwise
   */
  Optional<Order> findByTravelPlanId(String travelPlanId);
}

