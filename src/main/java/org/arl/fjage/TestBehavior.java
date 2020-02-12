/******************************************************************************

Copyright (c) 2013, Mandar Chitre

This file is part of fjage which is released under Simplified BSD License.
See file LICENSE.txt or go to http://www.opensource.org/licenses/BSD-3-Clause
for full license details.

******************************************************************************/

package org.arl.fjage;

/**
 * A behavior for JUnit test case design. This behavior is executed only once.
 * The {@link #test()} method of this behavior is called only once. Any
 * {@link java.lang.AssertionError} encountered during the test is stored
 * and can be later thrown using the {@link #checkAssertions()} method.
 * <p>
 * Typical usage of this behavior is shown below:
 * <pre>
 * // setup
 * Platform platform = new RealtimePlatform();
 * Container container = new Container(platform);
 * Agent agent = new Agent();
 * container.add(agent);
 * platform.start();
 *
 * // test
 * TestBehavior test = new TestBehavior() {
 *   public void test() {
 *     :
 *     :
 *   }
 * };
 * test.runOn(agent);
 *
 * // tear down
 * platform.shutdown();
 * </pre>
 *
 * @author  Mandar Chitre
 */
public class TestBehavior extends OneShotBehavior {

  /////////// Private attributes

  private AssertionError error = null;
  private boolean completed = false;

  ////////// Interface methods

  /**
   * Creates a test behavior.
   */
  public TestBehavior() {
  }

  /**
   * Creates a test behavior.
   *
   * @param runnable Runnable to run.
   */
  public TestBehavior(Runnable runnable) {
    this();
    if (runnable != null) {
      this.action = param -> runnable.run();
    }
  }

  ////////// Overridden methods

  @Override
  public final void action() {
    try {
      test();
    } catch (AssertionError ex) {
      error = ex;
    }
    completed = true;
  }

  @Override
  public void reset() {
    super.reset();
    completed = false;
    error = null;
  }

  /////////// Interface methods

  /**
   * Checks if the test has been completed.
   *
   * @return true if the test is completed, false otherwise.
   */
  public boolean hasCompleted() {
    return completed;
  }

  /**
   * Throws any AssertionError that was encountered during the test.
   */
  public void checkAssertions() {
    if (error != null) throw error;
  }

  /**
   * Runs a test as a behavior of a specified agent.
   *
   * @param agent agent to use for the test.
   */
  public void runOn(Agent agent) {
    reset();
    agent.add(this);
    while (!hasCompleted()) {
      try {
        Thread.sleep(100);
      } catch (InterruptedException ex) {
        Thread.currentThread().interrupt();
      }
    }
    checkAssertions();
  }

  ///////////// Methods for sub-classes to override

  /**
   * This method should be overridden by sub-classes. AssertionErrors may be
   * thrown if the test fails.
   */
  public void test() {
    super.action();
  }
}
