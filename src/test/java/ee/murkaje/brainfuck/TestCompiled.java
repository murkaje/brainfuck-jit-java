package ee.murkaje.brainfuck;

public class TestCompiled extends TestBasicOps {

  @Override
  protected void runMode(BrainFuck machine) {
    BrainFuckProgram program = machine.compile();
    program.run();
  }
}
