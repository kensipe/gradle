/*
 * Copyright 2007 the original author or authors.
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

package org.gradle.api.tasks.testing

import groovy.mock.interceptor.MockFor
import org.gradle.api.DependencyManager
import org.gradle.api.GradleException
import org.gradle.api.InvalidUserDataException
import org.gradle.api.Task
import org.gradle.api.tasks.AbstractConventionTaskTest
import org.gradle.api.tasks.AbstractTaskTest
import org.gradle.api.tasks.compile.ClasspathConverter
import org.gradle.api.tasks.util.ExistingDirsFilter

/**
 * @author Hans Dockter
 */
class TestTest extends AbstractConventionTaskTest {
    static final String TEST_PATTERN_1 = 'pattern1'
    static final String TEST_PATTERN_2 = 'pattern2'
    static final String TEST_PATTERN_3 = 'pattern3'

    static final File TEST_TEST_CLASSES_DIR = '/testClsssesDir' as File
    static final File TEST_TEST_RESULTS_DIR = '/resultDir' as File
    static final File TEST_ROOT_DIR = '/ROOTDir' as File

    static final List TEST_DEPENDENCY_MANAGER_CLASSPATH = ['jar1' as File]
    static final List TEST_CONVERTED_UNMANAGED_CLASSPATH = ['jar2' as File]
    static final List TEST_UNMANAGED_CLASSPATH = ['jar2']

    Test test

    MockFor antJUnitMocker

    MockFor dependencyManagerMocker

    void setUp() {
        super.setUp()
        test = new Test(project, AbstractTaskTest.TEST_TASK_NAME)
        test.project.rootDir = TEST_ROOT_DIR
        antJUnitMocker = new MockFor(AntJunit)
        dependencyManagerMocker = new MockFor(DependencyManager)
    }

    Task getTask() {test}

    void testCompile() {
        assertNotNull(test.antJunit)
        assertNotNull(test.options)
        assertNotNull(test.existingDirsFilter)
        assertNotNull(test.classpathConverter)
        assertNull(test.compiledTestsDir)
        assertNull(test.dependencyManager)
        assertNull(test.testResultsDir)
        assertEquals([], test.includes)
        assertEquals([], test.excludes)
        assert test.stopAtFailuresOrErrors
    }

    void testExecute() {
        setUpMocks(test)

        test.existingDirsFilter = [checkExistenceAndLogExitMessage: {File dir ->
            assertEquals(TEST_TEST_CLASSES_DIR, dir)
            true}] as ExistingDirsFilter

        antJUnitMocker.demand.execute(1..1) {File compiledTestClassesDir, List classpath, File testResultsDir, List includes,
                                             List excludes, JunitOptions junitOptions, AntBuilder ant ->
            assertEquals(TEST_TEST_CLASSES_DIR, compiledTestClassesDir)
            assertEquals(TEST_CONVERTED_UNMANAGED_CLASSPATH + TEST_DEPENDENCY_MANAGER_CLASSPATH, classpath)
            assertEquals(TEST_TEST_RESULTS_DIR, testResultsDir)
            assertEquals(test.options, junitOptions)
            assert includes.is(test.includes)
            assert excludes.is(test.excludes)
            assert ant.is(project.ant)
        }

        antJUnitMocker.use(test.antJunit) {
            test.execute()
        }
    }

    void testExecuteWithTestFailuresAndStopAtFailures() {
        setUpMocks(test)

        test.existingDirsFilter = [checkExistenceAndLogExitMessage: {File dir ->
            assertEquals(TEST_TEST_CLASSES_DIR, dir)
            true}] as ExistingDirsFilter

        antJUnitMocker.demand.execute(1..1) {File compiledTestClassesDir, List classpath, File testResultsDir, List includes,
                                             List excludes, JunitOptions junitOptions, AntBuilder ant ->
            ant.project.setProperty(AntJunit.FAILURES_OR_ERRORS_PROPERTY, 'somevalue')
        }

        antJUnitMocker.use(test.antJunit) {
            shouldFailWithCause(GradleException) {
                test.execute()
            }
        }
    }

    void testExecuteWithTestFailuresAndContinueWithFailures() {
        setUpMocks(test)
        test.stopAtFailuresOrErrors = false
        test.existingDirsFilter = [checkExistenceAndLogExitMessage: {File dir ->
            assertEquals(TEST_TEST_CLASSES_DIR, dir)
            true}] as ExistingDirsFilter

        antJUnitMocker.demand.execute(1..1) {File compiledTestClassesDir, List classpath, File testResultsDir, List includes,
                                             List excludes, JunitOptions junitOptions, AntBuilder ant ->
            ant.project.setProperty(AntJunit.FAILURES_OR_ERRORS_PROPERTY, 'somevalue')
        }

        antJUnitMocker.use(test.antJunit) {
            test.execute()
        }
    }

    void testExecuteWithUnspecifiedCompiledTestsDir() {
        setUpMocks(test)
        test.compiledTestsDir = null
        shouldFailWithCause(InvalidUserDataException) {
            test.execute()
        }
    }

    void testExecuteWithUnspecifiedTestResultsDir() {
        setUpMocks(test)
        test.testResultsDir = null
        shouldFailWithCause(InvalidUserDataException) {
            test.execute()
        }
    }

    void testExecuteWithNonExistingCompiledTestsDir() {
        setUpMocks(test)
        test.unmanagedClasspath = null

        test.existingDirsFilter = [checkExistenceAndLogExitMessage: {false}] as ExistingDirsFilter

        antJUnitMocker.demand.execute(0..0) {File compiledTestClassesDir, List classpath, File testResultsDir,
                                             List includes, List excludes, JunitOptions junitOptions, List jvmArgs,
                                             Map systemProperties, AntBuilder ant ->}

        antJUnitMocker.use(test.antJunit) {
            test.execute()
        }
    }

    private void setUpMocks(Test test) {
        test.compiledTestsDir = TEST_TEST_CLASSES_DIR
        test.testResultsDir = TEST_TEST_RESULTS_DIR
        test.unmanagedClasspath = TEST_UNMANAGED_CLASSPATH

        test.dependencyManager = [resolveClasspath: {String taskName ->
            assertEquals(test.name, taskName)
            TEST_DEPENDENCY_MANAGER_CLASSPATH
        }] as DependencyManager

        test.classpathConverter = [createFileClasspath: {File baseDir, Object[] pathElements ->
            assertEquals(TEST_ROOT_DIR, baseDir)
            assertEquals(TEST_UNMANAGED_CLASSPATH, pathElements as List)
            TEST_CONVERTED_UNMANAGED_CLASSPATH
        }] as ClasspathConverter
    }

    void testIncludes() {
        checkIncludesExcludes('include')
    }

    void testExcludes() {
        checkIncludesExcludes('exclude')
    }

    void checkIncludesExcludes(String name) {
        assert test."$name"(TEST_PATTERN_1, TEST_PATTERN_2).is(test)
        assertEquals([TEST_PATTERN_1, TEST_PATTERN_2], test."${name}s")
        test."$name"(TEST_PATTERN_3)
        assertEquals([TEST_PATTERN_1, TEST_PATTERN_2, TEST_PATTERN_3], test."${name}s")
    }

}