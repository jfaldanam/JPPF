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
package org.jppf.server.peer;

import static org.jppf.server.protocol.BundleParameter.NODE_UUID_PARAM;

import org.apache.commons.logging.*;
import org.jppf.JPPFException;
import org.jppf.comm.socket.SocketClient;
import org.jppf.io.*;
import org.jppf.node.AbstractMonitoredNode;
import org.jppf.node.event.NodeEventType;
import org.jppf.server.*;
import org.jppf.server.protocol.*;
import org.jppf.utils.*;

/**
 * Instances of this class encapsulate execution nodes.
 * @author Laurent Cohen
 * @author Domingos Creado
 */
public class PeerNode extends AbstractMonitoredNode
{
	/**
	 * Logger for this class.
	 */
	private static Log log = LogFactory.getLog(PeerNode.class);
	/**
	 * Determines whether the debug level is enabled in the logging configuration, without the cost of a method call.
	 */
	private static boolean debugEnabled = log.isDebugEnabled();
	/**
	 * Used to send the task results back to the requester.
	 */
	private AbstractResultSender resultSender = null;
	/**
	 * The name of the peer in the configuration file.
	 */
	private String peerName = null;
	/**
	 * Input source for the socket client.
	 */
	private InputSource is = null;
	/**
	 * Security credentials associated with this JPPF node.
	 */
	//private JPPFSecurityContext credentials = null;

	/**
	 * Initialize this peer node with the specified configuration name.
	 * @param peerName the name of the peer int he configuration file.
	 */
	public PeerNode(String peerName)
	{
		this.peerName = peerName;
		//this.uuid = new JPPFUuid().toString();
		this.uuid = JPPFDriver.getInstance().getUuid();
		this.helper = new SerializationHelperImpl();
	}

	/**
	 * Main processing loop of this node.
	 * @see java.lang.Runnable#run()
	 */
	public void run()
	{
		stopped = false;
		if (debugEnabled) log.debug(getName() + "Start of peer node main loop");
		while (!stopped)
		{
			try
			{
				init();
			}
			catch(Exception e)
			{
				stopped = true;
				if (socketInitializer != null) socketInitializer.close();
				TypedProperties props = JPPFConfiguration.getProperties();
				if (props.getBoolean("jppf.discovery.enabled", true) && props.getBoolean("jppf.peer.discovery.enabled", true))
					JPPFDriver.getInstance().getPeerDiscoveryThread().removePeer(peerName);
				if (debugEnabled) log.debug(getName() + " : " + e.getMessage(), e);
			}
			if (!stopped)
			{
				try
				{
					resultSender = new PeerNodeResultSender(socketClient);
					perform();
				}
				catch(Exception e)
				{
					log.error(e.getMessage(), e);
					try
					{
						socketClient.close();
						socketClient = null;
					}
					catch(Exception ex)
					{
						log.error(ex.getMessage(), ex);
					}
				}
				catch(Error e)
				{
					log.error(e.getMessage(), e);
					e.printStackTrace();
					throw e;
				}
			}
		}
		if (debugEnabled) log.debug(getName() + "End of peer node main loop");
		if (notifying) fireNodeEvent(NodeEventType.DISCONNECTED);
	}

	/**
	 * Perform the main execution loop for this node. At each iteration, this method listens for a task to execute,
	 * receives it, executes it and sends the results back.
	 * @throws Exception if an error was raised from the underlying socket connection or the class loader.
	 */
	public void perform() throws Exception
	{
		JPPFDriver driver = JPPFDriver.getInstance();
		if (debugEnabled) log.debug(getName() + "Start of peer node secondary loop");
		while (!stopped)
		{
			BundleWrapper bundleWrapper = readBundle();
			JPPFTaskBundle bundle = bundleWrapper.getBundle();
			if (JPPFTaskBundle.State.INITIAL_BUNDLE.equals(bundle.getState()))
			{
				bundle.setBundleUuid(uuid);
				boolean override = bundle.getParameter(BundleParameter.BUNDLE_TUNING_TYPE_PARAM) != null;
				bundle.setParameter(BundleParameter.IS_PEER, true);
				bundle.setParameter(NODE_UUID_PARAM, uuid);
			}
			if (notifying) fireNodeEvent(NodeEventType.START_EXEC);
			//boolean notEmpty = (bundle.getTasks() != null) && (bundle.getTaskCount() > 0);
			boolean notEmpty = !bundleWrapper.getTasks().isEmpty();
			if (notEmpty)
			{
				int n = bundle.getTaskCount();
				bundle.getUuidPath().add(driver.getUuid());
				bundle.setCompletionListener(resultSender);
				JPPFDriver.getQueue().addBundle(bundleWrapper);
				resultSender.run(n);
				//resultSender.sendPartialResults(bundleWrapper);
				setTaskCount(getTaskCount() + n);
				if (debugEnabled) log.debug(getName() + "tasks executed: "+getTaskCount());
			}
			else
			{
				resultSender.sendPartialResults(bundleWrapper);
			}
			if (notifying) fireNodeEvent(NodeEventType.END_EXEC);
		}
		if (debugEnabled) log.debug(getName() + " End of peer node secondary loop");
	}

	/**
	 * Initialize this node's resources.
	 * @throws Exception if an error is raised during initialization.
	 */
	public synchronized void init() throws Exception
	{
		if (debugEnabled) log.debug(getName() + "] initializing socket client");
		boolean mustInit = false;
		if (socketClient == null)
		{
			mustInit = true;
			initSocketClient();
		}
		initCredentials();
		if (notifying) fireNodeEvent(NodeEventType.START_CONNECT);
		if (mustInit)
		{
			if (debugEnabled) log.debug(getName() + "initializing socket");
			System.out.println(getName() + "PeerNode.init(): Attempting connection to the JPPF driver");
			socketInitializer.initializeSocket(socketClient);
			if (!socketInitializer.isSuccessfull()) throw new JPPFException(getName() + " : Unable to reconnect to peer server");
			System.out.println(getName() + "PeerNode.init(): Reconnected to the JPPF driver");
			is = new SocketWrapperInputSource(socketClient);
		}
		if (notifying) fireNodeEvent(NodeEventType.END_CONNECT);
	}

	/**
	 * Initialize this node's resources.
	 * @throws Exception if an error is raised during initialization.
	 */
	public void initSocketClient() throws Exception
	{
		if (debugEnabled) log.debug(getName() + "initializing socket client");
		TypedProperties props = JPPFConfiguration.getProperties();
		String host = props.getString("jppf.peer."+peerName+".server.host", "localhost");
		int port = props.getInt("node.peer."+peerName+".server.port", 11113);
		socketClient = new SocketClient();
		socketClient.setHost(host);
		socketClient.setPort(port);
		socketClient.setSerializer(helper.getSerializer());
	}

	/**
	 * Initialize the security credentials associated with this JPPF node.
	 */
	private void initCredentials()
	{
	}
	
	/**
	 * Perform the deserialization of the objects received through the socket connection.
	 * @return an array of deserialized objects.
	 * @throws Exception if an error occurs while deserializing.
	 */
	private BundleWrapper readBundle() throws Exception
	{
		// Read the request header - with tasks count information
		int n = is.readInt();
		byte bytes[] = socketClient.receiveBytes(0).getBuffer();
		JPPFTaskBundle header = (JPPFTaskBundle) helper.getSerializer().deserialize(bytes);
		if (debugEnabled) log.debug("received header from peer driver, data length = " + bytes.length);
		bytes = null;
		BundleWrapper headerWrapper = new BundleWrapper(header);

		int count = header.getTaskCount();
		if (debugEnabled) log.debug("Received " + count + " tasks");

		for (int i=0; i<count+1; i++)
		{
			DataLocation dl = IOHelper.readData(is);
			if (i == 0)
			{
				headerWrapper.setDataProvider(dl);
				if (debugEnabled) log.debug("received data provider from peer driver, data length = " + dl.getSize());
			}
			else
			{
				headerWrapper.addTask(dl);
				if (debugEnabled) log.debug("received task #"+ i + " from peer driver, data length = " + dl.getSize());
			}
		}
		return headerWrapper;
	}

	/**
	 * Stop this node and release the resources it is using.
	 * @param closeSocket determines whether the underlying socket should be closed.
	 * @see org.jppf.node.MonitoredNode#stopNode(boolean)
	 */
	public void stopNode(boolean closeSocket)
	{
		if (debugEnabled) log.debug(getName() + "closing node");
		stopped = true;
		if (closeSocket)
		{
			try
			{
				if (debugEnabled) log.debug(getName() + "closing socket: " + socketClient.getSocket());
				socketClient.close();
			}
			catch(Exception ex)
			{
				log.error(ex.getMessage(), ex);
			}
			socketClient = null;
		}
	}

	/**
	 * Get a string representation of this peer node's name.
	 * @return the name as a string.
	 */
	private String getName()
	{
		return "[peer: " + peerName +"] ";
	}
}
