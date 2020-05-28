import org.arl.fjage.*

class TestMessage
    extends Message {

}

class TestMessageGeneratorAgent
    extends Agent {

  void init() {
    add new TickerBehavior(5000, {
      final Message message = new TestMessage()
      message.recipient = agent('lambda-support')
      send(message);
    })
  }
}

class GroovyLambdaSupportTestAgent
    extends Agent {

  void init() {
    add new OneShotBehavior({ System.out.println("Hello, world!") })

    add new PoissonBehavior(5000, { System.out.println("Hello, world every 5s (Poisson)!") })

    add new TickerBehavior(5000, { System.out.println("Hello, world every 5s (ticker)!") })

    add new WakerBehavior(5000, { System.out.println("Hello, world after 5s (waker)!") })

    add new MessageBehavior(TestMessage.class, { message -> System.out.println("Hello, world (message)!") })
  }
}

container.add 'test-message-generator', new TestMessageGeneratorAgent()
container.add 'lambda-support', new GroovyLambdaSupportTestAgent()
