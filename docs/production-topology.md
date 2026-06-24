# Production Topology Monitoring

The lab profile can monitor a single-node PCCE setup. Production should define every important node as a separate `pcce.components` item.

Use the same `name` for the component type, and distinguish nodes with:

- `display-name`
- `side`: `A`, `B`, `SHARED`, `DR`
- `site`: `DC1`, `DC2`, `DR`, `WAN`, etc.
- `tier`: `ICM`, `PG`, `CVP`, `IVR`, `DESKTOP`, `REPORTING`, `VOICE`, `NETWORK`, etc.

Example:

```yaml
pcce:
  components:
    - name: ICM_Router
      display-name: Router A
      side: A
      site: DC1
      tier: ICM
      enabled: true
      probe: TCP
      host: router-a.example.local
      port: 4000

    - name: ICM_Router
      display-name: Router B
      side: B
      site: DC2
      tier: ICM
      enabled: true
      probe: TCP
      host: router-b.example.local
      port: 4000
```

## Recommended Components For 2000-Agent PCCE

Monitor at least:

- Router A/B
- Logger A/B
- AW / Distributor A/B
- HDS / reporting SQL nodes
- Live Data nodes
- CUCM PG A/B
- CVP PG A/B
- CTI Server A/B
- CVP Call Servers
- CVP VXML Servers
- CVP OAMP
- CVP Reporting Informix
- VVB nodes
- Finesse A/B
- CUIC Publisher / Subscribers
- CUCM Publisher / Subscribers
- IM&P nodes if used
- Voice gateways / CUBE / SIP proxy
- Load balancer VIPs
- ECE if chat/email is used
- Eleveo QM / Recording / WFM
- DNS, NTP, domain controllers

## Probe Types

- `HOST`: DNS/host reachability only. It does not prove the Cisco service is healthy.
- `TCP`: port reachability. Use for Router, CTI, gateway SIP ports, SQL, and service listeners.
- `HTTP`: web/service endpoint health. Use for Finesse, CUIC, CVP diag, VVB, CUCM admin, load balancer VIPs.
- `JDBC_AW`, `JDBC_HDS`, `JDBC_CVP_REPORTING`: app database connectivity checks.

## Server Metrics

CPU, memory, disk, Windows service state, process state and log health still require one of:

- SNMP v2/v3
- WMI/WinRM
- Prometheus/windows exporter
- Enterprise monitoring agent
- SIEM/log collector

The app now models the production topology, but remote OS counters need one of those collection methods.
