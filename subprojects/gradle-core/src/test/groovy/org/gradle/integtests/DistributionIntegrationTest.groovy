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
package org.gradle.integtests

import org.junit.runner.RunWith
import org.junit.Test
import org.gradle.util.GradleVersion
import org.gradle.util.AntUtil
import org.apache.tools.ant.taskdefs.Expand

@RunWith(DistributionIntegrationTestRunner)
class DistributionIntegrationTest {
    // Injected by test runner
    private GradleDistribution dist;
    private GradleExecuter executer;
    private String version = new GradleVersion().version

    @Test
    public void binZipContents() {
        TestFile binZip = dist.distributionsDir.file("gradle-$version-bin.zip")
        binZip.usingNativeTools().unzipTo(dist.testDir)
        TestFile contentsDir = dist.testDir.file("gradle-$version")

        checkMinimalContents(contentsDir)

        // Extra stuff
        contentsDir.file('src').assertDoesNotExist()
        contentsDir.file('samples').assertDoesNotExist()
        contentsDir.file('docs').assertDoesNotExist()
    }

    @Test
    public void allZipContents() {
        TestFile binZip = dist.distributionsDir.file("gradle-$version-all.zip")
        binZip.usingNativeTools().unzipTo(dist.testDir)
        TestFile contentsDir = dist.testDir.file("gradle-$version")

        checkMinimalContents(contentsDir)

        // Source
        contentsDir.file('src/org/gradle/api/Project.java').assertIsFile()
        contentsDir.file('src/org/gradle/initialization/defaultBuildSourceScript.txt').assertIsFile()
        contentsDir.file('src/org/gradle/gradleplugin/userinterface/swing/standalone/BlockingApplication.java').assertIsFile()
        contentsDir.file('src/org/gradle/wrapper/Wrapper.java').assertIsFile()

        // Samples
        contentsDir.file('samples/java/quickstart/build.gradle').assertIsFile()

        // Docs
        contentsDir.file('docs/javadoc/index.html').assertIsFile()
        contentsDir.file('docs/javadoc/org/gradle/api/Project.html').assertIsFile()
        contentsDir.file('docs/groovydoc/index.html').assertIsFile()
        contentsDir.file('docs/groovydoc/org/gradle/api/Project.html').assertIsFile()
        contentsDir.file('docs/groovydoc/org/gradle/api/tasks/bundling/Zip.html').assertIsFile()
        contentsDir.file('docs/userguide/userguide.html').assertIsFile()
        contentsDir.file('docs/userguide/userguide_single.html').assertIsFile()
//        contentsDir.file('docs/userguide/userguide.pdf').assertIsFile()
    }

    private def checkMinimalContents(TestFile contentsDir) {
        // Check it can be executed
        executer.inDirectory(contentsDir).usingExecutable('bin/gradle').withTaskList().run()

        // Scripts
        contentsDir.file('bin/gradle').assertIsFile()
        contentsDir.file('bin/gradle.bat').assertIsFile()

        // Top level files
        contentsDir.file('LICENSE').assertIsFile()

        // Libs
        contentsDir.file("lib/gradle-core-${version}.jar").assertIsFile()
        contentsDir.file("lib/gradle-ui-${version}.jar").assertIsFile()
        contentsDir.file("lib/gradle-launcher-${version}.jar").assertIsFile()
        contentsDir.file("lib/gradle-jetty-${version}.jar").assertIsFile()
        contentsDir.file("lib/gradle-wrapper-${version}.jar").assertIsFile()

        // Docs
        contentsDir.file('getting-started.html').assertIsFile()
    }

    @Test
    public void sourceZipContents() {
        TestFile srcZip = dist.distributionsDir.file("gradle-$version-src.zip")
        srcZip.usingNativeTools().unzipTo(dist.testDir)
        TestFile contentsDir = dist.testDir.file("gradle-$version")

        // Build self using wrapper in source distribution
        executer.inDirectory(contentsDir).usingExecutable('gradlew').withTasks('binZip').run()

        File binZip = contentsDir.file('build/distributions').listFiles()[0]
        Expand unpack = new Expand()
        unpack.src = binZip
        unpack.dest = contentsDir.file('build/distributions/unzip')
        AntUtil.execute(unpack)
        TestFile unpackedRoot = new TestFile(contentsDir.file('build/distributions/unzip').listFiles()[0])

        // Make sure the build distribution does something useful
        unpackedRoot.file("bin/gradle").assertIsFile()
        // todo run something with the gradle build by the source dist
    }
}
