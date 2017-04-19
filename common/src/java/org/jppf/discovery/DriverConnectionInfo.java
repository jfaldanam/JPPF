/*
 * JPPF.
 * Copyright (C) 2005-2017 JPPF Team.
 * http://www.jppf.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jppf.discovery;

import static org.jppf.utils.configuration.JPPFProperties.*;

/**
 * Superclass for connection info in custom driver discovery mechanisms.
 * @author Laurent Cohen
 * @since 5.2.1
 */
public class DriverConnectionInfo {
  /**
   * The name given to this connection, used as umbered prefix for individual connection names.
   */
  final String name;
  /**
   * Whether SSL/TLS should be used.
   */
  final boolean secure;
  /**
   * The driver host name or IP address.
   */
  final String host;
  /**
   * The driver port to connect to.
   */
  final int port;

  /**
   * Initialize a plain connection with default name("driver"), host ("localhost") and port (11111).
   */
  public DriverConnectionInfo() {
    this("driver", false, SERVER_HOST.getDefaultValue(), SERVER_PORT.getDefaultValue());
  }

  /**
   * Initialize a plain connection.
   * @param name the name given to this connection.
   * @param host the driver host name or IP address.
   * @param port the driver port to connect to.
   */
  public DriverConnectionInfo(final String name, final String host, final int port) {
    this(name, false, host, port);
  }

  /**
   * Initialize a connection.
   * @param name the name given to this connection.
   * @param secure whether SSL/TLS should be used.
   * @param host the driver host name or IP address.
   * @param port the driver port to connect to.
   */
  public DriverConnectionInfo(final String name, final boolean secure, final String host, final int port) {
    this.name = name;
    this.secure = secure;
    this.host = host;
    this.port = port;
  }

  /**
   * Get the name given to this connection.
   * @return the connection name as a string.
   */
  public String getName() {
    return name;
  }

  /**
   * Determine whether secure (with SSL/TLS) connections should be established.
   * @return {@code true} for secure connections, {@code false} otherwise.
   */
  public boolean isSecure() {
    return secure;
  }

  /**
   * Get the driver host name or IP address.
   * @return the host as a string.
   */
  public String getHost() {
    return host;
  }

  /**
   * Get the driver port to connect to.
   * @return the driver port as an int value.
   */
  public int getPort() {
    return port;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = prime + ((name == null) ? 0 : name.hashCode());
    result = prime * result + ((host == null) ? 0 : host.hashCode());
    result = prime * result + port;
    result = prime * result + (secure ? 1231 : 1237);
    return result;
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null) return false;
    if (getClass() != obj.getClass()) return false;
    DriverConnectionInfo other = (DriverConnectionInfo) obj;
    if (name == null) {
      if (other.name != null) return false;
    }
    else if (!name.equals(other.name)) return false;
    if (host == null) {
      if (other.host != null) return false;
    }
    else if (!host.equals(other.host)) return false;
    if (port != other.port) return false;
    if (secure != other.secure) return false;
    return true;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder(getClass().getSimpleName()).append('[');
    sb.append("name=").append(name);
    sb.append(", secure=").append(secure);
    sb.append(", host=").append(host);
    sb.append(", port=").append(port);
    sb.append(']');
    return sb.toString();
  }
}