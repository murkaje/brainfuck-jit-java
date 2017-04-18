package ee.murkaje.brainfuck;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.Duration;
import java.util.stream.Collectors;

public class Main {

  public static void main(String[] args) throws Exception {

    String program;

    try(InputStream in = Main.class.getClassLoader().getResourceAsStream("mandelbrot.bf");
        BufferedReader reader = new BufferedReader(new InputStreamReader(in))) {
      program = reader.lines().collect(Collectors.joining());
    }

    BrainFuck brainFuck = new BrainFuck();
    Duration time = brainFuck.interpret(program.toCharArray());

    System.out.println("Elapsed: " + time);
  }
}
