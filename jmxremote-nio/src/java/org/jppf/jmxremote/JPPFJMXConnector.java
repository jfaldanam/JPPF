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

package org.jppf.jmxremote;

import java.io.IOException;
import java.net.ConnectException;
import java.nio.channels.SocketChannel;
import java.util.*;

import javax.management.*;
import javax.management.remote.*;
import javax.security.auth.Subject;

import org.jppf.JPPFException;
import org.jppf.comm.interceptor.InterceptorHandler;
import org.jppf.comm.socket.*;
import org.jppf.jmxremote.nio.*;
import org.jppf.nio.ChannelWrapper;
import org.jppf.utils.JPPFIdentifiers;
import org.slf4j.*;

/**
 * 
 * @author Laurent Cohen
 */
public class JPPFJMXConnector implements JMXConnector {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(JPPFJMXConnector.class);
  /**
   * Determines whether the debug level is enabled in the log configuration, without the cost of a method call.
   */
  private static boolean debugEnabled = log.isDebugEnabled();
  /**
   * The environment for this connector.
   */
  private final Map<String, Object> environment;
  /**
   * The address of this connector.
   */
  private final JMXServiceURL address;
  /**
   * Whether the connection is secured through TLS.
   */
  private boolean secure = false;
  /**
   * The mbean server connection.
   */
  private JPPFMBeanServerConnection mbsc;
  /**
   * The onnectin ID.
   */
  private String connectionID;

  /**
   * 
   * @param serviceURL the address of this connector.
   * @param environment the environment for this connector.
   */
  public JPPFJMXConnector(final JMXServiceURL serviceURL, final Map<String, ?> environment) {
    this.environment = (environment == null) ? new HashMap<String, Object>() : new HashMap<>(environment);
    this.address = serviceURL;
  }

  @Override
  public void connect() throws IOException {
    connect(null);
  }

  @Override
  public void connect(Map<String, ?> env) throws IOException {
    if (env != null) environment.putAll(env);
    Boolean tls = (Boolean) environment.get("jppf.jmx.remote.tls.enabled");
    secure = (tls != null) && tls;
    try {
      init();
    } catch (IOException e) {
      throw e;
    } catch (Exception e) {
      throw new IOException(e);
    }
  }

  @Override
  public MBeanServerConnection getMBeanServerConnection() throws IOException {
    return mbsc;
  }

  @Override
  public MBeanServerConnection getMBeanServerConnection(Subject delegationSubject) throws IOException {
    return mbsc;
  }

  @Override
  public void close() throws IOException {
  }

  @Override
  public void addConnectionNotificationListener(NotificationListener listener, NotificationFilter filter, Object handback) {
  }

  @Override
  public void removeConnectionNotificationListener(NotificationListener listener) throws ListenerNotFoundException {
  }

  @Override
  public void removeConnectionNotificationListener(NotificationListener l, NotificationFilter f, Object handback) throws ListenerNotFoundException {
  }

  @Override
  public String getConnectionId() throws IOException {
    return connectionID;
  }

  /**
   * @return the environment for this connector.
   */
  public Map<String, ?> getEnvironment() {
    return environment;
  }

  /**
   * @return the address of this connector.
   */
  public JMXServiceURL getAddress() {
    return address;
  }

  /**
   * Initialize this node's resources.
   * @throws Exception if an error is raised during initialization.
   */
  private synchronized void init() throws Exception {
    String host = address.getHost();
    int port = address.getPort();
    SocketChannelClient socketClient =  new SocketChannelClient(host, port, false);
    if (debugEnabled) log.debug("Attempting connection to remote peer at {}", address);
    SocketInitializer socketInitializer = new SocketInitializerImpl();
    socketInitializer.initializeSocket(socketClient);
    if (!socketInitializer.isSuccessful()) throw new ConnectException("could not connect to remote JMX server " + address);
    if (!InterceptorHandler.invokeOnConnect(socketClient)) throw new JPPFException("peer connection denied by interceptor");
    if (debugEnabled) log.debug("Connected to JMX server {}, sending channel identifier", address);
    socketClient.writeInt(JPPFIdentifiers.JMX_REMOTE_CHANNEL);
    if (debugEnabled) log.debug("Reconnected to JMX server {}", address);

    SocketChannel channel = socketClient.getChannel();
    JMXNioServer server = JMXNioServer.getInstance();
    ChannelWrapper<?> readingChannel = server.createChannel(environment, channel, secure, true);
    ((JMXContext) readingChannel.getContext()).setClient(false).setServerPort(port);
    ChannelWrapper<?> writingChannel = server.createChannel(environment, channel, secure, false);
    ((JMXContext) writingChannel.getContext()).setClient(true).setServerPort(port);
    ChannelsPair pair = new ChannelsPair(readingChannel, writingChannel);
    mbsc = new JPPFMBeanServerConnection(pair);
  }
}
