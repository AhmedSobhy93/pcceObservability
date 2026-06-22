# Cisco Stock Report Mapping

Use Cisco CUIC/PCCE stock reports as the functional reference for dashboard pages and metric names. The app APIs remain custom, but dashboard sections should map to familiar Cisco reporting views so operations, supervisors, and workforce teams can validate numbers against CUIC.

## Dashboard Overview

Reference report families:

- Call Type Historical / Interval
- Skill Group Historical / Interval
- Precision Queue Historical / Interval, where deployed

Mapped widgets:

- Calls Offered
- Calls Handled
- Calls Abandoned
- Service Level
- Average Speed of Answer
- Average Handle Time
- Calls by Skill Group
- Call Volume by Hour

Current API sources:

- `GET /api/v1/metrics/calls`
- `GET /api/v1/calls/dropped`
- `GET /api/v1/calls/disposition-breakdown`

## Business Metrics

Reference report families:

- Call Type Historical All Fields
- Skill Group Historical All Fields
- Service Historical / Interval

Mapped widgets:

- Service Level %
- Abandonment count/rate
- Average queue/speed answer
- Average talk/wrap/handle time
- Transfer rate, once validated in HDS schema

## Agent Performance

Reference report families:

- Agent Historical
- Agent Team Historical
- Agent Skill Group Historical
- Agent State Trace, if enabled

Mapped widgets:

- Agent name/login
- Team
- Calls handled
- Agent state
- AHT, talk, hold, wrap, occupancy after exact HDS columns are validated

Current safe mode:

- The default query returns roster/team data first.
- Interval performance columns should be enabled after confirming exact `t_Agent_Interval` columns in your HDS.

## Call Analytics

Reference report families:

- Termination Call Detail
- Call Type Historical
- Route Call Detail, if used by operations

Mapped widgets:

- Dropped/abandoned calls by hour
- Disposition breakdown
- Skill group drop hotspots

Current dropped-call definition:

- Conservative default: `CallDisposition IN (1,2,3,4,5,6,7,10,19)`
- Validate with operations using `/api/v1/calls/disposition-breakdown`.

## CVP / IVR

Reference report families:

- CVP Call Detail
- CVP Application Summary
- CVP Call Server / VXML Server reporting

Mapped widgets:

- IVR containment
- CVP Reporting DB connectivity
- CVP Call Server probe

Current source:

- IBM Informix CVP Reporting database through `jdbc:informix-sqli`.

## System Health

Reference operational checks:

- PCCE Router/Logger reachability
- PG/CTI reachability
- CVP Call Server reachability
- CVP Reporting database probe
- Finesse/CUIC web availability
- Voice gateway SIP reachability

Mapped API:

- `GET /api/v1/components/status`
- `GET /api/v1/operations/assessment`

## Official References

- Cisco Packaged Contact Center Enterprise documentation: https://www.cisco.com/c/en/us/support/customer-collaboration/packaged-contact-center-enterprise/series.html
- Cisco Unified Intelligence Center documentation: https://www.cisco.com/c/en/us/support/customer-collaboration/unified-intelligence-center/series.html
- Cisco Unified Customer Voice Portal documentation: https://www.cisco.com/c/en/us/support/customer-collaboration/unified-customer-voice-portal/series.html
