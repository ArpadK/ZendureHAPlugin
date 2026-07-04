# Zendure Battery Control Add-on

A Home Assistant add-on for controlling Zendure Solarflow batteries via the local zenSDK HTTP API. This add-on replaces the Gielz YAML-based integration with a self-contained, typed Java/Spring Boot service that exposes a control select (Fast Charge / Fast Discharge / Standby) and read-only status sensors for mode and state-of-charge.

## Features

- **Direct Device Control**: Commands sent directly to the battery via the local zenSDK REST API — no reliance on third-party HA integrations.
- **Three Control Modes**: Fast Charge, Fast Discharge, or Standby, selectable via a Home Assistant select entity.
- **Live Status Monitoring**: Polls the device for current operating mode and state-of-charge, displayed as read-only status sensors.
- **Configurable Power Limits**: Set maximum charge and discharge power per your battery and inverter specs.
- **Flexible Polling**: Adjust the device status polling interval to balance freshness and network load.
- **Local API Only**: Operates entirely on your LAN — no cloud dependency or authentication required.

## Prerequisites

### 1. Enable Local API on the Zendure Device

The Zendure Solarflow must have the local API enabled. This is configured in the official Zendure app (iOS/Android) or the web interface **before** adding this add-on.

**Steps to enable zenSDK local API:**

1. Open the **Zendure App** or access the device's web interface (if available on your network).
2. Navigate to **Settings** → **Smart Mode** or **Local Control** (name varies by firmware version).
3. Enable **Local API** or **HEMS Control** (sometimes called "Enable HTTP local API").
4. The API will then be accessible on the device's LAN IP at the configured port (default: `13248`).

**Note:** Consult the [official Zendure zenSDK documentation](https://github.com/Zendure/zenSDK) for the latest setup instructions if the above steps do not match your device's UI.

### 2. Determine Device IP, Port, and Serial Number

You will need three pieces of information to configure this add-on:

#### Finding the Device IP Address

- **Via Router**: Log into your router's admin interface and look for the device's hostname (often "Zendure-XXXXX") and its assigned IP address.
- **Via mDNS**: From a machine on the same network, try `ping zendure.local` or `nslookup zendure.local` (if the device advertises mDNS; not all firmware versions do).
- **Via Zendure App**: Some app versions show the device IP under Device → Settings.
- **Manual Discovery**: If your Home Assistant is on the same subnet, try a network scan tool (e.g., `arp-scan`, `nmap`) to find the device, then verify by attempting `curl http://<ip>:13248/` — a successful response (even a 404) means the API is reachable.

#### Finding the Device Port

The default zenSDK local API port is **13248**. Some firmware versions or custom setups may use a different port. If you know it was changed, use that port. Otherwise, start with 13248.

#### Finding the Serial Number

The serial number (format: typically `48xxxxxxxxxx` or similar) is printed on the device or visible in:
- The **Zendure App** under Device Information or Settings.
- The device's web interface (if accessible).
- A sticker or label on the physical device.

### 3. Home Assistant Add-on Manager

Ensure your Home Assistant instance is running and you have access to the add-on store or configuration.

## Installation

### Option A: Install via HACS (Recommended)

[HACS](https://hacs.xyz/) is the Home Assistant Community Store, a package manager for Home Assistant add-ons, integrations, and scripts.

1. **Install HACS** (if not already installed):
   - Visit [HACS Installation](https://hacs.xyz/docs/setup/prerequisites) for detailed instructions.
   - Briefly: add the HACS integration via **Settings** → **Devices & Services** → **Create Automation** → **HACS**.

2. **Add the Repository**:
   - In Home Assistant, open **HACS** → **⋮ (menu)** → **Custom repositories**.
   - Add the repository URL: `https://github.com/arpadkolkert/ZendureHAPlugin`
   - Select **Category**: `Add-ons`
   - Click **Create**.

3. **Install the Add-on**:
   - In **HACS**, go to **Add-ons** and search for "Zendure Battery Control" or "ZendureHAPlugin".
   - Click the entry, then **Install**.
   - Wait for the download and installation to complete.

4. **Configure the Add-on** (see below).

### Option B: Install via Add-on Store (Manual Repository)

1. **Add the Repository**:
   - In Home Assistant, go to **Settings** → **Add-ons** → **Add-on Store** → **⋮ (menu)** → **Repositories**.
   - Add the repository URL: `https://github.com/arpadkolkert/ZendureHAPlugin`
   - Click **Create**.
   - Refresh the store.

2. **Install the Add-on**:
   - Search for "Zendure Battery Control" in the add-on store.
   - Click **Install**.
   - Wait for the build and installation to complete.

3. **Configure the Add-on** (see below).

### Configuration

Once installed (via HACS or the add-on store):

1. Click **Configuration** on the add-on page.
2. Fill in the required fields:
   - **Device IP** *(required)*: The LAN IP address of your Zendure device.
   - **Device Port**: The port the zenSDK API listens on (default: `13248`).
   - **Device Serial** *(required)*: The device's serial number (e.g., `48xxxxxxxxxx`).
   - **Max Charge Power** (W): Maximum power for fast charging (default: `2400`).
   - **Max Discharge Power** (W): Maximum power for fast discharging (default: `2400`).
   - **Poll Interval** (seconds): How often to refresh device status (default: `30`).
   - **Log Level**: `debug`, `info` (default), `warn`, or `error`.
3. Click **Save**.

### Start the Add-on

1. Click **Start** on the add-on page.
2. Observe the logs to ensure a successful startup. You should see messages like:
   - "Home Assistant authenticated"
   - "Publishing virtual entities"
   - "Battery status poller started"

## Configuration Reference

All configuration is done through the Home Assistant add-on options interface. Below is the mapping of UI fields to internal property names:

| UI Field | Property Name | Type | Default | Notes |
|----------|---------------|------|---------|-------|
| Device IP | `zendure.device-ip` | String | *(required)* | LAN IP of the battery |
| Device Port | `zendure.device-port` | Integer | `13248` | zenSDK local API port |
| Device Serial | `zendure.device-serial` | String | *(required)* | Device serial number |
| Max Charge Power | `zendure.max-charge-power` | Integer | `2400` | Watts; must be positive |
| Max Discharge Power | `zendure.max-discharge-power` | Integer | `2400` | Watts; must be positive |
| Poll Interval | `zendure.poll-interval-seconds` | Integer | `30` | Seconds between device reads |
| Log Level | `logging.level.nl.jpoint.zendure` | String | `info` | `debug`, `info`, `warn`, `error` |

## Home Assistant Entities

Once the add-on is running and connected to your Home Assistant instance, the following entities are automatically created and available in the Home Assistant UI and automations.

### Summary

| Entity ID | Type | Mode | Description |
|-----------|------|------|-------------|
| `select.zendure_battery_mode` | Select | Read/Write | Battery operating mode control |
| `sensor.zendure_battery_mode_status` | Sensor | Read-only | Current device operating mode |
| `sensor.zendure_battery_soc` | Sensor | Read-only | Battery state-of-charge (%) |

### Detailed Entity Reference

#### Control Entity

**`select.zendure_battery_mode`**
- **Type**: Select (writable)
- **Options**:
  - `Fast Charge` — Charge at maximum configured power
  - `Fast Discharge` — Discharge at maximum configured power
  - `Standby` — Stop charging and discharging
- **Default**: `Standby`
- **Update Behavior**: Selection is published to Home Assistant immediately and sent to the device within 1 second.
- **Usage in Automations**:
  ```yaml
  - service: select.select_option
    target:
      entity_id: select.zendure_battery_mode
    data:
      option: "Fast Charge"
  ```
- **Accessible via**: Automations, scripts, Lovelace UI, history stats, and any Home Assistant service that works with select entities.

#### Status Entities

**`sensor.zendure_battery_mode_status`**
- **Type**: Sensor (read-only)
- **Description**: Reflects the device's current operating mode as reported by the battery.
- **Possible States**:
  - `fast_charge` — Device is charging at max power
  - `fast_discharge` — Device is discharging at max power
  - `standby` — Device is idle (not charging or discharging)
  - `unknown` — Device reported an unrecognized mode value
  - `unavailable` — Device is unreachable (check network and configuration)
- **Update Frequency**: Every `poll_interval` seconds (default: 30 s).
- **Note**: This sensor reflects *actual device state*, not the user's selection. If the user selects "Fast Charge" but the device is already fully charged, the device may reject the command and stay in standby; this sensor will show the true state.

**`sensor.zendure_battery_soc`**
- **Type**: Sensor (read-only)
- **Unit**: Percentage (%)
- **Range**: 0–100
- **Description**: Battery state-of-charge reported by the device.
- **Update Frequency**: Every `poll_interval` seconds (default: 30 s).
- **State**: `unavailable` if the device cannot be reached or if the value is missing from the device report.
- **Usage in Automations**:
  ```yaml
  - alias: Stop charging when battery is full
    trigger:
      platform: numeric_state
      entity_id: sensor.zendure_battery_soc
      above: 95
    action:
      service: select.select_option
      target:
        entity_id: select.zendure_battery_mode
      data:
        option: "Standby"
  ```
- **Accessible via**: History statistics, automations, templates, Lovelace gauges, and any Home Assistant service or card that works with numeric sensors.

### Using Entities in Home Assistant

#### In Automations

```yaml
# Example: Charge during cheap electricity hours
- alias: Charge during off-peak
  trigger:
    platform: time
    at: "22:00:00"
  action:
    service: select.select_option
    target:
      entity_id: select.zendure_battery_mode
    data:
      option: "Fast Charge"

# Example: Discharge when price is high
- alias: Discharge during peak
  trigger:
    platform: time
    at: "18:00:00"
  condition:
    condition: numeric_state
    entity_id: sensor.zendure_battery_soc
    above: 50
  action:
    service: select.select_option
    target:
      entity_id: select.zendure_battery_mode
    data:
      option: "Fast Discharge"
```

#### In Lovelace Dashboard

```yaml
# Example card to display and control battery
type: vertical-stack
cards:
  - type: entities
    entities:
      - entity: select.zendure_battery_mode
        name: Battery Mode
      - entity: sensor.zendure_battery_soc
        name: Battery Charge
      - entity: sensor.zendure_battery_mode_status
        name: Device Status
  
  - type: gauge
    entity: sensor.zendure_battery_soc
    min: 0
    max: 100
    needle: true
    segments:
      - from: 0
        color: red
      - from: 20
        color: orange
      - from: 50
        color: yellow
      - from: 80
        color: green
```

#### In Templates

```yaml
# Example template sensor for battery status
- sensor:
    - name: Battery State Description
      unique_id: battery_state_description
      state: >
        {% set mode = states('select.zendure_battery_mode') %}
        {% set soc = states('sensor.zendure_battery_soc') | int(0) %}
        Mode: {{ mode }} | SOC: {{ soc }}%
```

## Troubleshooting

### Add-on fails to start

1. **Check the add-on logs**: In Home Assistant, go to Settings → Add-ons → Zendure Battery Control → Logs.
2. **Verify required configuration**: Ensure `device_ip` and `device_serial` are set (no empty fields).
3. **Confirm device reachability**: From a machine on your LAN, run:
   ```bash
   curl http://<device_ip>:13248/
   ```
   If this times out or returns "Connection refused," the device is not reachable. Check:
   - Device IP is correct.
   - Device is powered on and connected to your network.
   - Local API is enabled on the device (see **Prerequisites** above).
   - No firewall rule is blocking port 13248.

### Add-on starts but device shows "unavailable"

1. **Verify the device IP and port** by testing with `curl` (see above).
2. **Check if the device has rebooted**: RAM-only writes (used for command control) revert after a reboot. This is expected behavior in v1. Re-select the desired mode in Home Assistant to re-assert it.
3. **Check the logs** for errors parsing `/properties/report`.

### Device shows "unavailable" but curl works

1. The add-on may have a timeout or network routing issue. Check the logs for more details.
2. Try increasing the `poll_interval` to reduce request frequency and allow more time for each request.
3. Restart the add-on.

### Status does not update frequently enough

1. **Reduce the `poll_interval`** (minimum: `1` second; default: `30` seconds). Note that faster polling increases network load.
2. **Check network latency**: If your device is far from your Home Assistant host or on a weak WiFi connection, increase the interval or improve connectivity.

### Command sent but device did not change

1. **Verify the command in the logs**: Look for `INFO` or `DEBUG` level logs showing the command payload.
2. **Confirm device still has local API enabled**: Some firmware updates or device reboots can reset this setting.
3. **Check device state of charge**: Some devices may reject "fast charge" if already fully charged, or "fast discharge" if already empty. This is expected device behavior.
4. **Verify power limits**: If you set very low `max_charge_power` or `max_discharge_power`, the device may ignore the command if it is below its minimum operating threshold.

## Standalone / Local Development

For development or standalone deployment (outside Home Assistant), you can use the provided `docker-compose.yml`:

```bash
export HA_TOKEN=<your_home_assistant_token>
export ZENDURE_DEVICE_IP=192.168.1.100
export ZENDURE_DEVICE_SERIAL=48xxxxxxxxxx
export ZENDURE_DEVICE_PORT=13248
export ZENDURE_MAX_CHARGE_POWER=2400
export ZENDURE_MAX_DISCHARGE_POWER=2400
export ZENDURE_POLL_INTERVAL=30
export ZENDURE_LOG_LEVEL=info

docker-compose up
```

The add-on will connect to your Home Assistant instance using the WebSocket and REST APIs, with all settings driven by environment variables.

## zenSDK Protocol Reference

This add-on communicates with the Zendure device using the official zenSDK protocol. Key endpoints and properties:

### Endpoints

- **Report**: `GET http://<device_ip>:<device_port>/properties/report`
  - Returns all device properties as JSON, including `acMode` and `stateOfCharge`.

- **Write**: `POST http://<device_ip>:<device_port>/properties/write`
  - Writes properties. Payload format:
    ```json
    {
      "sn": "<device_serial>",
      "properties": {
        "acMode": 1,
        "inputLimit": 2400,
        "outputLimit": 0,
        "smartMode": 1
      }
    }
    ```

### Property Reference

| Property | Type | Values | Purpose |
|----------|------|--------|---------|
| `acMode` | Integer | `1` = Charge, `2` = Discharge | Set operating mode (ignored if both limits are 0) |
| `inputLimit` | Integer | 0–max (W) | Max charge power; 0 = disabled |
| `outputLimit` | Integer | 0–max (W) | Max discharge power; 0 = disabled |
| `smartMode` | Integer | `0` = Flash, `1` = RAM | Storage target; 1 avoids flash wear (use for manual control) |
| `stateOfCharge` | Integer | 0–100 | Battery charge percentage (read-only) |

### Command Examples

**Fast Charge at 2400 W:**
```json
POST /properties/write
{
  "sn": "48xxxxxxxxxx",
  "properties": {
    "acMode": 1,
    "inputLimit": 2400,
    "smartMode": 1
  }
}
```

**Fast Discharge at 2400 W:**
```json
POST /properties/write
{
  "sn": "48xxxxxxxxxx",
  "properties": {
    "acMode": 2,
    "outputLimit": 2400,
    "smartMode": 1
  }
}
```

**Standby (stop charging and discharging):**
```json
POST /properties/write
{
  "sn": "48xxxxxxxxxx",
  "properties": {
    "inputLimit": 0,
    "outputLimit": 0,
    "smartMode": 1
  }
}
```

## Replacing the Gielz Integration

If you previously used the Gielz YAML-based Zendure integration:

1. **Disable the Gielz add-on** in Home Assistant (Settings → Add-ons → Gielz Zendure → Disable).
2. **Remove the Gielz automation helpers** you created (Automations & scenes → Automations → delete any Gielz-related automation).
3. **Install this add-on** and configure it as described above.
4. **Update your automations**: Any automations that called the Gielz service can now use the new select entity (`select.zendure_battery_mode`) directly:
   ```yaml
   - alias: Charge at cheap hours
     trigger: ...
     action:
       - service: select.select_option
         target:
           entity_id: select.zendure_battery_mode
         data:
           option: "Fast Charge"
   ```

This add-on is fully generic and compatible with any Home Assistant instance; no setup-specific values are hardcoded.

## License & Support

For issues, feature requests, or contributions, please visit the [project repository](https://github.com/arpadkolkert/ZendureHAPlugin).

## Changelog

### 0.1.0 (Initial Release)
- Basic fast charge / fast discharge / standby control.
- Device status polling (mode + state-of-charge).
- Home Assistant select entity and read-only sensors.
- Configurable power limits and polling interval.
- Local API only (no cloud).
