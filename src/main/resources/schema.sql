-- VoyageAI Database Schema
-- This script creates all necessary tables for the application

-- ============================================
-- Users Table
-- Stores user account information for both local and OAuth authentication
-- ============================================
CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT 'User unique identifier',
    email VARCHAR(255) NOT NULL UNIQUE COMMENT 'User email address (unique)',
    password_hash VARCHAR(255) COMMENT 'BCrypt password hash (null for OAuth users)',
    display_name VARCHAR(255) COMMENT 'User display name',
    avatar_url VARCHAR(500) COMMENT 'User profile picture URL',
    auth_provider VARCHAR(50) NOT NULL DEFAULT 'LOCAL' COMMENT 'Authentication provider: LOCAL or GOOGLE',
    provider_user_id VARCHAR(255) COMMENT 'OAuth provider user ID (e.g., Google sub claim)',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Account creation timestamp',
    
    INDEX idx_users_email (email),
    INDEX idx_users_provider (auth_provider, provider_user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='User accounts table';

-- ============================================
-- Community Posts Table
-- Stores user-generated travel plan posts shared with the community
-- ============================================
CREATE TABLE IF NOT EXISTS community_posts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT 'Post unique identifier',
    user_id BIGINT NOT NULL COMMENT 'Author user ID',
    plan_id VARCHAR(255) COMMENT 'Reference to DynamoDB travel plan ID',
    title VARCHAR(255) NOT NULL COMMENT 'Post title',
    description TEXT COMMENT 'Post description/summary',
    cover_image_url VARCHAR(500) COMMENT 'Cover image URL',
    likes_count INT NOT NULL DEFAULT 0 COMMENT 'Total number of likes',
    comments_count INT NOT NULL DEFAULT 0 COMMENT 'Total number of comments',
    views_count INT NOT NULL DEFAULT 0 COMMENT 'Total number of views',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Post creation timestamp',
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Last update timestamp',
    
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_posts_user (user_id),
    INDEX idx_posts_created (created_at DESC),
    INDEX idx_posts_likes (likes_count DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Community posts table';

-- ============================================
-- Post Likes Table
-- Tracks which users liked which posts (many-to-many relationship)
-- ============================================
CREATE TABLE IF NOT EXISTS post_likes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT 'Like unique identifier',
    post_id BIGINT NOT NULL COMMENT 'Post ID that was liked',
    user_id BIGINT NOT NULL COMMENT 'User who liked the post',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Like timestamp',
    
    UNIQUE KEY uk_post_user (post_id, user_id) COMMENT 'One user can only like a post once',
    FOREIGN KEY (post_id) REFERENCES community_posts(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_likes_post (post_id),
    INDEX idx_likes_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Post likes table';

-- ============================================
-- Comments Table
-- Stores comments on community posts (supports nested replies)
-- ============================================
CREATE TABLE IF NOT EXISTS comments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT 'Comment unique identifier',
    post_id BIGINT NOT NULL COMMENT 'Post ID this comment belongs to',
    user_id BIGINT NOT NULL COMMENT 'Comment author user ID',
    parent_comment_id BIGINT COMMENT 'Parent comment ID for nested replies (null for top-level comments)',
    content TEXT NOT NULL COMMENT 'Comment text content',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Comment creation timestamp',
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Last update timestamp',
    
    FOREIGN KEY (post_id) REFERENCES community_posts(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (parent_comment_id) REFERENCES comments(id) ON DELETE CASCADE,
    INDEX idx_comments_post (post_id),
    INDEX idx_comments_user (user_id),
    INDEX idx_comments_parent (parent_comment_id),
    INDEX idx_comments_created (created_at ASC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Comments table (supports nested replies)';

-- ============================================
-- Orders Table (Optional - for future payment integration)
-- Stores order information when users purchase premium features
-- ============================================
CREATE TABLE IF NOT EXISTS orders (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT 'Order unique identifier',
    user_id BIGINT NOT NULL COMMENT 'Customer user ID',
    order_number VARCHAR(100) NOT NULL UNIQUE COMMENT 'Human-readable order number',
    plan_id VARCHAR(255) COMMENT 'Related travel plan ID',
    total_amount DECIMAL(10, 2) NOT NULL COMMENT 'Total order amount in USD',
    currency VARCHAR(10) NOT NULL DEFAULT 'USD' COMMENT 'Currency code',
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING' COMMENT 'Order status: PENDING, PAID, CANCELLED, REFUNDED',
    payment_method VARCHAR(50) COMMENT 'Payment method used',
    payment_id VARCHAR(255) COMMENT 'External payment provider transaction ID',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Order creation timestamp',
    paid_at TIMESTAMP COMMENT 'Payment completion timestamp',
    
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE RESTRICT,
    INDEX idx_orders_user (user_id),
    INDEX idx_orders_number (order_number),
    INDEX idx_orders_status (status),
    INDEX idx_orders_created (created_at DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Orders table (for future payment features)';

-- ============================================
-- Initial Data (Optional)
-- Create a test user for development
-- ============================================
-- Uncomment the following line to create a test user
-- Password: password123 (BCrypt hash: $2a$10$N9qo8uLOickgx2ZMRZoMye3RQ4.0C4.Y4/4/XxQVzPxb4l.e6m4q)
-- INSERT INTO users (email, password_hash, display_name, auth_provider, created_at) 
-- VALUES ('test@example.com', '$2a$10$N9qo8uLOickgx2ZMRZoMye3RQ4.0C4.Y4/4/XxQVzPxb4l.e6m4q', 'Test User', 'LOCAL', NOW())
-- ON DUPLICATE KEY UPDATE email=email;

