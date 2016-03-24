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

package org.jppf.server.node;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Abstract common connection checker implementation for remote and local nodes.
 * @author Laurent Cohen
 * @exclude
 */
public abstract class AbstractNodeConnectionChecker implements NodeConnectionChecker
{
  /**
   * 
   */
  protected AtomicBoolean stopped = new AtomicBoolean(true);
  /**
   * 
   */
  protected AtomicBoolean suspended = new AtomicBoolean(true);
  /**
   * 
   */
  protected Exception exception = null;

  @Override
  public boolean isStopped()
  {
    return stopped.get();
  }

  @Override
  public boolean isSuspended()
  {
    return suspended.get();
  }

  @Override
  public Exception getException()
  {
    return exception;
  }
}
