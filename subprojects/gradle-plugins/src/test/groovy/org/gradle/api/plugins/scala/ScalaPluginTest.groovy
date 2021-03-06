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
package org.gradle.api.plugins.scala

import org.gradle.api.Project
import org.gradle.api.internal.artifacts.configurations.Configurations
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.tasks.scala.ScalaCompile
import org.gradle.api.tasks.scala.ScalaDoc
import org.gradle.util.HelperUtil
import org.junit.Test
import static org.gradle.util.Matchers.*
import static org.gradle.util.WrapUtil.*
import static org.hamcrest.Matchers.*
import static org.junit.Assert.*

public class ScalaPluginTest {

    private final Project project = HelperUtil.createRootProject()
    private final ScalaPlugin scalaPlugin = new ScalaPlugin()

    @Test public void appliesTheJavaPluginToTheProject() {
        scalaPlugin.use(project, project.getPlugins())
        assertTrue(project.getPlugins().hasPlugin(JavaPlugin))
    }

    @Test public void addsScalaToolsConfigurationToTheProject() {
        scalaPlugin.use(project, project.getPlugins())
        def configuration = project.configurations.getByName(ScalaPlugin.SCALA_TOOLS_CONFIGURATION_NAME)
        assertThat(Configurations.getNames(configuration.extendsFrom, false), equalTo(toSet()))
        assertFalse(configuration.visible)
        assertTrue(configuration.transitive)
    }

    @Test public void addsScalaConventionToEachSourceSetAndAppliesMappings() {
        scalaPlugin.use(project, project.getPlugins())

        def sourceSet = project.sourceSets.main
        assertThat(sourceSet.scala.displayName, equalTo("main Scala source"))
        assertThat(sourceSet.scala.srcDirs, equalTo(toLinkedSet(project.file("src/main/scala"))))

        sourceSet = project.sourceSets.test
        assertThat(sourceSet.scala.displayName, equalTo("test Scala source"))
        assertThat(sourceSet.scala.srcDirs, equalTo(toLinkedSet(project.file("src/test/scala"))))

        sourceSet = project.sourceSets.add('custom')
        assertThat(sourceSet.scala.displayName, equalTo("custom Scala source"))
        assertThat(sourceSet.scala.srcDirs, equalTo(toLinkedSet(project.file("src/custom/scala"))))
    }

    @Test public void addsCompileTaskForEachSourceSet() {
        scalaPlugin.use(project, project.getPlugins())

        def task = project.tasks['compileScala']
        assertThat(task, instanceOf(ScalaCompile.class))
        assertThat(task.description, equalTo('Compiles the main Scala source.'))
        assertThat(task.classpath, equalTo(project.sourceSets.main.compileClasspath))
        assertThat(task.defaultSource, equalTo(project.sourceSets.main.scala))
        assertThat(task, dependsOn(ScalaPlugin.SCALA_DEFINE_TASK_NAME, JavaPlugin.COMPILE_JAVA_TASK_NAME))

        task = project.tasks['compileTestScala']
        assertThat(task, instanceOf(ScalaCompile.class))
        assertThat(task.description, equalTo('Compiles the test Scala source.'))
        assertThat(task.classpath, equalTo(project.sourceSets.test.compileClasspath))
        assertThat(task.defaultSource, equalTo(project.sourceSets.test.scala))
        assertThat(task, dependsOn(ScalaPlugin.SCALA_DEFINE_TASK_NAME, JavaPlugin.COMPILE_TEST_JAVA_TASK_NAME, JavaPlugin.CLASSES_TASK_NAME))

        project.sourceSets.add('custom')
        task = project.tasks['compileCustomScala']
        assertThat(task, instanceOf(ScalaCompile.class))
        assertThat(task.description, equalTo('Compiles the custom Scala source.'))
        assertThat(task.classpath, equalTo(project.sourceSets.custom.compileClasspath))
        assertThat(task.defaultSource, equalTo(project.sourceSets.custom.scala))
        assertThat(task, dependsOn(ScalaPlugin.SCALA_DEFINE_TASK_NAME, 'compileCustomJava'))
    }

    @Test public void dependenciesOfJavaPluginTasksIncludeScalaCompileTasks() {
        scalaPlugin.use(project, project.getPlugins())

        def task = project.tasks[JavaPlugin.CLASSES_TASK_NAME]
        assertThat(task, dependsOn(hasItem('compileScala')))

        task = project.tasks[JavaPlugin.TEST_CLASSES_TASK_NAME]
        assertThat(task, dependsOn(hasItem('compileTestScala')))
    }
    
    @Test public void configuresCompileTasksDefinedByTheBuildScript() {
        scalaPlugin.use(project, project.getPlugins())

        def task = project.createTask('otherCompile', type: ScalaCompile)
        assertThat(task.classpath, equalTo(project.sourceSets.main.compileClasspath))
        assertThat(task.defaultSource, nullValue())
        assertThat(task, dependsOn(ScalaPlugin.SCALA_DEFINE_TASK_NAME))
    }

    @Test public void addsScalaDocTasksToTheProject() {
        scalaPlugin.use(project, project.getPlugins())

        def task = project.tasks[ScalaPlugin.SCALA_DOC_TASK_NAME]
        assertThat(task, instanceOf(ScalaDoc.class))
        assertThat(task, dependsOn(JavaPlugin.CLASSES_TASK_NAME, ScalaPlugin.SCALA_DEFINE_TASK_NAME))
        assertThat(task.destinationDir, equalTo(project.file("$project.docsDir/scaladoc")))
        assertThat(task.defaultSource, equalTo(project.sourceSets.main.scala))
        assertThat(task.classpath.sourceCollections, hasItem(project.sourceSets.main.classes))
        assertThat(task.classpath.sourceCollections, hasItem(project.sourceSets.main.compileClasspath))
        assertThat(task.title, equalTo(project.apiDocTitle))
    }

    @Test public void configuresScalaDocTasksDefinedByTheBuildScript() {
        scalaPlugin.use(project, project.getPlugins())

        def task = project.createTask('otherScaladoc', type: ScalaDoc)
        assertThat(task, dependsOn(JavaPlugin.CLASSES_TASK_NAME, ScalaPlugin.SCALA_DEFINE_TASK_NAME))
        assertThat(task.destinationDir, equalTo(project.file("$project.docsDir/scaladoc")))
        assertThat(task.defaultSource, equalTo(project.sourceSets.main.scala))
        assertThat(task.classpath.sourceCollections, hasItem(project.sourceSets.main.classes))
        assertThat(task.classpath.sourceCollections, hasItem(project.sourceSets.main.compileClasspath))
        assertThat(task.title, equalTo(project.apiDocTitle))
    }
}
