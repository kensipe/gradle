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

package org.gradle.integtests;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

public class TaskExecutionIntegrationTest extends AbstractIntegrationTest {
    public static boolean graphListenerNotified;

    @Before
    public void setUp() {
        graphListenerNotified = false;
    }

    @Test
    public void taskCanAccessTaskGraph() {
        TestFile buildFile = testFile("build.gradle");
        buildFile.writelns("import org.gradle.integtests.TaskExecutionIntegrationTest",
                "task a(dependsOn: 'b') << { task ->", "    assertTrue(gradle.taskGraph.hasTask(task))",
                "    assertTrue(gradle.taskGraph.hasTask(':a'))", "    assertTrue(gradle.taskGraph.hasTask(a))",
                "    assertTrue(gradle.taskGraph.hasTask(':b'))", "    assertTrue(gradle.taskGraph.hasTask(b))",
                "    assertTrue(gradle.taskGraph.allTasks.contains(task))",
                "    assertTrue(gradle.taskGraph.allTasks.contains(tasks.getByName('b')))", "}", "task b",
                "gradle.taskGraph.whenReady { graph ->", "    assertTrue(graph.hasTask(':a'))",
                "    assertTrue(graph.hasTask(a))", "    assertTrue(graph.hasTask(':b'))",
                "    assertTrue(graph.hasTask(b))", "    assertTrue(graph.allTasks.contains(a))",
                "    assertTrue(graph.allTasks.contains(b))",
                "    TaskExecutionIntegrationTest.graphListenerNotified = true", "}");
        usingBuildFile(buildFile).withTasks("a").run().assertTasksExecuted(":b", ":a");

        assertTrue(graphListenerNotified);
    }

    @Test
    public void executesAllTasksInASingleBuildAndEachTaskAtMostOnce() {
        TestFile buildFile = testFile("build.gradle");
        buildFile.writelns(
                "gradle.taskGraph.whenReady { assertFalse(project.hasProperty('graphReady')); graphReady = true }",
                "task a << { task -> project.executedA = task }", "task b << { ",
                "    assertSame(a, project.executedA);", "    assertTrue(gradle.taskGraph.hasTask(':a'))", "}",
                "task c(dependsOn: a)", "task d(dependsOn: a)", "task e(dependsOn: [a, d])");
        usingBuildFile(buildFile).withTasks("a", "b").run().assertTasksExecuted(":a", ":b");
        usingBuildFile(buildFile).withTasks("a", "a").run().assertTasksExecuted(":a");
        usingBuildFile(buildFile).withTasks("c", "a").run().assertTasksExecuted(":a", ":c");
        usingBuildFile(buildFile).withTasks("c", "e").run().assertTasksExecuted(":a", ":c", ":d", ":e");
    }

    @Test
    public void executesMultiProjectsTasksInASingleBuildAndEachTaskAtMostOnce() {
        testFile("settings.gradle").writelns("include 'child1', 'child2'");
        TestFile buildFile = testFile("build.gradle");
        buildFile.writelns("task a", "allprojects {", "    task b", "    task c(dependsOn: ['b', ':a'])", "}");
        usingBuildFile(buildFile).withTasks("a", "c").run().assertTasksExecuted(":a", ":b", ":c", ":child1:b",
                ":child1:c", ":child2:b", ":child2:c");
        usingBuildFile(buildFile).withTasks("b", ":child2:c").run().assertTasksExecuted(":b", ":child1:b", ":child2:b",
                ":a", ":child2:c");
    }

    @Test
    public void executesMultiProjectDefaultTasksInASingleBuildAndEachTaskAtMostOnce() {
        testFile("settings.gradle").writelns("include 'child1', 'child2'");
        TestFile buildFile = testFile("build.gradle");
        buildFile.writelns("defaultTasks 'a', 'b'", "task a", "subprojects {", "    task a(dependsOn: ':a')",
                "    task b(dependsOn: ':a')", "}");
        usingBuildFile(buildFile).run().assertTasksExecuted(":a", ":child1:a", ":child2:a", ":child1:b", ":child2:b");
    }

    @Test
    public void executesProjectDefaultTasksWhenNoneSpecified() {
        TestFile buildFile = testFile("build.gradle");
        buildFile.writelns(
                "task a",
                "task b(dependsOn: a)",
                "defaultTasks 'b'"
        );
        usingBuildFile(buildFile).withTasks().run().assertTasksExecuted(":a", ":b");
    }
    
    @Test
    public void doesNotExecuteTaskActionsWhenDryRunSpecified() {
        TestFile buildFile = testFile("build.gradle");
        buildFile.writelns(
                "task a << { fail() }",
                "task b(dependsOn: a) << { fail() }",
                "defaultTasks 'b'"
        );

        // project defaults
        usingBuildFile(buildFile).withArguments("-m").run().assertTasksExecuted(":a", ":b");
        // named tasks
        usingBuildFile(buildFile).withArguments("-m").withTasks("b").run().assertTasksExecuted(":a", ":b");
    }

    @Test
    public void excludesTasksWhenExcludePatternSpecified() {
        testFile("settings.gradle").write("include 'sub'");
        TestFile buildFile = testFile("build.gradle");
        buildFile.writelns(
                "task a",
                "task b(dependsOn: a)",
                "task c(dependsOn: [a, b])",
                "task d(dependsOn: c)",
                "defaultTasks 'd'"
        );
        testFile("sub/build.gradle").writelns(
                "task c",
                "task d(dependsOn: c)"
        );

        // Exclude entire branch
        usingBuildFile(buildFile).withTasks(":d").withArguments("-x", "c").run().assertTasksExecuted(":d");
        // Exclude direct dependency
        usingBuildFile(buildFile).withTasks(":d").withArguments("-x", "b").run().assertTasksExecuted(":a", ":c", ":d");
        // Exclude using paths and multi-project
        usingBuildFile(buildFile).withTasks("d").withArguments("-x", "c").run().assertTasksExecuted(":d", ":sub:d");
        usingBuildFile(buildFile).withTasks("d").withArguments("-x", "sub:c").run().assertTasksExecuted(":a", ":b", ":c", ":d", ":sub:d");
        usingBuildFile(buildFile).withTasks("d").withArguments("-x", ":sub:c").run().assertTasksExecuted(":a", ":b", ":c", ":d", ":sub:d");
        usingBuildFile(buildFile).withTasks("d").withArguments("-x", "d").run().assertTasksExecuted();
        // Project defaults
        usingBuildFile(buildFile).withArguments("-x", "b").run().assertTasksExecuted(":a", ":c", ":d", ":sub:c", ":sub:d");
        // Unknown task
        usingBuildFile(buildFile).withTasks("d").withArguments("-x", "unknown").runWithFailure().assertThatDescription(startsWith("Task 'unknown' not found in root project"));
    }
}
