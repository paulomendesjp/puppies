-- Migration: Create posts table
-- Description: Create the posts table for user posts with images
-- Author: Puppies API Team
-- Date: 2024-01-15

CREATE TABLE posts (
    id BIGSERIAL PRIMARY KEY,
    author_id BIGINT NOT NULL,
    image_url VARCHAR(500) NOT NULL,
    text_content TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- Foreign key constraints
    CONSTRAINT fk_posts_author_id FOREIGN KEY (author_id) 
        REFERENCES users(id) ON DELETE CASCADE,
    
    -- Check constraints
    CONSTRAINT posts_image_url_not_empty CHECK (trim(image_url) != ''),
    CONSTRAINT posts_text_content_length CHECK (length(text_content) <= 2000)
);

-- Indexes for performance
CREATE INDEX idx_posts_author_id ON posts(author_id);
CREATE INDEX idx_posts_created_at ON posts(created_at DESC);
CREATE INDEX idx_posts_author_created ON posts(author_id, created_at DESC);

-- Comments for documentation
COMMENT ON TABLE posts IS 'User posts containing images and optional text content';
COMMENT ON COLUMN posts.id IS 'Primary key - auto-incrementing post ID';
COMMENT ON COLUMN posts.author_id IS 'Foreign key to users table - who created this post';
COMMENT ON COLUMN posts.image_url IS 'URL to the uploaded image file (max 500 chars)';
COMMENT ON COLUMN posts.text_content IS 'Optional text caption for the post (max 2000 chars)';
COMMENT ON COLUMN posts.created_at IS 'Timestamp when post was created';