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

package org.jppf.admin.web;

import org.apache.wicket.protocol.http.WebSession;
import org.apache.wicket.request.Request;
import org.jppf.admin.web.topology.TopologyTreeData;
import org.jppf.ui.treetable.TreeViewType;
import org.jppf.utils.LoggingUtils;
import org.slf4j.*;

/**
 *
 * @author Laurent Cohen
 */
public class JPPFWebSession extends WebSession {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(JPPFWebSession.class);
  /**
   * Determines whether debug log statements are enabled.
   */
  private static boolean debugEnabled = LoggingUtils.isDebugEnabled(log);
  /**
   * The JPPF-internal id for this session data.
   */
  private final long sessionDataId;

  /**
   * Initialize a new session.
   * @param request the request.
   * @param sessionDataId the identifier for the non-persisted session data.
   */
  public JPPFWebSession(final Request request, final long sessionDataId) {
    super(request);
    this.sessionDataId = sessionDataId;
    if (debugEnabled) log.debug(String.format("new instance #%d, request=%s", sessionDataId, request));
  }

  @Override
  public void onInvalidate() {
    super.onInvalidate();
    JPPFWebConsoleApplication.removeSessionData(sessionDataId);
  }

  /**
   * @return the data elements for the grid topology.
   */
  public TopologyTreeData getTopologyData() {
    return (TopologyTreeData) getSessionData().getData(TreeViewType.TOPOLOGY);
  }

  /**
   * @return the JPPF-internal id for this session.
   */
  public long getSessionDataId() {
    return sessionDataId;
  }

  /**
   * @return the session data for this session.
   */
  public SessionData getSessionData() {
    SessionData data = JPPFWebConsoleApplication.getSessionData(sessionDataId);
    if (data == null) {
      data = new SessionData(sessionDataId);
      JPPFWebConsoleApplication.setSessionData(sessionDataId, data);
    }
    return data;
  }
}