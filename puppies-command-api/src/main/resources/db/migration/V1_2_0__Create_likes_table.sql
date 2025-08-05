-- Migration: Create likes table
-- Description: Create the likes table for post likes with optimized design
-- Author: Puppies API Team
-- Date: 2024-01-15

CREATE TABLE likes (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    post_id BIGINT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- Foreign key constraints
    CONSTRAINT fk_likes_user_id FOREIGN KEY (user_id) 
        REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_likes_post_id FOREIGN KEY (post_id) 
        REFERENCES posts(id) ON DELETE CASCADE,
    
    -- Unique constraint to prevent duplicate likes
    CONSTRAINT uk_user_post_like UNIQUE (user_id, post_id)
);

-- Indexes for performance
CREATE INDEX idx_likes_post_id ON likes(post_id);
CREATE INDEX idx_likes_user_id ON likes(user_id);
CREATE INDEX idx_likes_created_at ON likes(created_at);

-- Optimized index for counting likes by post
CREATE INDEX idx_likes_post_count ON likes(post_id) 
    INCLUDE (created_at); -- Include created_at for covering index

-- Optimized index for user's liked posts timeline
CREATE INDEX idx_likes_user_timeline ON likes(user_id, created_at DESC) 
    INCLUDE (post_id); -- Include post_id for covering index

-- Comments for documentation
COMMENT ON TABLE likes IS 'Post likes - optimized for scalability with dedicated entity design';
COMMENT ON COLUMN likes.id IS 'Primary key - auto-incrementing like ID';
COMMENT ON COLUMN likes.user_id IS 'Foreign key to users table - who liked the post';
COMMENT ON COLUMN likes.post_id IS 'Foreign key to posts table - which post was liked';
COMMENT ON COLUMN likes.created_at IS 'Timestamp when like occurred';

-- Performance analysis comment
COMMENT ON CONSTRAINT uk_user_post_like ON likes IS 'Prevents duplicate likes and enables efficient existence checks';