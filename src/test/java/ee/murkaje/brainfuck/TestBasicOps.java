package ee.murkaje.brainfuck;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;

import org.junit.jupiter.api.Test;

public class TestBasicOps {

  private static ByteArrayOutputStream runProgram(String program, InputStream in) {
    ByteArrayOutputStream out = new ByteArrayOutputStream();

    BrainFuck machine = new BrainFuck(program.toCharArray());
    machine.setOut(new PrintStream(out));
    machine.setIn(in);

    machine.interpretOpt();

    return out;
  }

  @Test
  void testAdd() {
    String program = "++++ +++ .";

    ByteArrayOutputStream out = runProgram(program, null);

    assertEquals(7, out.toByteArray()[0]);
  }

  @Test
  void testAddSub() {
    String program = "++++ +++ -- + -- .";

    ByteArrayOutputStream out = runProgram(program, null);

    assertEquals(4, out.toByteArray()[0]);
  }

  @Test
  void testMultLoop() {
    String program = "> +++ +++ [-<++++>] < .";

    ByteArrayOutputStream out = runProgram(program, null);

    assertEquals(24, out.toByteArray()[0]);
  }

  @Test
  void testInput() {
    String program = "+++ +++ > , [-<+>] < .";

    ByteArrayOutputStream out = runProgram(program, new ByteArrayInputStream(new byte[]{10}));

    assertEquals(16, out.toByteArray()[0]);
  }
}
