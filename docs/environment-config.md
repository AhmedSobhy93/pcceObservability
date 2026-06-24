# Environment Configuration

## Profiles

Use Spring profiles to separate local, test, and production configuration.

```powershell
SPRING_PROFILES_ACTIVE=local
```

```powershell
SPRING_PROFILES_ACTIVE=prod
```

## App-Owned Database

The app-owned database stores operational configuration and business-owned data that must survive restarts:

- Project plan tasks
- App settings
- Future app users, roles, permissions
- Server log targets
- Audit events

PCCE AW/HDS SQL Server and CVP Informix remain read-only reporting sources.

## Local

Local defaults use H2 file database:

```text
jdbc:h2:file:./data/pcce-observability
```

Useful local settings:

```powershell
SPRING_PROFILES_ACTIVE=local
APP_DB_URL=jdbc:h2:file:./data/pcce-observability;AUTO_SERVER=TRUE;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH
APP_DB_USERNAME=sa
APP_DB_PASSWORD=
H2_CONSOLE_ENABLED=true
```

H2 console:

```text
http://localhost:8080/h2-console
```

Only admins can open it.

## Production

Recommended production database: PostgreSQL.

```powershell
SPRING_PROFILES_ACTIVE=prod
APP_DB_URL=jdbc:postgresql://db-host:5432/pcce_observability
APP_DB_USERNAME=pcce_observability
APP_DB_PASSWORD=<secret>
APP_DB_DRIVER=org.postgresql.Driver
APP_DB_MAX_POOL_SIZE=10
```

## Secrets

Do not store these in YAML:

- `APP_DB_PASSWORD`
- `PCCE_AW_PASSWORD`
- `PCCE_HDS_PASSWORD`
- `CVP_REPORTING_PASSWORD`
- `FINESSE_PASSWORD`
- `PCCE_ADMIN_PASSWORD`
- `PCCE_SMTP_PASSWORD`
- SMS authorization header

Use environment variables, Windows service variables, Kubernetes/OpenShift secrets, or your enterprise secret manager.

## Migrations

Flyway runs automatically from:

```text
src/main/resources/db/migration
```

The first migration creates the app core schema:

```text
V1__app_core.sql
```

For production changes, add a new migration file. Do not edit old migrations after deployment.
