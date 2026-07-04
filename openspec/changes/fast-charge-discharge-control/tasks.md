## 1. Project skeleton & build

- [x] 1.1 Create Maven `pom.xml`: Spring Boot parent, Java 25, artifact/coordinates for the Zendure plugin; dependencies `spring-boot-starter-websocket`, `spring-boot-starter-web` (RestClient), `jackson-databind`; test deps `spring-boot-starter-test`, `archunit-junit5`
- [x] 1.2 Create the base package layout mirroring ArpadsHomeAutomations: `automation`, `domain` (`device`, `value`, `event`, `virtualentity`), `homeassistant` (`dto`), `zendure`, `config`
- [x] 1.3 Create `@SpringBootApplication` main class with `@EnableScheduling` and `@EnableConfigurationProperties`
- [x] 1.4 Add `application.yml` with HA connection defaults (Supervisor WS/REST URLs, token from env) and Zendure/poll settings bound from env with placeholders

## 2. Configuration (capability: addon-configuration)

- [x] 2.1 Add typed `@ConfigurationProperties` record for Zendure device config: `deviceIp`, `devicePort` (default = documented zenSDK port), `deviceSerial`, `maxChargePower`, `maxDischargePower`, `pollIntervalSeconds`
- [x] 2.2 Add typed config for HA connection (websocket-url, rest-url, token) with Supervisor defaults, overridable for standalone
- [x] 2.3 Add startup validation (fail-fast) for required settings (`deviceIp`, `deviceSerial`, positive `maxChargePower`/`maxDischargePower`) with clear error messages
- [x] 2.4 Unit test: validation rejects missing `deviceIp` and non-positive power limits; defaults applied when `devicePort` omitted

## 3. Home Assistant connection (capability: home-assistant-connection)

- [x] 3.1 Implement `HomeAssistantClient` WebSocket handler: connect on `ApplicationReadyEvent`, auth handshake with token, message-id sequencing, publish an authenticated event on success
- [x] 3.2 Implement automatic reconnect with bounded backoff
- [x] 3.3 Implement `HomeAssistantRestClient` (Spring `RestClient`) for reading/writing entity state (`/api/states/...`)
- [x] 3.4 Implement inbound `state_changed` dispatch: parse events, filter to watched/owned entity IDs, publish an application event
- [x] 3.5 Unit test: auth handshake message shape; reconnect backoff schedule

## 4. zenSDK device control (capability: zendure-device-control)

- [x] 4.1 Add `SensorReading<T>` sealed value type (Available | Unavailable) in `domain.value` (mirror ArpadsHomeAutomations)
- [x] 4.2 Implement `ZendureRestClient` in `zendure`: `GET /properties/report` and `POST /properties/write` with body `{"sn":..,"properties":{..}}`, `Content-Type: application/json`, base URL from config (`http://<ip>:<port>`)
- [ ] 4.3 Define zenSDK protocol constants (paths, property keys `acMode`/`inputLimit`/`outputLimit`/`smartMode`, mode values 1/2) as code constants
- [ ] 4.4 Implement `Battery` device abstraction in `domain.device`: `fastCharge()`, `fastDischarge()`, `standby()` building the documented property payloads; `stateOfCharge()` and `mode()` reads returning `SensorReading<T>` parsed from `/properties/report`
- [ ] 4.5 Map command failures (non-2xx / timeout / unreachable) to an observable failure result + logging; reads map failures to `Unavailable` without throwing
- [ ] 4.6 Unit tests with a fake/stubbed REST transport: assert exact write payloads for fast charge / fast discharge / standby; report parsing into SOC + mode; unreachable → `Unavailable`; failed write → failure result

## 5. Owned entities & mode control (capabilities: home-assistant-connection, battery-mode-control)

- [x] 5.1 Implement a minimal virtual-entity registry: register an owned select and owned sensors, publish their state to HA, republish on (re)auth, deliver inbound select changes to listeners
- [x] 5.2 Register the control select with options Fast Charge / Fast Discharge / Standby (default Standby); register status sensors for mode and SOC
- [x] 5.3 Implement the mode-control automation: on select change, invoke the matching `Battery` command; on command failure, log and do not optimistically update status
- [x] 5.4 Implement scheduled poller (`@Scheduled`, interval from config) that reads the device report and publishes mode + SOC status sensors; unavailable device → sensors reflect unavailable
- [x] 5.5 Unit tests with a fake `Battery`: selecting Fast Charge/Discharge/Standby triggers the correct command; failed command does not update status; poller publishes readings and handles unavailable

## 6. Architecture enforcement & tests

- [ ] 6.1 Add ArchUnit test: `automation` must not depend on `homeassistant`/`zendure` infrastructure packages; `domain` must not depend on infrastructure
- [ ] 6.2 Ensure `mvn test` runs green (unit + ArchUnit)

## 7. Deployment (HA add-on, mirror ArpadsHomeAutomations)

- [ ] 7.1 Create `Dockerfile` (multi-stage: Temurin JDK build → JRE runtime), exposing the ingress port
- [ ] 7.2 Create add-on `config.yaml`: name/slug/version, `homeassistant_api: true`, ingress, and `options`/`schema` for `device_ip`, `device_port`, `device_serial`, `max_charge_power`, `max_discharge_power`, `poll_interval`, `log_level` (no setup-specific defaults committed)
- [ ] 7.3 Create `run.sh` reading `/data/options.json` (via `jq`) and passing values as Spring properties/env; fall back to env for standalone
- [ ] 7.4 Create `docker-compose.yml` for standalone local dev (env-driven, host network)
- [ ] 7.5 Write add-on README: enabling the zenSDK local API on the device, finding IP/port/serial, configuring the add-on, and that it replaces the Gielz setup

## 8. Docs & project hygiene

- [ ] 8.1 Update project `CLAUDE.md` Build & Test section with the now-known Maven commands (build, test, single test)
