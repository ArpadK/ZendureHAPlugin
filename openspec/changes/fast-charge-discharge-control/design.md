## Context

The battery is a Zendure Solarflow. Zendure ships an official local-control API — **zenSDK** (`github.com/Zendure/zenSDK`) — reachable over the LAN via HTTP: `POST /properties/write` (set writable properties) and `GET /properties/report` (read all properties). Write bodies are `{"sn":"<serial>","properties":{...}}`. Relevant writable properties: `acMode` (1 = charge, 2 = discharge), `inputLimit` (max charge W), `outputLimit` (max discharge W), `smartMode` (1 = write to RAM only, not flash).

Today this is done by the third-party Gielz1986 YAML bundle. We are replacing it with a self-contained Home Assistant add-on built like the existing **ArpadsHomeAutomations** project: Spring Boot (JDK 25, Maven), packaged as an ingress-enabled HA add-on, connecting to Home Assistant over WebSocket (events) + REST (state), and owning/publishing its own HA entities via a virtual-entity registry.

This change is the foundation plus the smallest useful behavior: manual fast charge / fast discharge / standby. It must be fully generic — nothing setup-specific in the repo.

## Goals / Non-Goals

**Goals:**
- Stand up the project skeleton with the same layered architecture as ArpadsHomeAutomations (`automation` → `domain` → infra `homeassistant` + new `zendure`), enforced by ArchUnit.
- Talk to the battery directly via zenSDK REST; no dependency on Gielz or any other HA integration.
- Expose one plugin-owned control select (Fast Charge / Fast Discharge / Standby) and read-only status sensors (mode, SOC) in Home Assistant.
- Be deployable unchanged on any HA + any supported Zendure battery, all instance values coming from add-on config.
- Deploy exactly like ArpadsHomeAutomations (`Dockerfile`, `config.yaml`, `run.sh` reading `/data/options.json`).

**Non-Goals:**
- P1/CT meter reading and threshold/price-driven automation.
- Persistence (Postgres) and a custom React frontend — deferred to later changes.
- mDNS device auto-discovery (nice-to-have later; port is configured for now).
- Supporting transports other than local HTTP REST (no MQTT/Bluetooth in this change).

## Decisions

**D1 — Control the device directly over zenSDK REST, not through another HA integration.**
The whole point is to eliminate the Gielz dependency and the layered YAML/helper setup. A direct REST client (in a new `zendure` infrastructure package) owns the protocol. Alternative considered: reuse the official `Zendure/Zendure-HA` custom component and read/write its entities (like ArpadsHomeAutomations does for the current battery). Rejected because it re-introduces a mandatory third-party integration dependency, which is exactly what we're removing.

**D2 — Use `smartMode: 1` (RAM-only writes) for all commands.**
Manual charge/discharge toggling could write frequently. zenSDK documents `smartMode: 1` as "written to RAM, not flash," which avoids flash wear and matches Gielz's "save in RAM" commands. Trade-off: RAM values revert on device reboot; acceptable for manual control (the user re-selects, and later automation will re-assert periodically).

**D3 — Poll `GET /properties/report` on a fixed interval for status.**
The local REST API is request/response with no push channel. A scheduled poller (Spring `@Scheduled`) reads the report, maps it to a typed `Battery` reading, and publishes the mode/SOC status sensors. Interval is configurable with a sensible default (e.g. 30 s). Alternative: only read on demand — rejected because status sensors should stay reasonably fresh.

**D4 — Mirror the ArpadsHomeAutomations domain patterns.**
`SensorReading<T>` sealed type (Available | Unavailable) for all device reads; a `Battery` device abstraction translating domain intent (`fastCharge()` / `fastDischarge()` / `standby()`) into zenSDK payloads and mode/SOC out of reports; a virtual-entity registry to own the control select and status sensors and republish on (re)auth. This buys consistency with the existing project and testability (fake `Battery` for unit tests). ArchUnit enforces that `automation` never imports infrastructure.

**D5 — The control select is the source of user intent; the device is the source of truth for status.**
The select captures what the user asked for and drives commands. Status sensors reflect what the device actually reports. If a command fails, status is not optimistically updated. No persistence is needed in v1: on restart the select re-publishes its last known/default value and status is re-derived from the device.

**D6 — All instance values are config; protocol constants are code.**
`device_ip`, `device_port`, `device_serial`, `max_charge_power`, `max_discharge_power`, poll interval, HA connection/token, and log level are add-on options surfaced via `run.sh` → Spring properties (typed `@ConfigurationProperties` record). The zenSDK paths and property keys (`acMode`, `inputLimit`, `outputLimit`, `smartMode`) are generic constants in code. Nothing setup-specific is committed.

## Risks / Trade-offs

- **Unknown/variable local API port across firmware** → Make `device_port` a config var with the documented default; document how to find it; add mDNS discovery in a later change.
- **Local API must be enabled on the device** (zenSDK: enable via the Zendure app; HEMS/SmartMode interaction) → Document the one-time device setup in the add-on README; fail fast with a clear error if the device is unreachable at startup.
- **RAM-only writes revert on device reboot (D2)** → Acceptable for manual v1; note it, and have future automation periodically re-assert intent.
- **No authentication on the local REST API** → Rely on LAN trust boundary as zenSDK does; document that the add-on and battery should be on a trusted network.
- **Polling adds latency/load (D3)** → Keep interval modest and configurable; treat unreachable device as `Unavailable`, never crash the loop.
- **Publishing owned entities into HA from an add-on** → Reuse the proven ArpadsHomeAutomations virtual-entity approach; republish on every (re)auth so entities survive restarts.

## Open Questions

- What is the correct default value for `device_port` for current zenSDK firmware? (Confirm from device/mDNS during implementation; keep it a documented default either way.)
- Should Standby also explicitly set `acMode`, or is zeroing `inputLimit`/`outputLimit` sufficient? (Start with zeroing limits per the documented standby example; verify against a real device.)
- Exact set of `properties/report` field names for SOC and mode across models — confirm against the zenSDK properties doc and a real device during implementation; keep the mapping isolated in the `zendure` package so it is easy to adjust.
