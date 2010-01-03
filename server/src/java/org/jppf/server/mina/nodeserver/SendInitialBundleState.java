/*
 * JPPF.
 * Copyright (C) 2005-2009 JPPF Team.
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

package org.jppf.server.mina.nodeserver;

import static org.jppf.utils.StringUtils.getRemoteHost;

import org.apache.commons.logging.*;
import org.apache.mina.core.session.IoSession;
import org.jppf.server.nio.nodeserver.NodeTransition;

/**
 * This class represents the state of sending the initial hand-shaking data to a newly connected node.
 * @author Laurent Cohen
 */
public class SendInitialBundleState extends NodeServerState
{
	/**
	 * Logger for this class.
	 */
	private static Log log = LogFactory.getLog(SendInitialBundleState.class);
	/**
	 * Determines whether DEBUG logging level is enabled.
	 */
	private static boolean debugEnabled = log.isDebugEnabled();
	/**
	 * Initialize this state.
	 * @param server the server that handles this state.
	 */
	public SendInitialBundleState(MinaNodeServer server)
	{
		super(server);
	}

	/**
	 * Execute the action associated with this channel state.
	 * @param session the current session to which the state applies.
	 * @return true if the transition could be applied, false otherwise. If true, then <code>endTransition()</code> will be called.
	 * @throws Exception if an error occurs while transitioning to another state.
	 * @see org.jppf.server.nio.NioState#performTransition(java.nio.channels.SelectionKey)
	 */
	public boolean startTransition(IoSession session) throws Exception
	{
		/*
		if (!session.isReaderIdle())
		{
			throw new ConnectException("node " + getRemoteHost(session.getRemoteAddress()) + " has been disconnected");
		}
		*/
		NodeContext context = getContext(session);
		if (context.getNodeMessage() == null)
		{
			if (debugEnabled) log.debug("serializing initial bundle for " + getRemoteHost(session.getRemoteAddress()));
			context.serializeBundle();
		}
		session.write(context.getBundle());
		return true;
	}


	/**
	 * End the transition associated with this channel state.
	 * @param session the current session to which the state applies.
	 * @throws Exception if an error occurs while transitioning to another state.
	 * @see org.jppf.server.mina.nodeserver.NodeServerState#endTransition(org.apache.mina.core.session.IoSession)
	 */
	public void endTransition(IoSession session) throws Exception
	{
		if (debugEnabled) log.debug("sent entire initial bundle for " + getRemoteHost(session.getRemoteAddress()));
		NodeContext context = getContext(session);
		context.setNodeMessage(null);
		context.setBundle(null);
		server.transitionSession(session, NodeTransition.TO_WAIT_INITIAL);
	}
}
