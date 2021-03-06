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
package org.gradle.api.plugins

import org.gradle.api.InvalidUserDataException
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.tasks.bundling.GradleManifest
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.internal.tasks.DefaultSourceSetContainer

/**
 * @author Hans Dockter
 */
// todo Think about moving the mkdir method to the project.
// todo Refactor to Java
class JavaPluginConvention {
    Project project

    String dependencyCacheDirName
    String docsDirName
    String testResultsDirName
    String testReportDirName
    final SourceSetContainer sourceSets
    private JavaVersion srcCompat
    private JavaVersion targetCompat
    GradleManifest manifest
    List metaInf

    JavaPluginConvention(Project project) {
        this.project = project
        sourceSets = new DefaultSourceSetContainer(project.fileResolver, project.tasks)
        manifest = new GradleManifest()
        metaInf = []
        dependencyCacheDirName = 'dependency-cache'
        docsDirName = 'docs'
        testResultsDirName = 'test-results'
        testReportDirName = 'tests'
    }

    def sourceSets(Closure closure) {
        sourceSets.configure(closure)
    }
    
    File mkdir(File parent = null, String name) {
        if (!name) {throw new InvalidUserDataException('You must specify the name of the directory')}
        File baseDir = parent ?: project.buildDir
        File result = new File(baseDir, name)
        result.mkdirs()
        result
    }

    File getDependencyCacheDir() {
        new File(project.buildDir, dependencyCacheDirName)
    }

    File getDocsDir() {
        new File(project.buildDir, docsDirName)
    }

    File getTestResultsDir() {
        new File(project.buildDir, testResultsDirName)
    }

    File getTestReportDir() {
        new File(reportsDir, testReportDirName)
    }

    private File getReportsDir() {
        project.convention.plugins.reportingBase.reportsDir
    }

    JavaVersion getSourceCompatibility() {
        srcCompat ?: JavaVersion.VERSION_1_5
    }

    void setSourceCompatibility(def value) {
        srcCompat = JavaVersion.toVersion(value)
    }

    JavaVersion getTargetCompatibility() {
        targetCompat ?: sourceCompatibility
    }

    void setTargetCompatibility(def value) {
        targetCompat = JavaVersion.toVersion(value)
    }
}
