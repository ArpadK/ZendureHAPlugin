## Why

Controlling a Zendure Solarflow battery today relies on the third-party `Gielz1986/Zendure-HA-zenSDK` project: a bundle of Home Assistant YAML packages, `rest_command` definitions, helper entities, automations, and a dashboard that a user must copy into their HA config. It works, but it is fragile (config lives in the user's HA, upgrades are manual copy/paste), setup-specific, and hard to evolve. We want a self-contained, installable Home Assistant add-on — deployed exactly like the ArpadsHomeAutomations project — that talks to the battery directly over the documented zenSDK local REST API, so a user can install one add-on and get local battery control with zero YAML and zero dependency on Gielz.

This first change establishes the foundation and the smallest useful behavior: **manual fast charge / fast discharge / standby**. Smart, meter-driven automation (P1/CT thresholds, price optimization) and a custom UI are explicitly deferred to later changes.

## What Changes

- **New Home Assistant add-on** (Java 25 / Spring Boot / Maven), packaged and deployed as an ingress-enabled HA add-on, mirroring the ArpadsHomeAutomations deployment model (Dockerfile, `config.yaml`, `run.sh` reading `/data/options.json`).
- **Direct zenSDK local control**: the add-on issues `POST /properties/write` and `GET /properties/report` to the battery over the local network, replacing every Gielz `rest_command`.
- **One plugin-owned control entity** published into Home Assistant: a select with options **Fast Charge**, **Fast Discharge**, **Standby**. Choosing an option commands the battery:
  - Fast Charge → `{acMode:1, inputLimit:<maxChargeW>, smartMode:1}`
  - Fast Discharge → `{acMode:2, outputLimit:<maxDischargeW>, smartMode:1}`
  - Standby → `{inputLimit:0, outputLimit:0, smartMode:1}`
- **Read-only status sensors** mirrored from the device (at minimum current mode and battery state-of-charge), published as plugin-owned entities.
- **Fully generic, config-driven**: device IP, HTTP port, serial number (`sn`), max charge watts, and max discharge watts are add-on configuration variables read at runtime. No user-, device-, or setup-specific value is committed to the repository. The plugin must run on any HA instance with any supported Zendure Solarflow battery.
- **Layered architecture from day one**: `automation` → `domain` → infrastructure (`homeassistant`, `zendure`), with the layer dependency rule enforced by ArchUnit and unit tests using fake device doubles.
- **Not in this change** (deferred): P1/CT meter integration, threshold- or price-based automation, persistence (Postgres), and a custom React frontend.

## Capabilities

### New Capabilities
- `addon-configuration`: Load all instance-specific settings (device IP, port, serial, max charge/discharge power, HA connection) from add-on options / environment at runtime, with validation and sensible defaults; guarantee no setup-specific values are hardcoded.
- `home-assistant-connection`: Establish and maintain an authenticated connection to Home Assistant (WebSocket for events + REST for state), with reconnect; publish plugin-owned entities and receive their state changes.
- `zendure-device-control`: A zenSDK local REST client and a Battery device abstraction that reads device properties (`/properties/report`) and commands charge / discharge / standby (`/properties/write`), translating domain intent to zenSDK property payloads.
- `battery-mode-control`: Expose the user-facing Fast Charge / Fast Discharge / Standby control select and read-only status sensors in Home Assistant, and translate a user's mode selection into the corresponding device command.

### Modified Capabilities
<!-- None — this is the foundational change; no existing specs. -->

## Impact

- **New codebase**: build system (Maven `pom.xml`, JDK 25), Spring Boot application, package layout (`automation`, `domain`, `homeassistant`, `zendure`, `config`), and deployment assets (`Dockerfile`, `config.yaml`, `run.sh`, `docker-compose.yml` for standalone dev).
- **External API dependency**: the Zendure zenSDK local REST API (`/properties/write`, `/properties/report`); requires the device's local API to be enabled and reachable on the LAN.
- **Runtime config surface**: new add-on options (`device_ip`, `device_port`, `device_serial`, `max_charge_power`, `max_discharge_power`, HA connection/token, log level).
- **User migration**: users move off the Gielz YAML/automations/dashboard; this change replaces the charge/discharge/standby control path (meter-driven behavior and dashboard parity come in later changes).
- **Testing**: unit tests with fake device doubles; ArchUnit layer-dependency test. (HA/device integration tests deferred but the architecture leaves room for them.)
