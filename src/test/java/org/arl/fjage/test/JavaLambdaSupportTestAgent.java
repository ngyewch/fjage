package org.arl.fjage.test;

import org.arl.fjage.*;

public class JavaLambdaSupportTestAgent
    extends Agent {

  @Override
  public void init() {
    add(new OneShotBehavior(() -> System.out.println("Hello, world!")));

    add(new PoissonBehavior(5000, () -> System.out.println("Hello, world every 5s (Poisson)!")));

    add(new TickerBehavior(5000, () -> System.out.println("Hello, world every 5s (ticker)!")));

    add(new WakerBehavior(5000, () -> System.out.println("Hello, world after 5s (waker)!")));
  }
}
