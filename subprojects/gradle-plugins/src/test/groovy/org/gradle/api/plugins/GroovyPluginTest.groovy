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
 
package org.gradle.api.plugins

import org.gradle.api.Project
import org.gradle.api.internal.artifacts.configurations.Configurations
import org.gradle.api.tasks.compile.GroovyCompile
import org.gradle.api.tasks.javadoc.Groovydoc
import org.gradle.util.HelperUtil
import org.junit.Test
import static org.gradle.util.Matchers.*
import static org.gradle.util.WrapUtil.*
import static org.hamcrest.Matchers.*
import static org.junit.Assert.*

/**
 * @author Hans Dockter
 */
class GroovyPluginTest {
    private final Project project = HelperUtil.createRootProject()
    private final GroovyPlugin groovyPlugin = new GroovyPlugin()

    @Test public void appliesTheJavaPluginToTheProject() {
        groovyPlugin.use(project, project.getPlugins())

        assertTrue(project.getPlugins().hasPlugin(JavaPlugin));
    }

    @Test public void addsGroovyConfigurationToTheProject() {
        groovyPlugin.use(project, project.getPlugins())

        def configuration = project.configurations.getByName(JavaPlugin.COMPILE_CONFIGURATION_NAME)
        assertThat(Configurations.getNames(configuration.extendsFrom, false), equalTo(toSet(GroovyPlugin.GROOVY_CONFIGURATION_NAME)))
        assertFalse(configuration.visible)
        assertFalse(configuration.transitive)

        configuration = project.configurations.getByName(GroovyPlugin.GROOVY_CONFIGURATION_NAME)
        assertThat(Configurations.getNames(configuration.extendsFrom, false), equalTo(toSet()))
        assertFalse(configuration.visible)
        assertFalse(configuration.transitive)
    }

    @Test public void addsGroovyConventionToEachSourceSetAndAppliesMappings() {
        groovyPlugin.use(project, project.getPlugins())

        def sourceSet = project.sourceSets.main
        assertThat(sourceSet.groovy.displayName, equalTo("main Groovy source"))
        assertThat(sourceSet.groovy.srcDirs, equalTo(toLinkedSet(project.file("src/main/groovy"))))

        sourceSet = project.sourceSets.test
        assertThat(sourceSet.groovy.displayName, equalTo("test Groovy source"))
        assertThat(sourceSet.groovy.srcDirs, equalTo(toLinkedSet(project.file("src/test/groovy"))))

        sourceSet = project.sourceSets.add('custom')
        assertThat(sourceSet.groovy.displayName, equalTo("custom Groovy source"))
        assertThat(sourceSet.groovy.srcDirs, equalTo(toLinkedSet(project.file("src/custom/groovy"))))
    }

    @Test public void addsCompileTaskForEachSourceSet() {
        groovyPlugin.use(project, project.getPlugins())

        def task = project.tasks['compileGroovy']
        assertThat(task, instanceOf(GroovyCompile.class))
        assertThat(task.description, equalTo('Compiles the main Groovy source.'))
        assertThat(task.defaultSource, equalTo(project.sourceSets.main.groovy))
        assertThat(task, dependsOn(JavaPlugin.COMPILE_JAVA_TASK_NAME))

        task = project.tasks['compileTestGroovy']
        assertThat(task, instanceOf(GroovyCompile.class))
        assertThat(task.description, equalTo('Compiles the test Groovy source.'))
        assertThat(task.defaultSource, equalTo(project.sourceSets.test.groovy))
        assertThat(task, dependsOn(JavaPlugin.COMPILE_TEST_JAVA_TASK_NAME, JavaPlugin.CLASSES_TASK_NAME))

        project.sourceSets.add('custom')

        task = project.tasks['compileCustomGroovy']
        assertThat(task, instanceOf(GroovyCompile.class))
        assertThat(task.description, equalTo('Compiles the custom Groovy source.'))
        assertThat(task.defaultSource, equalTo(project.sourceSets.custom.groovy))
        assertThat(task, dependsOn('compileCustomJava'))
    }

    @Test public void dependenciesOfJavaPluginTasksIncludeGroovyCompileTasks() {
        groovyPlugin.use(project, project.getPlugins())

        def task = project.tasks[JavaPlugin.CLASSES_TASK_NAME]
        assertThat(task, dependsOn(hasItem('compileGroovy')))

        task = project.tasks[JavaPlugin.TEST_CLASSES_TASK_NAME]
        assertThat(task, dependsOn(hasItem('compileTestGroovy')))
    }
    
    @Test public void addsStandardTasksToTheProject() {
        groovyPlugin.use(project, project.getPlugins())

        def task = project.tasks[GroovyPlugin.GROOVYDOC_TASK_NAME]
        assertThat(task, instanceOf(Groovydoc.class))
        assertThat(task.destinationDir, equalTo(new File(project.docsDir, 'groovydoc')))
        assertThat(task.defaultSource, equalTo(project.sourceSets.main.groovy))
        assertThat(task.docTitle, equalTo(project.apiDocTitle))
        assertThat(task.windowTitle, equalTo(project.apiDocTitle))
    }

    @Test public void configuresAdditionalTasksDefinedByTheBuildScript() {
        groovyPlugin.use(project, project.getPlugins())
        
        def task = project.createTask('otherCompile', type: GroovyCompile)
        assertThat(task.classpath, sameInstance(project.sourceSets.main.compileClasspath))
        assertThat(task.defaultSource, equalTo(project.sourceSets.main.groovy))

        task = project.createTask('otherGroovydoc', type: Groovydoc)
        assertThat(task.destinationDir, equalTo(new File(project.docsDir, 'groovydoc')))
        assertThat(task.defaultSource, equalTo(project.sourceSets.main.groovy))
        assertThat(task.docTitle, equalTo(project.apiDocTitle))
        assertThat(task.windowTitle, equalTo(project.apiDocTitle))
    }
}
