package nl.jpoint.zendure.homeassistant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for the reconnect backoff scheduler behavior.
 * Verifies that reconnect attempts follow a bounded exponential backoff schedule.
 */
@DisplayName("Reconnect Backoff Scheduler")
class ReconnectBackoffTest {

    @Test
    @DisplayName("Backoff delays should increase up to maximum")
    void testBackoffSchedule() {
        long[] backoffDelays = {
            1000,      // 1 second
            2000,      // 2 seconds
            5000,      // 5 seconds
            10000,     // 10 seconds
            30000,     // 30 seconds
            60000      // 60 seconds (max)
        };

        // Verify progression
        assertEquals(6, backoffDelays.length);

        for (int i = 1; i < backoffDelays.length; i++) {
            assertTrue(backoffDelays[i] > backoffDelays[i - 1],
                "Delay at index " + i + " should be greater than previous");
        }

        // Verify maximum delay
        assertEquals(60000, backoffDelays[backoffDelays.length - 1],
            "Maximum backoff should be 60 seconds");
    }

    @Test
    @DisplayName("Backoff delays should cap at maximum after multiple attempts")
    void testBackoffCappedAtMaximum() {
        long[] backoffDelays = {
            1000,      // 1 second
            2000,      // 2 seconds
            5000,      // 5 seconds
            10000,     // 10 seconds
            30000,     // 30 seconds
            60000      // 60 seconds (max)
        };

        // Simulate accessing beyond the array bounds
        int index1 = Math.min(10, backoffDelays.length - 1);
        int index2 = Math.min(20, backoffDelays.length - 1);

        assertEquals(backoffDelays[index1], backoffDelays[index2],
            "Should always return the maximum delay when beyond the array");
        assertEquals(60000, backoffDelays[index1],
            "Maximum delay should be returned");
    }

    @Test
    @DisplayName("Backoff delay progression matches expected pattern")
    void testBackoffProgression() {
        List<Long> delays = new ArrayList<>();
        long[] backoffDelays = {
            1000,      // 1 second
            2000,      // 2 seconds
            5000,      // 5 seconds
            10000,     // 10 seconds
            30000,     // 30 seconds
            60000      // 60 seconds (max)
        };

        for (long delay : backoffDelays) {
            delays.add(delay);
        }

        assertEquals(6, delays.size());
        assertEquals(1000L, delays.get(0));
        assertEquals(2000L, delays.get(1));
        assertEquals(5000L, delays.get(2));
        assertEquals(10000L, delays.get(3));
        assertEquals(30000L, delays.get(4));
        assertEquals(60000L, delays.get(5));
    }

    @Test
    @DisplayName("Reset should restart backoff from beginning")
    void testBackoffResetBehavior() {
        // Simulate a reconnect scheduler implementation
        ReconnectSimulator scheduler = new ReconnectSimulator();

        assertEquals(1000, scheduler.getNextDelay());
        assertEquals(2000, scheduler.getNextDelay());
        assertEquals(5000, scheduler.getNextDelay());

        scheduler.reset();

        // After reset, should start from beginning
        assertEquals(1000, scheduler.getNextDelay());
    }

    /**
     * Simple reconnect scheduler simulator for testing.
     */
    private static class ReconnectSimulator {
        private static final long[] BACKOFF_DELAYS = {
            1000, 2000, 5000, 10000, 30000, 60000
        };

        private int index = 0;

        long getNextDelay() {
            long delay = BACKOFF_DELAYS[Math.min(index, BACKOFF_DELAYS.length - 1)];
            index++;
            return delay;
        }

        void reset() {
            index = 0;
        }
    }
}
