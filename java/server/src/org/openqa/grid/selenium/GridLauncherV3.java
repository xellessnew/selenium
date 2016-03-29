// Licensed to the Software Freedom Conservancy (SFC) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The SFC licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.

package org.openqa.grid.selenium;

import com.google.common.collect.ImmutableMap;

import com.beust.jcommander.JCommander;

import org.openqa.grid.common.GridRole;
import org.openqa.grid.common.RegistrationRequest;
import org.openqa.grid.common.exception.GridConfigurationException;
import org.openqa.grid.internal.utils.configuration.GridHubConfiguration;
import org.openqa.grid.internal.utils.SelfRegisteringRemote;
import org.openqa.grid.internal.utils.configuration.GridNodeConfiguration;
import org.openqa.grid.internal.utils.configuration.StandaloneConfiguration;
import org.openqa.grid.shared.CliUtils;
import org.openqa.grid.web.Hub;
import org.openqa.selenium.remote.server.SeleniumServer;
import org.openqa.selenium.remote.server.log.LoggingOptions;
import org.openqa.selenium.remote.server.log.TerseFormatter;

import java.io.File;
import java.io.IOException;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

public class GridLauncherV3 {

  private static final Logger log = Logger.getLogger(GridLauncherV3.class.getName());

  public static abstract class GridItemLauncher {
    protected Object configuration;
    abstract void setConfiguration(String[] args);
    abstract void launch() throws Exception;
    void printUsage() {
      new JCommander(configuration).usage();
    }
  }

  private static ImmutableMap<GridRole, GridItemLauncher> launchers
    = new ImmutableMap.Builder<GridRole, GridItemLauncher>()
      .put(GridRole.NOT_GRID, new GridItemLauncher() {
        public void setConfiguration(String[] args) {
          configuration = new StandaloneConfiguration();
          new JCommander(configuration, args);
        }
        public void launch() throws Exception {
          log.info("Launching a standalone Selenium Server");
          SeleniumServer server = new SeleniumServer((StandaloneConfiguration)configuration);
          server.boot();
          log.info("Selenium Server is up and running");
        }
      })
    .put(GridRole.HUB, new GridItemLauncher() {
      public void setConfiguration(String[] args) {
        configuration = new GridHubConfiguration();
        new JCommander(configuration, args);
      }
      public void launch() throws Exception {
        log.info("Launching Selenium Grid hub");
        Hub h = new Hub((GridHubConfiguration)configuration);
        h.start();
        log.info("Nodes should register to " + h.getRegistrationURL());
        log.info("Selenium Grid hub is up and running");
      }
    })
    .put(GridRole.NODE, new GridItemLauncher() {
      public void setConfiguration(String[] args) {
        configuration = new GridNodeConfiguration();
        new JCommander(configuration, args);
      }
      public void launch() throws Exception {
        log.info("Launching a Selenium Grid node");
        RegistrationRequest c = RegistrationRequest.build((GridNodeConfiguration)configuration);
        SelfRegisteringRemote remote = new SelfRegisteringRemote(c);
        remote.setRemoteServer(new SeleniumServer((StandaloneConfiguration)configuration));
        remote.startRemoteServer();
        log.info("Selenium Grid node is up and ready to register to the hub");
        remote.startRegistrationProcess();
      }
    })
    .build();

  public static void main(String[] args) throws Exception {
    StandaloneConfiguration configuration = new StandaloneConfiguration();
    new JCommander(configuration, args);

    GridRole role = GridRole.get(configuration.role);

    if (role == null) {
      printInfoAboutRoles(configuration.role);
      return;
    }

    launchers.get(role).setConfiguration(args);

    if (configuration.help) {
      launchers.get(role).printUsage();
      return;
    }

    configureLogging(configuration);

    try {
      launchers.get(role).launch();
    } catch (Exception e) {
      launchers.get(role).printUsage();
      e.printStackTrace();
    }
  }

  private static void printInfoAboutRoles(String roleCommandLineArg) {
    if (roleCommandLineArg != null) {
      CliUtils.printWrappedLine(
        "",
        "Error: the role '" + roleCommandLineArg + "' does not match a recognized server role: node/hub/standalone\n");
    } else {
      CliUtils.printWrappedLine(
        "",
        "Error: -role option needs to be followed by the value that defines role of this component in the grid\n");
    }
    System.out.println(
      "Selenium server can run in one of the following roles:\n" +
      "  hub         as a hub of a Selenium grid\n" +
      "  node        as a node of a Selenium grid\n" +
      "  standalone  as a standalone server not being a part of a grid\n" +
      "\n" +
      "If -role option is omitted the server runs standalone\n");
    CliUtils.printWrappedLine(
      "",
      "To get help on the options available for a specific role run the server"
      + " with -help option and the corresponding -role option value");
  }

  private static void configureLogging(StandaloneConfiguration configuration) {
    Level logLevel =
        configuration.debug
        ? Level.FINE
        : LoggingOptions.getDefaultLogLevel();
    if (logLevel == null) {
      logLevel = Level.INFO;
    }
    Logger.getLogger("").setLevel(logLevel);
    Logger.getLogger("org.openqa.jetty").setLevel(Level.WARNING);

    String logFilename =
        configuration.log != null
        ? configuration.log
        : LoggingOptions.getDefaultLogOutFile();
    if (logFilename != null) {
      for (Handler handler : Logger.getLogger("").getHandlers()) {
        if (handler instanceof ConsoleHandler) {
          Logger.getLogger("").removeHandler(handler);
        }
      }
      try {
        Handler logFile = new FileHandler(new File(logFilename).getAbsolutePath(), true);
        logFile.setFormatter(new TerseFormatter(true));
        logFile.setLevel(logLevel);
        Logger.getLogger("").addHandler(logFile);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    } else {
      for (Handler handler : Logger.getLogger("").getHandlers()) {
        if (handler instanceof ConsoleHandler) {
          handler.setLevel(logLevel);
          handler.setFormatter(new TerseFormatter(configuration.logLongForm));
        }
      }
    }
  }
}
