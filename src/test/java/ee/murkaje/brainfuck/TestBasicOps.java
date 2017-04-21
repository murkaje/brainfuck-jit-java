package ee.murkaje.brainfuck;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

public class TestBasicOps {

  private ByteArrayOutputStream runProgram(String program, InputStream in) {
    ByteArrayOutputStream out = new ByteArrayOutputStream();

    BrainFuck machine = new BrainFuck(program.toCharArray());
    machine.setOut(new PrintStream(out));
    machine.setIn(in);

    try {
      runMode(machine);
    }
    catch (Throwable t) {
      t.printStackTrace();
      throw t;
    }

    return out;
  }

  protected void runMode(BrainFuck machine) {
    machine.interpretOpt();
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
    String program = "> +++ +++ [--<++++>+] < ."; //extra add+sub disables LOOP_MOVD optimization

    ByteArrayOutputStream out = runProgram(program, null);

    assertEquals(24, out.toByteArray()[0]);
  }

  @Test
  void testInput() {
    String program = ", .";

    ByteArrayOutputStream out = runProgram(program, new ByteArrayInputStream(new byte[]{10}));

    assertEquals(10, out.toByteArray()[0]);
  }

  @Test
  void testInputAdd() {
    String program = "++ > , +++ +++ .";

    ByteArrayOutputStream out = runProgram(program, new ByteArrayInputStream(new byte[]{10}));

    assertEquals(16, out.toByteArray()[0]);
  }

  @Test
  void testLoopMovD() {
    String program = "+++ +++ [->>++++<<] >> .";

    ByteArrayOutputStream out = runProgram(program, null);

    assertEquals(6 * 4, out.toByteArray()[0]);
  }

  @Test
  void testLoopNotMovD() {
    // [0]=3 [1]=2 [2]=0
    // [0]=2 [1]=2 [2]=0 [3]=4
    // [0]=2 [1]=1 [2]=0 [3]=4 [4]=4
    // [2]=0 -> break
    // print [4]=4
    String program = "+++ > ++ >  << [->>>++++<<] >> .";

    ByteArrayOutputStream out = runProgram(program, null);

    assertEquals(4, out.toByteArray()[0]);
  }
}
