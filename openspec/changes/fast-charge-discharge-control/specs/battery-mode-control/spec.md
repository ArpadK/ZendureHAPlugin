## ADDED Requirements

### Requirement: User-facing control select

The system SHALL publish into Home Assistant a plugin-owned select entity offering exactly the options **Fast Charge**, **Fast Discharge**, and **Standby**, allowing a user (or a user's own automation) to choose the battery's operating intent from the Home Assistant UI.

#### Scenario: Control select available after startup

- **WHEN** the add-on has started and connected to Home Assistant
- **THEN** a select entity with options Fast Charge, Fast Discharge, and Standby is present in Home Assistant

### Requirement: Selection translates to a device command

The system SHALL, when the control select changes value, issue the corresponding zenSDK command: Fast Charge → fast charge, Fast Discharge → fast discharge, Standby → standby.

#### Scenario: User selects Fast Charge

- **WHEN** the user sets the control select to Fast Charge
- **THEN** the system commands the battery to fast charge at the configured maximum charge power

#### Scenario: User selects Standby

- **WHEN** the user sets the control select to Standby
- **THEN** the system commands the battery to stop charging and discharging

#### Scenario: Command fails

- **WHEN** the user selects a mode but the device command fails
- **THEN** the failure is logged and the status is not reported as if the mode had been successfully applied

### Requirement: Read-only status sensors

The system SHALL publish read-only status sensors reflecting the battery's actual state as read from the device — at minimum the current operating mode and the battery state-of-charge — and SHALL refresh them periodically.

#### Scenario: Status reflects device state

- **WHEN** the device reports a state-of-charge and operating mode
- **THEN** the corresponding Home Assistant status sensors show those values

#### Scenario: Device unavailable

- **WHEN** the device cannot be reached during a refresh
- **THEN** the status sensors reflect an unavailable state rather than a stale or fabricated value
