package com.voyageai.voyageaibackend.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

/**
 * Order entity representing a purchase order in the system.
 * 
 * <p>Each order belongs to a user and contains transaction details
 * such as amount, status, and payment information.
 */
@Entity
@Table(name = "orders", indexes = {
    @Index(name = "idx_orders_user_id", columnList = "user_id"),
    @Index(name = "idx_orders_status", columnList = "status"),
    @Index(name = "idx_orders_created_at", columnList = "created_at")
})
@Getter
@Setter
public class Order {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  /**
   * The user who placed this order.
   * Many orders can belong to one user.
   */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  /**
   * Travel plan ID from DynamoDB.
   * Stores the reference to the travel plan this order is for.
   */
  @Column(name = "travel_plan_id")
  private String travelPlanId;

  /**
   * Order amount in USD.
   * Uses BigDecimal for precise decimal arithmetic.
   */
  @Column(nullable = false, precision = 10, scale = 2)
  private BigDecimal amount;

  /**
   * Current status of the order.
   */
  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private OrderStatus status = OrderStatus.PENDING;

  /**
   * Payment provider (e.g., STRIPE, PAYPAL).
   */
  @Column(name = "payment_provider")
  private String paymentProvider;

  /**
   * External payment transaction ID from the payment provider.
   */
  @Column(name = "payment_transaction_id")
  private String paymentTransactionId;

  /**
   * Timestamp when the order was created.
   */
  @Column(nullable = false, name = "created_at")
  private Instant createdAt = Instant.now();

  /**
   * Timestamp when the order was last updated.
   */
  @Column(nullable = false, name = "updated_at")
  private Instant updatedAt = Instant.now();

  /**
   * Enum representing possible order statuses.
   */
  public enum OrderStatus {
    /**
     * Order has been created but payment not yet processed.
     */
    PENDING,

    /**
     * Payment has been successfully processed.
     */
    COMPLETED,

    /**
     * Payment failed or was declined.
     */
    FAILED,

    /**
     * Order was cancelled by user or system.
     */
    CANCELLED,

    /**
     * Order was refunded.
     */
    REFUNDED
  }
}

