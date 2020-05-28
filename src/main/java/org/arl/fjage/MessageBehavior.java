/******************************************************************************

Copyright (c) 2013, Mandar Chitre

This file is part of fjage which is released under Simplified BSD License.
See file LICENSE.txt or go to http://www.opensource.org/licenses/BSD-3-Clause
for full license details.

******************************************************************************/

package org.arl.fjage;

/**
 * A behavior that continuously monitors the incoming message queue. The
 * {@link #onReceive(Message)} method of this behavior is called for every
 * incoming message that matches the input filter defined for the behavior.
 *
 * @author  Mandar Chitre
 */
public class MessageBehavior extends Behavior {

  ///////////// Private attributes

  private MessageFilter filter;

  //////////// Interface methods

  /**
   * Creates a MessageBehavior that accepts all incoming messages.
   */
  public MessageBehavior() {
    filter = null;
  }

  /**
   * Creates a MessageBehavior that accepts all incoming messages of a given
   * class.
   *
   * @param cls message class of interest.
   */
  public MessageBehavior(final Class<?> cls) {
    filter = m -> cls.isInstance(m);
  }

  /**
   * Creates a MessageBehavior that accepts all incoming messages that meet
   * a given MessageFilter criteria.
   *
   * @param filter message filter.
   */
  public MessageBehavior(final MessageFilter filter) {
    this.filter = filter;
  }

  /**
   * Checks if this MessageBehavior has a filter associated with it.
   */
  boolean hasFilter() {
    return filter != null;
  }

  /**
   * Check if this MessageBehavior accepts a specific message.
   *
   * @param msg message to check.
   * @return true if it accepts, false otherwise.
   */
  public boolean accepts(Message msg) {
    if (filter == null) return true;
    return filter.matches(msg);
  }

  //////////// Method to be overridden by subclass

  /**
   * This method is called for each message meeting the acceptance criteria of
   * this behavior. A behavior must override this.
   *
   * @param msg received message.
   */
  public void onReceive(Message msg) {
    if (action != null) action.call(msg);
  }

  //////////// Overridden methods

  /**
   * This method calls {@link #onReceive(Message)} for every message received that meets
   * the acceptance criteria. The method causes the behavior to be blocked until
   * appropriate message becomes available.
   *
   * @see org.arl.fjage.Behavior#action()
   */
  @Override
  public final void action() {
    Message msg;
    if (filter == null) msg = agent.receive();
    else msg = agent.receive(filter, 0);
    if (msg == null) block();
    else onReceive(msg);
  }

  /**
   * This method always returns false, since this behavior never terminates.
   *
   * @return false.
   * @see org.arl.fjage.Behavior#done()
   */
  @Override
  public final boolean done() {
    return false;
  }

  /**
   * Message behaviors with filters return a priority value of -100 to allow them to be
   * executed before general message behaviors (no filters, priority value of 0).
   */
  @Override
  public int getPriority() {
    if (filter != null) return -100;
    return 0;
  }

}

