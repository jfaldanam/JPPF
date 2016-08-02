/*
 * JPPF.
 * Copyright (C) 2005-2016 JPPF Team.
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

package org.jppf.management.generated;

import org.jppf.management.AbstractMBeanStaticProxy;
import org.jppf.management.JMXConnectionWrapper;
import org.jppf.management.PeerDriverMBean;
import org.jppf.utils.TypedProperties;

/**
 * Generated static proxy for the {@link org.jppf.management.PeerDriverMBean} MBean interface.
 * @author /common/src/java/org/jppf/utils/generator/MBeanStaticProxyGenerator.java
 */
public class PeerDriverMBeanStaticProxy extends AbstractMBeanStaticProxy implements PeerDriverMBean {
  /**
   * Initialize this MBean static proxy.
   * @param connection the JMX connection used to invoke remote MBean methods.
   */
  public PeerDriverMBeanStaticProxy(final JMXConnectionWrapper connection) {
    super(connection, "org.jppf:name=peerAttributes,type=driver");
  }

  /**
   * Get the JMX object name for this MBean static proxy.
   * @return the object name as a string.
   */
  public static final String getMBeanName() {
    return "org.jppf:name=peerAttributes,type=driver";
  }

  @Override
  public TypedProperties getPeerProperties() {
    return (TypedProperties) getAttribute("PeerProperties");
  }
}
