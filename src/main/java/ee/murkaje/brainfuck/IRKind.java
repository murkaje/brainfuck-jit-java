package ee.murkaje.brainfuck;

public enum IRKind {
  ADDP,       // > | x      : ptr += x
  DECP,       // < | x      : ptr -= x
  ADD,        // + | x | y  : data[ptr+y] += x
  DEC,        // - | x | y  : data[ptr+y] -= x
  READ,       // ,
  WRITE,      // .
  JZ,         // [ | x      : if(data[ptr] == 0) pc = x
  JNZ,        // ] | x      : if(data[ptr] != 0) pc = x

  LOOP_ZERO,  // [-]                        : data[ptr] = x
  LOOP_MOVP,  // [(>,x)]            | x     : while(data[ptr] != 0) { ptr += x; }
  LOOP_MOVD;  // [-(+,x,y)] | x | y : data[ptr+y] += x * data[ptr]; data[ptr] = 0

  public static IRKind fromChar(char opcode) {
    switch (opcode) {
      case '>':
        return ADDP;
      case '<':
        return DECP;
      case '+':
        return ADD;
      case '-':
        return DEC;
      case ',':
        return READ;
      case '.':
        return WRITE;
      case '[':
        return JZ;
      case ']':
        return JNZ;
      default:
        throw new IllegalArgumentException("Unknown opcode: " + opcode);
    }
  }
}
