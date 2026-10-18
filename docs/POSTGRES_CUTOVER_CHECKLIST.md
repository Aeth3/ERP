# PostgreSQL Cutover Checklist

Use this when moving the ERP backend from MySQL to PostgreSQL.

## 1. Prepare the target database

- Create a PostgreSQL database, user, and password.
- Make sure the backend can reach port `5432` on the Postgres host.
- Confirm the database timezone and app timezone expectations.

Suggested connection shape:

```env
SPRING_DATASOURCE_URL=jdbc:postgresql://your-postgres-host:5432/erp_db
SPRING_DATASOURCE_USERNAME=your-db-user
SPRING_DATASOURCE_PASSWORD=your-db-password
```

## 2. Update deployment env values

Fill these files with real Postgres values:

- `../.env.compose.prod`
- `../containers.json` if your deployment platform still reads it

Use [`.env.compose.prod.example`](/d:/Projects/maiu/.env.compose.prod.example) as the source template.

## 3. Choose a bootstrap path

Current Flyway files are incremental, not a full baseline schema.

For a brand-new empty PostgreSQL database, do one of these:

1. Set `JPA_DDL_AUTO=update` for the first backend startup.
2. Let Hibernate create the missing base tables.
3. Keep `FLYWAY_ENABLED=true` so the incremental migrations still run.
4. After the first successful startup, change `JPA_DDL_AUTO` back to `validate`.

If you later add a full baseline Flyway migration, you can remove this bootstrap step.

## 4. If you are migrating existing MySQL data

Recommended order:

1. Freeze writes to the old MySQL-backed deployment.
2. Export the MySQL data.
3. Transform types before import:
   - MySQL `BINARY(16)` UUID storage should become PostgreSQL `UUID`
   - MySQL `DATETIME(6)` values should become PostgreSQL `TIMESTAMP WITH TIME ZONE` or compatible timestamps
   - identity/auto-increment values should become PostgreSQL identity columns or sequences
4. Import into PostgreSQL.
5. Start the backend against PostgreSQL.
6. Run smoke tests before reopening writes.

## 5. First startup checks

- Backend starts without Flyway errors.
- No schema-validation failures appear in logs.
- Login works.
- Dashboard pages load.
- Core create flows work:
  - products
  - suppliers
  - warehouses
  - purchase orders
  - sales orders
  - projects
  - approvals

## 6. After cutover

- Rotate any old MySQL or exposed DB passwords.
- Remove stale MySQL JDBC URLs from deployment systems.
- Keep `JPA_DDL_AUTO=validate` in production after bootstrap.
- Monitor logs for SQL grammar or UUID-casting errors.
