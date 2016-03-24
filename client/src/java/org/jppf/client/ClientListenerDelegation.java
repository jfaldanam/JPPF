/*
 * JPPF.
 * Copyright (C) 2005-2015 JPPF Team.
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

package org.jppf.client;

import org.jppf.client.event.*;

/**
 * This class converts a deprecated {@link ClientListener} into a {@link ConnectionPoolListener}. 
 * @author Laurent Cohen
 * @since 5.1
 */
@SuppressWarnings("deprecation")
class ClientListenerDelegation extends ConnectionPoolListenerAdapter {
  /**
   * The client listener to delegate notifications to.
   */
  private final ClientListener delegate;

  /**
   * Initialize with the specified client listener to delegate notifications to.
   * @param delegate the client listener to delegate notifications to.
   */
  public ClientListenerDelegation(final ClientListener delegate) {
    this.delegate = delegate;
  }

  @Override
  public void connectionAdded(final ConnectionPoolEvent event) {
    ClientEvent clientEvent = new ClientEvent(event.getConnection());
    delegate.newConnection(clientEvent);
  }

  @Override
  public void connectionRemoved(final ConnectionPoolEvent event) {
    ClientEvent clientEvent = new ClientEvent(event.getConnection());
    delegate.connectionFailed(clientEvent);
  }

  /**
   * Get the client listener to delegate notifications to.
   * @return a {@link ClientListener} instance.
   */
  ClientListener getDelegate() {
    return delegate;
  }
}
