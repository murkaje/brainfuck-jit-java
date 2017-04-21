package ee.murkaje.brainfuck;

import java.time.Duration;
import java.time.Instant;

public class Timer {

  public static Duration measureExecution(Runnable task) {
    Instant begin = Instant.now();

    task.run();

    Instant end = Instant.now();
    return Duration.between(begin, end);
  }
}
