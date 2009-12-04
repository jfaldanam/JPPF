/*
 * JPPF.
 *  Copyright (C) 2005-2009 JPPF Team. 
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

package org.jppf.server.nio.classloader;

import static org.jppf.server.nio.classloader.ClassTransition.*;
import static org.jppf.utils.StringUtils.getRemoteHost;

import java.nio.channels.*;
import java.util.*;

import org.apache.commons.logging.*;
import org.jppf.node.JPPFResourceWrapper;
import org.jppf.server.JPPFDriver;
import org.jppf.utils.*;

/**
 * This class represents the state of waiting for a request from a node.
 * @author Laurent Cohen
 */
public class WaitingNodeRequestState extends ClassServerState
{
	/**
	 * Logger for this class.
	 */
	private static Log log = LogFactory.getLog(WaitingNodeRequestState.class);
	/**
	 * Determines whether DEBUG logging level is enabled.
	 */
	private static boolean debugEnabled = log.isDebugEnabled();

	/**
	 * Initialize this state with a specified NioServer.
	 * @param server the JPPFNIOServer this state relates to.
	 */
	public WaitingNodeRequestState(ClassNioServer server)
	{
		super(server);
	}

	/**
	 * Execute the action associated with this channel state.
	 * @param key the selection key corresponding to the channel and selector for this state.
	 * @return a state transition as an <code>NioTransition</code> instance.
	 * @throws Exception if an error occurs while transitioning to another state.
	 * @see org.jppf.server.nio.NioState#performTransition(java.nio.channels.SelectionKey)
	 */
	public ClassTransition performTransition(SelectionKey key) throws Exception
	{
		SelectableChannel channel = key.channel();
		ClassContext context = (ClassContext) key.attachment();
		if (context.readMessage((ReadableByteChannel) channel))
		{
			if (debugEnabled) log.debug("read resource request from node: " + getRemoteHost(channel));
			JPPFResourceWrapper resource = context.deserializeResource();
			TraversalList<String> uuidPath = resource.getUuidPath();
			boolean dynamic = resource.isDynamic();
			String name = resource.getName();
			byte[] callable = resource.getCallable();
			String uuid = (uuidPath.size() > 0) ? uuidPath.getCurrentElement() : null; 
			byte[] b = null;
			if (((uuid == null) || uuid.equals(JPPFDriver.getInstance().getUuid())) && (callable == null))
			{
				if ((uuid == null) && !dynamic) uuid = JPPFDriver.getInstance().getUuid();
				if (uuid != null) b = server.getCacheContent(uuid, name);
				boolean alreadyInCache = (b != null);
				if (debugEnabled) log.debug("resource " + (alreadyInCache ? "" : "not ") + "found [" + name + "] in cache for node: " + getRemoteHost(channel));
				if (!alreadyInCache)
				{
					b = server.getResourceProvider().getResourceAsBytes(name);
					if (b != null) b = CompressionUtils.zip(b, 0, b.length);
				}
				if ((b != null) || !dynamic)
				{
					if (debugEnabled) log.debug("resource " + (b == null ? "not " : "") + "found [" + name + "] in the driver's classpath for node: " + getRemoteHost(channel));
					if ((b != null) && !alreadyInCache) server.setCacheContent(JPPFDriver.getInstance().getUuid(), name, b);
					resource.setDefinition(b);
					context.serializeResource();
					return TO_SENDING_NODE_RESPONSE;
				}
			}
			if ((b == null) && dynamic)
			{
				if (callable == null) b = server.getCacheContent(uuidPath.getFirst(), name);
				if (b != null)
				{
					if (debugEnabled) log.debug("found cached resource [" + name + "] for node: " + getRemoteHost(channel));
					resource.setDefinition(b);
					context.serializeResource();
					return TO_SENDING_NODE_RESPONSE;
				}
				else
				{
					uuidPath.decPosition();
					uuid = uuidPath.getCurrentElement();
					SelectableChannel provider = findProviderConnection(uuid);
					if (provider != null)
					{
						if (debugEnabled) log.debug("request resource [" + name + "] from client: " + getRemoteHost(provider) + " for node: " + getRemoteHost(channel));
						SelectionKey providerKey = provider.keyFor(server.getSelector());
						ClassContext providerContext = (ClassContext) providerKey.attachment();
						providerContext.addRequest(key);
						if (ClassState.IDLE_PROVIDER.equals(providerContext.getState()))
						{
							if (debugEnabled) log.debug("node " + getRemoteHost(channel) + " changing key ops for provider " + getRemoteHost(provider));
							providerContext.setState(ClassState.SENDING_PROVIDER_REQUEST);
							server.getTransitionManager().setKeyOps(providerKey, SelectionKey.OP_READ|SelectionKey.OP_WRITE);
						}
						return TO_IDLE_NODE;
					}
				}
			}
			if (debugEnabled) log.debug("resource [" + name + "] not found for node: " + getRemoteHost(channel));
			resource.setDefinition(null);
			context.serializeResource();
			return TO_SENDING_NODE_RESPONSE;
		}
		return TO_WAITING_NODE_REQUEST;
	}

	/**
	 * Find a provider connection for the specified provider uuid.
	 * @param uuid the uuid for which to find a conenction.
	 * @return a <code>SelectableChannel</code> instance.
	 * @throws Exception if an error occurs while searching for a connection.
	 */
	private SelectableChannel findProviderConnection(String uuid) throws Exception
	{
		SelectableChannel result = null;
		List<SelectableChannel> connections = server.providerConnections.get(uuid);
		int minRequests = Integer.MAX_VALUE;
		Selector selector = server.getSelector();
		for (SelectableChannel channel: connections)
		{
			ClassContext ctx = (ClassContext) channel.keyFor(selector).attachment();
			int size = ctx.getNbPendingRequests();
			if (size < minRequests)
			{
				minRequests = size;
				result = channel;
			}
		}
		return result;
	}
}

