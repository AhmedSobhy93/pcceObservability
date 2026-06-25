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

## Required PCCE Lab/SIT Integrations

Set these in IntelliJ Run Configuration or as Windows service variables:

```powershell
PCCE_AW_JDBC_URL=jdbc:sqlserver://10.10.90.193:1433;databaseName=pcce_awdb;encrypt=true;trustServerCertificate=true
PCCE_AW_USERNAME=obser_user
PCCE_AW_PASSWORD=<secret>

PCCE_HDS_JDBC_URL=jdbc:sqlserver://10.10.90.193:1433;databaseName=pcce_hds;encrypt=true;trustServerCertificate=true
PCCE_HDS_USERNAME=obser_user
PCCE_HDS_PASSWORD=<secret>

CVP_REPORTING_JDBC_URL=jdbc:informix-sqli://10.10.90.198:1526/cvp_data:INFORMIXSERVER=cvp;DELIMIDENT=y;
CVP_REPORTING_USERNAME=cvp_dbuser
CVP_REPORTING_PASSWORD=<secret>
```

## PCCE Unified Config API

Read-only actions and monitoring are enabled when the API itself is enabled:

```powershell
PCCE_API_ENABLED=true
PCCE_API_BASE_URL=https://vswsitaw01.dev.mdi
PCCE_API_USERNAME=<api-user>
PCCE_API_PASSWORD=<secret>
PCCE_API_AUTH_MODE=BASIC
PCCE_API_MONITORING_ENABLED=true
```

Mutating PCCE API actions stay disabled unless explicitly enabled in YAML and must remain admin-only.

## Finesse API

Finesse live state is used to overlay agent status on top of AW/HDS historical data:

```powershell
FINESSE_ENABLED=true
FINESSE_BASE_URL=https://vssitfin01.dev.mdi:8445
FINESSE_USERNAME=<finesse-api-user>
FINESSE_PASSWORD=<secret>
FINESSE_TRUST_ALL_CERTIFICATES=true
FINESSE_TIMEOUT=15s
FINESSE_TEAM_IDS=
FINESSE_QUEUE_IDS=
```

Leave `FINESSE_USER_IDS` empty when `/finesse/api/Users` works; the app discovers users automatically and adds AW login IDs as fallback.

## Alerts

SMTP:

```powershell
PCCE_SMTP_ENABLED=true
PCCE_SMTP_HOST=<smtp-host>
PCCE_SMTP_PORT=25
PCCE_SMTP_USERNAME=<optional>
PCCE_SMTP_PASSWORD=<optional-secret>
PCCE_SMTP_FROM=pcce-observability@bank.local
PCCE_SMTP_RECIPIENTS=ops@bank.local,support@bank.local
PCCE_SMTP_AUTH=false
PCCE_SMTP_STARTTLS=false
```

SMS through D-Tech microgateway:

```powershell
PCCE_SMS_ENABLED=true
PCCE_SMS_URL=http://sms-microgateway.sit.apps.openshift.dev.mdi/mgw/v1/system/secure/sms-gateway/sms/immediate
PCCE_SMS_AUTHORIZATION=Basic <base64-user-pass>
PCCE_SMS_USER_AGENT=D-Tech / v1.6
PCCE_SMS_RECIPIENTS=+201xxxxxxxxx,+201yyyyyyyyy
```

## LDAP / Active Directory

Local app users remain supported. LDAP/AD variables define the second authentication source:

```powershell
APP_LDAP_ENABLED=true
APP_LDAP_URLS=ldap://ad01.bank.local:389
APP_LDAP_BASE=DC=bank,DC=local
APP_LDAP_MANAGER_DN=CN=svc-pcce-observability,OU=Service Accounts,DC=bank,DC=local
APP_LDAP_MANAGER_PASSWORD=<secret>
APP_LDAP_USER_SEARCH_BASE=OU=Users
APP_LDAP_USER_SEARCH_FILTER=(sAMAccountName={0})
APP_LDAP_GROUP_SEARCH_BASE=OU=Groups
APP_LDAP_ADMIN_GROUP=PCCE-OBS-Admins
APP_LDAP_WFM_GROUP=PCCE-OBS-WFM
APP_LDAP_SUPERVISOR_GROUP=PCCE-OBS-Supervisors
APP_LDAP_VIEWER_GROUP=PCCE-OBS-Viewers
```

## Server Metrics and Logs

HTTP/TCP/JDBC probes do not provide CPU, memory, disk, Windows services, or process counters. Configure one enterprise collector path per server:

- SNMP from Windows/Linux nodes
- WMI/WinRM for Windows performance counters and services
- Prometheus/Windows exporter
- Existing monitoring/SIEM/agent feed

Use the SPOG Ops tab to enable log targets for Router, Logger/AW/HDS, PG/CTI, CVP, Finesse, CUIC, VVB, and CUCM.

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

## Secure CVP JMX

Use JMX only after the CVP/OAMP secure JMX certificate setup is complete and the application truststore is approved by security.

Required values:

```powershell
PCCE_JMX_ENABLED=true
PCCE_JMX_USERNAME=<jmx-user>
PCCE_JMX_PASSWORD=<secret>
PCCE_JMX_TRUST_STORE_CONFIGURED=true
PCCE_JMX_CVP_CALLSERVER_A_URL=<service:jmx:rmi...>
PCCE_JMX_CVP_CALLSERVER_B_URL=<service:jmx:rmi...>
PCCE_JMX_CVP_REPORTING_URL=<service:jmx:rmi...>
```

Use JMX for JVM and CVP service health such as memory, threads, selected MBeans, and service-specific counters. Keep the target list explicit and keep timeouts low.

## PCCE Live Data

Use Live Data for realtime dashboards and keep HDS for historical windows. This reduces the risk of expensive refresh loops on HDS.

SIT values shared for CUIC Live Data:

```powershell
PCCE_LIVE_DATA_ENABLED=true
PCCE_LIVE_DATA_HOST=vssitcuic01.dev.mdi
PCCE_LIVE_DATA_PORT=443
PCCE_LIVE_DATA_WEBSOCKET_PORT=443
PCCE_LIVE_DATA_TOKEN_PATH=livedata/token/new
PCCE_LIVE_DATA_USERNAME=LDi1edulmtibl2
PCCE_LIVE_DATA_PASSWORD=<from CUIC datasource>
```

Recommended Live Data use cases:

- Agent realtime state
- Skill group realtime
- Call type realtime
- Precision queue realtime
- Supervisor/team wallboards

Implemented app endpoints:

- `GET /api/v1/live-data/token-probe` checks the CUIC Live Data token endpoint and exposes the WebSocket target.
- `GET /api/v1/live-data/realtime-snapshots` checks bounded Cisco AW realtime stock sources:
  - `t_Agent_Real_Time`
  - `t_Skill_Group_Real_Time`
  - `t_Call_Type_Real_Time`
  - `t_Precision_Queue_Real_Time`

Use Live Data for realtime wallboards and use these AW realtime snapshots as a safe validation/fallback path. Keep historical reports on HDS/CUIC stock SQL.

## AppDynamics

If AppDynamics agents are already deployed on PCCE/CVP/Finesse/CUIC nodes, use this app as an operations portal that links or embeds approved controller dashboards.

Required values:

```powershell
APPD_ENABLED=true
APPD_CONTROLLER_URL=<controller-url>
APPD_ACCOUNT_NAME=<account>
APPD_USERNAME=<read-only-user>
APPD_PASSWORD=<secret>
APPD_APPLICATION_NAME=PCCE
APPD_PCCE_FLOWMAP_URL=<dashboard-or-flowmap-url>
APPD_CVP_JVM_URL=<dashboard-url>
```

Use a read-only AppDynamics API user. Keep alert ownership clear: AppDynamics detects infrastructure/APM symptoms; this portal correlates them with contact-center KPIs, component status, and SMS/SMTP escalation.

## Database Load Protection

For AW/HDS/CVP Reporting, production settings should be conservative:

- Read-only DB users
- Small Hikari pools per datasource
- Query timeout enabled through `pcce.performance.jdbc-query-timeout-seconds`
- Short UI refresh intervals avoided for historical HDS pages
- Live Data used for realtime screens
- Heavy CUIC-aligned historical reports scheduled or paginated
