/*
 * Copyright 2014 Charles Baptiste (cbaptiste--at--blacksparkcorp.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jetbrains.youtrack.db.internal.server.network;

import com.jetbrains.youtrack.db.internal.server.config.ServerParameterConfiguration;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;

public abstract class ServerSocketFactory {

  private static ServerSocketFactory theFactory;
  private String name;

  public ServerSocketFactory() {
  }

  public static ServerSocketFactory getDefault() {
    synchronized (ServerSocketFactory.class) {
      if (theFactory == null) {
        theFactory = new DefaultServerSocketFactory();
      }
    }

    return theFactory;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public void config(String name, final ServerParameterConfiguration[] iParameters) {
    this.name = name;
  }

  public abstract ServerSocket createServerSocket(int port) throws IOException;

  public abstract ServerSocket createServerSocket(int port, int backlog) throws IOException;

  public abstract ServerSocket createServerSocket(int port, int backlog, InetAddress ifAddress)
      throws IOException;

  public boolean isEncrypted() {
    return false;
  }
}
