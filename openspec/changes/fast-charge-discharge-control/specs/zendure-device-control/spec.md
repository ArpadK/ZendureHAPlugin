## ADDED Requirements

### Requirement: Read device state via zenSDK

The system SHALL read the battery's current state from the zenSDK local REST API using `GET /properties/report`, and SHALL expose at least the operating mode and battery state-of-charge to the application. Readings SHALL be represented such that "unavailable" or unreachable states are distinguishable from real values and never surface as a fabricated number.

#### Scenario: Successful report read

- **WHEN** the device responds to `GET /properties/report`
- **THEN** the application parses the response into a typed device reading exposing state-of-charge and mode

#### Scenario: Device unreachable

- **WHEN** the device does not respond or returns an error to a report request
- **THEN** the reading is marked unavailable (with the raw cause retained for diagnostics) and no exception propagates to break the control loop

### Requirement: Command fast charge

The system SHALL command the battery to charge at maximum configured power by writing `{"acMode":1,"inputLimit":<maxChargePower>,"smartMode":1}` to the zenSDK `POST /properties/write` endpoint with the configured serial number.

#### Scenario: Fast charge issued

- **WHEN** the application requests fast charge with a configured max charge power of 2400 W
- **THEN** it POSTs `{"sn":"<serial>","properties":{"acMode":1,"inputLimit":2400,"smartMode":1}}` to `/properties/write`

### Requirement: Command fast discharge

The system SHALL command the battery to discharge at maximum configured power by writing `{"acMode":2,"outputLimit":<maxDischargePower>,"smartMode":1}` to the zenSDK `POST /properties/write` endpoint with the configured serial number.

#### Scenario: Fast discharge issued

- **WHEN** the application requests fast discharge with a configured max discharge power of 2400 W
- **THEN** it POSTs `{"sn":"<serial>","properties":{"acMode":2,"outputLimit":2400,"smartMode":1}}` to `/properties/write`

### Requirement: Command standby

The system SHALL command the battery to stop charging and discharging by writing `{"inputLimit":0,"outputLimit":0,"smartMode":1}` to the zenSDK `POST /properties/write` endpoint with the configured serial number.

#### Scenario: Standby issued

- **WHEN** the application requests standby
- **THEN** it POSTs `{"sn":"<serial>","properties":{"inputLimit":0,"outputLimit":0,"smartMode":1}}` to `/properties/write`

### Requirement: Command failures are reported, not swallowed silently

The system SHALL treat a failed command (non-success HTTP status or unreachable device) as a failure the application can observe and log, rather than reporting success.

#### Scenario: Write rejected by device

- **WHEN** a `POST /properties/write` returns a non-success status or times out
- **THEN** the command is reported as failed and logged with the cause, and the control layer can react (e.g. avoid updating status to a state that was not applied)
