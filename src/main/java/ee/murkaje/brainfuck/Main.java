package ee.murkaje.brainfuck;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.Duration;
import java.util.stream.Collectors;

public class Main {

  public static void main(String[] args) throws Exception {
//    Thread.sleep(8000);
    String code;

    try (InputStream in = Main.class.getClassLoader().getResourceAsStream("mandelbrot.bf");
         BufferedReader reader = new BufferedReader(new InputStreamReader(in))) {
      code = reader.lines().collect(Collectors.joining());
    }

    BrainFuck brainFuck = new BrainFuck(code.toCharArray());
//    Duration time = Timer.measureExecution(brainFuck::interpretOpt);
//    System.out.println("Elapsed: " + time);

    // Should also measure this to be fair
    BrainFuckProgram program = brainFuck.compile();
    Duration time2 = Timer.measureExecution(program::run);
    System.out.println("Elapsed: " + time2);

    for(int i=0; i<10000; i++) {
      program = brainFuck.compile();
      time2 = Timer.measureExecution(program::run);
      System.out.println(i + ": Elapsed: " + time2);
    }
  }
}
