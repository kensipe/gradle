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
package org.gradle.api;

import groovy.lang.Closure;
import groovy.lang.MissingPropertyException;
import org.gradle.api.logging.LogLevel;
import org.gradle.api.logging.Logger;
import org.gradle.api.plugins.Convention;
import org.gradle.api.specs.Spec;
import org.gradle.api.tasks.TaskDependency;
import org.gradle.api.tasks.TaskInputs;
import org.gradle.api.tasks.TaskOutputs;

import java.util.List;
import java.util.Set;

/**
 * <p>A <code>Task</code> represents a single step of a build, such as compiling classes or generating javadoc.</p>
 *
 * <p>Each task belongs to a {@link Project}. You can use the various methods on {@link
 * org.gradle.api.tasks.TaskContainer} to create and lookup task instances. For example, {@link
 * org.gradle.api.tasks.TaskContainer#add(String)} creates an empty task with the given name. You can also use the
 * {@code task} keyword in your build file: </p>
 * <pre>
 * task myTask
 * task myTask { configure closure }
 * task myType &lt;&lt; { task action }
 * task myTask(type: SomeType)
 * task myTask(type: SomeType) { configure closure }
 * </pre>
 *
 * <p>Each task has a name, which can be used to refer to the task within its owning project, and a fully qualified
 * path, which is unique across all tasks in all projects. The path is the concatenation of the owning project's path
 * and the task's name. Path elements are separated using the {@value org.gradle.api.Project#PATH_SEPARATOR}
 * character.</p>
 *
 * <h3>Task Actions</h3>
 *
 * <p>A <code>Task</code> is made up of a sequence of {@link Action} objects. When the task is executed, each of the
 * actions is executed in turn, by calling {@link Action#execute}.  You can add actions to a task by calling {@link
 * #doFirst(Action)} or {@link #doLast(Action)}.</p>
 *
 * <p>Groovy closures can also be used to provide a task action. When the action is executed, the closure is called with
 * the task as parameter.  You can add action closures to a task by calling {@link #doFirst(groovy.lang.Closure)} or
 * {@link #doLast(groovy.lang.Closure)}  or using the left-shift &lt;&lt; operator.</p>
 *
 * There are 2 special exceptions which a task action can throw to abort execution and continue without failing the
 * build. A task action can abort execution of the action and continue to the next action of the task by throwing a
 * {@link org.gradle.api.tasks.StopActionException}. A task action can abort execution of the task and continue to the
 * next task by throwing a {@link org.gradle.api.tasks.StopExecutionException}. Using these exceptions allows you to
 * have precondition actions which skip execution of the task, or part of the task, if not true.</p>
 *
 * <a name="dependencies"/><h3>Dependencies</h3>
 *
 * <p>A task may have dependencies on other tasks. Gradle ensures that tasks are executed in dependency order, so that
 * the dependencies of a task are executed before the task is executed.  You can add dependencies to a task using {@link
 * #dependsOn(Object...)} or {@link #setDependsOn(Iterable)}.  You can add objects of any of the following types as a
 * dependency:</p>
 *
 * <ul>
 *
 * <li>A {@code String} task path or name. A relative path is interpreted relative to the task's {@link Project}. This
 * allows you to refer to tasks in other projects.</li>
 *
 * <li>A {@link Task}.</li>
 *
 * <li>A closure. The closure may take a {@code Task} as parameter. It may return any of the types listed here. Its
 * return value is recursively converted to tasks. A {@code null} return value is treated as an empty collection.</li>
 *
 * <li>A {@link TaskDependency} object.</li>
 *
 * <li>A {@link Buildable} object.</li>
 *
 * <li>A {@code Collection}, {@code Map} or an array. May contain any of the types listed here. The elements of the
 * collection/map/array are recursively converted to tasks.</li>
 *
 * <li>A {@code Callable}. The {@code call()} method may return any of the types listed here. Its return value is
 * recursively converted to tasks. A {@code null} return value is treated as an empty collection.</li>
 *
 * <li>An {@code Object}. The object's {@code toString()} method is interpreted as a task path or name.</li>
 *
 * </ul>
 *
 * <h3>Using a Task in a Build File</h3>
 *
 * <a name="properties"/> <h4>Dynamic Properties</h4>
 *
 * <p>A {@code Task} has 3 'scopes' for properties. You can access these properties by name from the build file or by
 * calling the {@link #property(String)} method.</p>
 *
 * <ul>
 *
 * <li>The {@code Task} object itself. This includes any property getters and setters declared by the {@code Task}
 * implementation class.  The properties of this scope are readable or writable based on the presence of the
 * corresponding getter and setter methods.</li>
 *
 * <li>The <em>additional properties</em> of the task. Each task object maintains a map of additional properties. These
 * are arbitrary name -> value pairs which you can use to dynamically add properties to a task object.  The properties
 * of this scope are readable and writable.</li>
 *
 * <li>The <em>convention</em> properties added to the task by each {@link Plugin} applied to the project. A {@link
 * Plugin} can add properties and methods to a task through the task's {@link Convention} object.  The properties of
 * this scope may be readable or writable, depending on the convention objects.</li>
 *
 * </ul>
 *
 * <h4>Dynamic Methods</h4>
 *
 * <p>A {@link Plugin} may add methods to a {@code Task} using its {@link Convention} object.</p>
 *
 * @author Hans Dockter
 */
public interface Task extends Comparable<Task> {
    public static final String TASK_NAME = "name";

    public static final String TASK_DESCRIPTION = "description";

    public static final String TASK_TYPE = "type";

    public static final String TASK_DEPENDS_ON = "dependsOn";

    public static final String TASK_OVERWRITE = "overwrite";

    public static final String TASK_ACTION = "action";

    public final static String AUTOSKIP_PROPERTY_PREFIX = "skip.";

    /**
     * </p>Returns the name of this task. The name uniquely identifies the task within its {@link Project}.</p>
     *
     * @return The name of the task. Never returns null.
     */
    String getName();

    /**
     * <p>Returns the {@link Project} which this task belongs to.</p>
     *
     * @return The project this task belongs to. Never returns null.
     */
    Project getProject();

    /**
     * <p>Returns the sequence of {@link Action} objects which will be executed by this task, in the order of
     * execution.</p>
     *
     * @return The task actions in the order they are executed. Returns an empty list if this task has no actions.
     */
    List<Action<? super Task>> getActions();

    /**
     * <p>Sets the sequence of {@link Action} objects which will be executed by this task.</p>
     *
     * @param actions The actions.
     */
    void setActions(List<Action<? super Task>> actions);

    /**
     * <p>Returns a {@link TaskDependency} which contains all the tasks that this task depends on.</p>
     *
     * @return The dependencies of this task. Never returns null.
     */
    TaskDependency getTaskDependencies();

    /**
     * <p>Returns the dependencies of this task.</p>
     *
     * @return The dependencies of this task. Returns an empty set if this task has no dependencies.
     */
    Set<Object> getDependsOn();

    /**
     * <p>Sets the dependencies of this task. See <a href="#dependencies">here</a> for a description of the types of
     * objects which can be used as task dependencies.</p>
     *
     * @param dependsOnTasks The set of task paths.
     */
    void setDependsOn(Iterable<?> dependsOnTasks);

    /**
     * <p>Adds the given dependencies to this task. See <a href="#dependencies">here</a> for a description of the types
     * of objects which can be used as task dependencies.</p>
     *
     * @param paths The dependencies to add to this task.
     * @return the task object this method is applied to
     */
    Task dependsOn(Object... paths);

    /**
     * <p>Execute the task only if the closure returns true.  The closure will be evaluated at the task execution time,
     * not during configuration.  The closure will be passed a single parameter, this task.  The closure will be coerced
     * to a {@link org.gradle.api.specs.Spec} of type {@link Task}. </p> <p>If the closure returns false, the task will
     * not execute, but will log a message of 'SKIPPED as onlyIf is false'.</p> <p>Typical usage:</p>
     * <code>myTask.onlyIf{ dependsOnTaskDidWork() } </code>
     *
     * @param onlyIfClosure code to execute to determine if task should be run
     * @see #getDidWork()
     */
    void onlyIf(Closure onlyIfClosure);

    /**
     * <p>Execute the task only if the spec is satisfied. The spec will be evaluated at task execution time, not during
     * configuration. </p> <p>If the Spec is not satisfied, the task will not execute, but will log a message of
     * 'SKIPPED as onlyIf is false'.</p> <p>Typical usage (from Java):</p>
     * <pre>myTask.onlyIf(new Spec<Task>() {
     *    boolean isSatisfiedBy(Task task) {
     *       return task.dependsOnTaskDidWork();
     *    }
     * });
     * </pre>
     *
     * @param onlyIfSpec specifies if a task should be run
     * @see #getDidWork()
     */
    void onlyIf(Spec<? super Task> onlyIfSpec);

    /**
     * <p>Checks if the task actually did any work.  Even if a Task executes, it may determine that it has nothing to
     * do.  For example, the Compile task may determine that source files have not changed since the last time a the
     * task was run.</p>
     *
     * @return true if this task did any work
     */
    boolean getDidWork();

    /**
     * <p>Returns true if this task has been executed.</p>
     *
     * @return true if this task has been executed already, false otherwise.
     */
    boolean getExecuted();

    /**
     * <p>Returns the path of the task, which is a fully qualified name for the task. The path of a task is the path of
     * its {@link Project} plus the name of the task, separated by <code>:</code>.</p>
     *
     * @return the path of the task, which is equal to the path of the project plus the name of the task.
     */
    String getPath();

    /**
     * <p>Adds the given {@link Action} to the beginning of this task's action list.</p>
     *
     * @param action The action to add
     * @return the task object this method is applied to
     */
    Task doFirst(Action<? super Task> action);

    /**
     * <p>Adds the given closure to the beginning of this task's action list. The closure is passed this task as a
     * parameter when executed.</p>
     *
     * @param action The action closure to execute.
     * @return This task.
     */
    Task doFirst(Closure action);

    /**
     * <p>Adds the given {@link Action} to the end of this task's action list.</p>
     *
     * @param action The action to add.
     * @return the task object this method is applied to
     */
    Task doLast(Action<? super Task> action);

    /**
     * <p>Adds the given closure to the end of this task's action list.  The closure is passed this task as a parameter
     * when executed.</p>
     *
     * @param action The action closure to execute.
     * @return This task.
     */
    Task doLast(Closure action);

    /**
     * <p>Adds the given closure to the end of this task's action list.  The closure is passed this task as a parameter
     * when executed. You can call this method from your build script using the &lt;&lt; left shift operator.</p>
     *
     * @param action The action closure to execute.
     * @return This task.
     */
    Task leftShift(Closure action);

    /**
     * <p>Removes all the actions of this task.</p>
     *
     * @return the task object this method is applied to
     */
    Task deleteAllActions();

    /**
     * <p>Returns if this task is enabled or not.</p>
     *
     * @see #setEnabled(boolean)
     */
    boolean getEnabled();

    /**
     * <p>Set the enabled state of a task. If a task is disabled none of the its actions are executed. Note that
     * disabling a task does not prevent the execution of the tasks which this task depends on.</p>
     *
     * @param enabled The enabled state of this task (true or false)
     */
    void setEnabled(boolean enabled);

    /**
     * <p>Applies the statements of the closure against this task object. The delegate object for the closure is set to
     * this task.</p>
     *
     * @param configureClosure The closure to be applied (can be null).
     * @return This task
     */
    Task configure(Closure configureClosure);

    /**
     * <p>Returns the <code>AntBuilder</code> for this task.  You can use this in your build file to execute ant
     * tasks.</p>
     *
     * @return The <code>AntBuilder</code>
     */
    AntBuilder getAnt();

    /**
     * <p>Returns the logger for this task. You can use this in your build file to write log messages.</p>
     *
     * @return The logger. Never returns null.
     */
    Logger getLogger();

    /**
     * Disables redirection of standard output during task execution. By default redirection is enabled.
     *
     * @return this
     * @see #captureStandardOutput(org.gradle.api.logging.LogLevel)
     */
    Task disableStandardOutputCapture();

    /**
     * Enables redirection of standard output during task execution to the logging system. By default redirection is
     * enabled and the task output is redirected to the QUIET level. System.err is always redirected to the ERROR level.
     * An exception is thrown, if this method is called during the execution of the task
     *
     * For more fine-grained control on redirecting standard output see {@link org.gradle.api.logging.StandardOutputLogging}.
     *
     * @param level The level standard out should be logged to.
     * @return this
     * @see #disableStandardOutputCapture()
     */
    Task captureStandardOutput(LogLevel level);

    /**
     * Returns the value of the given property of this task.  This method locates a property as follows:</p>
     *
     * <ol>
     *
     * <li>If this task object has a property with the given name, return the value of the property.</li>
     *
     * <li>If this task has an additional property with the given name, return the value of the property.</li>
     *
     * <li>If this task's convention object has a property with the given name, return the value of the property.</li>
     *
     * <li>If not found, throw {@link MissingPropertyException}</li>
     *
     * </ol>
     *
     * @param propertyName The name of the property.
     * @return The value of the property, possibly null.
     * @throws MissingPropertyException When the given property is unknown.
     */
    Object property(String propertyName) throws MissingPropertyException;

    /**
     * <p>Determines if this task has the given property. See <a href="#properties">here</a> for details of the
     * properties which are available for a task.</p>
     *
     * @param propertyName The name of the property to locate.
     * @return True if this project has the given property, false otherwise.
     */
    boolean hasProperty(String propertyName);

    /**
     * <p>Sets a property of this task.  This method searches for a property with the given name in the following
     * locations, and sets the property on the first location where it finds the property.</p>
     *
     * <ol>
     *
     * <li>The task object itself.  For example, the <code>enabled</code> project property.</li>
     *
     * <li>The task's convention object.</li>
     *
     * <li>The task's additional properties.</li>
     *
     * </ol>
     *
     * <p>If the property is not found in any of these locations, it is added to the project's additional
     * properties.</p>
     *
     * @param name The name of the property
     * @param value The value of the property
     */
    void setProperty(String name, Object value);

    /**
     * <p>Returns the {@link Convention} object for this task. A {@link Plugin} can use the convention object to
     * contribute properties and methods to this task.</p>
     *
     * @return The convention object. Never returns null.
     */
    Convention getConvention();

    /**
     * Returns the description of a task.
     *
     * @see #setDescription(String)
     */
    String getDescription();

    /**
     * Adds a text to describe what the task does to the user of the build. The description will be displayed when
     * <code>gradle -t</code> is called.
     *
     * @param description The description of the task. Might be null.
     */
    void setDescription(String description);

    /**
     * <p>Checks if any of the tasks that this task depends on {@link Task#getDidWork() didWork}.</p>
     *
     * @return true if any task this task depends on did work.
     */
    boolean dependsOnTaskDidWork();

    /**
     * <p>Returns the inputs of this task.</p>
     *
     * @return The inputs. Never returns null.
     */
    TaskInputs getInputs();

    /**
     * <p>Returns the outputs of this task.</p>
     *
     * @return The outputs. Never returns null.
     */
    TaskOutputs getOutputs();
}

