-- Creates the first ADMIN so you can log in immediately

INSERT INTO users (email,
                   password_hash,
                   first_name,
                   last_name,
                   role_id,
                   created_at)
VALUES ('admin@ttclub.local',
        -- BCrypt of 'Password123!' (cost 10)
        '$2a$10$Dow1nUm4E9z1FMvx6bM6leKFFitvolG/GpNZgbf28pQ5e0siJmq9C',
        'System',
        'Admin',
        (SELECT id FROM roles WHERE name = 'ADMIN'),
        NOW())
ON CONFLICT (email) DO NOTHING;

