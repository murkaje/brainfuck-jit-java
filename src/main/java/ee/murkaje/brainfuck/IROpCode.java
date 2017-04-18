package ee.murkaje.brainfuck;

public class IROpCode {

  private IRKind kind;
  private int argument;

  public IROpCode(IRKind kind, int argument) {
    this.kind = kind;
    this.argument = argument;
  }

  public IRKind getKind() {
    return kind;
  }

  public int getArgument() {
    return argument;
  }
}
