/* Seed initial roles – executes only if a row with that name is missing */
INSERT INTO roles (name) VALUES
  ('CLIENT'),
  ('COACH'),
  ('OWNER'),
  ('ADMIN')
ON CONFLICT (name) DO NOTHING;

