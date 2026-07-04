# Architecture

This document is the durable architecture plan for **ZendureHAPlugin**. Keep it in sync with reality as the project evolves — it exists so the design stays consistent across sessions. For the motivation and the current in-flight work, see `openspec/changes/`.

## What this is

A native **Home Assistant add-on** (Java 25 / Spring Boot / Maven, ingress-enabled) that controls a Zendure Solarflow battery **locally** over the official **zenSDK REST API**. It replaces the third-party `Gielz1986/Zendure-HA-zenSDK` YAML + automations bundle entirely — no dependency on Gielz or any other Zendure HA integration.

Deployment mirrors the sibling project **ArpadsHomeAutomations** (`../ArpadsHomeAutomations`), which is also the source of the architectural patterns below.

## Design principles

1. **Layered / hexagonal.** Decision logic never talks to infrastructure directly. Dependencies point inward: `automation → domain → infrastructure`. Enforced by an ArchUnit test.
2. **Generic, config-driven.** The same build runs on any HA instance with any supported Zendure battery. Instance-specific values (device IP, port, serial, power limits, HA token) are runtime configuration; only the generic zenSDK protocol lives in code. **Nothing setup-specific is committed.**
3. **Typed boundaries.** Raw HA/device strings are converted to typed domain values (`SensorReading<T>`, enums, typed events) at the edges. Automations work with types, never raw JSON/state strings.
4. **Fail visibly, degrade gracefully.** Device reads that fail become `Unavailable` (never a fabricated number) and never crash the loop; command failures are observable and logged, and status is not optimistically updated.

## Layers & package layout

Base package (proposed): `com.arpad.zendure` — confirm before implementation.

```
automation/         Decision logic. Reacts to user intent / schedules; calls domain only.
                    e.g. BatteryModeControlAutomation (select → device command)
domain/
  device/           Device abstractions. Battery: fastCharge()/fastDischarge()/standby(),
                    stateOfCharge(), mode(). Translates domain intent ↔ zenSDK properties.
  value/            Value objects. SensorReading<T> (sealed: Available | Unavailable), enums.
  event/            Typed domain events.
  virtualentity/    Registry for plugin-owned HA entities (control select + status sensors);
                    publishes state, republishes on (re)auth, routes inbound changes.
homeassistant/      INFRA. HA connection: WebSocket (events) + REST (state), auth, reconnect,
  dto/              state-change dispatch. Not visible to `automation`.
zendure/            INFRA. zenSDK local REST client + protocol constants. Not visible to `automation`.
config/             Typed @ConfigurationProperties records; wiring; startup validation.
```

**Dependency rule (ArchUnit-enforced):** `automation` must not import `homeassistant` or `zendure`; `domain` must not import infrastructure. Infrastructure is reached only through `domain` abstractions.

## Two integration boundaries

The plugin sits between two systems and owns a client for each:

1. **Home Assistant** (`homeassistant/`) — WebSocket API for events + REST API for state. Uses the Supervisor token/endpoints by default (`ws://supervisor/core/websocket`, `http://supervisor/core`), overridable for standalone dev. Auto-reconnects with bounded backoff. The plugin **owns and publishes** its HA entities (it does not read a pre-existing Zendure integration's entities).

2. **Zendure device** (`zendure/`) — zenSDK local HTTP REST at `http://<deviceIp>:<devicePort>`:
   - `GET /properties/report` — read all properties (SOC, mode, …).
   - `POST /properties/write`, body `{"sn":"<serial>","properties":{…}}`, `Content-Type: application/json`.
   - Control properties: `acMode` (1=charge, 2=discharge), `inputLimit` (max charge W), `outputLimit` (max discharge W), `smartMode` (1 = RAM-only write, avoids flash wear; reverts on device reboot).
   - Command payloads:
     - Fast Charge → `{acMode:1, inputLimit:<maxChargeW>, smartMode:1}`
     - Fast Discharge → `{acMode:2, outputLimit:<maxDischargeW>, smartMode:1}`
     - Standby → `{inputLimit:0, outputLimit:0, smartMode:1}`

Protocol source: official https://github.com/Zendure/zenSDK (`docs/en_properties.md`).

## Data flow

**Command (user → device):**
`HA select change` → `homeassistant` dispatch → `BatteryModeControlAutomation` → `Battery.fastCharge()/…` → `zendure` REST `POST /properties/write`. On failure: log, do not update status optimistically.

**Status (device → user):**
`@Scheduled` poll (interval from config) → `Battery.stateOfCharge()/mode()` → `zendure` REST `GET /properties/report` → `SensorReading<T>` → virtual-entity registry publishes mode + SOC sensors to HA. Unreachable device → sensors show unavailable.

Rationale: the local REST API is request/response with no push, so status is polled. The control select is the source of *user intent*; the device report is the source of truth for *actual status*.

## Configuration (generic, no secrets/setup in repo)

Instance values come from add-on options (`/data/options.json`, surfaced by `run.sh` as Spring properties) or environment (standalone). Typed via `@ConfigurationProperties`, validated fail-fast at startup:

- `device_ip` (required), `device_port` (default = documented zenSDK port), `device_serial` (required)
- `max_charge_power` W (required, >0), `max_discharge_power` W (required, >0)
- `poll_interval` seconds (default), `log_level` (default `info`)
- HA connection/token (Supervisor defaults)

Generic zenSDK constants (paths, property keys, mode values) are code constants, never config.

## Tech stack & deployment

- **Language/build:** Java 25 (Adoptium Temurin), Maven, Spring Boot.
- **HA comms:** `spring-boot-starter-websocket`, Spring `RestClient`, Jackson.
- **Tests:** JUnit + Spring test, ArchUnit (layer rule), fake device doubles (extend `Battery`, no Mockito).
- **Deployment:** multi-stage `Dockerfile` (JDK build → JRE runtime), add-on `config.yaml` (`homeassistant_api: true`, ingress, options/schema), `run.sh` reads `/data/options.json` via `jq`; `docker-compose.yml` for standalone dev. Mirrors ArpadsHomeAutomations.

## Explicitly deferred (not yet in the architecture)

- P1/CT meter reading and threshold- or price-driven automation.
- Persistence (Postgres) — v1 is stateless; the select re-publishes a default on restart and status is re-derived from the device.
- Custom React frontend (planned later, similar to the Gielz dashboard).
- mDNS device auto-discovery (port is configured for now).
- Transports other than local HTTP REST (no MQTT/Bluetooth).

## Open questions

- Default `device_port` for current zenSDK firmware (confirm against a real device; keep as documented default regardless).
- Exact `/properties/report` field names for SOC and mode across models (isolate the mapping in `zendure/` so it is easy to adjust).
- Whether Standby must also set `acMode` (start with zeroed limits per the documented example; verify on device).
