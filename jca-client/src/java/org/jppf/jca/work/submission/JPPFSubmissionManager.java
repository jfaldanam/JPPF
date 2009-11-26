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

package org.jppf.jca.work.submission;

import static org.jppf.jca.work.submission.SubmissionStatus.*;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.resource.spi.work.*;

import org.apache.commons.logging.*;
import org.jppf.client.*;
import org.jppf.jca.work.*;
import org.jppf.utils.ThreadSynchronization;

/**
 * This task provides asynchronous management of tasks submitted through the resource adapter.
 * It relies on a queue where job are first added, then submitted when a driver connection becomes available.
 * It also provides methods to check the status of a submission and retrieve the results.
 * @author Laurent Cohen
 */
public class JPPFSubmissionManager extends ThreadSynchronization implements Work
{
	/**
	 * Logger for this class.
	 */
	private static Log log = LogFactory.getLog(JPPFSubmissionManager.class);
	/**
	 * The queue of submissions pending execution.
	 */
	private ConcurrentLinkedQueue<JPPFJob> execQueue = new ConcurrentLinkedQueue<JPPFJob>();
	/**
	 * Mapping of submissions to their submission id.
	 */
	private Map<String, JPPFSubmissionResult> submissionMap = new Hashtable<String, JPPFSubmissionResult>();
	/**
	 * The JPPF client that manages connections to the JPPF drivers.
	 */
	private JPPFJcaClient client = null;
	/**
	 * The work manager provided by the applications server, used to submit asynchronous
	 * JPPF submissions.
	 */
	private WorkManager workManager = null;
	/**
	 * Mapping of class laoder to requests uuids.
	 */
	private Map<String, ClassLoader> classLoaderMap = new Hashtable<String, ClassLoader>();

	/**
	 * Initialize this submission worker with the specified JPPF client.
	 * @param client the JPPF client that manages connections to the JPPF drivers.
	 * @param workManager the work manager provided by the applications server, used to submit asynchronous
	 * JPPF submissions.
	 */
	public JPPFSubmissionManager(JPPFJcaClient client, WorkManager workManager)
	{
		this.client = client;
		this.workManager = workManager;
	}

	/**
	 * Stop this submission manager.
	 * @see javax.resource.spi.work.Work#release()
	 */
	public void release()
	{
		setStopped(true);
		wakeUp();
	}

	/**
	 * Run the loop of this submission manager, watching for the queue and starting a job
	 * when the queue has one and a connnection is available.
	 * @see java.lang.Runnable#run()
	 */
	public void run()
	{
		while (!isStopped())
		{
			while ((execQueue.peek() == null) || !client.hasAvailableConnection())
			{
				goToSleep();
			}
			if (isStopped()) break;
			JPPFJob job = execQueue.poll();
			JPPFJcaClientConnection c = null;
			synchronized(client)
			{
				c = (JPPFJcaClientConnection) client.getClientConnection();
				c.setStatus(JPPFClientConnectionStatus.EXECUTING);
			}
			JPPFSubmissionResult submission = (JPPFSubmissionResult) job.getResultListener();
			try
			{
				workManager.scheduleWork(new JcaResultProcessor(c, job));
			}
			catch(WorkException e)
			{
				submission.setStatus(FAILED);
				c.setStatus(JPPFClientConnectionStatus.ACTIVE);
				log.error(e.getMessage(), e);
			}
		}
	}

	/**
	 * Add a task submission to the execution queue.
	 * @param job encapsulation of the execution data.
	 * @return the unique id of the submission.
	 */
	public String addSubmission(JPPFJob job)
	{
		return addSubmission(job, null);
	}

	/**
	 * Add a task submission to the execution queue.
	 * @param job encapsulation of the execution data.
	 * @param listener an optional listener to receive submission status change notifications, may be null.
	 * @return the unique id of the submission.
	 */
	public String addSubmission(JPPFJob job, SubmissionStatusListener listener)
	{
		JPPFSubmissionResult submission = new JPPFSubmissionResult(job.getTasks().size());
		if (listener != null) submission.addSubmissionStatusListener(listener);
		job.setResultListener(submission);
		submission.setStatus(PENDING);
		execQueue.add(job);
		submissionMap.put(submission.getId(), submission);
		wakeUp();
		return submission.getId();
	}

	/**
	 * Add an existing submission back into the execution queue.
	 * @param job encapsulation of the execution data.
	 * @return the unique id of the submission.
	 */
	public String addExistingSubmission(JPPFJob job)
	{
		JPPFSubmissionResult submission = (JPPFSubmissionResult) job.getResultListener();
		submission.reset();
		submission.setStatus(PENDING);
		execQueue.add(job);
		submissionMap.put(submission.getId(), submission);
		wakeUp();
		return submission.getId();
	}

	/**
	 * Get a submission given its id, without removing it from this submissison manager.
	 * @param id the id of the submission to find.
	 * @return the submisison corresponding to the id, or null if the submission could not be found.
	 */
	public JPPFSubmissionResult peekSubmission(String id)
	{
		return submissionMap.get(id);
	}

	/**
	 * Get a submission given its id, and remove it from this submissison manager.
	 * @param id the id of the submission to find.
	 * @return the submisison corresponding to the id, or null if the submission could not be found.
	 */
	public JPPFSubmissionResult pollSubmission(String id)
	{
		return submissionMap.remove(id);
	}

	/**
	 * Get the ids of all currently available submissions.
	 * @return a collection of ids as strings.
	 */
	public Collection<String> getAllSubmissionIds()
	{
		return Collections.unmodifiableSet(submissionMap.keySet());
	}

	/**
	 * Add a request uuid to class loader mapping to this submission manager.
	 * @param uuid the uuid of the request.
	 * @param cl trhe class loader for the request.
	 */
	public void addRequestClassLoader(String uuid, ClassLoader cl)
	{
		classLoaderMap.put(uuid, cl);
	}

	/**
	 * Add a request uuid to class loader mapping to this submission manager.
	 * @param uuid the uuid of the request.
	 */
	public void removeRequestClassLoader(String uuid)
	{
		classLoaderMap.remove(uuid);
	}

	/**
	 * Get a class loader from its request uuid.
	 * @param uuid the uuid of the request.
	 * @return a <code>ClassLoader</code> instance, or null if none exists for the key.
	 */
	public ClassLoader getRequestClassLoader(String uuid)
	{
		return classLoaderMap.get(uuid);
	}
}
