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
package com.jetbrains.youtrack.db.internal.server.config;

import jakarta.xml.bind.annotation.XmlAnyElement;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElementRef;
import jakarta.xml.bind.annotation.XmlElementWrapper;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;

@XmlRootElement(name = "listener")
@XmlType(propOrder = {"commands", "parameters", "protocol", "socket", "portRange", "ipAddress"})
public class ServerNetworkListenerConfiguration {

  @XmlAttribute(name = "ip-address", required = true)
  public String ipAddress = "127.0.0.1";

  @XmlAttribute(name = "port-range")
  public String portRange = "2424-2430";

  @XmlAttribute
  public String protocol = "binary";

  @XmlAttribute
  public String socket = "default";

  @XmlElementWrapper
  @XmlElementRef(type = ServerParameterConfiguration.class)
  public ServerParameterConfiguration[] parameters;

  @XmlElementWrapper(required = false)
  @XmlAnyElement
  @XmlElementRef(type = ServerCommandConfiguration.class)
  public ServerCommandConfiguration[] commands;
}
