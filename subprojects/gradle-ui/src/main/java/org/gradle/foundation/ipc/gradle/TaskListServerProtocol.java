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
package org.gradle.foundation.ipc.gradle;

import org.gradle.StartParameter;
import org.gradle.api.logging.LogLevel;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;
import org.gradle.foundation.ProjectView;
import org.gradle.foundation.ipc.basic.MessageObject;

import java.io.File;
import java.util.List;

/**
 * This manages the communication between the UI and an externally-launched copy of Gradle when using socket-based
 * inter-process communication. This is the server side for building a task list. This listens for messages from the
 * gradle client.
 *
 * @author mhunsicker
 */
public class TaskListServerProtocol extends AbstractGradleServerProtocol {
    private final Logger logger = Logging.getLogger(TaskListServerProtocol.class);

    private static final String INIT_SCRIPT_NAME = "refresh-tasks-init-script";

    private ExecutionInteraction executionInteraction;

    public interface ExecutionInteraction {
        /**
         * Notification that gradle has started execution. This may not get called if some error occurs that prevents
         * gradle from running.
         */
        void reportExecutionStarted();

        /**
         * Notification that execution has finished. Note: if the client fails to launch at all, this should still be
         * called.
         *
         * @param wasSuccessful true if gradle was successful (returned 0)
         * @param message the output of gradle if it ran. If it didn't, an error message.
         * @param throwable an exception if one occurred
         * @param projects a hierachical list of projects. This is the final result.
         */
        void reportExecutionFinished(boolean wasSuccessful, String message, Throwable throwable,
                                     List<ProjectView> projects);

        void reportLiveOutput(String message);
    }

    public TaskListServerProtocol(File currentDirectory, File gradleHomeDirectory, File customGradleExecutor,
                                  String fullCommandLine, LogLevel logLevel,
                                  StartParameter.ShowStacktrace stackTraceLevel,
                                  ExecutionInteraction executionInteraction) {
        super(currentDirectory, gradleHomeDirectory, customGradleExecutor, fullCommandLine, logLevel, stackTraceLevel);
        this.executionInteraction = executionInteraction;
    }

    /**
     * Notification that a message was received that we didn't process. Implement this to handle the specifics of your
     * protocol. Basically, the base class handles the handshake. The rest of the conversation is up to you.
     *
     * @param message the message we received.
     */
    protected boolean handleMessageReceived(MessageObject message) {
        if (ProtocolConstants.TASK_LIST_COMPLETED_WITH_ERRORS_TYPE.equals(
                message.getMessageType())) {  //if we were NOT successful, we'll have a BuildResultsWrapper instead of a project list as our data.
            setHasReceivedBuildCompleteNotification();

            Boolean wasSuccessful = (Boolean) message.getData();

            executionInteraction.reportExecutionFinished(wasSuccessful, message.getMessage(), null, null);
            return true;
        }

        if (ProtocolConstants.TASK_LIST_COMPLETED_SUCCESSFULLY_TYPE.equals(
                message.getMessageType())) {  //if we were successful, we'll have a project list instead of a BuildResultsWrapper as our data.
            setHasReceivedBuildCompleteNotification();

            List<ProjectView> projects = (List<ProjectView>) message.getData();

            executionInteraction.reportExecutionFinished(true, message.getMessage(), null, projects);
            return true;
        }

        if (ProtocolConstants.EXITING.equals(message.getMessageType())) {
            closeConnection();   //the client is done.
            return true;
        }

        return false;
    }

    /**
     * This is called if the client exits prematurely. That is, we never connected to it or it didn't finish. This can
     * happen because of setup issues or errors that occur in gradle.
     *
     * @param returnCode the return code of the client application
     * @param message Whatever information we can gleen about what went wrong.
     */
    protected void reportPrematureClientExit(int returnCode, String message) {
        //in our case, if it returned prematurely, then its a failure.
        executionInteraction.reportExecutionFinished(false, message, null, null);
    }

    /**
     * Notification of any status that might be helpful to the user.
     *
     * @param status a status message
     */
    protected void addStatus(String status) {
        executionInteraction.reportLiveOutput(status);
    }

    /**
     * This is called before we execute a command. Here, return an init script for this protocol. An init script is a
     * gradle script that gets run before the other scripts are processed. This is useful here for initiating the gradle
     * client that talks to the server.
     *
     * @return The path to an init script. Null if you have no init script.
     */
    public File getInitScriptFile() {
        return this.extractInitScriptFile(TaskListServerProtocol.class, INIT_SCRIPT_NAME);
    }
}
