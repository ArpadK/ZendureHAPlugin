## ADDED Requirements

### Requirement: Authenticated connection to Home Assistant

The system SHALL connect to Home Assistant using the Supervisor-provided token, over the WebSocket API for events and the REST API for state access. When running as an add-on it SHALL default to the Supervisor endpoints (`ws://supervisor/core/websocket`, `http://supervisor/core`) and the `SUPERVISOR_TOKEN`, while allowing these to be overridden by configuration for standalone runs.

#### Scenario: Successful authentication

- **WHEN** the application starts with a valid token and reachable Home Assistant endpoints
- **THEN** the WebSocket connection authenticates and the application is ready to publish entities and receive events

#### Scenario: Authentication rejected

- **WHEN** Home Assistant rejects the provided token
- **THEN** the application logs an authentication failure and does not enter the ready state

### Requirement: Automatic reconnection

The system SHALL detect a dropped Home Assistant connection and automatically attempt to reconnect using a bounded backoff delay, resuming normal operation once reconnected.

#### Scenario: Connection lost then restored

- **WHEN** the WebSocket connection to Home Assistant drops
- **THEN** the application retries with increasing delay up to a maximum and re-establishes an authenticated connection when Home Assistant is reachable again

### Requirement: Publish and track plugin-owned entities

The system SHALL publish entities it owns (a control select and status sensors) into Home Assistant, SHALL re-publish their current values after each (re)authentication so they survive restarts and reconnects, and SHALL deliver inbound state changes for owned entities to interested application components.

#### Scenario: Republish after reconnect

- **WHEN** the application re-authenticates to Home Assistant after a disconnect
- **THEN** it republishes the current state of every plugin-owned entity

#### Scenario: Inbound state change delivered

- **WHEN** a user changes the value of a plugin-owned control entity in Home Assistant
- **THEN** the corresponding application component is notified of the new value
