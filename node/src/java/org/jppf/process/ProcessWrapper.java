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
package org.jppf.process;

import java.io.*;
import java.util.*;

import org.jppf.process.event.*;

/**
 * Wrapper around an external process started with {@link java.lang.ProcessBuilder ProcessBuilder}.
 * Instances of this class read the output ands error streams generated by the process and provide
 * a notification mechanism with separate events for the respective streams. 
 * @author Laurent Cohen
 */
public final class ProcessWrapper
{
	/**
	 * The process to handle.
	 */
	private Process process = null;
	/**
	 * List of listeners to this wrapper's output and error stream events.
	 */
	private List<ProcessWrapperEventListener> listeners = new ArrayList<ProcessWrapperEventListener>();

	/**
	 * Initialize this process handler. 
	 */
	public ProcessWrapper()
	{
	}

	/**
	 * Initialize this process handler with the specified process. 
	 * @param process the process to handle.
	 */
	public ProcessWrapper(Process process)
	{
		setProcess(process);
	}

	/**
	 * Get the process to handle.
	 * @return a <code>Process</code> instance.
	 */
	public Process getProcess()
	{
		return process;
	}

	/**
	 * Set the process to handle.
	 * If the process has already been set through this setter or through the corresponding contructor, this method does nothing.
	 * @param process - a <code>Process</code> instance.
	 */
	public void setProcess(Process process)
	{
		if (this.process == null)
		{
			this.process = process;
			new StreamHandler(process.getInputStream(), true).start();
			new StreamHandler(process.getErrorStream(), false).start();
		}
	}

	/**
	 * Add a listener to this process wrapper's listeners list.
	 * @param listener the listener to add.
	 */
	public void addProcessWrapperEventListener(ProcessWrapperEventListener listener)
	{
		listeners.add(listener);
	}

	/**
	 * Remove a listener from this process wrapper's listeners list.
	 * @param listener the listener to remove.
	 */
	public void removeProcessWrapperEventListener(ProcessWrapperEventListener listener)
	{
		listeners.remove(listener);
	}

	/**
	 * Notify all listeners that a stream event has occurred.
	 * @param output true if the event is for the outpuit stream, false if it is for the error stream.   
	 * @param content the text that written to the stream.
	 */
	protected synchronized void fireStreamEvent(boolean output, String content)
	{
		ProcessWrapperEvent event = new ProcessWrapperEvent(content);
		for (ProcessWrapperEventListener listener: listeners)
		{
			if (output) listener.outputStreamAltered(event);
			else listener.errorStreamAltered(event);
		}
	}

	/**
	 * Used to empty the standard or error output of a process, so as not to block the process.
	 */
	private class StreamHandler extends Thread
	{
		/**
		 * Flag to determine whether the event is for output or error stream.
		 */
		private boolean output = true;
		/**
		 * The stream to get the output from.
		 */
		private InputStream is = null;

		/**
		 * Initialize this handler with the specified stream and buffer receiving its content.
		 * @param is the stream where output is taken from.
		 * @param output true if this event is for an output stream, false for an error stream.
		 */
		public StreamHandler(InputStream is, boolean output)
		{
			this.is = is;
			this.output = output;
		}

		/**
		 * Monitor the stream for avalaible data and write that data to the buffer.
		 * @see java.lang.Thread#run()
		 */
		public void run()
		{
			try
			{
				BufferedReader reader = new BufferedReader(new InputStreamReader(is));
				String s = "";
				while (s != null)
				{
					s = reader.readLine();
					if (s != null) fireStreamEvent(output, s);
				}
				Thread.sleep(10);
			}
			catch(Throwable t)
			{
				t.printStackTrace();
			}
		}
	}
}
