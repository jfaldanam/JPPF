/*
 * JPPF.
 * Copyright (C) 2005-2019 JPPF Team.
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

package test.org.jppf.server.protocol;

import static org.junit.Assert.*;
import static test.org.jppf.server.protocol.TaskDependenciesHelper.createDiamondTasks;

import java.util.List;

import org.jppf.client.JPPFJob;
import org.jppf.node.protocol.Task;
import org.junit.Test;

import test.org.jppf.server.protocol.TaskDependenciesHelper.*;
import test.org.jppf.test.setup.Setup1D2N1C;

/**
 * 
 * @author Laurent Cohen
 */
public class TestTaskDependencies2 extends Setup1D2N1C {
  /**
   * Test the submission of a job with tasks dependencies.
   * @throws Exception if any error occurs.
   */
  @Test(timeout = 5000L)
  public void testGraphSubmission() throws Exception {
    final MyTask[] tasks = createDiamondTasks();
    final JPPFJob job = new JPPFJob();
    job.addWithDpendencies(tasks[0]);
    assertTrue(job.hasTaskGraph());
    final DispatchListener listener = new DispatchListener();
    job.addJobListener(listener);
    final List<Task<?>> result = client.submit(job);
    assertNotNull(result);
    assertEquals(tasks.length, result.size());
    for (final Task<?> task: result) {
      assertTrue(task instanceof MyTask);
      final MyTask myTask = (MyTask) task;
      assertNull(myTask.getThrowable());
      assertNotNull(myTask.getResult());
      assertEquals("executed " + myTask.getId(), myTask.getResult());
    }
    final List<Integer> dispatches = listener.dispatches;
    assertEquals(3, dispatches.size());
    assertEquals(1, (int) dispatches.get(0));
    assertEquals(2, (int) dispatches.get(1));
    assertEquals(1, (int) dispatches.get(2));
  }
}