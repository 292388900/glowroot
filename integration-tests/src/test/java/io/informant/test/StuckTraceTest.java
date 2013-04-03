/**
 * Copyright 2011-2013 the original author or authors.
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
package io.informant.test;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import checkers.nullness.quals.Nullable;
import com.google.common.base.Stopwatch;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import io.informant.Containers;
import io.informant.container.AppUnderTest;
import io.informant.container.Container;
import io.informant.container.TraceMarker;
import io.informant.container.config.GeneralConfig;
import io.informant.container.trace.Trace;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.fest.assertions.api.Assertions.assertThat;

/**
 * @author Trask Stalnaker
 * @since 0.5
 */
public class StuckTraceTest {

    private static Container container;

    @BeforeClass
    public static void setUp() throws Exception {
        container = Containers.create();
        // capture one trace to warm up the system since this test involves some timing
        container.getConfigService().setStoreThresholdMillis(0);
        container.executeAppUnderTest(WarmupTrace.class);
    }

    @AfterClass
    public static void tearDown() throws Exception {
        container.close();
    }

    @After
    public void afterEachTest() throws Exception {
        container.checkAndReset();
    }

    @Test
    public void shouldReadActiveStuckTrace() throws Exception {
        // given
        GeneralConfig generalConfig = container.getConfigService().getGeneralConfig();
        generalConfig.setStoreThresholdMillis(0);
        generalConfig.setStuckThresholdSeconds(0);
        container.getConfigService().updateGeneralConfig(generalConfig);
        // when
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        Future<Void> future = executorService.submit(new Callable<Void>() {
            @Nullable
            public Void call() throws Exception {
                container.executeAppUnderTest(ShouldGenerateStuckTrace.class);
                return null;
            }
        });
        // then
        // wait for trace to be marked stuck
        Stopwatch stopwatch = new Stopwatch().start();
        Trace trace = null;
        while (stopwatch.elapsed(SECONDS) < 2) {
            trace = container.getTraceService().getActiveTraceSummary(0, MILLISECONDS);
            if (trace != null && trace.isStuck()) {
                break;
            }
            Thread.sleep(10);
        }
        assertThat(trace).isNotNull();
        assertThat(trace.isStuck()).isTrue();
        assertThat(trace.isActive()).isTrue();
        assertThat(trace.isCompleted()).isFalse();
        // interrupt trace
        container.interruptAppUnderTest();
        future.get();
        // should now be reported as unstuck
        trace = container.getTraceService().getLastTraceSummary();
        assertThat(trace.isStuck()).isFalse();
        assertThat(trace.isCompleted()).isTrue();
        // cleanup
        executorService.shutdown();
    }

    public static class ShouldGenerateStuckTrace implements AppUnderTest, TraceMarker {
        public void executeApp() throws InterruptedException {
            traceMarker();
        }
        public void traceMarker() throws InterruptedException {
            while (true) {
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    return;
                }
            }
        }
    }

    public static class WarmupTrace implements AppUnderTest, TraceMarker {
        public void executeApp() throws InterruptedException {
            traceMarker();
        }
        public void traceMarker() throws InterruptedException {
            Thread.sleep(1);
        }
    }
}
