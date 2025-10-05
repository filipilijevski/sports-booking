# Sports Club Booking and Pro Shop Platform

A full stack platform for local sports clubs (table tennis, tennis, badminton, basketball, gyms, pickleball, and more). It provides authentication, program and membership sales, bookings, a pro shop with online payments and shipping quotes, a blog, and admin tooling.

> This repository contains a React + Vite front end and a Spring Boot + PostgreSQL back end. Payments are powered by Stripe. Shipping quotes are supported via Canada Post. Email uses Gmail OAuth2.

---

## Table of Contents

* [Features](#features)
* [Tech Stack](#tech-stack)
* [Architecture and Folder Structure](#architecture-and-folder-structure)
* [Screenshots](#screenshots)
* [Prerequisites](#prerequisites)
* [Configuration and Environment](#configuration-and-environment)

  * [Backend env vars](#backend-env-vars)
  * [Frontend env vars](#frontend-env-vars)
  * [Example files to copy](#example-files-to-copy)
* [Local Development: Step by Step](#local-development-step-by-step)

  * [1. Clone and install](#1-clone-and-install)
  * [2. Create a local database](#2-create-a-local-database)
  * [3. Run database migrations with Flyway](#3-run-database-migrations-with-flyway)
  * [4. Start the backend](#4-start-the-backend)
  * [5. Start the frontend](#5-start-the-frontend)
  * [6. Stripe webhook for local development](#6-stripe-webhook-for-local-development)
  * [7. Google OAuth2 client setup](#7-google-oauth2-client-setup)
  * [8. Canada Post setup optional](#8-canada-post-setup-optional)
  * [9. Email setup optional](#9-email-setup-optional)
* [How to Use](#how-to-use)
* [Roadmap and Next Steps](#roadmap-and-next-steps)
* [Security, Secrets, and Safety](#security-secrets-and-safety)
* [Troubleshooting](#troubleshooting)
* [License](#license)

---

## Features

### Authentication and Accounts

* Create, update, and delete users
* Password resets via admin and self-serve
* JWT based API authentication
* Optional two factor authentication with TOTP
* OAuth2 login and registration with Google

### Blog

* Create, edit, delete blog posts
* Optional HTML markdown with multiple images
* Reorder posts and toggle visibility
* Public view for clients

### Pro Shop

* Product catalog with SKU, brand, name, price, stock, weight, description, category, image
* Category management visible to clients
* Checkout with Stripe and Canada Post shipping rates
* Support for pickup in store without shipping
* Refunds full or partial that adjust inventory and refund via Stripe
* Inventory management with full audit trail
* Order notification emails
* Record in person payments such as e transfer, cash, or terminal
* Order fulfillment workflow with filters and status updates

### Booking and Programs

**Admins can:**

* Create memberships, including enforcement of an initial membership before granting access to club services
* Create training programs and sell packages that grant a number of sessions
* Populate a calendar that members can view
* Create rental credit packages for table or court rentals and track consumption
* Filter and sort across memberships, enrollments, remaining sessions, and rental credits
* Manually enroll members and record purchases made in person

**Clients can:**

* Purchase memberships and enroll in programs online via Stripe
* View enrollments, sessions attended, and sessions remaining
* View and purchase rental credit balances online or through staff

---

## Tech Stack

* **Frontend:** React 19, Vite 6, TypeScript 5, MUI 7, React Router 6, Stripe Elements, DOMPurify, Marked, Yup
* **Backend:** Java 17, Spring Boot 3.4.x, Spring Security, Spring Data JPA, Validation, Cache, WebSocket, Actuator, Spring Mail, springdoc OpenAPI
* **Database:** PostgreSQL 14 or newer recommended
* **Migrations:** Flyway
* **Auth:** JWT, optional 2FA TOTP, OAuth2 Google
* **Payments:** Stripe Payment Intents with hosted checkout compatibility
* **Shipping:** Canada Post rate quotes
* **Other libs:** MapStruct 1.6.x, JJWT 0.11.x, ZXing 3.5.x, stripe-java 28.x

---

## Architecture and Folder Structure

Monorepo with `backend/` and `frontend/`:

```
.
├── backend
│   ├── pom.xml
│   ├── mvnw, mvnw.cmd, .mvn/
│   ├── src
│   │   ├── main
│   │   │   ├── java/com/ttclub/backend
│   │   │   │   ├── booking/           # booking domain (programs, memberships, rentals)
│   │   │   │   ├── config/            # Spring configuration (security, CORS, mail, etc.)
│   │   │   │   ├── controller/        # REST controllers
│   │   │   │   ├── dto/               # request/response DTOs
│   │   │   │   ├── job/               # scheduled tasks and materializers
│   │   │   │   ├── mapper/            # MapStruct mappers
│   │   │   │   ├── model/             # JPA entities
│   │   │   │   ├── repository/        # Spring Data JPA repositories
│   │   │   │   ├── security/          # JWT, OAuth2, MFA helpers
│   │   │   │   └── service/           # business logic
│   │   │   └── resources
│   │   │       ├── application.yml    # env driven, safe to commit with placeholders
│   │   │       └── db/migration/      # Flyway migrations V1__...sql, V2__...sql, etc.
│   │   └── test                       # unit and integration tests
│   ├── uploads/                       # runtime uploads (images); do not commit
│   └── target/                        # build output; ignored
│
├── frontend
│   ├── package.json
│   ├── vite.config.ts
│   ├── .env.local                     # local only; ignored
│   ├── public/                        # static assets for Vite dev and build
│   └── src/
│       ├── components/                # shared UI
│       ├── pages/                     # route pages (shop, booking, blog, admin, etc.)
│       ├── context/                   # auth, role, dialogs
│       ├── lib/                       # API helpers (shop, booking, auth)
│       └── routes/                    # app routing
│
├── docs/                              # optional screenshots for the README
├── .gitignore                         # root ignore rules
└── README.md
```

Notes:

* The backend Flyway migrations create and evolve the schema for users, auth, shop, bookings, memberships, programs, credits, orders, refunds, and audits.
* The `uploads/` directory is used for runtime images or attachments and should be ignored by Git.
* The front end is a Vite SPA that communicates with the API via HTTP and Stripe Elements for payments.

---

## Screenshots

You may view screenshots of the UI and some of the functionality within `relevantimages/`
Please note that certain images and logos have been covered to protect personal information. 

---

## Prerequisites

* **Java 17**
* **Maven 3.9+**
* **Node.js 18.17+ or 20+** and **npm 9+**
* **PostgreSQL 14+**
* **Stripe CLI** for local webhook forwarding
* **Canada Post developer account and Gmail OAuth2 setup for email**

---

## Configuration and Environment

The application is environment driven. The committed `application.yml` uses environment variables for secrets and endpoints. For local development, export variables in your shell or create local `.env` files that are not committed.

### Backend environment variables

Set these in your shell before starting the backend. Example values are for local development.

| Variable                | Description                                                                         |
| ----------------------- | ----------------------------------------------------------------------------------- |
| `TTCLUB_DB_URL`         | JDBC URL for Postgres, e.g. `jdbc:postgresql://localhost:5432/club_db`              |
| `TTCLUB_DB_USER`        | Database user for the app                                                           |
| `TTCLUB_DB_PASSWORD`    | Database password for the app                                                       |
| `FLYWAY_USER`           | Database user with migration privileges (often same as app user)                    |
| `FLYWAY_PW`             | Password for `FLYWAY_USER`                                                          |
| `TTCLUB_JWT_SECRET`     | Long random string for signing access tokens                                        |
| `TTCLUB_MFA_ENC_KEY`    | Encryption key for MFA secrets, at least 16 bytes (dev default exists; set in prod) |
| `GOOGLE_CLIENT_ID`      | Google OAuth2 client ID                                                             |
| `GOOGLE_CLIENT_SECRET`  | Google OAuth2 client secret                                                         |
| `STRIPE_SECRET_KEY`     | Stripe secret key, e.g. `sk_test_...`                                               |
| `STRIPE_WEBHOOK_SECRET` | Stripe webhook secret from Stripe CLI or dashboard, e.g. `whsec_...`                |
| `CANADAPOST_USERNAME`   | Canada Post API username (optional)                                                 |
| `CANADAPOST_PASSWORD`   | Canada Post API password (optional)                                                 |
| `GMAIL_OAUTH2_ENABLED`  | Set to `true` to send real email via Gmail XOAUTH2                                  |
| `MAIL_USER`             | Gmail address used to send emails                                                   |
| `GMAIL_REFRESH`         | Refresh token for Gmail OAuth2                                                      |
| `COACH_CODE`            | Optional self registration code for coach role                                      |
| `OWNER_CODE`            | Optional self registration code for owner role                                      |
| `ADMIN_CODE`            | Optional self registration code for admin role                                      |

Other behavior toggles available in `application.yml` for local safety:

* `ttclub.mail.enabled` default is false in dev. Set to true to send real email.
* `ttclub.cookies.secure` should be true in production behind HTTPS.
* `ttclub.cors.allowed-origins` includes the front end origin for dev.

### Frontend env vars

Create `frontend/.env.local` (ignored by Git):

```env
VITE_API_URL=http://localhost:8080
VITE_STRIPE_PUB_KEY=pk_test_xxx
VITE_USE_COOKIE_AUTH=true
```

Note: if your code references `VITE_API_BASE` instead of `VITE_API_URL`, set that instead. The default in this repository uses `VITE_API_URL`.

---

## Local Development: Step by Step

### 1. Clone and install

```bash
# clone
git clone git@github.com:filipilijevski/sports-booking.git
cd ttclubwebsite

# backend dependencies are resolved by Maven at build
# verify Java and Maven
java -version
mvn -v

# frontend
cd frontend
npm ci
cd ..
```

### 2. Create a local database

Create a database and user. Names are examples; you can choose any.

```bash
# psql shell or your GUI of choice
psql -U postgres

-- inside psql
CREATE USER club_user WITH PASSWORD 'club_pass';
CREATE DATABASE club_db OWNER club_user;
GRANT ALL PRIVILEGES ON DATABASE club_db TO club_user;
\q
```

Export application database variables:

```bash
# in your shell startup or terminal session
export TTCLUB_DB_URL="jdbc:postgresql://localhost:5432/club_db"
export TTCLUB_DB_USER="club_user"
export TTCLUB_DB_PASSWORD="club_pass"
```

### 3. Run database migrations with Flyway

The Flyway Maven plugin runs the SQL files under `backend/src/main/resources/db/migration`.

Set your migration credentials and run the plugin from the repo root:

```bash
export FLYWAY_USER="club_user"
export FLYWAY_PW="club_pass"

# run from repo root; -pl ensures the backend module is used
mvn -pl backend -am flyway:migrate \
  -Dflyway.url="jdbc:postgresql://localhost:5432/club_db"
```

If you need to clean and reapply during development:

```bash
mvn -pl backend -am flyway:clean flyway:migrate -Dflyway.url="jdbc:postgresql://localhost:5432/club_db"
```

Schema migrations under backend/src/main/resources/db/migration/ are version-controlled so the database can be created locally by Flyway. These scripts contain only DDL and non-sensitive seed data. No production data or secrets are included. Demo data, when used, is synthetic and enabled only under the dev profile.

### 4. Start the backend

Set minimal environment variables and run the app:

```bash
# secrets for local dev (use test keys)
export TTCLUB_JWT_SECRET="dev-local-please-change"
export TTCLUB_MFA_ENC_KEY="development-please-change-me-32-bytes-long"

# Stripe (you can start without these, but shop and payments will be disabled)
export STRIPE_SECRET_KEY="sk_test_xxx"
export STRIPE_WEBHOOK_SECRET="whsec_xxx"

# optional: email and Google login
export GOOGLE_CLIENT_ID="your-google-client-id"
export GOOGLE_CLIENT_SECRET="your-google-client-secret"

# from repo root
mvn -pl backend spring-boot:run
```

Health checks :

* API base: `http://localhost:8080`
* Actuator health: `http://localhost:8080/actuator/health`

### 5. Start the frontend

Please note prior to starting the frontend there are certain image paths you will have to replace with your own images in `Home.tsx` , `About.tsx` , `Navbar.tsx` and `HeroSection.tsx` prior to running the frontend.

```bash
cd frontend
# .env.local should exist with VITE_API_URL, VITE_STRIPE_PUB_KEY, VITE_USE_COOKIE_AUTH
npm run dev
# open http://localhost:5173
```

### 6. Stripe webhook for local development

Use Stripe CLI to forward events to the backend and capture the webhook secret.

```bash
# in a new terminal
stripe login

# forward webhooks to your backend endpoint
stripe listen --events payment_intent.succeeded,payment_intent.payment_failed,charge.refunded --forward-to http://localhost:8080/api/stripe/webhook
```

Copy the displayed `whsec_...` and export it:

```bash
export STRIPE_WEBHOOK_SECRET="whsec_xxx"
```

If your backend uses a different webhook path, adjust `--forward-to` accordingly.

### 7. Google OAuth2 client setup

1. In Google Cloud Console, create an OAuth2 Client ID for a Web application.
2. Authorized redirect URI for local dev:

   ```
   http://localhost:8080/login/oauth2/code/google
   ```
3. Set environment variables:

   ```bash
   export GOOGLE_CLIENT_ID="..."
   export GOOGLE_CLIENT_SECRET="..."
   ```
4. Start the backend and front end. The front end will redirect to Google for auth when selected.

Notes:

* In dev, the application uses cookies for auth by default (`VITE_USE_COOKIE_AUTH=true`).
* Ensure `ttclub.cors.allowed-origins` in `application.yml` contains your front end origin, e.g. `http://localhost:5173`.

### 8. Canada Post setup optional

Obtain API credentials from Canada Post and set:

```bash
export CANADAPOST_USERNAME="your-username"
export CANADAPOST_PASSWORD="your-password"
```

Configure the origin postal code in `application.yml` under `store.origin.postal`. Shipping quotes will be used during checkout if credentials are present; otherwise the shop can still operate with flat rates you implement.

### 9. Email setup optional

To send real email with Gmail XOAUTH2:

```bash
export GMAIL_OAUTH2_ENABLED=true
export MAIL_USER="you@gmail.com"
export GOOGLE_CLIENT_ID="..."
export GOOGLE_CLIENT_SECRET="..."
export GMAIL_REFRESH="refresh-token-from-your-consent-flow"
export MAIL_ENABLED=true   # enables sending vs logging-only
```

In local development, you can leave mail disabled. The app will log messages instead of sending them when `ttclub.mail.enabled=false`.

---

## How to Use

### Admin

1. Sign in as an admin or owner. There is a seed admin role for testing with the following credentials:
    * Username: admin@ttclub.local
    * Password: Password123!
2. Manage users:
    * Create users
    * Update roles
    * Reset passwords for local accounts
    * Delete accounts
3. Shop:

   * Create categories and products with SKU, brand, price, stock, weight, description, and images.
   * Fulfill orders, issue refunds, and view audit logs.
   * Record in person sales (cash, e-transfer, terminal).
   * Configure pickup vs shipping behavior. With Canada Post credentials, live rate quotes are available via the parameters you set in `application.yml`.
4. Booking:

   * Create memberships and define entitlements, such as program credits or rental hours.
   * Create programs and packages that grant a number of sessions that clients can consume.
   * Manage club event calendars and active programs.
   * Publish schedules, enroll members manually, and track attendance and credit consumption.
5. Blog:

   * Create, update and delete posts with optional markdown and images, reorder, and toggle visibility.

### Client

1. Register or sign in, including Google OAuth if enabled.
2. Browse and purchase memberships, rental credits and enroll in programs online via Stripe.
3. Shop for products online via the proshop and track order status.
4. View membership status, session usage, and rental credit balances.

---

## Roadmap and Next Steps

* Add a public visibility toggle for programs and memberships so certain items can be hidden from the public.
* Improve booking logic to include program consumption credits controlled by memberships.
* Add analytics and CSV export to avoid ad hoc database queries.

---

## Security, Secrets, and Safety

* In production, set:

  * `ttclub.cookies.secure=true`
  * `spring.jpa.show-sql=false`
  * Appropriate `ttclub.cors.allowed-origins` for your domain
* Rotate Stripe, Google, or mail credentials if they were ever exposed.

---

## Troubleshooting

* **Cannot connect to Postgres**

  * Verify `TTCLUB_DB_URL`, user, and password.
  * Check that the DB accepts TCP connections on the correct port and host.
* **Flyway migration failure**

  * Ensure the DB user has privileges.
  * If this is a dev database, `flyway:clean` then `flyway:migrate`.
* **CORS errors in the browser**

  * Confirm `ttclub.cors.allowed-origins` includes `http://localhost:5173` for dev.
  * Verify `VITE_API_URL` points to the backend.
* **OAuth2 login fails**

  * Check the redirect URI in Google Cloud matches `http://localhost:8080/login/oauth2/code/google`.
  * Ensure `GOOGLE_CLIENT_ID` and `GOOGLE_CLIENT_SECRET` are set.
* **Stripe payment not completing**

  * Start `stripe listen` and verify `STRIPE_WEBHOOK_SECRET`.
  * Use Stripe test cards. Ensure the webhook endpoint matches your backend path.
* **Shipping quotes missing**

  * Set Canada Post credentials. If not set, offer pickup to avoid blocking checkout.
* **Email not sending**

  * Set `GMAIL_OAUTH2_ENABLED=true` and `ttclub.mail.enabled=true`.
  * Verify `MAIL_USER` and `GMAIL_REFRESH`. In dev, leaving mail disabled will log instead of send.
* **Cookie issues**

  * In dev, cookies are not `secure`. In production behind HTTPS, set `ttclub.cookies.secure=true`.
* **Port conflicts**

  * Change `server.port` in `application.yml` or free the port.

---

## License

MIT License

Copyright &copy; 2025 filipilijevski

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.


