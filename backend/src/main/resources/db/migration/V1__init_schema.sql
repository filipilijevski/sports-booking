/*  Roles and Users */
CREATE TABLE roles (
  id   SERIAL PRIMARY KEY,
  name VARCHAR(20) UNIQUE NOT NULL  -- CLIENT, COACH, OWNER, ADMIN
);

CREATE TABLE users (
  id            SERIAL PRIMARY KEY,
  email         VARCHAR(255) UNIQUE NOT NULL,
  password_hash VARCHAR(255)        NOT NULL,
  role_id       INT REFERENCES roles(id),
  first_name    VARCHAR(100),
  last_name     VARCHAR(100),
  phone         VARCHAR(20),
  address       TEXT,
  bio           TEXT,
  created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);

/*  Product Catalog */
CREATE TABLE categories (
  id          SERIAL PRIMARY KEY,
  name        VARCHAR(100) UNIQUE NOT NULL,
  description TEXT
);

CREATE TABLE products (
  id          SERIAL PRIMARY KEY,
  category_id INT REFERENCES categories(id),
  name        VARCHAR(255) NOT NULL,
  description TEXT         NOT NULL,
  price       NUMERIC(10,2) NOT NULL CHECK (price >= 0),
  is_active   BOOLEAN DEFAULT TRUE,
  created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_products_category ON products(category_id);

CREATE TABLE inventory (
  product_id INT PRIMARY KEY REFERENCES products(id) ON DELETE CASCADE,
  quantity   INT NOT NULL DEFAULT 0 CHECK (quantity >= 0)
);

CREATE TABLE product_images (
  id         SERIAL PRIMARY KEY,
  product_id INT REFERENCES products(id) ON DELETE CASCADE,
  url        TEXT NOT NULL,
  alt_text   TEXT,
  is_primary BOOLEAN NOT NULL DEFAULT FALSE
);

/*  Orders and Payments */
CREATE TABLE orders (
  id           SERIAL PRIMARY KEY,
  user_id      INT NOT NULL REFERENCES users(id),
  total_amount NUMERIC(10,2) NOT NULL CHECK (total_amount >= 0),
  status       VARCHAR(20) NOT NULL
               CHECK (status IN ('PENDING','PAID','CANCELLED')),
  created_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE order_items (
  id          SERIAL PRIMARY KEY,
  order_id    INT NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
  product_id  INT NOT NULL REFERENCES products(id),
  quantity    INT NOT NULL CHECK (quantity > 0),
  unit_price  NUMERIC(10,2) NOT NULL CHECK (unit_price >= 0),
  total_price NUMERIC(10,2) NOT NULL CHECK (total_price >= 0)
);

CREATE TABLE payments (
  id               SERIAL PRIMARY KEY,
  order_id         INT UNIQUE NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
  provider         VARCHAR(50) NOT NULL,        -- 'stripe'
  provider_txn_id  VARCHAR(255) NOT NULL,
  amount           NUMERIC(10,2) NOT NULL CHECK (amount >= 0),
  currency         VARCHAR(10)  NOT NULL DEFAULT 'CAD',
  status           VARCHAR(20)  NOT NULL,       -- SUCCEEDED, FAILED, REFUNDED
  created_at       TIMESTAMPTZ  NOT NULL DEFAULT now()
);

/* Lesson Booking (Note this implementation of lesson booking is no longer functional and is now moved to the booking package) */
CREATE TABLE group_lessons (
  id          SERIAL PRIMARY KEY,
  name        VARCHAR(100) NOT NULL,
  description TEXT,
  capacity    INT NOT NULL CHECK (capacity > 1)
);

-- recurring schedule templates
CREATE TABLE schedule_templates (
  id          SERIAL PRIMARY KEY,
  coach_id    INT NOT NULL REFERENCES users(id),
  weekday     INT NOT NULL CHECK (weekday BETWEEN 0 AND 6), -- 0=Mon
  start_time  TIME NOT NULL,
  end_time    TIME NOT NULL,
  capacity    INT  NOT NULL CHECK (capacity > 0),
  price       NUMERIC(10,2) NOT NULL CHECK (price >= 0),
  is_active   BOOLEAN DEFAULT TRUE
);

-- concrete bookable slots (generated from templates or ad‑hoc)
CREATE TABLE time_slots (
  id              SERIAL PRIMARY KEY,
  coach_id        INT NOT NULL REFERENCES users(id),
  group_lesson_id INT REFERENCES group_lessons(id),
  start_ts        TIMESTAMPTZ NOT NULL,
  end_ts          TIMESTAMPTZ NOT NULL,
  capacity        INT NOT NULL CHECK (capacity > 0),
  price           NUMERIC(10,2) NOT NULL CHECK (price >= 0),
  version         INT NOT NULL DEFAULT 0,
  CONSTRAINT uq_slot UNIQUE (coach_id, start_ts)  -- prevent overlaps
);
CREATE INDEX idx_slots_coach_start ON time_slots(coach_id, start_ts);

CREATE TABLE lessons (
  id         SERIAL PRIMARY KEY,
  slot_id    INT NOT NULL REFERENCES time_slots(id) ON DELETE CASCADE,
  player_id  INT NOT NULL REFERENCES users(id),
  status     VARCHAR(20) NOT NULL
             CHECK (status IN ('BOOKED','CANCELLED','COMPLETED','NO_SHOW')),
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  UNIQUE(slot_id, player_id)
);

/* Tournaments and Training Programs (also moved to booking package and later flyway migrations) */
CREATE TABLE tournament_categories (
  id   SERIAL PRIMARY KEY,
  name VARCHAR(100) UNIQUE NOT NULL
);

CREATE TABLE tournaments (
  id            SERIAL PRIMARY KEY,
  category_id   INT NOT NULL REFERENCES tournament_categories(id),
  title         VARCHAR(255) NOT NULL,
  description   TEXT,
  start_date    DATE NOT NULL,
  end_date      DATE NOT NULL,
  entry_fee     NUMERIC(10,2) DEFAULT 0,
  max_players   INT,
  created_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE tournament_registrations (
  id           SERIAL PRIMARY KEY,
  tournament_id INT REFERENCES tournaments(id) ON DELETE CASCADE,
  player_id     INT REFERENCES users(id),
  registered_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  UNIQUE(tournament_id, player_id)
);

CREATE TABLE training_programs (
  id          SERIAL PRIMARY KEY,
  name        VARCHAR(255) NOT NULL,
  description TEXT,
  start_date  DATE NOT NULL,
  end_date    DATE NOT NULL,
  weekday_mask INT NOT NULL  -- bitmask for active days
);

CREATE TABLE program_enrollments (
  id           SERIAL PRIMARY KEY,
  program_id   INT REFERENCES training_programs(id) ON DELETE CASCADE,
  player_id    INT REFERENCES users(id),
  enrolled_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
  UNIQUE(program_id, player_id)
);

/*  Forum */
CREATE TABLE forum_threads (
  id         SERIAL PRIMARY KEY,
  user_id    INT NOT NULL REFERENCES users(id),
  title      VARCHAR(255) NOT NULL,
  category   VARCHAR(50)  NOT NULL,
  created_at TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE forum_posts (
  id         SERIAL PRIMARY KEY,
  thread_id  INT NOT NULL REFERENCES forum_threads(id) ON DELETE CASCADE,
  user_id    INT NOT NULL REFERENCES users(id),
  content    TEXT NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

/*  Auth Tokens */
CREATE TABLE refresh_tokens (
  id          SERIAL PRIMARY KEY,
  user_id     INT REFERENCES users(id) ON DELETE CASCADE,
  token_hash  TEXT     NOT NULL,
  expires_at  TIMESTAMPTZ NOT NULL,
  created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

/* Club Settings (Not really used for anything yet) */
CREATE TABLE club_settings (
  id             SMALLINT PRIMARY KEY DEFAULT 1,
  name           VARCHAR(255),
  description    TEXT,
  contact_email  VARCHAR(255),
  address        TEXT,
  phone          VARCHAR(20),
  opening_hours  JSONB   -- e.g. { "mon":"9–21", "tue":"9–21" }
);

