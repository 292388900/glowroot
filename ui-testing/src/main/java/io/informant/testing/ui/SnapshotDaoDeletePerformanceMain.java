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
package io.informant.testing.ui;

import java.io.File;

import com.google.common.base.Stopwatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.informant.Containers;
import io.informant.container.AppUnderTest;
import io.informant.container.Container;
import io.informant.container.config.CoarseProfilingConfig;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

/**
 * @author Trask Stalnaker
 * @since 0.5
 */
public class SnapshotDaoDeletePerformanceMain {

    private static final Logger logger =
            LoggerFactory.getLogger(SnapshotDaoDeletePerformanceMain.class);

    private SnapshotDaoDeletePerformanceMain() {}

    public static void main(String... args) throws Exception {
        Container container = Containers.create();
        // set thresholds low so there will be lots of data to view
        container.getConfigService().setStoreThresholdMillis(0);
        CoarseProfilingConfig profilingConfig = container.getConfigService()
                .getCoarseProfilingConfig();
        profilingConfig.setInitialDelayMillis(100);
        profilingConfig.setIntervalMillis(10);
        container.getConfigService().updateCoarseProfilingConfig(profilingConfig);
        container.executeAppUnderTest(GenerateTraces.class);
        int pendingWrites = container.getTraceService().getNumPendingCompleteTraces();
        while (pendingWrites > 0) {
            logger.info("pending trace writes: {}", pendingWrites);
            Thread.sleep(1000);
            pendingWrites = container.getTraceService().getNumPendingCompleteTraces();
        }
        File dbFile = new File("informant.h2.db");
        long dbSize = dbFile.length();
        logger.info("informant.h2.db: {} bytes", dbSize);
        Stopwatch stopwatch = new Stopwatch().start();
        container.checkAndReset();
        logger.info("all traces deleted in: {} millis", stopwatch.elapsed(MILLISECONDS));
        logger.info("informant.h2.db: {} bytes", dbFile.length());
        container.close();
        logger.info("informant.h2.db: {} bytes", dbFile.length());
    }

    private static class GenerateTraces implements AppUnderTest {
        public void executeApp() throws InterruptedException {
            File rollingFile = new File("informant.rolling.db");
            while (rollingFile.length() < 100 * 1024 * 1024) {
                new NestableCall(new NestableCall(10, 2, 5000), 20, 2, 5000).execute();
                logger.info("rolling file: {} mb", rollingFile.length() / (1024 * 1024));
            }
        }
    }
}
