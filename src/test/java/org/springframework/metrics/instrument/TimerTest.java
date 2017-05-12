/**
 * Copyright 2017 Pivotal Software, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.metrics.instrument;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.metrics.instrument.MockClock.clock;

class TimerTest {

    @DisplayName("total time and count are preserved for a single timing")
    @ParameterizedTest
    @ArgumentsSource(MeterRegistriesProvider.class)
    void record(MeterRegistry collector) {
        Timer t = collector.timer("myTimer");
        t.record(42, TimeUnit.MILLISECONDS);

        assertAll(() -> assertEquals(1L, t.count()),
                () -> assertEquals(42, t.totalTime(TimeUnit.MILLISECONDS), 1.0e-12));
    }

    @DisplayName("negative times are discarded by the Timer")
    @ParameterizedTest
    @ArgumentsSource(MeterRegistriesProvider.class)
    void recordNegative(MeterRegistry collector) {
        Timer t = collector.timer("myTimer");
        t.record(-42, TimeUnit.MILLISECONDS);

        assertAll(() -> assertEquals(0L, t.count()),
                () -> assertEquals(0, t.totalTimeNanos(), 1.0e-12));
    }

    @DisplayName("zero times contribute to the count of overall events but do not add to total time")
    @ParameterizedTest
    @ArgumentsSource(MeterRegistriesProvider.class)
    void recordZero(MeterRegistry collector) {
        Timer t = collector.timer("myTimer");
        t.record(0, TimeUnit.MILLISECONDS);

        assertAll(() -> assertEquals(1L, t.count()),
                () -> assertEquals(0L, t.totalTimeNanos()));
    }

    @DisplayName("record a runnable task")
    @ParameterizedTest
    @ArgumentsSource(MeterRegistriesProvider.class)
    void recordWithRunnable(MeterRegistry collector) throws Exception {
        Timer t = collector.timer("myTimer");

        try {
            t.record((Runnable) () -> clock(collector).addAndGetNanos(10));
        } finally {
            assertAll(() -> assertEquals(1L, t.count()),
                    () -> assertEquals(10, t.totalTimeNanos() ,1.0e-12));
        }
    }

    @DisplayName("callable task that throws exception is still recorded")
    @ParameterizedTest
    @ArgumentsSource(MeterRegistriesProvider.class)
    void recordCallableException(MeterRegistry collector) {
        Timer t = collector.timer("myTimer");

        assertThrows(Exception.class, () -> {
            t.record(() -> {
                clock(collector).addAndGetNanos(10);
                throw new Exception("uh oh");
            });
        });

        assertAll(() -> assertEquals(1L, t.count()),
                () -> assertEquals(10, t.totalTimeNanos(), 1.0e-12));
    }
}