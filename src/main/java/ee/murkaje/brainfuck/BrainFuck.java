package ee.murkaje.brainfuck;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.NoSuchElementException;

import gnu.trove.map.TIntIntMap;
import gnu.trove.map.hash.TIntIntHashMap;

public class BrainFuck {

  // TODO: Brainfuck profiling, most common loops, etc.

  private char[] program;
  private List<IROpCode> irCode = new ArrayList<>();

  private PrintStream out = System.out;
  private InputStream in = System.in;

  public BrainFuck(char[] program) {
    this.program = program;
  }

  public void setOut(PrintStream out) {
    this.out = out;
  }

  public void setIn(InputStream in) {
    this.in = in;
  }

  private void parseIR() {
    Deque<IROpCode> jmpStack = new ArrayDeque<>();

    for (int pc = 0; pc < program.length; pc++) {
      char instruction = program[pc];
      int rep;

      switch (instruction) {
        case '>':
        case '<':
        case '+':
        case '-':
          rep = countRep(pc, instruction);
          irCode.add(new IROpCode(IRKind.fromChar(instruction), rep));
          pc += rep - 1;
          break;

        case '[': {
          // TODO: Handle common hot loops like [-] [>>>>>] [-<<<<<+>>>>>]
          IROpCode label = new IROpCode(IRKind.fromChar(instruction), irCode.size());
          irCode.add(label);
          jmpStack.push(label);
          break;
        }

        case ']': {
          IROpCode prevLabel;
          try {
            prevLabel = jmpStack.pop();
          }
          catch (NoSuchElementException e) {
            throw new RuntimeException("Syntax error: unmatched ']' at pc=" + pc);
          }

          // Take PC from previous label in stack and backpatch label with current PC
          IROpCode curLabel = new IROpCode(IRKind.fromChar(instruction), prevLabel.getArg1());
          prevLabel.setArg1(irCode.size());
          irCode.add(curLabel);
          break;
        }

        case ',':
        case '.':
          irCode.add(new IROpCode(IRKind.fromChar(instruction)));
        default:
          // Comment
      }
    }
  }

  private int countRep(int pc, char ins) {
    int count = 1;
    while (++pc < program.length && program[pc] == ins) {
      count++;
    }
    return count;
  }

  public Duration interpretDirect() {
    Instant begin = Instant.now();

    TIntIntMap jmpCache = new TIntIntHashMap();

    byte[] data = new byte[4096];
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
            data[ptr] = (byte) in.read();
          }
          catch (IOException e) {
            throw new RuntimeException("Could not read input at pc=" + pc, e);
          }
          break;
        case '.':
          out.print((char) data[ptr]);
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
          // Comment
      }
    }

    Instant end = Instant.now();
    return Duration.between(begin, end);
  }

  public Duration interpretOpt() {
    Instant begin = Instant.now();

    parseIR();

    byte[] data = new byte[4096];
    int ptr = 0;

    for (int pc = 0; pc < irCode.size(); pc++) {
      IROpCode instruction = irCode.get(pc);

      switch (instruction.getKind()) {
        case ADDP:
          ptr += instruction.getArg1();
          break;
        case DECP:
          ptr -= instruction.getArg1();
          break;
        case ADD:
          data[ptr] += instruction.getArg1();
          break;
        case DEC:
          data[ptr] -= instruction.getArg1();
          break;
        case READ:
          try {
            data[ptr] = (byte) in.read();
          }
          catch (IOException e) {
            throw new RuntimeException("Could not read input at pc=" + pc, e);
          }
          break;
        case WRITE:
          out.print((char) data[ptr]);
          break;
        case JZ:
          if (data[ptr] == 0) {
            pc = instruction.getArg1();
          }
          break;
        case JNZ:
          if (data[ptr] != 0) {
            pc = instruction.getArg1();
          }
          break;
        default:
          throw new RuntimeException("Not implemented: " + instruction.getKind());
      }
    }

    Instant end = Instant.now();
    return Duration.between(begin, end);
  }

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
