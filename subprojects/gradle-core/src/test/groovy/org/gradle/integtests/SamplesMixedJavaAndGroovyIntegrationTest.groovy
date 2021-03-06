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

package org.gradle.integtests

import org.junit.Test
import org.junit.runner.RunWith
import static org.hamcrest.Matchers.*

@RunWith(DistributionIntegrationTestRunner.class)
class SamplesMixedJavaAndGroovyIntegrationTest {
    // Injected by test runner
    private GradleDistribution dist;
    private GradleExecuter executer;

    @Test
    public void canBuildJar() {
        TestFile projectDir = dist.samplesDir.file('groovy/mixedJavaAndGroovy')
        executer.inDirectory(projectDir).withTasks('clean', 'build').run()

        // Check tests have run
        projectDir.file('build/test-results/TEST-org.gradle.PersonTest.xml').assertIsFile()
        projectDir.file('build/test-results/TESTS-TestSuites.xml').assertIsFile()

        // Check contents of jar
        TestFile tmpDir = dist.testDir.file('jarContents')
        projectDir.file('build/libs/mixedJavaAndGroovy-1.0.jar').unzipTo(tmpDir)
        tmpDir.assertHasDescendants(
                'META-INF/MANIFEST.MF',
                'org/gradle/Person.class',
                'org/gradle/GroovyPerson.class',
                'org/gradle/JavaPerson.class',
                'org/gradle/PersonList.class'
        )
    }

    @Test
    public void canBuildDocs() {
        TestFile projectDir = dist.samplesDir.file('groovy/mixedJavaAndGroovy')
        executer.inDirectory(projectDir).withTasks('clean', 'javadoc', 'groovydoc').run()

        TestFile javadocsDir = projectDir.file("build/docs/javadoc")
        javadocsDir.file("index.html").assertIsFile()
        javadocsDir.file("index.html").assertContents(containsString('mixedJavaAndGroovy 1.0 API'))
        javadocsDir.file("org/gradle/Person.html").assertIsFile()
        javadocsDir.file("org/gradle/JavaPerson.html").assertIsFile()

        TestFile groovydocsDir = projectDir.file("build/docs/groovydoc")
        groovydocsDir.file("index.html").assertIsFile()
        groovydocsDir.file("overview-summary.html").assertContents(containsString('mixedJavaAndGroovy 1.0 API'))
        groovydocsDir.file("org/gradle/JavaPerson.html").assertIsFile()
        groovydocsDir.file("org/gradle/GroovyPerson.html").assertIsFile()
        groovydocsDir.file("org/gradle/PersonList.html").assertIsFile()
    }
}