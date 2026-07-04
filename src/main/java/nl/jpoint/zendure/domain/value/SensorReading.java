package nl.jpoint.zendure.domain.value;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * The result of reading a sensor through a device: either an {@link Available} typed value
 * with the instant it was read, or {@link Unavailable} carrying the raw Home Assistant state
 * string for diagnostics.
 *
 * <p>Devices map HA states {@code "unavailable"}, {@code "unknown"}, and unparseable values
 * to {@link Unavailable}. Automations decide the policy per read (skip tick, fall back,
 * default) instead of parsing raw strings.
 */
public sealed interface SensorReading<T> {

    static <T> SensorReading<T> available(T value, Instant at) {
        return new Available<>(value, at);
    }

    static <T> SensorReading<T> unavailable(String raw) {
        return new Unavailable<>(raw);
    }

    boolean isAvailable();

    Optional<T> value();

    T orElse(T fallback);

    record Available<T>(T reading, Instant at) implements SensorReading<T> {

        public Available {
            Objects.requireNonNull(reading, "reading must not be null");
            Objects.requireNonNull(at, "at must not be null");
        }

        @Override
        public boolean isAvailable() {
            return true;
        }

        @Override
        public Optional<T> value() {
            return Optional.of(reading);
        }

        @Override
        public T orElse(T fallback) {
            return reading;
        }
    }

    record Unavailable<T>(String raw) implements SensorReading<T> {

        @Override
        public boolean isAvailable() {
            return false;
        }

        @Override
        public Optional<T> value() {
            return Optional.empty();
        }

        @Override
        public T orElse(T fallback) {
            return fallback;
        }
    }
}
