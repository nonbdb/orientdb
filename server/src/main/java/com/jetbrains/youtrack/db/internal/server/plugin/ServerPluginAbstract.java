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

import com.jetbrains.youtrack.db.internal.server.ClientConnection;
import com.jetbrains.youtrack.db.internal.server.YouTrackDBServer;
import com.jetbrains.youtrack.db.internal.tools.config.ServerParameterConfiguration;

/**
 * Abstract class to make ServerHandler implementation easier.
 */
public abstract class ServerPluginAbstract implements ServerPlugin {

  protected boolean enabled = true;

  @Override
  public void startup() {
    if (!enabled) {
    }
  }

  @Override
  public void shutdown() {
  }

  @Override
  public void sendShutdown() {
    shutdown();
  }

  @Override
  public void config(YouTrackDBServer youTrackDBServer, ServerParameterConfiguration[] iParams) {
  }

  @Override
  public void onClientConnection(final ClientConnection iConnection) {
  }

  @Override
  public void onClientDisconnection(final ClientConnection iConnection) {
  }

  @Override
  public void onBeforeClientRequest(final ClientConnection iConnection, final byte iRequestType) {
  }

  @Override
  public void onAfterClientRequest(final ClientConnection iConnection, final byte iRequestType) {
  }

  @Override
  public void onClientError(final ClientConnection iConnection, final Throwable iThrowable) {
  }

  @Override
  public Object getContent(final String iURL) {
    return null;
  }
}
