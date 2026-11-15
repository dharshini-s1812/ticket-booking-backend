# VelvetReel — Backend

REST API for the VelvetReel movie ticket booking app. The React frontend lives in a separate repository.

## Features

- Email + OTP login, JWT access/refresh tokens with refresh-token rotation
- Movies (now showing / coming soon), theatres, screens and seat layouts
- Shows and recurring show runs (extend / decline / cancel), automatic expiry schedulers
- Seat map, booking, simulated payment, cancellation and refunds
- QR-code ticket images (ZXing), image hosting via Cloudinary
- Offers, in-app notifications, audit logs, admin management

## Tech stack

Java 17 · Spring Boot 3.3 (Web, Data JPA, Security, Mail) · PostgreSQL · JJWT · ZXing · Cloudinary · Lombok · Maven

## Getting started

**Prerequisites:** JDK 17+, Maven 3.9+, PostgreSQL.

1. Create the database:
   ```sql
   CREATE DATABASE ticketbooking;
   ```
2. Configure environment variables (see [`.env.example`](.env.example)):

   | Variable | Required | Description |
   | --- | --- | --- |
   | `DB_URL` | no | JDBC URL (default `jdbc:postgresql://localhost:5432/ticketbooking`) |
   | `DB_USERNAME` | no | default `postgres` |
   | `DB_PASSWORD` | yes | Database password |
   | `MAIL_USERNAME` / `MAIL_PASSWORD` | yes | SMTP account (Gmail app password) used to send OTP emails |
   | `JWT_SECRET` | yes | Signing key, 32+ characters |
   | `CLOUDINARY_CLOUD_NAME` / `CLOUDINARY_API_KEY` / `CLOUDINARY_API_SECRET` | yes | Cloudinary credentials |

3. Run:
   ```bash
   mvn spring-boot:run
   ```
   API is served at `http://localhost:8080/api`. Tables are created automatically (`ddl-auto=update`) and the `ROLE_USER` / `ROLE_ADMIN` roles are seeded on startup.

### Creating the first admin

Admins sign in with an email OTP like everyone else, but nothing seeds the first admin account. After the first run, insert one directly (role id `2` is `ROLE_ADMIN`):

```sql
INSERT INTO users (email, role_id) VALUES ('you@example.com', 2);
```

(Adjust the table/column names to match your generated schema.) Further admins can be added from the admin panel.

## API overview

| Prefix | Purpose |
| --- | --- |
| `/api/auth` | `email`, `verify-otp`, `refresh-token`, `logout`, `session` |
| `/api/movies` | Now showing / coming soon |
| `/api/theatres`, `/api/screens` | Theatre and screen management |
| `/api/shows` | Public show search and seat map |
| `/api/bookings` | Create, pay, cancel, list bookings |
| `/api/offers` | Public offers |
| `/api/notifications` | User notifications |
| `/api/admin/**` | Shows, show-runs, offers, streaming movies, logs, admins |

## Project structure

```
src/main/java/com/TicketBooking/
├── Config/       # Security, JWT filter, CORS, Cloudinary
├── Controller/   # REST controllers
├── DTO/          # Request / Response objects
├── Exception/    # Custom exceptions + global handler
├── Model/        # JPA entities and enums
├── Repository/   # Spring Data repositories
├── Scheduler/    # Booking cleanup, movie expiry, show-run schedulers
├── Service/      # Business logic (interfaces + impls)
└── Util/
```

## Known limitations

- `SecurityConfig` currently has `/api/**` set to `permitAll` (marked "change this at end"), so role checks are not enforced yet. Lock down `/api/admin/**` to `ROLE_ADMIN` before any public deployment.
- CORS allows all origins; restrict it to your frontend's origin in production.
- Payments are simulated.
