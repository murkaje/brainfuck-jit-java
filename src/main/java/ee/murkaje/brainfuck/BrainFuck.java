package ee.murkaje.brainfuck;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;

import gnu.trove.map.TIntIntMap;
import gnu.trove.map.hash.TIntIntHashMap;

public class BrainFuck {

  // TODO: Brainfuck profiling, most common loops, etc.

  public Duration interpret(char[] instructions) {
    Instant begin = Instant.now();

    TIntIntMap jmpCache = new TIntIntHashMap();

    byte[] data = new byte[4096];
    int ptr = 0;

    for (int pc = 0; pc < instructions.length; pc++) {
      char instruction = instructions[pc];
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
            int readValue = System.console().reader().read();
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
            int bracketLevel = 1;
            int seek = pc;

            while (bracketLevel != 0 && ++seek < instructions.length) {
              if (instructions[seek] == '[') {
                bracketLevel++;
              }
              else if (instructions[seek] == ']') {
                bracketLevel--;
              }
            }

            if (bracketLevel != 0) {
              throw new RuntimeException("Syntax Error: Unmatched '[' at pc=" + pc);
            }
            jmpCache.put(pc, seek);
          }

          pc = jmpCache.get(pc);

          break;
        case ']':
          if (data[ptr] == 0) break;

          if (!jmpCache.containsKey(pc)) {
            int bracketLevel = 1;
            int seek = pc;

            while (bracketLevel != 0 && seek-- > 0) {
              if (instructions[seek] == '[') {
                bracketLevel--;
              }
              else if (instructions[seek] == ']') {
                bracketLevel++;
              }
            }

            if (bracketLevel != 0) {
              throw new RuntimeException("Syntax Error: Unmatched ']' at pc=" + pc);
            }
            jmpCache.put(pc, seek);
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
}
