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

package org.gradle.api.plugins.jetty;

import org.gradle.api.*;
import org.gradle.api.plugins.jetty.internal.*;
import org.gradle.api.tasks.*;
import org.gradle.api.internal.ConventionTask;
import org.mortbay.jetty.Connector;
import org.mortbay.jetty.RequestLog;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.security.UserRealm;
import org.mortbay.util.Scanner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;

public abstract class AbstractJettyRunTask extends ConventionTask {
    private static Logger logger = LoggerFactory.getLogger(AbstractJettyRunTask.class);

    private Iterable<File> additionalRuntimeJars = new ArrayList<File>();

    /**
     * The proxy for the Server object
     */
    private JettyPluginServer server;

    /**
     * The "virtual" webapp created by the plugin
     */
    private JettyPluginWebAppContext webAppConfig;

    /**
     * The context path for the webapp.
     */
    private String contextPath;

    /**
     * The temporary directory to use for the webapp.
     */
    private File tmpDirectory;

    /**
     * A webdefault.xml file to use instead of the default for the webapp. Optional.
     */
    private File webDefaultXml;

    /**
     * A web.xml file to be applied AFTER the webapp's web.xml file. Useful for applying different build profiles, eg
     * test, production etc. Optional.
     */
    private File overrideWebXml;

    /**
     * The interval in seconds to scan the webapp for changes and restart the context if necessary. Ignored if reload is
     * enabled. Disabled by default.
     */
    private int scanIntervalSeconds;

    /**
     * reload can be set to either 'automatic' or 'manual' <p/> if 'manual' then the context can be reloaded by a
     * linefeed in the console if 'automatic' then traditional reloading on changed files is enabled.
     */
    protected String reload;

    /**
     * Location of a jetty xml configuration file whose contents will be applied before any plugin configuration.
     * Optional.
     */
    private File jettyConfig;

    /**
     * Port to listen to stop jetty on executing -DSTOP.PORT=&lt;stopPort&gt; -DSTOP.KEY=&lt;stopKey&gt; -jar start.jar
     * --stop
     */
    private Integer stopPort;

    /**
     * Key to provide when stopping jetty on executing java -DSTOP.KEY=&lt;stopKey&gt; -DSTOP.PORT=&lt;stopPort&gt; -jar
     * start.jar --stop
     */
    private String stopKey;

    /**
     * <p> Determines whether or not the server blocks when started. The default behavior (daemon = false) will cause
     * the server to pause other processes while it continues to handle web requests. This is useful when starting the
     * server with the intent to work with it interactively. </p><p> Often, it is desirable to let the server start and
     * continue running subsequent processes in an automated build environment. This can be facilitated by setting
     * daemon to true. </p>
     */
    private boolean daemon;

    private Integer httpPort;

    /**
     * List of connectors to use. If none are configured then we use a single SelectChannelConnector at port 8080
     */
    private Connector[] connectors;

    /**
     * List of security realms to set up. Optional.
     */
    private UserRealm[] userRealms;

    /**
     * A RequestLog implementation to use for the webapp at runtime. Optional.
     */
    private RequestLog requestLog;

    /**
     * A scanner to check for changes to the webapp
     */
    private Scanner scanner = new Scanner();

    /**
     * List of Listeners for the scanner
     */
    protected ArrayList scannerListeners;

    /**
     * A scanner to check ENTER hits on the console
     */
    protected Thread consoleScanner;

    public static final String PORT_SYSPROPERTY = "jetty.port";

    public abstract void validateConfiguration();

    public abstract void configureScanner();

    public abstract void applyJettyXml() throws Exception;

    /**
     * create a proxy that wraps a particular jetty version Server object
     *
     * @return The Jetty Plugin Server
     */
    public abstract JettyPluginServer createServer() throws Exception;

    public abstract void finishConfigurationBeforeStart() throws Exception;

    @org.gradle.api.tasks.TaskAction
    protected void start() {
        ClassLoader originalClassloader = Thread.currentThread().getContextClassLoader();
        List<URL> additionalClasspath = new ArrayList<URL>();
        for (File additionalRuntimeJar : getAdditionalRuntimeJars()) {
            try {
                additionalClasspath.add(additionalRuntimeJar.toURI().toURL());
            } catch (MalformedURLException e) {
                throw new InvalidUserDataException(e);
            }
        }
        URLClassLoader jettyClassloader = new URLClassLoader(additionalClasspath.toArray(
                new URL[additionalClasspath.size()]), originalClassloader);
        try {
            Thread.currentThread().setContextClassLoader(jettyClassloader);
            startJetty();
        } finally {
            Thread.currentThread().setContextClassLoader(originalClassloader);
        }
    }

    public JettyPluginServer getServer() {
        return this.server;
    }

    public void setServer(JettyPluginServer server) {
        this.server = server;
    }

    public void setScannerListeners(ArrayList listeners) {
        this.scannerListeners = new ArrayList(listeners);
    }

    public ArrayList getScannerListeners() {
        return this.scannerListeners;
    }

    public Scanner getScanner() {
        return scanner;
    }

    public void startJetty() {
        logger.info("Configuring Jetty for " + getProject());
        validateConfiguration();
        startJettyInternal();
    }

    public void startJettyInternal() {
        try {
            logger.debug("Starting Jetty Server ...");

            setServer(createServer());

            applyJettyXml();

            JettyPluginServer plugin = getServer();

            Object[] configuredConnectors = getConnectors();

            plugin.setConnectors(configuredConnectors);
            Object[] connectors = plugin.getConnectors();

            if (connectors == null || connectors.length == 0) {
                configuredConnectors = new Object[]{plugin.createDefaultConnector(getHttpPort())};
                plugin.setConnectors(configuredConnectors);
            }

            //set up a RequestLog if one is provided
            if (getRequestLog() != null) {
                getServer().setRequestLog(getRequestLog());
            }

            //set up the webapp and any context provided
            getServer().configureHandlers();
            configureWebApplication();
            getServer().addWebApplication(webAppConfig);

            // set up security realms
            Object[] configuredRealms = getUserRealms();
            for (int i = 0; (configuredRealms != null) && i < configuredRealms.length; i++) {
                logger.debug(configuredRealms[i].getClass().getName() + ": " + configuredRealms[i].toString());
            }

            plugin.setUserRealms(configuredRealms);

            //do any other configuration required by the
            //particular Jetty version
            finishConfigurationBeforeStart();

            // start Jetty
            server.start();

            logger.info("Started Jetty Server");

            if (getStopPort() != null && getStopPort() > 0 && getStopKey() != null) {
                Monitor monitor = new Monitor(getStopPort(), getStopKey(),
                        new Server[]{(Server) server.getProxiedObject()}, !daemon);
                monitor.start();
            }

            // start the scanner thread (if necessary) on the main webapp
            configureScanner();
            startScanner();

            // start the new line scanner thread if necessary
            startConsoleScanner();

            // keep the thread going if not in daemon mode
            if (!daemon) {
                server.join();
            }
        } catch (Exception e) {
            throw new GradleException("An error occurred starting the Jetty server.", e);
        } finally {
            if (!daemon) {
                logger.info("Jetty server exiting.");
            }
        }
    }

    public abstract void restartWebApp(boolean reconfigureScanner) throws Exception;

    /**
     * Subclasses should invoke this to setup basic info on the webapp
     */
    public void configureWebApplication() throws Exception {
        //use EITHER a <webAppConfig> element or the now deprecated <contextPath>, <tmpDirectory>, <webDefaultXml>, <overrideWebXml>
        //way of doing things
        if (webAppConfig == null) {
            webAppConfig = new JettyPluginWebAppContext();
        }
        webAppConfig.setContextPath((getContextPath().startsWith("/") ? getContextPath() : "/" + getContextPath()));
        if (getTmpDirectory() != null) {
            webAppConfig.setTempDirectory(getTmpDirectory());
        }
        if (getWebDefaultXml() != null) {
            webAppConfig.setDefaultsDescriptor(getWebDefaultXml().getCanonicalPath());
        }
        if (getOverrideWebXml() != null) {
            webAppConfig.setOverrideDescriptor(getOverrideWebXml().getCanonicalPath());
        }

        // Don't treat JCL or Log4j as system classes
        Set<String> systemClasses = new LinkedHashSet<String>(Arrays.asList(webAppConfig.getSystemClasses()));
        systemClasses.remove("org.apache.commons.logging.");
        systemClasses.remove("org.apache.log4j.");
        webAppConfig.setSystemClasses(systemClasses.toArray(new String[systemClasses.size()]));

        webAppConfig.setParentLoaderPriority(false);

        logger.info("Context path = " + webAppConfig.getContextPath());
        logger.info("Tmp directory = " + " determined at runtime");
        logger.info("Web defaults = " + (webAppConfig.getDefaultsDescriptor() == null ? " jetty default"
                : webAppConfig.getDefaultsDescriptor()));
        logger.info("Web overrides = " + (webAppConfig.getOverrideDescriptor() == null ? " none"
                : webAppConfig.getOverrideDescriptor()));
    }

    /**
     * Run a scanner thread on the given list of files and directories, calling stop/start on the given list of
     * LifeCycle objects if any of the watched files change.
     */
    private void startScanner() {

        // check if scanning is enabled
        if (getScanIntervalSeconds() <= 0) {
            return;
        }

        // check if reload is manual. It disables file scanning
        if ("manual".equalsIgnoreCase(reload)) {
            // issue a warning if both scanIntervalSeconds and reload
            // are enabled
            logger.warn("scanIntervalSeconds is set to " + scanIntervalSeconds
                    + " but will be IGNORED due to manual reloading");
            return;
        }

        scanner.setReportExistingFilesOnStartup(false);
        scanner.setScanInterval(getScanIntervalSeconds());
        scanner.setRecursive(true);
        List listeners = getScannerListeners();
        Iterator itor = (listeners == null ? null : listeners.iterator());
        while (itor != null && itor.hasNext()) {
            scanner.addListener((Scanner.Listener) itor.next());
        }
        logger.info("Starting scanner at interval of " + getScanIntervalSeconds() + " seconds.");
        scanner.start();
    }

    /**
     * Run a thread that monitors the console input to detect ENTER hits.
     */
    protected void startConsoleScanner() {
        if ("manual".equalsIgnoreCase(reload)) {
            logger.info("Console reloading is ENABLED. Hit ENTER on the console to restart the context.");
            consoleScanner = new ConsoleScanner(this);
            consoleScanner.start();
        }
    }

    /**
     * Try and find a jetty-web.xml file, using some historical naming conventions if necessary.
     *
     * @return File object to the location of the jetty-web.xml
     */
    public File findJettyWebXmlFile(File webInfDir) {
        if (webInfDir == null) {
            return null;
        }
        if (!webInfDir.exists()) {
            return null;
        }

        File f = new File(webInfDir, "jetty-web.xml");
        if (f.exists()) {
            return f;
        }

        //try some historical alternatives
        f = new File(webInfDir, "web-jetty.xml");
        if (f.exists()) {
            return f;
        }
        f = new File(webInfDir, "jetty6-web.xml");
        if (f.exists()) {
            return f;
        }

        return null;
    }

    @OutputDirectory
    @Optional
    public File getTmpDirectory() {
        return tmpDirectory;
    }

    public void setTmpDirectory(File tmpDirectory) {
        this.tmpDirectory = tmpDirectory;
    }

    @InputFile
    @Optional
    public File getWebDefaultXml() {
        return webDefaultXml;
    }

    public void setWebDefaultXml(File webDefaultXml) {
        this.webDefaultXml = webDefaultXml;
    }

    @InputFile
    @Optional
    public File getOverrideWebXml() {
        return overrideWebXml;
    }

    public void setOverrideWebXml(File overrideWebXml) {
        this.overrideWebXml = overrideWebXml;
    }

    public int getScanIntervalSeconds() {
        return scanIntervalSeconds;
    }

    public void setScanIntervalSeconds(int scanIntervalSeconds) {
        this.scanIntervalSeconds = scanIntervalSeconds;
    }

    public String getContextPath() {
        return contextPath;
    }

    public void setContextPath(String contextPath) {
        this.contextPath = contextPath;
    }

    public JettyPluginWebAppContext getWebAppConfig() {
        return webAppConfig;
    }

    public void setWebAppConfig(JettyPluginWebAppContext webAppConfig) {
        this.webAppConfig = webAppConfig;
    }

    public String getReload() {
        return reload;
    }

    public void setReload(String reload) {
        this.reload = reload;
    }

    @InputFile
    @Optional
    public File getJettyConfig() {
        return jettyConfig;
    }

    public void setJettyConfig(File jettyConfig) {
        this.jettyConfig = jettyConfig;
    }

    public Integer getStopPort() {
        return stopPort;
    }

    public void setStopPort(Integer stopPort) {
        this.stopPort = stopPort;
    }

    public String getStopKey() {
        return stopKey;
    }

    public void setStopKey(String stopKey) {
        this.stopKey = stopKey;
    }

    public boolean isDaemon() {
        return daemon;
    }

    public void setDaemon(boolean daemon) {
        this.daemon = daemon;
    }

    public Integer getHttpPort() {
        return httpPort;
    }

    public void setHttpPort(Integer httpPort) {
        this.httpPort = httpPort;
    }

    public Connector[] getConnectors() {
        return connectors;
    }

    public void setConnectors(Connector[] connectors) {
        this.connectors = connectors;
    }

    public UserRealm[] getUserRealms() {
        return userRealms;
    }

    public void setUserRealms(UserRealm[] userRealms) {
        this.userRealms = userRealms;
    }

    public RequestLog getRequestLog() {
        return requestLog;
    }

    public void setRequestLog(RequestLog requestLog) {
        this.requestLog = requestLog;
    }

    @InputFiles
    public Iterable<File> getAdditionalRuntimeJars() {
        return additionalRuntimeJars;
    }

    public void setAdditionalRuntimeJars(Iterable<File> additionalRuntimeJars) {
        this.additionalRuntimeJars = additionalRuntimeJars;
    }
}
