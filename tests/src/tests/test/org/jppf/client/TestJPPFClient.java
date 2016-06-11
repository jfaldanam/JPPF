/*
 * JPPF.
 * Copyright (C) 2005-2015 JPPF Team.
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

package test.org.jppf.client;

import static org.junit.Assert.*;

import java.io.NotSerializableException;
import java.lang.management.*;
import java.util.*;
import java.util.regex.Pattern;

import org.jppf.classloader.AbstractJPPFClassLoader;
import org.jppf.client.*;
import org.jppf.client.event.*;
import org.jppf.execute.AbstractThreadManager;
import org.jppf.management.*;
import org.jppf.node.protocol.*;
import org.jppf.utils.*;
import org.junit.Test;
import org.slf4j.*;

import test.org.jppf.test.setup.*;
import test.org.jppf.test.setup.common.*;

/**
 * Unit tests for <code>JPPFClient</code>.
 * @author Laurent Cohen
 */
public class TestJPPFClient extends Setup1D1N {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(TestJPPFClient.class);

  /**
   * Invocation of the <code>JPPFClient()</code> constructor.
   * @throws Exception if any error occurs
   */
  @Test(timeout=10000)
  public void testDefaultConstructor() throws Exception {
    try (JPPFClient client = new JPPFClient()) {
      while (!client.hasAvailableConnection()) Thread.sleep(10L);
    }
  }

  /**
   * Invocation of the <code>JPPFClient(String uuid)</code> constructor.
   * @throws Exception if any error occurs
   */
  @Test(timeout=10000)
  public void testConstructorWithUuid() throws Exception {
    try (JPPFClient client = new JPPFClient("some_uuid")) {
      while (!client.hasAvailableConnection()) Thread.sleep(10L);
    }
  }

  /**
   * Test the submission of a job.
   * @throws Exception if any error occurs
   */
  @Test(timeout=10000)
  public void testSubmit() throws Exception {
    try (JPPFClient client = BaseSetup.createClient(null)) {
      int nbTasks = 10;
      JPPFJob job = BaseTestHelper.createJob(ReflectionUtils.getCurrentClassAndMethod(), true, false, nbTasks, LifeCycleTask.class, 0L);
      int i = 0;
      for (Task<?> task: job.getJobTasks()) task.setId("" + i++);
      List<Task<?>> results = client.submitJob(job);
      assertNotNull(results);
      assertEquals(nbTasks, results.size());
      String msg = BaseTestHelper.EXECUTION_SUCCESSFUL_MESSAGE;
      for (i=0; i<nbTasks; i++) {
        Task<?> task = results.get(i);
        Throwable t = task.getThrowable();
        assertNull("task " + i +" has an exception " + t, t);
        assertEquals("result of task " + i + " should be " + msg + " but is " + task.getResult(), msg, task.getResult());
      }
    }
  }

  /**
   * Test the cancellation of a job.
   * @throws Exception if any error occurs
   */
  @SuppressWarnings("deprecation")
  @Test(timeout=10000)
  public void testCancelJob() throws Exception {
    try (JPPFClient client = BaseSetup.createClient(null)) {
      int nbTasks = 10;
      JPPFJob job = BaseTestHelper.createJob(ReflectionUtils.getCurrentClassAndMethod(), false, false, nbTasks, LifeCycleTask.class, 5000L);
      int i = 0;
      for (Task<?> task: job.getJobTasks()) task.setId("" + i++);
      ExecutingJobStatusListener statusListener = new ExecutingJobStatusListener();
      job.getResultCollector().addJobStatusListener(statusListener);
      client.submitJob(job);
      statusListener.await();
      client.cancelJob(job.getUuid());
      List<Task<?>> results = job.awaitResults();
      assertNotNull(results);
      assertTrue("results size should be " + nbTasks + " but is " + results.size(), results.size() == nbTasks);
      int count = 0;
      for (Task<?> task: results) {
        if (task.getResult() == null) count++;
      }
      assertTrue(count > 0);
    }
  }

  /**
   * Test that the number of threads for local execution is the configured one.
   * See bug <a href="http://sourceforge.net/tracker/?func=detail&aid=3539111&group_id=135654&atid=733518">3539111 - Local execution does not use configured number of threads</a>.
   * @throws Exception if any error occurs
   */
  @Test(timeout=10000)
  public void testLocalExecutionNbThreads() throws Exception
  {
    int nbThreads = 2;
    TypedProperties config = JPPFConfiguration.getProperties();
    config.setBoolean("jppf.local.execution.enabled", true);
    config.setInt("jppf.local.execution.threads", nbThreads);
    try (JPPFClient client = new JPPFClient()) {
      while (!client.hasAvailableConnection()) Thread.sleep(10L);
      // submit a job to ensure all local execution threads are created
      int nbTasks = 100;
      JPPFJob job = BaseTestHelper.createJob(ReflectionUtils.getCurrentClassAndMethod(), true, false, nbTasks, LifeCycleTask.class, 0L);
      int i = 0;
      for (Task<?> task: job) task.setId("" + i++);
      List<Task<?>> results = client.submitJob(job);
      assertNotNull(results);
      assertEquals(nbTasks, results.size());
      String msg = BaseTestHelper.EXECUTION_SUCCESSFUL_MESSAGE;
      for (Task<?> task: results) {
        Throwable t = task.getThrowable();
        assertNull(t);
        assertEquals(msg, task.getResult());
      }
      ThreadMXBean mxbean = ManagementFactory.getThreadMXBean();
      long[] ids = mxbean.getAllThreadIds();
      ThreadInfo[] allInfo = mxbean.getThreadInfo(ids);
      int count = 0;
      for (ThreadInfo ti: allInfo) {
        if (ti == null) continue;
        String name = ti.getThreadName();
        if (name == null) continue;
        if (name.startsWith(AbstractThreadManager.THREAD_NAME_PREFIX)) count++;
      }
      assertEquals(nbThreads, count);
    } finally {
      JPPFConfiguration.reset();
    }
  }

  /**
   * Test that the thread context class loader during local execution of a task is not null.
   * See bug <a href="http://www.jppf.org/tracker/tbg/jppf/issues/JPPF-174">JPPF-174 Thread context class loader is null for client-local execution</a>.
   * @throws Exception if any error occurs
   */
  @Test(timeout=10000)
  public void testLocalExecutionContextClassLoader() throws Exception {
    TypedProperties config = JPPFConfiguration.getProperties();
    config.setBoolean("jppf.remote.execution.enabled", false);
    config.setBoolean("jppf.local.execution.enabled", true);
    try (JPPFClient client = new JPPFClient()) {
      JPPFJob job = BaseTestHelper.createJob(ReflectionUtils.getCurrentClassAndMethod(), true, false, 1, ThreadContextClassLoaderTask.class);
      List<Task<?>> results = client.submitJob(job);
      assertNotNull(results);
      assertEquals(1, results.size());
      Task<?> task = results.get(0);
      assertEquals(null, task.getThrowable());
      assertNotNull(task.getResult());
    } finally {
      JPPFConfiguration.reset();
    }
  }

  /**
   * Test that the thread context class loader during remote execution of a task is not null, that it matches the task classloader
   * and that both are client class loader.
   * See bug <a href="http://www.jppf.org/tracker/tbg/jppf/issues/JPPF-153">JPPF-153 In the node, context class loader and task class loader do not match after first job execution</a>.
   * @throws Exception if any error occurs
   */
  @Test(timeout=10000)
  public void testRemoteExecutionContextClassLoader() throws Exception {
    TypedProperties config = JPPFConfiguration.getProperties();
    config.setProperty("jppf.rmeote.execution.enabled", "true");
    config.setProperty("jppf.local.execution.enabled", "false");
    try (JPPFClient client = new JPPFClient()) {
      while (!client.hasAvailableConnection()) Thread.sleep(10L);
      String name = ReflectionUtils.getCurrentClassAndMethod();
      client.submitJob(BaseTestHelper.createJob(name + "-1", true, false, 1, ThreadContextClassLoaderTask.class));
      JPPFJob job = BaseTestHelper.createJob(name + "-2", true, false, 1, ThreadContextClassLoaderTask.class);
      List<Task<?>> results = client.submitJob(job);
      assertNotNull(results);
      assertEquals(1, results.size());
      Task<?> task = results.get(0);
      assertEquals(null, task.getThrowable());
      assertNotNull(task.getResult());
    } finally {
      JPPFConfiguration.reset();
    }
  }

  /**
   * Test that a {@link java.io.NotSerializableException} occurring when submitting a job to a driver is properly handled.
   * @throws Exception if any error occurs
   */
  @Test(timeout=10000)
  public void testNotSerializableExceptionFromClient() throws Exception {
    try (JPPFClient client = new JPPFClient()) {
      JPPFJob job = BaseTestHelper.createJob(ReflectionUtils.getCurrentClassAndMethod(), true, false, 1, NotSerializableTask.class, true);
      List<Task<?>> results = client.submitJob(job);
      assertNotNull(results);
      assertEquals(1, results.size());
      Task<?> task = results.get(0);
      assertNotNull(task.getThrowable());
      assertTrue(task.getThrowable() instanceof NotSerializableException);
    }
  }

  /**
   * Test that a {@link java.io.NotSerializableException} occurring when a node returns execution results is properly handled.
   * @throws Exception if any error occurs
   */
  @Test(timeout=10000)
  public void testNotSerializableExceptionFromNode() throws Exception {

    try (JPPFClient client = new JPPFClient()) {
      JPPFJob job = BaseTestHelper.createJob(ReflectionUtils.getCurrentClassAndMethod(), true, false, 1, NotSerializableTask.class, false);
      List<Task<?>> results = client.submitJob(job);
      assertNotNull(results);
      assertEquals(1, results.size());
      Task<?> task = results.get(0);
      assertTrue(task instanceof NotSerializableTask);
      Throwable t = task.getThrowable();
      assertNotNull(t);
      assertTrue("wrong exception: " + ExceptionUtils.getStackTrace(t), t instanceof NotSerializableException);
    }
  }

  /**
   * Test that JMX connection threads are properly terminated when the JPPF connection fails.
   * This relates to the bug <a href="http://www.jppf.org/tracker/tbg/jppf/issues/JPPF-131">JPPF-131 JPPF client does not release JMX thread upon connection failure</a>
   * @throws Exception if any error occurs
   */
  @Test(timeout=20000)
  public void testNoJMXConnectionThreadsLeak() throws Exception {
    String name = Thread.currentThread().getName();
    MyClient client = null;
    TypedProperties config = JPPFConfiguration.getProperties();
    try {
      Thread.currentThread().setName("JPPF-test");
      int poolSize = 2;
      int maxReconnect = 3;
      config.setInt("jppf.reconnect.initial.delay", 1);
      config.setInt("jppf.reconnect.max.time", maxReconnect);
      config.setBoolean("jppf.discovery.enabled", false);
      config.setProperty("jppf.drivers", "test");
      config.setProperty("test.jppf.server.host", "localhost");
      config.setInt("test.jppf.server.port", 11101);
      config.setInt("test.jppf.pool.size", poolSize);
      config.setInt("test.jppf.management.port", 11201);
      ConnectionPoolListener listener = new ConnectionPoolListenerAdapter() {
        @Override public void connectionAdded(final ConnectionPoolEvent event) {
          TestUtils.printf(log, "connectionAdded(%s) : connectionCount = %d", event .getConnection(), event.getConnectionPool().getClient().getAllConnectionsCount());
        }
        @Override public void connectionRemoved(final ConnectionPoolEvent event) {
          TestUtils.printf(log, "connectionRemoved(%s) : connectionCount = %d", event.getConnection(), event.getConnectionPool().getClient().getAllConnectionsCount());
        }
        @Override public void connectionPoolAdded(final ConnectionPoolEvent event) {
          TestUtils.printf(log, "connectionPoolAdded() : added pool %s", event.getConnectionPool());
        }
        @Override public void connectionPoolRemoved(final ConnectionPoolEvent event) {
          TestUtils.printf(log, "connectionPoolRemoved() : removed pool %s, call stack:%n%s", event.getConnectionPool(), ExceptionUtils.getCallStack());
        }
      };
      client = new MyClient(listener);
      waitForNbConnections(client, poolSize, JPPFClientConnectionStatus.ACTIVE);
      restartDriver(client, poolSize, 1_000L * maxReconnect + 1500L);
      String[] threads = threadNames("^" + JMXConnectionWrapper.CONNECTION_NAME_PREFIX + ".*");
      assertEquals(0, threads.length);
    } catch(Exception e) {
      e.printStackTrace();
      throw e;
    } finally {
      if (client != null) client.close();
      JPPFConfiguration.reset();
      Thread.currentThread().setName(name);
    }
  }

  /**
   * Restart the driver.
   * @param client the JPPF client.
   * @param poolSize the number of expected connections.
   * @param restartDelay the driver restart delay in milliseconds.
   * @throws Exception if any error occurs.
   */
  private void restartDriver(final MyClient client, final int poolSize, final long restartDelay) throws Exception {
    JMXDriverConnectionWrapper jmx = getJmxConnection(client);
    TestUtils.printf(log, "***** restarting driver with restartDelay = %d ms *****", restartDelay);
    jmx.restartShutdown(100L, restartDelay);
    TestUtils.printf(log, "***** driver restart requested *****", restartDelay);
    waitForNbConnections(client, 0, JPPFClientConnectionStatus.ACTIVE);
    waitForNbConnections(client, 0, null);
    Runnable r = new Runnable() {
      @Override
      public void run() {
        client.initRemotePools(JPPFConfiguration.getProperties());
      }
    };
    new Thread(r, "InitPools").start();
    waitForNbConnections(client, poolSize, JPPFClientConnectionStatus.ACTIVE);
  }

  /**
   * Wait until the client has the specified number of connections.
   * @param client the JPPF client.
   * @param status the expected status of each connection.
   * @param nbConnections the number of connections to reach.
   * @throws Exception if any error occurs.
   */
  private void waitForNbConnections(final JPPFClient client, final int nbConnections, final JPPFClientConnectionStatus status) throws Exception {
    if (status != null) client.awaitConnectionPools(Operator.EQUAL, nbConnections, 0L, status);
    else {
      int n = -1;
      while ((n = client.getAllConnectionsCount()) != nbConnections) Thread.sleep(1L);
    }
    if (nbConnections > 0) getJmxConnection(client);
  }

  /**
   * Get a connected JMX connection for the specified client.
   * @param client the JPPF client.
   * @return a {@link JMXDriverConnectionWrapper} instance.
   * @throws Exception if any error occurs.
   */
  private JMXDriverConnectionWrapper getJmxConnection(final JPPFClient client) throws Exception {
    return client.awaitActiveConnectionPool().awaitJMXConnections(Operator.AT_LEAST, 1, true).get(0);
  }

  /**
   * Get the names of all threads in this JVM matching the specified regex pattern.
   * @param pattern the pattern to match against.
   * @return an array of thread names.
   */
  private String[] threadNames(final String pattern) {
    Pattern p = pattern == null ? null : Pattern.compile(pattern);
    ThreadMXBean threadsBean = ManagementFactory.getThreadMXBean();
    long[] ids = threadsBean.getAllThreadIds();
    ThreadInfo[] infos = threadsBean.getThreadInfo(ids, 0);
    List<String> result = new ArrayList<>();
    for (int i=0; i<infos.length; i++) {
      if ((p == null) || p.matcher(infos[i].getThreadName()).matches())
        result.add(infos[i].getThreadName());
    }
    return result.toArray(new String[result.size()]);
  }

  /**
   * A task that checks the current thread context class loader during its execution.
   */
  public static class ThreadContextClassLoaderTask extends AbstractTask<String> {
    @Override
    public void run() {
      ClassLoader cl = Thread.currentThread().getContextClassLoader();
      if (cl == null) throw new IllegalStateException("thread context class loader is null for " + (isInNode() ? "remote" : "local")  + " execution");
      if (isInNode()) {
        if (!(cl instanceof AbstractJPPFClassLoader))
          throw new IllegalStateException("thread context class loader for remote execution should be an AbstractJPPFClassLoader, but is " + cl);
        AbstractJPPFClassLoader ajcl2 = (AbstractJPPFClassLoader) getTaskClassLoader();
        if (cl != ajcl2) {
          throw new IllegalStateException("thread context class loader and task class loader do not match:\n" +
              "thread context class loader = " + cl + "\n" +
              "task class loader = " + ajcl2);
        }
        if (!ajcl2.isClientClassLoader()) throw new IllegalStateException("class loader is not a client class loader:" + ajcl2);
      }
      setResult(cl.toString());
    }
  }

  /**
   *
   */
  public static class MyClient extends JPPFClient {
    /**
     *
     * @param listener .
     */
    public MyClient(final ConnectionPoolListener listener) {
      super(listener);
    }

    @Override
    public void initRemotePools(final TypedProperties props) {
      super.initRemotePools(props);
    }
  }
}
