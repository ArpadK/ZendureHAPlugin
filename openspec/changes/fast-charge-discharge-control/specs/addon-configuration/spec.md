## ADDED Requirements

### Requirement: Runtime configuration from add-on options

The add-on SHALL load all instance-specific settings at startup from the Home Assistant add-on options (`/data/options.json`) or environment variables, without requiring a rebuild. The settings SHALL include the device IP address, device HTTP port, device serial number (`sn`), maximum charge power (watts), maximum discharge power (watts), and log level.

#### Scenario: Options provided by add-on

- **WHEN** the add-on starts with `device_ip`, `device_serial`, `max_charge_power`, and `max_discharge_power` set in `/data/options.json`
- **THEN** the application binds those values into its typed configuration and uses them for all device communication and command power limits

#### Scenario: Standalone environment overrides

- **WHEN** the application is run outside the add-on (e.g. local dev) with the same settings supplied as environment variables
- **THEN** the application starts with the same behavior as when read from `/data/options.json`

### Requirement: Sensible defaults for optional settings

The system SHALL apply documented default values for optional settings so that only truly instance-specific values are mandatory. The device HTTP port SHALL default to the documented zenSDK local API port, and the log level SHALL default to `info`.

#### Scenario: Port omitted

- **WHEN** no `device_port` is configured
- **THEN** the application uses the default zenSDK HTTP port rather than failing

### Requirement: Validation of required settings

The system SHALL validate that mandatory instance settings (device IP, serial number, and both power limits) are present and well-formed at startup, and SHALL fail fast with a clear error message when any is missing or invalid.

#### Scenario: Missing required setting

- **WHEN** the application starts without a configured `device_ip`
- **THEN** startup fails with an error message naming the missing `device_ip` setting

#### Scenario: Invalid power limit

- **WHEN** a power limit is configured as a non-positive or non-numeric value
- **THEN** startup fails with an error message identifying the invalid setting

### Requirement: No setup-specific values in the repository

The codebase SHALL NOT contain any hardcoded value specific to a single user's setup (device IP, serial number, tokens, or personal entity IDs). All such values SHALL come from configuration at runtime, so the same build is deployable on any Home Assistant instance with any supported Zendure Solarflow battery.

#### Scenario: Fresh install on a different setup

- **WHEN** the same add-on image is installed on a different Home Assistant instance with a different Zendure battery and provided that instance's configuration
- **THEN** it operates correctly without any code change
