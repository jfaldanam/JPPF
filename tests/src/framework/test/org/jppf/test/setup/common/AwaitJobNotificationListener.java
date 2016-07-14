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

package test.org.jppf.test.setup.common;

import javax.management.*;

import org.jppf.job.*;

/**
 * This notification listener waits until a specified job lifecycle event is received.
 */
public class AwaitJobNotificationListener implements NotificationListener {
  /**
   * The expected event.
   */
  private JobEventType expectedEvent = JobEventType.JOB_DISPATCHED; 

  @Override
  public void handleNotification(final Notification notification, final Object handback) {
    JobNotification jobNotif = (JobNotification) notification;
    String uuid = jobNotif.getJobInformation().getJobUuid();
    try {
      synchronized(this) {
        if (jobNotif.getEventType() == expectedEvent) notifyAll();
      }
    } catch (Exception ignore) {
      ignore.printStackTrace();
    }
  }

  /**
   * Await the specified event.
   * @param eventType the type of event to wait for.
   */
  public synchronized void await(final JobEventType eventType) {
    this.expectedEvent = eventType;
    try {
      wait();
    } catch (Exception e) {
    }
  }
}