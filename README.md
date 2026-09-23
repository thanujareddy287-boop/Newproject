# Secure Student Management Application

## Stack

- Java 17
- Spring Boot
- Spring Security
- Spring Data JPA
- MySQL
- HTML/CSS/JavaScript
- Maven

## Security layers demonstrated

1. HTTPS-ready deployment configuration
2. Frontend output safety (`textContent`, no untrusted `innerHTML`)
3. Spring Security
4. Session authentication
5. BCrypt password hashing
6. USER/ADMIN authorization
7. Bean Validation
8. JPA parameterization / no SQL string concatenation
9. CSRF protection
10. CORS
11. Security headers / CSP
12. Global exception handling
13. Rate limiting for login/register
14. Environment variables for secrets
15. Secure session cookie settings

## 1. Create MySQL database and application user

Run as a MySQL administrator:

```sql
CREATE DATABASE secure_student_app;

CREATE USER 'student_app'@'localhost'
IDENTIFIED BY 'change_me';

GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, ALTER, INDEX
ON secure_student_app.*
TO 'student_app'@'localhost';

FLUSH PRIVILEGES;
```

For a real deployment, use a strong generated password rather than `change_me`.

## 2. Configure environment variables

Linux/macOS:

```bash
export DB_URL="jdbc:mysql://localhost:3306/secure_student_app?useSSL=false&serverTimezone=UTC"
export DB_USERNAME="student_app"
export DB_PASSWORD="YOUR_STRONG_PASSWORD"
export SESSION_COOKIE_SECURE="false"
```

Windows PowerShell:

```powershell
$env:DB_URL="jdbc:mysql://localhost:3306/secure_student_app?useSSL=false&serverTimezone=UTC"
$env:DB_USERNAME="student_app"
$env:DB_PASSWORD="YOUR_STRONG_PASSWORD"
$env:SESSION_COOKIE_SECURE="false"
```

## 3. Run

```bash
mvn spring-boot:run
```

Open:

```text
http://localhost:8080/
```

## 4. First account

Registration intentionally creates only USER accounts.

For an admin account, after registering, change the role directly in MySQL:

```sql
UPDATE users
SET role = 'ADMIN'
WHERE email = 'your-email@example.com';
```

In a production system, admin provisioning should be controlled by a secure administrative process rather than allowing public registration to select ADMIN.

## 5. API behavior

Public:

```text
POST /api/auth/register
POST /api/auth/login
GET  /api/auth/csrf
GET  /actuator/health
```

Authenticated:

```text
GET  /api/auth/me
GET  /api/students
POST /api/auth/logout
```

ADMIN only:

```text
POST   /api/students
PUT    /api/students/{id}
DELETE /api/students/{id}
```

## Production notes

- Put the application behind HTTPS.
- Set `SESSION_COOKIE_SECURE=true`.
- Use a strong DB password stored in your hosting platform's secret/environment-variable system.
- Replace the simple in-memory rate limiter with a distributed limiter such as Redis-backed limiting when running multiple application instances.
- Do not expose Actuator endpoints publicly beyond what you need.
- Do not commit secrets to Git.
