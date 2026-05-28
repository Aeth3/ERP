# Secret Rotation Checklist

This checklist covers the credentials and secrets that have been used by the ERP backend and Docker deployment files.

## Rotate Immediately

- `DB_PASSWORD`
- `MYSQL_ROOT_PASSWORD`
- `SPRING_DATASOURCE_PASSWORD`
- `JWT_SECRET`
- any production database password currently referenced in deployment files or shell history

## Update Local Backend Files

Check these files and replace old secret values with the new ones:

- [`.env`](/d:/Projects/maiu/ERP/.env)
- [`.env.local`](/d:/Projects/maiu/ERP/.env.local) if you use it

Recommended values to update:

- `DB_PASSWORD`
- `JWT_SECRET`

## Update Docker / Deployment Env Files

Check these files from the workspace root and replace old values:

- [`docker-compose.yml`](/d:/Projects/maiu/docker-compose.yml)
- [`docker-compose-prod.yml`](/d:/Projects/maiu/docker-compose-prod.yml)
- [`.env.compose`](</d:/Projects/maiu/.env.compose>) if you create it
- [`.env.compose.prod`](</d:/Projects/maiu/.env.compose.prod>) if you create it

Recommended values to update:

- `MYSQL_ROOT_PASSWORD`
- `SPRING_DATASOURCE_PASSWORD`
- `JWT_SECRET`

## Update Infrastructure

Replace the same values anywhere else they may exist:

- cloud VM environment variables
- CI/CD secrets
- container hosting secrets
- database admin panel or RDS credentials
- password managers or shared team docs

## Safe Rotation Order

1. Generate new values first.
2. Update production secret storage or env vars.
3. Update application config files that should reference the new values.
4. Restart or redeploy the backend.
5. Verify login, DB connectivity, and token generation.
6. Remove any old copied secrets from notes or temporary files.

## Recommended Secret Formats

- `JWT_SECRET`: at least 32 bytes of random base64
- DB passwords: long random string, not reused across local and production

Example PowerShell commands:

```powershell
[Convert]::ToBase64String((1..32 | ForEach-Object { Get-Random -Maximum 256 }))
```

```powershell
-join ((33..126) | Get-Random -Count 24 | ForEach-Object { [char]$_ })
```

## Verification After Rotation

Confirm these still work:

- backend starts successfully
- Flyway migrations run
- database connection succeeds
- login/auth endpoints issue valid JWTs
- Docker compose services boot with the new env values

## Search Targets

Use these searches to find leftovers:

```powershell
rg "DB_PASSWORD|JWT_SECRET|MYSQL_ROOT_PASSWORD|SPRING_DATASOURCE_PASSWORD" d:\Projects\maiu
```

```powershell
rg "admin123|change-me|dbmasteruser|jdbc:mysql://" d:\Projects\maiu
```

## Follow-up Hardening

After rotation, the next improvements are:

- keep real secrets only in untracked env files
- move production secrets into platform-managed secret storage
- avoid storing raw credentials in compose files
- remove old credentials from any public image build or deployment notes
