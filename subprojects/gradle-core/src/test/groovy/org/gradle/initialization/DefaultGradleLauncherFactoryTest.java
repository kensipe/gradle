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
package org.gradle.initialization;

import org.gradle.StartParameter;
import org.gradle.util.HelperUtil;
import org.gradle.util.WrapUtil;
import org.hamcrest.Matchers;
import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnit4Mockery;
import static org.junit.Assert.*;
import org.junit.Test;

/**
 * @author Hans Dockter
 */
public class DefaultGradleLauncherFactoryTest {
    private JUnit4Mockery context = new JUnit4Mockery();

    @Test
    public void newInstanceWithStartParameter() {
        final StartParameter startParameter = HelperUtil.dummyStartParameter();
        final LoggingConfigurer loggingConfigurer = context.mock(LoggingConfigurer.class);
        context.checking(new Expectations() {{
            one(loggingConfigurer).configure(startParameter.getLogLevel());
        }});
        new DefaultGradleLauncherFactory(loggingConfigurer, context.mock(CommandLine2StartParameterConverter.class)).newInstance(startParameter);
    }

    @Test
    public void newInstanceWithCommandLineArgs() {
        final StartParameter startParameter = HelperUtil.dummyStartParameter();
        final LoggingConfigurer loggingConfigurer = context.mock(LoggingConfigurer.class);
        final String[] commandLineArgs = WrapUtil.toArray("A", "B");
        final CommandLine2StartParameterConverter parameterConverterStub = context.mock(CommandLine2StartParameterConverter.class);
        context.checking(new Expectations() {{
            one(loggingConfigurer).configure(startParameter.getLogLevel());
            allowing(parameterConverterStub).convert(commandLineArgs); will(returnValue(startParameter));
        }});
        new DefaultGradleLauncherFactory(loggingConfigurer, parameterConverterStub).newInstance(commandLineArgs);
    }

    @Test
    public void createStartParameter() {
        final StartParameter startParameter = HelperUtil.dummyStartParameter();
        final LoggingConfigurer loggingConfigurer = context.mock(LoggingConfigurer.class);
        final String[] commandLineArgs = WrapUtil.toArray("A", "B");
        final CommandLine2StartParameterConverter parameterConverterStub = context.mock(CommandLine2StartParameterConverter.class);
        context.checking(new Expectations() {{
            one(loggingConfigurer).configure(startParameter.getLogLevel());
            allowing(parameterConverterStub).convert(commandLineArgs); will(returnValue(startParameter));
        }});
        assertThat(new DefaultGradleLauncherFactory(loggingConfigurer, parameterConverterStub).createStartParameter(commandLineArgs),
            Matchers.sameInstance(startParameter));
    }
}
