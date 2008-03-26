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

package org.gradle.api.tasks.util

import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * @author Hans Dockter
 */
class ExistingDirsFilter {
    Logger logger = LoggerFactory.getLogger(ExistingDirsFilter)
    
    Collection findExistingDirs(Collection dirFiles) {
        dirFiles.findAll {File file -> file.isDirectory()}
    }

    Collection findExistingDirsAndLogexitMessages(Collection dirFiles) {
        logger.debug("Looking for existing folders: $dirFiles")
        Collection result = findExistingDirs(dirFiles)
        if (!result) {
            logger.debug("No existing directories to work on. We don't do anything here.")
        }
        result
    }

    boolean checkExistenceAndLogExitMessage(File dir) {
        findExistingDirsAndLogexitMessages([dir])    
    }
}