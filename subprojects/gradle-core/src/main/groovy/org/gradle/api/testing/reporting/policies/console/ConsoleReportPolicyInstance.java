/*
 * Copyright 2009 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gradle.api.testing.reporting.policies.console;

import org.gradle.api.testing.reporting.policies.ReportPolicyInstance;
import org.gradle.api.testing.reporting.ReportInfo;
import org.gradle.api.testing.reporting.Report;
import org.gradle.api.testing.reporting.TestClassProcessResultReportInfo;
import org.gradle.api.testing.execution.Pipeline;
import org.gradle.api.testing.fabric.*;
import org.gradle.util.queues.AbstractBlockingQueueItemConsumer;
import org.gradle.util.ThreadUtils;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ExecutorService;
import java.util.*;

/**
 * @author Tom Eyckmans
 */
public class ConsoleReportPolicyInstance implements ReportPolicyInstance {

    private static Logger logger = LoggerFactory.getLogger(ConsoleReportPolicyInstance.class);

    private Report report;

    private ExecutorService reportThreadPool;
    private List<ReportInfoQueueItemConsumer> consumers;

    private Map<TestMethodProcessResultState, TestMethodProcessResultState> methodProcessResultStateMapping;
    private ConsoleReportPolicyConfig config;

    public ConsoleReportPolicyInstance(TestFrameworkInstance testFrameworkInstance) {
        methodProcessResultStateMapping = testFrameworkInstance.getTestFramework().getMethodProcessResultStateMapping();
    }

    public void initialize(Report report) {
        this.report = report;

        config = (ConsoleReportPolicyConfig) report.getConfig().getPolicyConfig();

        reportThreadPool = ThreadUtils.newFixedThreadPool(config.getAmountOfReportingThreads());

        consumers = new ArrayList<ReportInfoQueueItemConsumer>();

        for (int i = 0; i < config.getAmountOfReportingThreads(); i++) {
            consumers.add(new ReportInfoQueueItemConsumer(report.getReportInfoQueue(), 100L, TimeUnit.MILLISECONDS,
                    this));
        }
    }

    public void start() {
        for (ReportInfoQueueItemConsumer reportConsumer : consumers) {
            reportThreadPool.submit(reportConsumer);
        }
    }

    public void stop() {
        for (ReportInfoQueueItemConsumer reportConsumer : consumers) {
            reportConsumer.stopConsuming();
        }

        ThreadUtils.shutdown(reportThreadPool);
        ThreadUtils.awaitTermination(reportThreadPool);
    }

    private class ReportInfoQueueItemConsumer extends AbstractBlockingQueueItemConsumer<ReportInfo> {

        private final ConsoleReportPolicyInstance reportPolicyInstance;

        public ReportInfoQueueItemConsumer(BlockingQueue<ReportInfo> toConsumeQueue, long pollTimeout,
                                           TimeUnit pollTimeoutTimeUnit,
                                           ConsoleReportPolicyInstance reportPolicyInstance) {
            super(toConsumeQueue, pollTimeout, pollTimeoutTimeUnit);
            this.reportPolicyInstance = reportPolicyInstance;
        }

        protected boolean consume(ReportInfo queueItem) {
            reportPolicyInstance.process(queueItem);

            return false;
        }
    }

    public void process(ReportInfo reportInfo) {
        if (TestClassProcessResultReportInfo.class == reportInfo.getClass()) {
            final TestClassProcessResultReportInfo testClassInfo = (TestClassProcessResultReportInfo) reportInfo;

            final Pipeline pipeline = testClassInfo.getPipeline();
            final TestClassProcessResult classResult = testClassInfo.getTestClassProcessResult();
            final List<TestMethodProcessResult> methodResults = classResult.getMethodResults();

            final Map<TestMethodProcessResultState, Integer> stateCounts
                    = new HashMap<TestMethodProcessResultState, Integer>();

            for (final TestMethodProcessResult methodResult : methodResults) {
                final TestMethodProcessResultState mappedState = methodProcessResultStateMapping.get(
                        methodResult.getState());

                Integer stateCount = stateCounts.get(mappedState);
                if (stateCount == null) {
                    stateCount = 1;
                } else {
                    stateCount++;
                }
                stateCounts.put(mappedState, stateCount);
            }

            final List<TestMethodProcessResultState> noneZeroStates = new ArrayList<TestMethodProcessResultState>();

            for (final TestMethodProcessResultState state : TestMethodProcessResultStates.values()) {
                final Integer stateCount = stateCounts.get(state);
                if (stateCount != null) {
                    noneZeroStates.add(state);
                }
            }

            boolean show = false;
            final List<TestMethodProcessResultState> toShowStates = config.getToShowStates();
            final Iterator<TestMethodProcessResultState> toShowStatesIterator = toShowStates.iterator();
            while (!show && toShowStatesIterator.hasNext()) {
                show = noneZeroStates.contains(toShowStatesIterator.next());
            }

            if (show) {
                final int failureCount = stateCounts.get(TestMethodProcessResultStates.FAILURE) == null ? 0
                        : stateCounts.get(TestMethodProcessResultStates.FAILURE);
                final int errorCount = stateCounts.get(TestMethodProcessResultStates.ERROR) == null ? 0
                        : stateCounts.get(TestMethodProcessResultStates.ERROR);
                final int successCount = stateCounts.get(TestMethodProcessResultStates.SUCCESS) == null ? 0
                        : stateCounts.get(TestMethodProcessResultStates.SUCCESS);

                logger.info("pipeline {}, fork {} : Test {} : success#{}, failure#{}, error#{}", new Object[]{
                        pipeline.getConfig().getName(), reportInfo.getForkId(),
                        classResult.getTestClassRunInfo().getTestClassName(), successCount, failureCount, errorCount
                });

                if (failureCount > 0 || errorCount > 0) {
                    for (final TestMethodProcessResult methodResult : methodResults) {
                        if (methodResult.getState() == TestMethodProcessResultStates.ERROR
                                || methodResult.getState() == TestMethodProcessResultStates.FAILURE) {
                            logger.info("pipeline {}, fork {} : Test {} :", new Object[]{
                                    pipeline.getConfig().getName(), reportInfo.getForkId(),
                                    classResult.getTestClassRunInfo().getTestClassName() + "." + methodResult
                                            .getMethodName()
                            });
                            logger.info("cause", methodResult.getThrownException());
                        }
                    }
                }
            }
        }
        // else unsupported reportInfo -> warning ?
    }
}
