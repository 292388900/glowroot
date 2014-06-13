/*
 * Copyright 2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.glowroot.trace.model;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;

import org.glowroot.api.TraceMetricName;
import org.glowroot.common.Ticker;

import static java.util.concurrent.TimeUnit.NANOSECONDS;
import static org.openjdk.jmh.annotations.Mode.AverageTime;
import static org.openjdk.jmh.annotations.Scope.Thread;

/**
 * @author Trask Stalnaker
 * @since 0.5
 */
@BenchmarkMode(AverageTime)
@OutputTimeUnit(NANOSECONDS)
@State(Thread)
public class TraceMetricBenchmark {

    private TraceMetric traceMetric;
    private TraceMetricName nestedTraceMetricName;

    @Setup
    public void setup() {
        traceMetric = new TraceMetric(new TraceMetricNameImpl("micro trace"), null,
                new CurrentTraceMetricHolder(), new NopTicker());
        nestedTraceMetricName = new TraceMetricNameImpl("nested trace metric");
    }

    @Benchmark
    public void measureTraceMetric() {
        TraceMetric timer = traceMetric.startNestedTraceMetric(nestedTraceMetricName);
        timer.stop();
    }

    private static class NopTicker extends Ticker {
        @Override
        public long read() {
            return 0;
        }
    }
}
