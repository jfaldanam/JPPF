/*
 * JPPF.
 * Copyright (C) 2005-2014 JPPF Team.
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

package org.jppf.utils;

/**
 * A set of utility methods to facilitate concurrent and multithreaded rpogramming.
 * @author Laurent Cohen
 * @since 5.0
 */
public final class ConcurrentUtils {
  /**
   * Instantiation is not permitted.
   */
  private ConcurrentUtils() {
  }

  /**
   * Wait until the specified condition is fulfilled, or the timeout exires, which ever happens first.
   * The specified monitor may be notified at any time during the execution of this method, at which time it will check the condition again.
   * @param monitor the monitor to wait for.
   * @param condition the condition to check.
   * @param millis the milliseconds part of the timeout. A value of zero means an infinite timeout.
   * @return true if the condition is {@code null} or was fulfilled before the timeout expired, {@code false} otherwise.
   * @throws IllegalArgumentException if the millis or nanos are negative, or if the nanos are greater than 999999.
   */
  public static boolean awaitCondition(final ThreadSynchronization monitor, final Condition condition, final long millis) throws IllegalArgumentException {
    if (monitor == null) throw new IllegalArgumentException("monitor cannot be null");
    if (condition == null) return true;
    if (millis < 0L) throw new IllegalArgumentException("millis cannot be negative");
    long timeout = (millis > 0L) ? millis : Long.MAX_VALUE;
    boolean fulfilled = false;
    long elapsed = 0L;
    final long start = System.currentTimeMillis();
    while (!(fulfilled = condition.evaluate()) && ((elapsed = System.currentTimeMillis() - start) < timeout)) {
      monitor.goToSleep(timeout - elapsed);
    }
    return fulfilled;
  }

  /**
   * Wait until the specified condition is fulfilled, or the timeout exires, which ever happens first.
   * This method waits for 1 millisecond each time the condition check fails and until the condition is fulfilled or the timeout expires.
   * @param condition the condition to check.
   * @param millis the milliseconds part of the timeout. A value of zero means an infinite timeout.
   * @param nanos additional nanoseconds added to the milliseconds part to refine the timeout.
   * @return true if the condition is {@code null} or was fulfilled before the timeout expired, {@code false} otherwise.
   * @throws IllegalArgumentException if the millis or nanos are negative, or if the nanos are greater than 999999.
   */
  public static boolean awaitCondition(final Condition condition, final long millis, final int nanos) throws IllegalArgumentException {
    if (condition == null) return true;
    if (millis < 0L) throw new IllegalArgumentException("millis cannot be negative");
    if ((nanos < 0L) || (nanos >= 1_000_000L)) throw new IllegalArgumentException("nanos must be in the range [0, 1_000_000[");
    long timeout = Long.MAX_VALUE;
    if (millis > 0L) {
      timeout = millis * 1_000_000L + nanos;
      // long overflow
      if (timeout < 0L) timeout = Long.MAX_VALUE;
    }
    long start = System.nanoTime();
    ThreadSynchronization lock = new ThreadSynchronization() { };
    boolean fulfilled = false;
    while (!(fulfilled = condition.evaluate()) && (System.nanoTime() - start < timeout)) {
      lock.goToSleep(1L);
    }
    return fulfilled;
  }

  /**
   * Wait until the specified condition is fulfilled, or the timeout exires, which ever happens first.
   * @param condition the condition to check.
   * @param millis the timeout in milliseconds. A value of zero means an infinite timeout.
   * @return true if the condition is {@code null} or was fulfilled before the timeout expired, {@code false} otherwise.
   * @throws IllegalArgumentException if the mililis are negative.
   */
  public static boolean awaitCondition(final Condition condition, final long millis) throws IllegalArgumentException {
    return awaitCondition(condition, millis);
  }

  /**
   * Wait until the specified condition is fulfilled.
   * @param condition the condition to check.
   * @return true whenever the condition is {@code null} or gets fulfilled, {@code false} otherwise.
   */
  public static boolean awaitCondition(final Condition condition) {
    return awaitCondition(condition, 0L, 0);
  }

  /**
   * This interface represents a condition to evaluate to either {@code true} or {@code false}.
   */
  public interface Condition {
    /**
     * Evaluate this condition.
     * @return {@code true} if the condition is fulfilled, {@code false} otherwise.
     */
    boolean evaluate();
  }
}
