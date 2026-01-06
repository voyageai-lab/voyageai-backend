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
-- Orders Table
-- Stores order information when users purchase premium features
-- ============================================
CREATE TABLE IF NOT EXISTS orders (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT 'Order unique identifier',
    user_id BIGINT NOT NULL COMMENT 'Customer user ID',
    travel_plan_id VARCHAR(255) COMMENT 'Related DynamoDB travel plan ID',
    amount DECIMAL(10, 2) NOT NULL COMMENT 'Order amount in USD',
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING' COMMENT 'Order status: PENDING, COMPLETED, FAILED, CANCELLED, REFUNDED',
    payment_provider VARCHAR(50) COMMENT 'Payment provider (e.g., STRIPE, PAYPAL)',
    payment_transaction_id VARCHAR(255) COMMENT 'External payment provider transaction ID',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Order creation timestamp',
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Last update timestamp',
    
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE RESTRICT,
    INDEX idx_orders_user_id (user_id),
    INDEX idx_orders_status (status),
    INDEX idx_orders_created_at (created_at DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Orders table';

-- ============================================
-- Travel Projects Table
-- Stores travel planning projects/conversations for multi-turn dialogue
-- ============================================
CREATE TABLE IF NOT EXISTS travel_projects (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT 'Project unique identifier',
    project_id VARCHAR(100) NOT NULL UNIQUE COMMENT 'Project UUID (e.g., proj-123e4567-...)',
    user_id BIGINT NOT NULL COMMENT 'Project owner user ID',
    title VARCHAR(255) NOT NULL COMMENT 'Project title (e.g., "Tokyo Trip 2024")',
    description TEXT COMMENT 'Project description or notes',
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT 'Project status: ACTIVE, ARCHIVED, DELETED',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Project creation timestamp',
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Last update timestamp',
    
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_projects_user_id (user_id),
    INDEX idx_projects_status (status),
    INDEX idx_projects_updated_at (updated_at DESC),
    INDEX idx_projects_user_status (user_id, status, updated_at DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Travel planning projects table';

-- ============================================
-- Conversation Messages Table
-- Stores conversation history for travel planning projects (dual-write with Redis)
-- ============================================
CREATE TABLE IF NOT EXISTS conversation_messages (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT 'Message unique identifier',
    message_id VARCHAR(100) NOT NULL UNIQUE COMMENT 'Message UUID (e.g., msg-123e4567-...)',
    project_id VARCHAR(100) NOT NULL COMMENT 'Associated project ID',
    role VARCHAR(20) NOT NULL COMMENT 'Message role: USER, ASSISTANT, SYSTEM',
    message_type VARCHAR(30) NOT NULL COMMENT 'Message type: TEXT, ITINERARY, TOOL_RESULT, PROGRESS_UPDATE',
    content TEXT NOT NULL COMMENT 'Message text content',
    structured_data JSON COMMENT 'JSON data for ITINERARY or TOOL_RESULT types',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Message creation timestamp',
    
    INDEX idx_messages_project_created (project_id, created_at ASC),
    INDEX idx_messages_project_id (project_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Conversation messages table';

-- ============================================
-- Initial Data (Optional)
-- Create a test user for development
-- ============================================
-- Uncomment the following line to create a test user
-- Password: password123 (BCrypt hash: $2a$10$N9qo8uLOickgx2ZMRZoMye3RQ4.0C4.Y4/4/XxQVzPxb4l.e6m4q)
-- INSERT INTO users (email, password_hash, display_name, auth_provider, created_at) 
-- VALUES ('test@example.com', '$2a$10$N9qo8uLOickgx2ZMRZoMye3RQ4.0C4.Y4/4/XxQVzPxb4l.e6m4q', 'Test User', 'LOCAL', NOW())
-- ON DUPLICATE KEY UPDATE email=email;

