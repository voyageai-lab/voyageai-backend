package com.voyageai.voyageaibackend.domain.repo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.voyageai.voyageaibackend.domain.entity.Order;
import com.voyageai.voyageaibackend.domain.entity.Order.OrderStatus;
import com.voyageai.voyageaibackend.domain.entity.User;
import com.voyageai.voyageaibackend.domain.entity.User.AuthProvider;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

/**
 * Integration tests for {@link OrderRepository}.
 */
@DataJpaTest
class OrderRepositoryTest {

  @Autowired
  private TestEntityManager entityManager;

  @Autowired
  private OrderRepository orderRepository;

  private User testUser;

  @BeforeEach
  void setUp() {
    // Create and persist a test user
    testUser = new User();
    testUser.setEmail("test@example.com");
    testUser.setDisplayName("Test User");
    testUser.setPasswordHash("hashedpassword");
    testUser.setAuthProvider(AuthProvider.LOCAL);
    testUser.setCreatedAt(Instant.now());
    testUser = entityManager.persistAndFlush(testUser);
  }

  @Test
  void orderRepository_save_shouldPersistOrder() {
    // Given
    Order order = createTestOrder(testUser, "plan-123", new BigDecimal("99.99"));

    // When
    Order savedOrder = orderRepository.save(order);

    // Then
    assertNotNull(savedOrder.getId());
    assertEquals(testUser.getId(), savedOrder.getUser().getId());
    assertEquals("plan-123", savedOrder.getTravelPlanId());
  }

  @Test
  void findByUser_Id_shouldReturnUserOrders() {
    // Given
    Order order1 = createTestOrder(testUser, "plan-1", new BigDecimal("100.00"));
    Order order2 = createTestOrder(testUser, "plan-2", new BigDecimal("200.00"));
    orderRepository.save(order1);
    orderRepository.save(order2);

    // When
    List<Order> orders = orderRepository.findByUser_Id(testUser.getId());

    // Then
    assertEquals(2, orders.size());
  }

  @Test
  void findByIdAndUser_Id_shouldReturnOrder_whenExists() {
    // Given
    Order order = createTestOrder(testUser, "plan-123", new BigDecimal("99.99"));
    Order savedOrder = orderRepository.save(order);

    // When
    Optional<Order> foundOrder = orderRepository.findByIdAndUser_Id(
        savedOrder.getId(), testUser.getId());

    // Then
    assertTrue(foundOrder.isPresent());
    assertEquals(savedOrder.getId(), foundOrder.get().getId());
  }

  @Test
  void findByIdAndUser_Id_shouldReturnEmpty_whenNotExists() {
    // When
    Optional<Order> foundOrder = orderRepository.findByIdAndUser_Id(999L, testUser.getId());

    // Then
    assertFalse(foundOrder.isPresent());
  }

  @Test
  void findByStatus_shouldReturnOrdersWithStatus() {
    // Given
    Order pendingOrder = createTestOrder(testUser, "plan-1", new BigDecimal("100.00"));
    pendingOrder.setStatus(OrderStatus.PENDING);
    
    Order completedOrder = createTestOrder(testUser, "plan-2", new BigDecimal("200.00"));
    completedOrder.setStatus(OrderStatus.COMPLETED);

    orderRepository.save(pendingOrder);
    orderRepository.save(completedOrder);

    // When
    List<Order> pendingOrders = orderRepository.findByStatus(OrderStatus.PENDING);
    List<Order> completedOrders = orderRepository.findByStatus(OrderStatus.COMPLETED);

    // Then
    assertEquals(1, pendingOrders.size());
    assertEquals(OrderStatus.PENDING, pendingOrders.get(0).getStatus());
    
    assertEquals(1, completedOrders.size());
    assertEquals(OrderStatus.COMPLETED, completedOrders.get(0).getStatus());
  }

  @Test
  void findByUser_IdAndStatus_shouldReturnFilteredOrders() {
    // Given
    Order pendingOrder = createTestOrder(testUser, "plan-1", new BigDecimal("100.00"));
    pendingOrder.setStatus(OrderStatus.PENDING);
    
    Order completedOrder = createTestOrder(testUser, "plan-2", new BigDecimal("200.00"));
    completedOrder.setStatus(OrderStatus.COMPLETED);

    orderRepository.save(pendingOrder);
    orderRepository.save(completedOrder);

    // When
    List<Order> userPendingOrders = orderRepository.findByUser_IdAndStatus(
        testUser.getId(), OrderStatus.PENDING);

    // Then
    assertEquals(1, userPendingOrders.size());
    assertEquals(testUser.getId(), userPendingOrders.get(0).getUser().getId());
    assertEquals(OrderStatus.PENDING, userPendingOrders.get(0).getStatus());
  }

  @Test
  void existsByUser_IdAndTravelPlanId_shouldReturnTrue_whenExists() {
    // Given
    Order order = createTestOrder(testUser, "plan-123", new BigDecimal("99.99"));
    orderRepository.save(order);

    // When
    boolean exists = orderRepository.existsByUser_IdAndTravelPlanId(
        testUser.getId(), "plan-123");

    // Then
    assertTrue(exists);
  }

  @Test
  void existsByUser_IdAndTravelPlanId_shouldReturnFalse_whenNotExists() {
    // When
    boolean exists = orderRepository.existsByUser_IdAndTravelPlanId(
        testUser.getId(), "nonexistent-plan");

    // Then
    assertFalse(exists);
  }

  @Test
  void findByTravelPlanId_shouldReturnOrder_whenExists() {
    // Given
    Order order = createTestOrder(testUser, "plan-456", new BigDecimal("150.00"));
    orderRepository.save(order);

    // When
    Optional<Order> foundOrder = orderRepository.findByTravelPlanId("plan-456");

    // Then
    assertTrue(foundOrder.isPresent());
    assertEquals("plan-456", foundOrder.get().getTravelPlanId());
  }

  @Test
  void findByTravelPlanId_shouldReturnEmpty_whenNotExists() {
    // When
    Optional<Order> foundOrder = orderRepository.findByTravelPlanId("nonexistent-plan");

    // Then
    assertFalse(foundOrder.isPresent());
  }

  private Order createTestOrder(User user, String travelPlanId, BigDecimal amount) {
    Order order = new Order();
    order.setUser(user);
    order.setTravelPlanId(travelPlanId);
    order.setAmount(amount);
    order.setStatus(OrderStatus.PENDING);
    order.setPaymentProvider("STRIPE");
    order.setCreatedAt(Instant.now());
    order.setUpdatedAt(Instant.now());
    return order;
  }
}

