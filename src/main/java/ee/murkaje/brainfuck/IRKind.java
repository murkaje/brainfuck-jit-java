package ee.murkaje.brainfuck;

public enum IRKind {
  ADDP,       // >
  DECP,       // <
  ADD,        // +
  DEC,        // -
  READ,       // ,
  WRITE,      // .
  JZ,         // [
  JNZ,        // ]

  LOOP_ZERO,  // [-] : c_n = 0
  LOOP_MOVP,  // [>>>>>] : while(data[ptr] != 0) { ptr += 5; }
  LOOP_MOVD;  // [-<<<<<+>>>>>] : c_{n-5} += c_n; c_n = 0
}
