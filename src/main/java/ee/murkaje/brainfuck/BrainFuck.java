package ee.murkaje.brainfuck;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.NoSuchElementException;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

/*
 * TODO: memset data+ptr ... data+ptr+off
 * (LOOP_ZERO, 1, 0)
 * (ADDP, 1, 0)
 * (LOOP_ZERO, 1, 0)
 * (ADDP, 1, 0)
 * (LOOP_ZERO, 1, 0)
 * (ADDP, 1, 0)
 * (LOOP_ZERO, 1, 0)
 * (ADDP, 1, 0)
 * (LOOP_ZERO, 1, 0)
 * (ADDP, 1, 0)
 * (LOOP_ZERO, 1, 0)
 * (ADDP, 1, 0)
 * (LOOP_ZERO, 1, 0)
 * (ADDP, 1, 0)
 * (LOOP_ZERO, 1, 0)
 * (ADDP, 1, 0)
 * (LOOP_ZERO, 1, 0)
 *
 * TODO: more complicated moves
 *
 * [- >>>-<<< <<<<<<<<<<<+>>>>>>>>>>>]
 * ->
 * (LOOP_MOVD, 3, -1)(LOOP_MOVD, -11, 1)
 *
 */

public class BrainFuck {

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

  private void checkAndOptAddOff() {
    if (irCode.size() < 3) return;

    IROpCode o1 = irCode.get(irCode.size() - 3);
    IROpCode o2 = irCode.get(irCode.size() - 2);
    IROpCode o3 = irCode.get(irCode.size() - 1);

    int off;
    int ptrDelta;

    if (o1.getKind() == IRKind.ADDP && o3.getKind() == IRKind.DECP) {
      off = o1.getArg1();
      ptrDelta = o1.getArg1() - o3.getArg1();
    }
    else if (o1.getKind() == IRKind.DECP && o3.getKind() == IRKind.ADDP) {
      off = -o1.getArg1();
      ptrDelta = o3.getArg1() - o1.getArg1();
    }
    else {
      return;
    }

    if (o2.getKind() == IRKind.ADD || o2.getKind() == IRKind.DEC) {
      irCode.remove(irCode.size() - 1);
      irCode.remove(irCode.size() - 1);
      irCode.remove(irCode.size() - 1);

      irCode.add(new IROpCode(o2.getKind(), o2.getArg1(), off));

      if (ptrDelta < 0) {
        irCode.add(new IROpCode(IRKind.DECP, -ptrDelta));
      }
      else if (ptrDelta > 0) {
        irCode.add(new IROpCode(IRKind.ADDP, ptrDelta));
      }
    }
  }

  // Expect: checkAndOptAddOff
  private boolean checkAndOptLoopAdd() {
    if (irCode.size() < 3) return false;

    IROpCode o1 = irCode.get(irCode.size() - 3);
    IROpCode o2 = irCode.get(irCode.size() - 2);
    IROpCode o3 = irCode.get(irCode.size() - 1);

    if (o1.getKind() != IRKind.JZ) return false;

    if (o2.getKind() != IRKind.DEC || o2.getArg1() != 1 || o2.getArg2() != 0) return false;

    if (o3.getKind() != IRKind.DEC && o3.getKind() != IRKind.ADD) return false;

    irCode.remove(irCode.size() - 1);
    irCode.remove(irCode.size() - 1);
    irCode.remove(irCode.size() - 1);

    int off = o3.getArg2();
    int mult = (o3.getKind() == IRKind.ADD) ? o3.getArg1() : -o3.getArg1();
    irCode.add(new IROpCode(IRKind.LOOP_MOVD, mult, off));

    return true;
  }

  private void parseIR() {
    Deque<IROpCode> jmpStack = new ArrayDeque<>();

    for (int pc = 0; pc < program.length; pc++) {
      char instruction = program[pc];

      switch (instruction) {
        case '>':
        case '<':
        case '+':
        case '-': {
          int rep = countRep(pc, instruction);
          irCode.add(new IROpCode(IRKind.fromChar(instruction), rep));
          pc += rep - 1;

          // (>,x)(+,y)(<,x) -> (+,y,x)
          if (instruction == '<' || instruction == '>') {
            checkAndOptAddOff();
          }

          break;
        }
        case '[': {
          // [-]
          if (pc + 2 < program.length && (program[pc + 1] == '-' || program[pc + 1] == '+') && program[pc + 2] == ']') {
            irCode.add(new IROpCode(IRKind.LOOP_ZERO));
            pc += 2;
            break;
          }

          // [(>,x)] or [(<,x)]
          if (pc + 2 < program.length && (program[pc + 1] == '<' || program[pc + 1] == '>')) {
            char ins = program[pc + 1];
            int rep = countRep(pc + 1, ins);
            if (pc + rep < program.length && program[pc + rep + 1] == ']') {
              int dir = (ins == '<') ? -1 : 1;
              irCode.add(new IROpCode(IRKind.LOOP_MOVP, rep * dir));
              pc += rep + 1;
              break;
            }
          }

          IROpCode label = new IROpCode(IRKind.fromChar(instruction), irCode.size());
          jmpStack.push(label);
          irCode.add(label);

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

          // [-<+>]
          if (!checkAndOptLoopAdd()) {
            // Take PC from previous label in stack and backpatch label with current PC
            IROpCode curLabel = new IROpCode(IRKind.fromChar(instruction), prevLabel.getArg1());
            prevLabel.setArg1(irCode.size());
            irCode.add(curLabel);
          }

          break;
        }

        case ',':
        case '.':
          irCode.add(new IROpCode(IRKind.fromChar(instruction)));
        default:
          // Comment
      }
    }

    for (int i = 0; i < irCode.size(); i++) {
      System.out.println(i + ": " + irCode.get(i));
    }
  }

  private int countRep(int pc, char ins) {
    int count = 1;
    while (++pc < program.length && program[pc] == ins) {
      count++;
    }
    return count;
  }

  byte[] data = new byte[4096];

  public void interpretOpt() {
    parseIR();

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
          data[ptr + instruction.getArg2()] += instruction.getArg1();
          break;
        case DEC:
          data[ptr + instruction.getArg2()] -= instruction.getArg1();
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
        case LOOP_ZERO:
          data[ptr] = 0;
          break;
        case LOOP_MOVP:
          while (data[ptr] != 0) {
            ptr += instruction.getArg1();
          }
          break;
        case LOOP_MOVD:
          if (data[ptr] != 0) {
            data[ptr + instruction.getArg2()] += (byte) (instruction.getArg1() * data[ptr]);
            data[ptr] = 0;
          }
          break;
        default:
          throw new RuntimeException("Not implemented: " + instruction.getKind());
      }
    }
  }

  private Class<?> cachedClass = null;

  public BrainFuckProgram compile() {
    try {
      if (cachedClass != null) {
        Constructor<?> constructor = cachedClass.getDeclaredConstructor(PrintStream.class, InputStream.class);
        return (BrainFuckProgram) constructor.newInstance(out, in);
      }

      parseIR();

      ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
      String className = Type.getInternalName(BrainFuckProgram.class) + "$compiled$" + BrainFuckProgram.getNextId();
      cw.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC, className, null, Type.getInternalName(BrainFuckProgram.class), null);

      cw.visitSource("mandelbrot.bf", null);

      MethodVisitor initMv = cw.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "(Ljava/io/PrintStream;Ljava/io/InputStream;)V", null, null);
      initMv.visitVarInsn(Opcodes.ALOAD, 0);
      initMv.visitVarInsn(Opcodes.ALOAD, 1);
      initMv.visitVarInsn(Opcodes.ALOAD, 2);
      initMv.visitMethodInsn(Opcodes.INVOKESPECIAL, Type.getInternalName(BrainFuckProgram.class), "<init>", "(Ljava/io/PrintStream;Ljava/io/InputStream;)V", false);
      initMv.visitInsn(Opcodes.RETURN);
      initMv.visitMaxs(-1, -1);
      initMv.visitEnd();

      MethodVisitor runMv = cw.visitMethod(Opcodes.ACC_PUBLIC, "run", "()V", null, null);
      IRCompilingMethodAdapter compilingMv = new IRCompilingMethodAdapter(runMv, className, irCode);
      compilingMv.visitCode();
      compilingMv.visitEnd();

      cw.visitEnd();
      byte[] classBytes = cw.toByteArray();

      Path filePath = Paths.get("target/generated/", className + ".class");
      Files.createDirectories(filePath.getParent());
      Files.write(filePath, classBytes);

      ClassLoader cl = BrainFuck.class.getClassLoader();
      Method defineClass = ClassLoader.class.getDeclaredMethod("defineClass", String.class, byte[].class, int.class, int.class);
      defineClass.setAccessible(true);
      Class<?> compiledProgramClass = (Class<?>) defineClass.invoke(cl, className.replace("/", "."), classBytes, 0, classBytes.length);

      cachedClass = compiledProgramClass;

      Constructor<?> constructor = compiledProgramClass.getDeclaredConstructor(PrintStream.class, InputStream.class);
      return (BrainFuckProgram) constructor.newInstance(out, in);
    }
    catch (Exception e) {
      throw new RuntimeException("Compilation failed", e);
    }
  }
}
