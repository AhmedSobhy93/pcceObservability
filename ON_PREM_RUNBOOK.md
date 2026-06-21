# On-Prem Production Runbook

## Minimum Runtime

- Windows Server or Linux VM inside the bank network.
- JDK 17+.
- Maven 3.9+ for build hosts.
- Network path from the app host to PCCE AW, HDS, CVP Reporting, Finesse, CUIC, CVP Call Server, CTI, PGs, and voice gateways.
- Read-only SQL Server users for AW/HDS and a read-only IBM Informix user for CVP Reporting.

## Build

```powershell
mvn clean package
```

Artifact:

```text
target/pcce-observability-0.0.1-SNAPSHOT.jar
```

## Required Environment Variables

Set real values before starting:

```powershell
$env:SPRING_PROFILES_ACTIVE="prod"
$env:PCCE_ADMIN_PASSWORD="{bcrypt}replace-with-bcrypt-hash"
$env:PCCE_AW_JDBC_URL="jdbc:sqlserver://aw-host:1433;databaseName=awdb;encrypt=true;trustServerCertificate=false"
$env:PCCE_AW_USERNAME="pcce_reader"
$env:PCCE_AW_PASSWORD="..."
$env:PCCE_HDS_JDBC_URL="jdbc:sqlserver://hds-host:1433;databaseName=ucce_hds;encrypt=true;trustServerCertificate=false"
$env:PCCE_HDS_USERNAME="pcce_reader"
$env:PCCE_HDS_PASSWORD="..."
$env:CVP_REPORTING_JDBC_URL="jdbc:informix-sqli://cvp-reporting-host:1526/cvp_data:INFORMIXSERVER=cvp_report;DELIMIDENT=y;"
$env:CVP_REPORTING_USERNAME="cvp_reader"
$env:CVP_REPORTING_PASSWORD="..."
```

## Start

```powershell
java -jar target/pcce-observability-0.0.1-SNAPSHOT.jar
```

For TLS on-prem:

```powershell
$env:SERVER_SSL_ENABLED="true"
$env:SERVER_SSL_KEY_STORE="C:\certs\pcce-observability.p12"
$env:SERVER_SSL_KEY_STORE_PASSWORD="..."
$env:SERVER_SSL_KEY_STORE_TYPE="PKCS12"
java -jar target/pcce-observability-0.0.1-SNAPSHOT.jar
```

## Smoke Test

```powershell
.\scripts\smoke-test.ps1 -BaseUrl "https://server-name:8443" -Username admin -Password "your-password"
```

## Production Validation Checklist

- Replace all `{noop}change-me` credentials.
- Enable component probes in `application.yml` for both sides of redundant PCCE components.
- Validate dropped-call `CallDisposition` mapping with Cisco reporting dictionary and bank operations definition.
- Validate all default SQL against the bank's AW/HDS SQL Server schema and CVP Reporting Informix schema.
- Confirm time zone alignment between app server, AW/HDS SQL Server, CVP Reporting Informix, CUIC, and reporting users.
- Configure log forwarding to SIEM.
- Place admin endpoints behind bank VPN/jump-host controls.
- Monitor `/api/v1/operations/assessment/last`.
- Keep maintenance mode disabled outside approved change windows.

## Useful Endpoints

- `GET /actuator/health`
- `GET /api/v1/components/status`
- `GET /api/v1/operations/assessment?from=YYYY-MM-DD&to=YYYY-MM-DD`
- `GET /api/v1/operations/assessment/last`
- `PUT /api/v1/operations/maintenance`
- `GET /api/v1/operations/audit`
- `GET /api/v1/admin/users`
- `PUT /api/v1/admin/users/{username}`
- `GET /api/v1/admin/components`
- `PUT /api/v1/admin/components/{name}`

## Incident Triage Flow

1. Check `/api/v1/operations/assessment/last`.
2. If component alerts exist, check the matching Cisco service and Windows/Linux host health.
3. If dropped-call alerts exist, compare HDS `t_Termination_Call_Detail`, CVP reporting, gateway disconnect cause, and carrier logs.
4. If service level is degraded without component alerts, check staffing, skill group queue depth, routing scripts, and agent state distribution.
5. Record admin changes and maintenance windows through the API so incident review has audit context.
