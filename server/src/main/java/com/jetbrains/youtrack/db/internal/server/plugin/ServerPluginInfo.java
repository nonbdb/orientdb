/*
 *
 *
 *  *
 *  *  Licensed under the Apache License, Version 2.0 (the "License");
 *  *  you may not use this file except in compliance with the License.
 *  *  You may obtain a copy of the License at
 *  *
 *  *       http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  *  Unless required by applicable law or agreed to in writing, software
 *  *  distributed under the License is distributed on an "AS IS" BASIS,
 *  *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  *  See the License for the specific language governing permissions and
 *  *  limitations under the License.
 *  *
 *
 *
 */
package com.jetbrains.youtrack.db.internal.server.plugin;

import com.jetbrains.youtrack.db.internal.common.log.LogManager;
import java.lang.reflect.Method;
import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.Map;

/**
 * Server plugin information
 */
public class ServerPluginInfo {

  private final String name;
  private final String version;
  private final String description;
  private final String web;
  private final ServerPlugin instance;
  private final Map<String, Object> parameters;
  private final long loadedOn;
  private final URLClassLoader pluginClassLoader;

  public ServerPluginInfo(
      final String name,
      final String version,
      final String description,
      final String web,
      final ServerPlugin instance,
      final Map<String, Object> parameters,
      final long loadedOn,
      final URLClassLoader pluginClassLoader) {
    this.name = name;
    this.version = version;
    this.description = description;
    this.web = web;
    this.instance = instance;
    this.parameters = parameters != null ? parameters : new HashMap<String, Object>();
    this.loadedOn = loadedOn;
    this.pluginClassLoader = pluginClassLoader;
  }

  public void shutdown() {
    shutdown(true);
  }

  public void shutdown(boolean closeClassLoader) {
    if (instance != null) {
      instance.sendShutdown();
    }

    if (pluginClassLoader != null && closeClassLoader) {
      // JAVA7 ONLY
      Method m;
      try {
        m = pluginClassLoader.getClass().getMethod("close");
        if (m != null) {
          m.invoke(pluginClassLoader);
        }
      } catch (NoSuchMethodException e) {
      } catch (Exception e) {
        LogManager.instance().error(this, "Error on closing plugin classloader", e);
      }
    }
  }

  public boolean isDynamic() {
    return loadedOn > 0;
  }

  public String getName() {
    return name;
  }

  public ServerPlugin getInstance() {
    return instance;
  }

  public long getLoadedOn() {
    return loadedOn;
  }

  public String getVersion() {
    return version;
  }

  public String getDescription() {
    return description;
  }

  public String getWeb() {
    return web;
  }

  public Object getParameter(final String iName) {
    return parameters.get(iName);
  }

  public URLClassLoader getClassLoader() {
    return pluginClassLoader;
  }
}
