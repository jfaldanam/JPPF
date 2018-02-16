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

package org.jppf.jmxremote.nio;

import java.nio.channels.SelectionKey;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicReference;

import org.jppf.io.IOHelper;
import org.jppf.jmxremote.message.*;
import org.jppf.nio.*;
import org.slf4j.*;

/**
 * Context associated with a {@link JMXChannelWrapper}.
 * @author Laurent Cohen
 */
public class JMXContext extends SimpleNioContext<JMXState> {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(JMXContext.class);
  /**
   * Determines whether the debug level is enabled in the log configuration, without the cost of a method call.
   */
  private static final boolean debugEnabled = log.isDebugEnabled();
  /**
   * The last read or written JMX message.
   */
  private MessageWrapper currentMessageWrapper;
  /**
   * The queue of pending messages to send.
   */
  private final Queue<MessageWrapper> pendingJmxMessages;
  /**
   * The JMX nio server to use.
   */
  private final JMXNioServer server;
  /**
   * The object that handles messages correlations.
   */
  private JMXMessageHandler messageHandler;
  /**
   *
   */
  private final AtomicReference<JMXState> stateRef = new AtomicReference<>();
  /**
   *
   */
  private SelectionKey selectionKey;

  /**
   * Initialize with the specified server.
   * @param server the JMX nio server to use.
   * @param reading whether the associated channel performs read operations ({@code true}) or write operations ({@code false}).
   */
  public JMXContext(final JMXNioServer server, final boolean reading) {
    this.server = server;
    pendingJmxMessages = reading ? null : new LinkedBlockingQueue<MessageWrapper>();
    this.peer = false;
  }

  @Override
  public void handleException(final ChannelWrapper<?> channel, final Exception exception) {
    try {
      server.closeConnection(getChannels(), exception, false);
    } catch (final Exception e) {
      log.error(e.getMessage(), e);
    }
  }

  /**
   * @return the connection id.
   */
  public String getConnectionID() {
    return messageHandler.getChannels().getConnectionID();
  }

  /**
   * Deserialize the specified message.
   * @param message the message to deserialize.
   * @return a deserialzed message.
   * @throws Exception if any error occurs.
   */
  public JMXMessage deserializeMessage(final SimpleNioMessage message) throws Exception {
    return (message != null) ? (JMXMessage) IOHelper.unwrappedData(message.getLocation()) : null;
  }

  /**
   * Add a JMX message to the pending queue.
   * @param jmxMessage the JMX message to offer.
   * @throws Exception if any error occurs.
   */
  public void offerJmxMessage(final JMXMessage jmxMessage) throws Exception {
    final SimpleNioMessage msg = new SimpleNioMessage(channel);
    msg.setLocation(IOHelper.serializeData(jmxMessage));
    pendingJmxMessages.offer(new MessageWrapper(jmxMessage, msg));
  }

  /**
   * Get the next JMX message from the pending queue.
   * @return a {@link JMXMessage} instance.
   */
  public MessageWrapper pollJmxMessage() {
    return pendingJmxMessages.poll();
  }

  /**
   * @return whether there is at least one pending message.
   */
  public boolean hasQueuedMessage() {
    return !pendingJmxMessages.isEmpty();
  }

  /**
   * @return the object that handles messages correlations.
   */
  public JMXMessageHandler getMessageHandler() {
    return messageHandler;
  }

  /**
   * @return the ChannelsPair this context's channel is a part of.
   */
  public ChannelsPair getChannels() {
    return messageHandler.getChannels();
  }

  /**
   * Set the object that handles messages correlations.
   * @param messageHandler a {@link JMXMessageHandler} instance.
   */
  public void setMessageHandler(final JMXMessageHandler messageHandler) {
    this.messageHandler = messageHandler;
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder(getClass().getSimpleName()).append('[');
    sb.append("channel=").append(channel.getClass().getSimpleName()).append("[id=").append(channel.getId()).append(']');
    sb.append(", state=").append(getState());
    sb.append(", connectionID=").append(messageHandler == null ? "null" : getConnectionID());
    sb.append(", serverSide=").append(messageHandler == null ? "null" : getChannels().isServerSide());
    sb.append(", ssl=").append(ssl);
    if (pendingJmxMessages != null) sb.append(", pendingMessages=").append(pendingJmxMessages.size());
    return sb.append(']').toString();
  }

  @Override
  public JMXState getState() {
    return stateRef.get();
  }

  @Override
  public boolean setState(final JMXState state) {
    stateRef.set(state);
    return true;
  }

  /**
   * Compare the state to the expected one, and set it to the update value only if they are the same.
   * @param expected the state to compare to.
   * @param update the state value to update with.
   * @return {@code true} if the update was performed, {@code false} otherwise.
   */
  public boolean compareAndSetState(final JMXState expected, final JMXState update) {
    return stateRef.compareAndSet(expected, update);
  }

  /**
   * Set the spcecified state to the channel and prepare it for selection.
   * @param state the transition to set.
   * @param updateOps the value to AND-wise update the interest ops with.
   * @param add whether to add the update ({@code true}) or remove it ({@code false}).
   * @return {@code null}.
   * @throws Exception if any error occurs.
   */
  JMXTransition transitionChannel(final JMXState state, final int updateOps, final boolean add) throws Exception {
    setState(state);
    server.updateInterestOps(getSelectionKey(), updateOps, add);
    return null;
  }

  /**
   * @return the JMX nio server to use.
   */
  public JMXNioServer getServer() {
    return server;
  }

  /**
   * @return the channel's selection key.
   */
  public SelectionKey getSelectionKey() {
    if (selectionKey == null) selectionKey = getChannel().getSocketChannel().keyFor(server.getSelector());
    return selectionKey;
  }

  /**
   * Read data from a channel.
   * @param wrapper the channel to read the data from.
   * @return true if all the data has been read, false otherwise.
   * @throws Exception if an error occurs while reading the data.
   */
  @Override
  public boolean readMessage(final ChannelWrapper<?> wrapper) throws Exception {
    if (message == null) message = new SimpleNioMessage(channel);
    byteCount = ((SimpleNioMessage) message).channelCount;
    final boolean b = message.read();
    byteCount = ((SimpleNioMessage) message).channelCount - byteCount;
    if (debugEnabled) log.debug("read {} bytes", byteCount);
    return b;
  }

  /**
   * Write data to a channel.
   * @param wrapper the channel to write the data to.
   * @return true if all the data has been written, false otherwise.
   * @throws Exception if an error occurs while writing the data.
   */
  @Override
  public boolean writeMessage(final ChannelWrapper<?> wrapper) throws Exception {
    byteCount = ((SimpleNioMessage) message).channelCount;
    final boolean b = message.write();
    byteCount = ((SimpleNioMessage) message).channelCount - byteCount;
    if (debugEnabled) log.debug("wrote {} bytes", byteCount);
    return b;
  }

  /**
   * @return the last read or written JMX message.
   */
  MessageWrapper getCurrentMessageWrapper() {
    return currentMessageWrapper;
  }

  /**
   * Set the last read or written JMX message.
   * @param currentMessageWrapper the last read or written JMX message to set.
   */
  void setCurrentMessageWrapper(final MessageWrapper currentMessageWrapper) {
    this.currentMessageWrapper = currentMessageWrapper;
  }
}