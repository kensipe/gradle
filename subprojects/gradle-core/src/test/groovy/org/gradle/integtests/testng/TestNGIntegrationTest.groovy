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
package org.gradle.integtests.testng

import org.gradle.integtests.DistributionIntegrationTestRunner
import org.gradle.integtests.GradleDistribution
import org.gradle.integtests.GradleExecuter
import org.junit.Test
import org.junit.runner.RunWith
import static org.gradle.integtests.testng.TestNGIntegrationProject.*
import org.gradle.integtests.TestFile
import org.junit.Ignore

/**
 * @author Tom Eyckmans
 */
@RunWith(DistributionIntegrationTestRunner.class)
public class TestNGIntegrationTest {
    static final String GROOVY = "groovy"
    static final String JAVA = "java"
    static final String JDK14 = "jdk14"
    static final String JDK15 = "jdk15"

    static final GROOVY_JDK15_FAILING = failingIntegrationProject(GROOVY, JDK15, { name, projectDir, result ->
        checkExists(projectDir, 'build/classes/main/org/gradle/Ok.class')
        checkExists(projectDir, 'build/classes/test/org/gradle/BadTest.class')
        checkExists(projectDir, 'build/reports/tests/index.html')
    })
    static final GROOVY_JDK15_PASSING = passingIntegrationProject(GROOVY, JDK15, { name, projectDir, result ->
        checkExists(projectDir, 'build/classes/main/org/gradle/Ok.class')
        checkExists(projectDir, 'build/classes/test/org/gradle/OkTest.class')
        checkExists(projectDir, 'build/reports/tests/index.html')
    })
    static final JAVA_JDK14_FAILING = failingIntegrationProject(JAVA, JDK14, { name, projectDir, result ->
        checkExists(projectDir, 'build/classes/main/org/gradle/Ok.class')
        checkExists(projectDir, 'build/classes/test/org/gradle/BadTest.class')
        checkExists(projectDir, 'build/reports/tests/index.html')
    })
    static final JAVA_JDK14_PASSING = passingIntegrationProject(JAVA, JDK14, { name, projectDir, result ->
        checkExists(projectDir, 'build/classes/main/org/gradle/Ok.class')
        checkExists(projectDir, 'build/classes/test/org/gradle/OkTest.class')
        checkExists(projectDir, 'build/reports/tests/index.html')
    })
    static final JAVA_JDK15_FAILING = failingIntegrationProject(JAVA, JDK15, { name, projectDir, result ->
        checkExists(projectDir, 'build/classes/main/org/gradle/Ok.class')
        checkExists(projectDir, 'build/classes/test/org/gradle/BadTest.class')
        checkExists(projectDir, 'build/reports/tests/index.html')
    })
    static final JAVA_JDK15_PASSING = passingIntegrationProject(JAVA, JDK15, { name, projectDir, result ->
        checkExists(projectDir, 'build/classes/main/org/gradle/Ok.class')
        checkExists(projectDir, 'build/classes/test/org/gradle/OkTest.class')
        checkExists(projectDir, 'build/reports/tests/index.html')
    })
    static final JAVA_JDK15_PASSING_NO_REPORT = passingIntegrationProject(JAVA, JDK15, "-no-report", { name, projectDir, result ->
        checkExists(projectDir, 'build/classes/main/org/gradle/Ok.class')
        checkExists(projectDir, 'build/classes/test/org/gradle/OkTest.class')
        checkDoesNotExists(projectDir, 'build/reports/tests/index.html')
    })
    static final SUITE_XML_BUILDER = new TestNGIntegrationProject("suitexmlbuilder", false, null, { name, projectDir, result ->
        checkExists(projectDir, 'build/classes/main/org/gradle/testng/User.class')
        checkExists(projectDir, 'build/classes/main/org/gradle/testng/UserImpl.class')
        checkExists(projectDir, 'build/classes/test/org/gradle/testng/UserImplTest.class')
        checkExists(projectDir, 'build/reports/tests/index.html')
        checkExists(projectDir, 'build/reports/tests/emailable-report.html')
    })

    // Injected by test runner
    private GradleDistribution dist;
    private GradleExecuter executer;

    @Test
    public void suiteXmlBuilder() {
        checkProject(SUITE_XML_BUILDER)
    }

    @Test
    public void groovyJdk15() {
        checkProject(GROOVY_JDK15_PASSING)
        checkProject(GROOVY_JDK15_FAILING)
    }

    @Test
    public void javaJdk14() {
        checkProject(GROOVY_JDK15_PASSING)
        checkProject(GROOVY_JDK15_FAILING)
    }

    @Test
    public void javaJdk15() {
        checkProject(JAVA_JDK15_PASSING)
        checkProject(JAVA_JDK15_FAILING)
        // TODO currently reports are always generated because the antTestNGExecute task uses the
        // default listeners and these generate reports by default.
        // checkProject(JAVA_JDK15_PASSING_NO_REPORT)
    }

    @Ignore
    public void javaJdk15WithNoReports() {
        // TODO currently reports are always generated because the antTestNGExecute task uses the
        // default listeners and these generate reports by default. Enable the test when this has changed.
        checkProject(JAVA_JDK15_PASSING_NO_REPORT)
    }

    private def checkProject(project) {
        final File projectDir = new File(new File(dist.samplesDir, "testng"), project.name)

        def result
        executer.inDirectory(projectDir).withTasks('clean', 'test')
        if (project.expectFailure) {
            result = executer.runWithFailure()
        } else {
            result = executer.run()
        }

        // output: output, error: error, command: actualCommand, unixCommand: unixCommand, windowsCommand: windowsCommand
        project.doAssert(projectDir, result)
    }

    static void checkExists(File baseDir, String[] path) {
        new TestFile(baseDir, path).assertExists()
    }

    static void checkDoesNotExists(File baseDir, String[] path) {
        new TestFile(baseDir, path).assertDoesNotExist()
    }
}