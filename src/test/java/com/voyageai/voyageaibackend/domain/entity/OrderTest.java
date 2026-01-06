package com.voyageai.voyageaibackend.domain.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.voyageai.voyageaibackend.domain.entity.Order.OrderStatus;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link Order} entity.
 */
class OrderTest {

  @Test
  void order_settersAndGetters_shouldWork() {
    // Given
    Order order = new Order();
    User user = new User();
    user.setId(1L);

    // When
    order.setId(100L);
    order.setUser(user);
    order.setTravelPlanId("plan-123");
    order.setAmount(new BigDecimal("99.99"));
    order.setStatus(OrderStatus.PENDING);
    order.setPaymentProvider("STRIPE");
    order.setPaymentTransactionId("txn-abc123");
    
    Instant now = Instant.now();
    order.setCreatedAt(now);
    order.setUpdatedAt(now);

    // Then
    assertEquals(100L, order.getId());
    assertEquals(user, order.getUser());
    assertEquals(1L, order.getUser().getId());
    assertEquals("plan-123", order.getTravelPlanId());
    assertEquals(new BigDecimal("99.99"), order.getAmount());
    assertEquals(OrderStatus.PENDING, order.getStatus());
    assertEquals("STRIPE", order.getPaymentProvider());
    assertEquals("txn-abc123", order.getPaymentTransactionId());
    assertEquals(now, order.getCreatedAt());
    assertEquals(now, order.getUpdatedAt());
  }

  @Test
  void order_defaultStatus_shouldBePending() {
    // When
    Order order = new Order();

    // Then
    assertEquals(OrderStatus.PENDING, order.getStatus());
  }

  @Test
  void order_defaultTimestamps_shouldBeSet() {
    // When
    Order order = new Order();

    // Then
    assertNotNull(order.getCreatedAt());
    assertNotNull(order.getUpdatedAt());
  }

  @Test
  void orderStatus_allStatuses_shouldExist() {
    // Then
    assertEquals(5, OrderStatus.values().length);
    assertNotNull(OrderStatus.PENDING);
    assertNotNull(OrderStatus.COMPLETED);
    assertNotNull(OrderStatus.FAILED);
    assertNotNull(OrderStatus.CANCELLED);
    assertNotNull(OrderStatus.REFUNDED);
  }

  @Test
  void orderStatus_toString_shouldReturnStatusName() {
    // Then
    assertEquals("PENDING", OrderStatus.PENDING.toString());
    assertEquals("COMPLETED", OrderStatus.COMPLETED.toString());
    assertEquals("FAILED", OrderStatus.FAILED.toString());
    assertEquals("CANCELLED", OrderStatus.CANCELLED.toString());
    assertEquals("REFUNDED", OrderStatus.REFUNDED.toString());
  }

  @Test
  void order_bigDecimalAmount_shouldHandlePrecision() {
    // Given
    Order order = new Order();
    BigDecimal amount = new BigDecimal("1234.56");

    // When
    order.setAmount(amount);

    // Then
    assertEquals(0, amount.compareTo(order.getAmount()));
    assertEquals("1234.56", order.getAmount().toString());
  }

  @Test
  void order_withUser_shouldMaintainRelationship() {
    // Given
    User user = new User();
    user.setId(1L);
    user.setEmail("test@example.com");
    user.setDisplayName("Test User");

    Order order = new Order();

    // When
    order.setUser(user);

    // Then
    assertNotNull(order.getUser());
    assertEquals(1L, order.getUser().getId());
    assertEquals("test@example.com", order.getUser().getEmail());
  }

  @Test
  void order_statusTransition_shouldBeAllowed() {
    // Given
    Order order = new Order();
    assertEquals(OrderStatus.PENDING, order.getStatus());

    // When - simulate status transitions
    order.setStatus(OrderStatus.COMPLETED);
    assertEquals(OrderStatus.COMPLETED, order.getStatus());

    order.setStatus(OrderStatus.REFUNDED);
    assertEquals(OrderStatus.REFUNDED, order.getStatus());
  }
}

