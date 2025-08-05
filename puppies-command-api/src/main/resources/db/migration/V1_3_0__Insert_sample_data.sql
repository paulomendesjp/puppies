-- Migration: Insert sample data for development
-- Description: Add sample users and posts for testing and development
-- Author: Puppies API Team
-- Date: 2024-01-15
-- Note: This migration only runs in development/test environments

-- Sample users (passwords are BCrypt hashed "password123")
INSERT INTO users (name, email, password, created_at) VALUES 
(
    'Alice Johnson', 
    'alice@example.com', 
    '$2a$10$rXOGGUbE8kJp6Z6f7dQA8eFn7XKcY1K9FQnVt4zO8wQ.3mW1FxY3G',
    CURRENT_TIMESTAMP - INTERVAL '30 days'
),
(
    'Bob Smith', 
    'bob@example.com', 
    '$2a$10$rXOGGUbE8kJp6Z6f7dQA8eFn7XKcY1K9FQnVt4zO8wQ.3mW1FxY3G',
    CURRENT_TIMESTAMP - INTERVAL '25 days'
),
(
    'Charlie Brown', 
    'charlie@example.com', 
    '$2a$10$rXOGGUbE8kJp6Z6f7dQA8eFn7XKcY1K9FQnVt4zO8wQ.3mW1FxY3G',
    CURRENT_TIMESTAMP - INTERVAL '20 days'
),
(
    'Diana Prince', 
    'diana@example.com', 
    '$2a$10$rXOGGUbE8kJp6Z6f7dQA8eFn7XKcY1K9FQnVt4zO8wQ.3mW1FxY3G',
    CURRENT_TIMESTAMP - INTERVAL '15 days'
),
(
    'Eve Wilson', 
    'eve@example.com', 
    '$2a$10$rXOGGUbE8kJp6Z6f7dQA8eFn7XKcY1K9FQnVt4zO8wQ.3mW1FxY3G',
    CURRENT_TIMESTAMP - INTERVAL '10 days'
);

-- Sample posts
INSERT INTO posts (author_id, image_url, text_content, created_at) VALUES 
-- Alice's posts
(1, 'http://localhost:8080/api/files/sample-golden-retriever.jpg', 'My beautiful Golden Retriever enjoying the sunset! 🌅🐕', CURRENT_TIMESTAMP - INTERVAL '5 days'),
(1, 'http://localhost:8080/api/files/sample-puppy-training.jpg', 'Training session with Max. He''s getting so smart! 🧠🎾', CURRENT_TIMESTAMP - INTERVAL '3 days'),

-- Bob's posts
(2, 'http://localhost:8080/api/files/sample-beagle.jpg', 'Luna the Beagle discovering the garden 🌿', CURRENT_TIMESTAMP - INTERVAL '4 days'),
(2, 'http://localhost:8080/api/files/sample-dog-park.jpg', 'Best friends at the dog park! 🐕‍🦺🐕', CURRENT_TIMESTAMP - INTERVAL '2 days'),

-- Charlie's posts
(3, 'http://localhost:8080/api/files/sample-french-bulldog.jpg', 'French Bulldog life: eat, sleep, repeat 😴', CURRENT_TIMESTAMP - INTERVAL '6 days'),
(3, 'http://localhost:8080/api/files/sample-bath-time.jpg', 'Bath time drama! 🛁💦', CURRENT_TIMESTAMP - INTERVAL '1 day'),

-- Diana's posts
(4, 'http://localhost:8080/api/files/sample-husky.jpg', 'Siberian Husky energy is unmatched! ❄️🏃‍♀️', CURRENT_TIMESTAMP - INTERVAL '7 days'),
(4, 'http://localhost:8080/api/files/sample-agility.jpg', 'Agility training champion! 🏆', CURRENT_TIMESTAMP - INTERVAL '4 hours'),

-- Eve's posts
(5, 'http://localhost:8080/api/files/sample-border-collie.jpg', 'Border Collie intelligence on display 🎯', CURRENT_TIMESTAMP - INTERVAL '8 days'),
(5, 'http://localhost:8080/api/files/sample-fetch.jpg', 'Fetch never gets old! 🎾', CURRENT_TIMESTAMP - INTERVAL '2 hours');

-- Sample likes to create engagement
INSERT INTO likes (user_id, post_id, created_at) VALUES 
-- Alice's posts liked by others
(2, 1, CURRENT_TIMESTAMP - INTERVAL '5 days' + INTERVAL '2 hours'),
(3, 1, CURRENT_TIMESTAMP - INTERVAL '5 days' + INTERVAL '4 hours'),
(4, 1, CURRENT_TIMESTAMP - INTERVAL '5 days' + INTERVAL '6 hours'),
(5, 1, CURRENT_TIMESTAMP - INTERVAL '5 days' + INTERVAL '8 hours'),

(3, 2, CURRENT_TIMESTAMP - INTERVAL '3 days' + INTERVAL '1 hour'),
(4, 2, CURRENT_TIMESTAMP - INTERVAL '3 days' + INTERVAL '3 hours'),

-- Bob's posts liked by others
(1, 3, CURRENT_TIMESTAMP - INTERVAL '4 days' + INTERVAL '1 hour'),
(3, 3, CURRENT_TIMESTAMP - INTERVAL '4 days' + INTERVAL '2 hours'),
(5, 3, CURRENT_TIMESTAMP - INTERVAL '4 days' + INTERVAL '5 hours'),

(1, 4, CURRENT_TIMESTAMP - INTERVAL '2 days' + INTERVAL '30 minutes'),
(4, 4, CURRENT_TIMESTAMP - INTERVAL '2 days' + INTERVAL '2 hours'),
(5, 4, CURRENT_TIMESTAMP - INTERVAL '2 days' + INTERVAL '4 hours'),

-- Charlie's posts liked by others
(1, 5, CURRENT_TIMESTAMP - INTERVAL '6 days' + INTERVAL '3 hours'),
(2, 5, CURRENT_TIMESTAMP - INTERVAL '6 days' + INTERVAL '5 hours'),

(2, 6, CURRENT_TIMESTAMP - INTERVAL '1 day' + INTERVAL '1 hour'),
(4, 6, CURRENT_TIMESTAMP - INTERVAL '1 day' + INTERVAL '2 hours'),
(5, 6, CURRENT_TIMESTAMP - INTERVAL '1 day' + INTERVAL '3 hours'),

-- Diana's posts liked by others
(1, 7, CURRENT_TIMESTAMP - INTERVAL '7 days' + INTERVAL '1 hour'),
(2, 7, CURRENT_TIMESTAMP - INTERVAL '7 days' + INTERVAL '3 hours'),
(3, 7, CURRENT_TIMESTAMP - INTERVAL '7 days' + INTERVAL '4 hours'),

(1, 8, CURRENT_TIMESTAMP - INTERVAL '4 hours' + INTERVAL '15 minutes'),
(2, 8, CURRENT_TIMESTAMP - INTERVAL '4 hours' + INTERVAL '30 minutes'),

-- Eve's posts liked by others
(2, 9, CURRENT_TIMESTAMP - INTERVAL '8 days' + INTERVAL '2 hours'),
(4, 9, CURRENT_TIMESTAMP - INTERVAL '8 days' + INTERVAL '6 hours'),

(1, 10, CURRENT_TIMESTAMP - INTERVAL '2 hours' + INTERVAL '10 minutes'),
(3, 10, CURRENT_TIMESTAMP - INTERVAL '2 hours' + INTERVAL '20 minutes'),
(4, 10, CURRENT_TIMESTAMP - INTERVAL '2 hours' + INTERVAL '30 minutes');

-- Update sequences to prevent conflicts
SELECT setval('users_id_seq', (SELECT MAX(id) FROM users));
SELECT setval('posts_id_seq', (SELECT MAX(id) FROM posts));
SELECT setval('likes_id_seq', (SELECT MAX(id) FROM likes));