package ee.murkaje.brainfuck;

public class IROpCode {

  private IRKind kind;
  private int arg1 = 1;
  private int arg2 = 0;

  public IROpCode(IRKind kind) {
    this.kind = kind;
  }

  public IROpCode(IRKind kind, int arg1) {
    this.kind = kind;
    this.arg1 = arg1;
  }

  public IROpCode(IRKind kind, int arg1, int arg2) {
    this.kind = kind;
    this.arg1 = arg1;
    this.arg2 = arg2;
  }

  public IRKind getKind() {
    return kind;
  }

  public int getArg1() {
    return arg1;
  }

  public void setArg1(int arg1) {
    this.arg1 = arg1;
  }

  public int getArg2() {
    return arg2;
  }

  public void setArg2(int arg2) {
    this.arg2 = arg2;
  }

  @Override
  public String toString() {
    return "(" + kind + ", " + arg1 + ", " + arg2 + ")";
  }
}
