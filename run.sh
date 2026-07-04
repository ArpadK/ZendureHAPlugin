#!/bin/sh
DEVICE_IP=$(jq -r '.device_ip // empty' /data/options.json)
DEVICE_PORT=$(jq -r '.device_port // "13248"' /data/options.json)
DEVICE_SERIAL=$(jq -r '.device_serial // empty' /data/options.json)
MAX_CHARGE_POWER=$(jq -r '.max_charge_power // "2400"' /data/options.json)
MAX_DISCHARGE_POWER=$(jq -r '.max_discharge_power // "2400"' /data/options.json)
POLL_INTERVAL=$(jq -r '.poll_interval // "30"' /data/options.json)
LOG_LEVEL=$(jq -r '.log_level // "info"' /data/options.json)

# Fall back to environment variables if not in options (for standalone dev)
DEVICE_IP=${DEVICE_IP:-${ZENDURE_DEVICE_IP}}
DEVICE_PORT=${DEVICE_PORT:-${ZENDURE_DEVICE_PORT:-13248}}
DEVICE_SERIAL=${DEVICE_SERIAL:-${ZENDURE_DEVICE_SERIAL}}
MAX_CHARGE_POWER=${MAX_CHARGE_POWER:-${ZENDURE_MAX_CHARGE_POWER:-2400}}
MAX_DISCHARGE_POWER=${MAX_DISCHARGE_POWER:-${ZENDURE_MAX_DISCHARGE_POWER:-2400}}
POLL_INTERVAL=${POLL_INTERVAL:-${ZENDURE_POLL_INTERVAL:-30}}
LOG_LEVEL=${LOG_LEVEL:-${ZENDURE_LOG_LEVEL:-info}}

exec java -jar /app/app.jar \
  --zendure.device-ip="${DEVICE_IP}" \
  --zendure.device-port="${DEVICE_PORT}" \
  --zendure.device-serial="${DEVICE_SERIAL}" \
  --zendure.max-charge-power="${MAX_CHARGE_POWER}" \
  --zendure.max-discharge-power="${MAX_DISCHARGE_POWER}" \
  --zendure.poll-interval-seconds="${POLL_INTERVAL}" \
  --logging.level.nl.jpoint.zendure="${LOG_LEVEL}"
