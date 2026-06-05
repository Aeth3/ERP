# Environment File Map

This is the exact split for the real env files used by the current ERP setup.

## 1. `ERP/.env`

Use this for shared backend defaults that are safe for the local team to keep as placeholders.

Suggested contents:

```env
SPRING_PROFILES_ACTIVE=dev

APP_NAME=maiu
DB_URL=jdbc:mysql://localhost:3306/maiu_db
DB_USERNAME=root
DB_PASSWORD=

JPA_DDL_AUTO=update
JPA_SHOW_SQL=true
JPA_DATABASE_PLATFORM=org.hibernate.dialect.MySQLDialect
JPA_FORMAT_SQL=true

FLYWAY_ENABLED=true
FLYWAY_BASELINE_ON_MIGRATE=true
FLYWAY_LOCATIONS=classpath:db/migration

JWT_SECRET=

LOG_LEVEL_ROOT=WARN
LOG_LEVEL_SPRING=DEBUG
LOG_LEVEL_HIBERNATE=DEBUG
LOG_LEVEL_HIBERNATE_SQL=TRACE
LOG_LEVEL_APP=DEBUG

APP_RATE_LIMIT_ENABLED=false
APP_RATE_LIMIT_CAPACITY=300
APP_RATE_LIMIT_REFILL_DURATION=PT1M
APP_FRONTEND_BASE_URL=http://localhost:3000
APP_DEFAULT_TENANT_ID=1

APP_SEED_DEFAULT_ADMIN_ENABLED=true
APP_SEED_DEFAULT_ADMIN_NAME=Default Admin
APP_SEED_DEFAULT_ADMIN_EMAIL=admin@maiu.local
APP_SEED_DEFAULT_ADMIN_PASSWORD=change-this-local-dev-password

APP_MAIL_FROM=no-reply@maiu.local
MAIL_HOST=
MAIL_PORT=587
MAIL_USERNAME=
MAIL_PASSWORD=
MAIL_SMTP_AUTH=true
MAIL_SMTP_STARTTLS_ENABLE=true
```

Put real secrets in `.env.local` instead when possible.

## 2. `ERP/.env.local`

Use this for machine-specific backend secrets.

Suggested contents:

```env
DB_PASSWORD=your-local-db-password
JWT_SECRET=your-local-jwt-secret
```

Optional:

```env
MAIL_HOST=smtp.example.com
MAIL_PORT=587
MAIL_USERNAME=your-mail-user
MAIL_PASSWORD=your-mail-password
```

## 3. `../.env.compose`

Use this for local Docker Compose runs from the workspace root.

Suggested contents:

```env
MYSQL_ROOT_PASSWORD=your-local-docker-db-password
MYSQL_DATABASE=erp_db_docker

SPRING_PROFILES_ACTIVE=docker
SPRING_DATASOURCE_URL=jdbc:mysql://mysql:3306/erp_db_docker?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
SPRING_DATASOURCE_USERNAME=root
SPRING_DATASOURCE_PASSWORD=your-local-docker-db-password

JPA_DDL_AUTO=validate
FLYWAY_ENABLED=true
FLYWAY_BASELINE_ON_MIGRATE=true

JWT_SECRET=your-local-docker-jwt-secret

NEXT_PUBLIC_API_URL=http://backend:8080
```

## 4. `../.env.compose.prod`

Use this for production Docker Compose runs from the workspace root or VM.

Suggested contents:

```env
SPRING_PROFILES_ACTIVE=prod
SPRING_DATASOURCE_URL=jdbc:mysql://your-prod-host:3306/erp_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
SPRING_DATASOURCE_USERNAME=your-prod-db-user
SPRING_DATASOURCE_PASSWORD=your-prod-db-password

JPA_DDL_AUTO=validate
FLYWAY_ENABLED=true
FLYWAY_BASELINE_ON_MIGRATE=true

JWT_SECRET=your-prod-jwt-secret

NEXT_PUBLIC_API_URL=https://api.your-domain.com
```

## Minimal Rule

Use this split:

- `ERP/.env`: non-sensitive backend defaults
- `ERP/.env.local`: local backend secrets
- `../.env.compose`: local Docker secrets/config
- `../.env.compose.prod`: production Docker secrets/config

## Production Guardrails

Production startup now fails fast if any of these are true:

- `JWT_SECRET` is blank or still a placeholder like `change-me`
- `JPA_DDL_AUTO` is not `validate` or `none`
- `APP_SEED_DEFAULT_ADMIN_ENABLED=true`
- `FLYWAY_ENABLED=false`

## Do Not Commit

These should stay untracked:

- `ERP/.env.local`
- `../.env.compose`
- `../.env.compose.local`
- `../.env.compose.prod`
