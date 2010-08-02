/*
 * JPPF.
 * Copyright (C) 2005-2010 JPPF Team.
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
package sample.test.profiling;

import java.util.*;

import org.apache.commons.logging.*;
import org.jppf.client.*;
import org.jppf.client.event.*;
import org.jppf.server.JPPFStats;
import org.jppf.server.protocol.JPPFTask;
import org.jppf.utils.*;

/**
 * Runner class for the &quot;Long Task&quot; demo.
 * @author Laurent Cohen
 */
public class ProfilingRunner
{
	/**
	 * Logger for this class.
	 */
	static Log log = LogFactory.getLog(ProfilingRunner.class);
	/**
	 * JPPF client used to submit execution requests.
	 */
	private static JPPFClient jppfClient = null;
	/**
	 * Size of the data in each task, in KB.
	 */
	private static int dataSize = JPPFConfiguration.getProperties().getInt("profiling.data.size");

	/**
	 * Entry point for this class, submits the tasks with a set duration to the server.
	 * @param args not used.
	 */
	public static void main(String...args)
	{
		try
		{
			MyClientListener clientListener = new MyClientListener();
			jppfClient = new JPPFClient(clientListener);
			TypedProperties props = JPPFConfiguration.getProperties();
			int nbTask = props.getInt("profiling.nbTasks");
			int iterations = props.getInt("profiling.iterations");
			System.out.println("Running with "+nbTask+" tasks for "+iterations+" iterations");
			//performSequential(nbTask, true);
			perform(nbTask, iterations);
			System.exit(0);
		}
		catch(Exception e)
		{
			e.printStackTrace();
			System.exit(1);
		}
	}
	
	/**
	 * Execute the specified number of tasks for the specified number of iterations.
	 * @param nbTask the number of tasks to send at each iteration.
	 * @param iterations the number of times the the tasks will be sent.
	 * @throws Exception if an error is raised during the execution.
	 */
	private static void perform(int nbTask, int iterations) throws Exception
	{
		// perform "iteration" times
		for (int iter=0; iter<iterations; iter++)
		{
			long start = System.currentTimeMillis();
			JPPFJob job = new JPPFJob();
			for (int i=0; i<nbTask; i++) job.addTask(new EmptyTask(dataSize));
			// submit the tasks for execution
			List<JPPFTask> results = jppfClient.submit(job);
			long elapsed = System.currentTimeMillis() - start;
			System.out.println("Iteration #"+(iter+1)+" performed in "+StringUtils.toStringDuration(elapsed));
		}
		/*
		JPPFStats stats = jppfClient.requestStatistics();
		System.out.println("End statistics :\n"+stats.toString());
		*/
	}

	/**
	 * Execute the specified number of tasks for the specified number of iterations.
	 * @param nbTask the number of tasks to send at each iteration.
	 * @param silent determines whether resuls should be displayed on the console.
	 * @throws Exception if an error is raised during the execution.
	 */
	private static void performSequential(int nbTask, boolean silent) throws Exception
	{
		long start = System.currentTimeMillis();
		List<JPPFTask> tasks = new ArrayList<JPPFTask>();
		for (int i=0; i<nbTask; i++) tasks.add(new EmptyTask(dataSize));
		// submit the tasks for execution
		for (JPPFTask task: tasks) task.run();
		long elapsed = System.currentTimeMillis() - start;
		if (!silent)
			System.out.println("Sequential iteration performed in "+StringUtils.toStringDuration(elapsed));
	}

	/**
	 * Check the status of a connection and determine grid availability.
	 * @param connection the driverconnection to check.
	 * @return true if the grid is available, false otherwise.
	 * @throws Exception if nay error occurs.
	 */
	protected static boolean isGridAvailable(JPPFClientConnection connection) throws Exception
	{
		boolean gridAvailable = false;

		if (connection != null)
		{
			if (connection.getStatus().equals(JPPFClientConnectionStatus.ACTIVE))
			{
				// jppfClient may not yet be initialized.
				JPPFClientConnectionImpl impl = (JPPFClientConnectionImpl) connection;
				//JPPFStats statistics = jppfClient.requestStatistics();
				JPPFStats statistics = impl.getJmxConnection().statistics();

				if (statistics != null && statistics.getNbNodes() > 0)
				{
					gridAvailable = true;
					log.info("Grid available");
				}
				else
				{
					log.warn("Nodes on grid not available");
				}
			}
			else
			{
				log.warn("Connection to grid not active: " + connection.getStatus());
			}
		}
		else
		{
			log.warn("Connection to grid not available");
		}

		return false;
	}

  /**
   * Listener for connection status change events.
   */
  public static class MyStatusListener implements ClientConnectionStatusListener
  {
    /**
     * {@inheritDoc}
     */
    public void statusChanged(ClientConnectionStatusEvent event)
    {
      JPPFClientConnection connection = (JPPFClientConnection) event.getClientConnectionStatusHandler();
      JPPFClientConnectionStatus status = connection.getStatus();
      log.info("Connection " + connection.getName() + " status changed to " + status);
      try
      {
      	boolean available = isGridAvailable(connection);
      }
      catch(Exception e)
      {
      	log.error(e.getMessage(), e);
      }
    }
  }

  /**
   * Listener for new driver connections.
   */
  public static class MyClientListener implements ClientListener
  {
    /**
     * {@inheritDoc}
     */
    public void newConnection(ClientEvent event)
    {
      // the new connection is the source of the event
      JPPFClientConnection connection = event.getConnection();
      log.info("New connection with name " + connection.getName());
      // register to receive staus events on the new connection
      connection.addClientConnectionStatusListener(new MyStatusListener());
    }
  }
}
