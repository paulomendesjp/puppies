-- Migration: Create users table
-- Description: Create the users table for authentication and user management
-- Author: Puppies API Team
-- Date: 2024-01-15

CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL CHECK (length(name) >= 2),
    email VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(60) NOT NULL, -- BCrypt hash is always 60 characters
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- Constraints
    CONSTRAINT users_email_format CHECK (email ~* '^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$'),
    CONSTRAINT users_name_not_empty CHECK (trim(name) != '')
);

-- Indexes for performance
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_created_at ON users(created_at);

-- Comments for documentation
COMMENT ON TABLE users IS 'Registered users of the Puppies API';
COMMENT ON COLUMN users.id IS 'Primary key - auto-incrementing user ID';
COMMENT ON COLUMN users.name IS 'User display name (2-50 characters)';
COMMENT ON COLUMN users.email IS 'User email address - must be unique and valid format';
COMMENT ON COLUMN users.password IS 'BCrypt hashed password (60 characters)';
COMMENT ON COLUMN users.created_at IS 'Timestamp when user account was created';