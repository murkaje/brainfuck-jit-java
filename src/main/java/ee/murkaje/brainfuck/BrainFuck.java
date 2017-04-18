package ee.murkaje.brainfuck;

import java.io.Console;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import gnu.trove.map.TIntIntMap;
import gnu.trove.map.hash.TIntIntHashMap;

public class BrainFuck {

  // TODO: Brainfuck profiling, most common loops, etc.

  private char[] program;
  private byte[] data;

  private List<IROpCode> irCode;

  public BrainFuck(char[] program) {
    this.program = program;
  }

  /**
   * Read instructions and translate them to IR
   * e.g. +++++ -> (ADD,5) ; [-] -> LOOP_ZERO
   */
  private void parse() {

  }

  public Duration interpret() {
    Instant begin = Instant.now();

    TIntIntMap jmpCache = new TIntIntHashMap();

    Console console = System.console();
    data = new byte[4096];
    int ptr = 0;

    for (int pc = 0; pc < program.length; pc++) {
      char instruction = program[pc];
      switch (instruction) {
        case '<':
          ptr--;
          break;
        case '>':
          ptr++;
          break;
        case '+':
          data[ptr]++;
          break;
        case '-':
          data[ptr]--;
          break;
        case ',':
          try {
            if(console == null) {
              throw new IllegalStateException("Program requires input but not started from interactive console");
            }

            int readValue = console.reader().read();
            data[ptr] = (byte) readValue;
          }
          catch (IOException e) {
            e.printStackTrace();
          }
          break;
        case '.':
          System.out.print((char) data[ptr]);
          break;
        case '[':
          if (data[ptr] != 0) break;

          if (!jmpCache.containsKey(pc)) {
            jmpCache.put(pc, seekJmp(pc, 1));
          }
          pc = jmpCache.get(pc);

          break;
        case ']':
          if (data[ptr] == 0) break;

          if (!jmpCache.containsKey(pc)) {
            jmpCache.put(pc, seekJmp(pc, -1));
          }
          pc = jmpCache.get(pc);

          break;
        default:
          //All other characters treated as comments
      }
    }

    System.out.println();
    Instant end = Instant.now();

    return Duration.between(begin, end);
  }

  // TODO: Do extra pass before interpreting and translate all ops to IR while keeping track of loop start-pc stack to backpatch jmp address
  // This function will not be needed anymore
  @Deprecated
  private int seekJmp(int pc, int direction) {
    if (direction != -1 && direction != 1) throw new IllegalArgumentException("direction must be 1 or -1");

    char instruction = program[pc];
    int bracketLevel = 1;

    while (bracketLevel != 0 && pc >= 0 && pc < program.length) {
      pc += direction;

      if (program[pc] == '[') {
        bracketLevel += direction;
      }
      else if (program[pc] == ']') {
        bracketLevel -= direction;
      }
    }

    if (bracketLevel != 0) {
      throw new RuntimeException("Syntax Error: Unmatched '" + instruction + "' at pc=" + pc);
    }

    return pc;
  }
}
