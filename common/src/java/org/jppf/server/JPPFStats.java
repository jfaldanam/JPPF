/*
 * JPPF.
 * Copyright (C) 2005-2011 JPPF Team.
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
package org.jppf.server;

import java.io.Serializable;

import org.jppf.utils.*;

/**
 * Instances of this class hold server-wide statitics and settings.
 * @author Laurent Cohen
 */
public class JPPFStats implements Serializable
{
	/**
	 * Explicit serialVersionUID.
	 */
	private static final long serialVersionUID = 1L;
	/**
	 * The total number of tasks executed.
	 */
	private int totalTasksExecuted = 0;
	/**
	 * Time statistics for the tasks execution, including network transport time and node execution time.
	 */
	private StatsSnapshot execution = new StatsSnapshot("execution");
	/**
	 * Time statistics for the tasks execution within the nodes only.
	 */
	private StatsSnapshot nodeExecution = new StatsSnapshot("node execution");
	/**
	 * Time statistics for the tasks network transport time between the nodes and server.
	 */
	private StatsSnapshot transport = new StatsSnapshot("transport");
	/**
	 * Time statistics for the tasks management overhead within the server.
	 */
	private StatsSnapshot server = new StatsSnapshot("server");
	/**
	 * Total footprint of all the data that was sent to the nodes.
	 */
	private long footprint = 0L;
	/**
	 * Statistics for the tasks in queue.
	 */
	private QueueStats taskQueue = new QueueStats("task");
	/**
	 * Statistics for the jobs in queue.
	 */
	private QueueStats jobQueue = new QueueStats("job");
	/**
	 * Statistics for the of takss in the jobs.
	 */
	private StatsSnapshot jobTasks = new StatsSnapshot("job stats");
	/**
	 * Statistics for the number of nodes in the grid.
	 */
	private StatsSnapshot nodes = new StatsSnapshot("nodes");
	/**
	 * Statistics for the number of nodes in the grid.
	 */
	private StatsSnapshot idleNodes = new StatsSnapshot("idle nodes");
	/**
	 * Statistics for the number of nodes in the grid.
	 */
	private StatsSnapshot clients = new StatsSnapshot("clients");

	/**
	 * Build a copy of this stats object.
	 * @return a new <code>JPPFStats</code> instance, populated with the current values
	 * of the fields in this stats object.
	 */
	public JPPFStats makeCopy()
	{
		JPPFStats s = new JPPFStats();
		s.totalTasksExecuted = totalTasksExecuted;
		s.execution = execution.makeCopy();
		s.nodeExecution = nodeExecution.makeCopy();
		s.transport = transport.makeCopy();
		s.taskQueue = taskQueue.makeCopy();
		s.jobQueue = jobQueue.makeCopy();
		s.jobTasks = jobTasks.makeCopy();
		s.nodes = nodes.makeCopy();
		s.idleNodes = idleNodes.makeCopy();
		/*
		s.queue = queue.makeCopy();
		s.totalQueued = totalQueued;
		s.queueSize = queueSize;
		s.maxQueueSize = maxQueueSize;
		s.nbNodes = nbNodes;
		s.maxNodes = maxNodes;
		*/
		s.clients = clients.makeCopy();
		s.footprint = footprint;
		return s;
	}

	/**
	 * Get a string representation of this stats object.
	 * @return a string display the various stats values.
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString()
	{
		StringBuilder sb = new StringBuilder();
		sb.append("totalTasksExecuted : ").append(totalTasksExecuted).append('\n');
		sb.append(execution.toString());
		sb.append(nodeExecution.toString());
		sb.append(transport.toString());
		sb.append(taskQueue.toString());
		sb.append(jobQueue.toString());
		sb.append(jobTasks.toString());
		sb.append(nodes.toString());
		sb.append(clients.toString());
		/*
		sb.append("totalQueued : ").append(totalQueued).append("\n");
		sb.append("queueSize : ").append(queueSize).append("\n");
		sb.append("maxQueueSize : ").append(maxQueueSize).append("\n");
		sb.append("nbNodes : ").append(nbNodes).append('\n');
		sb.append("maxNodes : ").append(maxNodes).append('\n');
		sb.append("nbClients : ").append(nbClients).append('\n');
		sb.append("maxClients : ").append(maxClients).append('\n');
		*/
		return sb.toString();
	}

	/**
	 * Set the total number of tasks executed.
	 * @param totalTasksExecuted - the number of tasks as an int value.
	 */
	public void setTotalTasksExecuted(int totalTasksExecuted)
	{
		this.totalTasksExecuted = totalTasksExecuted;
	}

	/**
	 * Get the total number of tasks executed.
	 * @return the number of tasks as an int value.
	 */
	public int getTotalTasksExecuted()
	{
		return totalTasksExecuted;
	}

	/**
	 * Get the time statistics for the tasks execution, including network transport and node execution time.
	 * @return a <code>TimeSnapshot</code> instance.
	 */
	public StatsSnapshot getExecution()
	{
		return execution;
	}

	/**
	 * Get the time statistics for execution within the nodes.
	 * @return a <code>TimeSnapshot</code> instance.
	 */
	public StatsSnapshot getNodeExecution()
	{
		return nodeExecution;
	}

	/**
	 * Get the time statistics for the network transport between nodes and server.
	 * @return a <code>TimeSnapshot</code> instance.
	 */
	public StatsSnapshot getTransport()
	{
		return transport;
	}

	/**
	 * Get the time statistics for the server overhead.
	 * @return a <code>TimeSnapshot</code> instance.
	 */
	public StatsSnapshot getServer()
	{
		return server;
	}

	/**
	 * Set the total footprint of all the data that was sent to the nodes.
	 * @param footprint the footprint as a long value.
	 */
	public void setFootprint(long footprint)
	{
		this.footprint = footprint;
	}

	/**
	 * Get the total footprint of all the data that was sent to the nodes.
	 * @return the footprint as a long value.
	 */
	public long getFootprint()
	{
		return footprint;
	}

	/**
	 * Get the statistics for the number of nodes in the grid.
	 * @return a {@link StatsSnapshot} instance.
	 */
	public StatsSnapshot getClients()
	{
		return clients;
	}

	/**
	 * Reset all fields of this <code>JPPFStats</code> object to their initial values.
	 */
	public synchronized void reset()
	{
		totalTasksExecuted = 0;
		execution = new StatsSnapshot("execution");
		nodeExecution = new StatsSnapshot("node execution");
		transport = new StatsSnapshot("transport");
		server = new StatsSnapshot("server");
		footprint = 0L;
		taskQueue = new QueueStats("task");
		jobQueue = new QueueStats("job");
		jobTasks = new StatsSnapshot("job tasks");
		nodes.setMax(nodes.getLatest());
		nodes.setMin(nodes.getLatest());
		clients.setMax(clients.getLatest());
		clients.setMin(clients.getLatest());
	}

	/**
	 * Get the statistics for the tasks in queue.
	 * @return a {@link QueueStats} instance.
	 */
	public QueueStats getTaskQueue()
	{
		return taskQueue;
	}

	/**
	 * Get the statistics for the jobs in queue.
	 * @return a {@link QueueStats} instance.
	 */
	public QueueStats getJobQueue()
	{
		return jobQueue;
	}

	/**
	 * Get the statistics for the tasks in the jobs.
	 * @return a {@link StatsSnapshot} instance.
	 */
	public StatsSnapshot getJobTasks()
	{
		return jobTasks;
	}

	/**
	 * Get the statistics for the number of nodes in the grid.
	 * @return a {@link StatsSnapshot} instance.
	 */
	public StatsSnapshot getNodes()
	{
		return nodes;
	}

	/**
	 * Get the statistics for the number of idle nodes in the grid.
	 * @return a {@link StatsSnapshot} instance.
	 */
	public StatsSnapshot getIdleNodes()
	{
		return idleNodes;
	}
}
