package ee.murkaje.brainfuck;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.InstructionAdapter;

public class IRCompilingMethodAdapter extends InstructionAdapter {

  public static final String DATA_FIELD_NAME = "data";
  public static final String DATA_FIELD_DESC = "[B";

  public static final String IN_FIELD_NAME = "in";
  public static final String IN_FIELD_DESC = "Ljava/io/InputStream;";

  public static final String OUT_FIELD_NAME = "out";
  public static final String OUT_FIELD_DESC = "Ljava/io/PrintStream;";

  private String className;
  private List<IROpCode> code;

  public IRCompilingMethodAdapter(MethodVisitor mv, String className, List<IROpCode> code) {
    super(Opcodes.ASM5, mv);

    this.className = className;
    this.code = code;
  }

  @Override
  public void visitCode() {
    Label methodStart = new Label();
    Label methodEnd = new Label();
    int lineNumber = 0;

    visitLabel(methodStart);
    iconst(0);
    store(1, Type.INT_TYPE);

    load(0, Type.getObjectType(className));
    getfield(className, DATA_FIELD_NAME, DATA_FIELD_DESC);
    store(2, Type.getType(byte[].class));

    Deque<Label> loopStartLabels = new ArrayDeque<>();
    Deque<Label> loopEndLabels = new ArrayDeque<>();

    for (IROpCode insn : code) {
      Label lineLabel = new Label();
      visitLabel(lineLabel);
      visitLineNumber(lineNumber++, lineLabel);

      switch (insn.getKind()) {
        // (>,x) -> ptr += x
        case ADDP:
          iinc(1, insn.getArg1());
          break;

        // (<,x) -> ptr -= x
        case DECP:
          iinc(1, -insn.getArg1());
          break;

        // (+,x,y) -> data[ptr+y] += x
        case ADD:
          load(2, Type.getType(byte[].class));
          load(1, Type.INT_TYPE);
          if (insn.getArg2() != 0) {
            iconst(insn.getArg2());
            add(Type.INT_TYPE);
          }
          dup2();
          aload(Type.BYTE_TYPE);
          iconst(insn.getArg1());
          add(Type.INT_TYPE);
          cast(Type.INT_TYPE, Type.BYTE_TYPE);
          astore(Type.BYTE_TYPE);
          break;

        // (-,x,y) -> data[ptr+y] -= x
        case DEC:
          load(2, Type.getType(byte[].class));
          load(1, Type.INT_TYPE);
          if (insn.getArg2() != 0) {
            iconst(insn.getArg2());
            add(Type.INT_TYPE);
          }
          dup2();
          aload(Type.BYTE_TYPE);
          iconst(insn.getArg1());
          sub(Type.INT_TYPE);
          cast(Type.INT_TYPE, Type.BYTE_TYPE);
          astore(Type.BYTE_TYPE);
          break;

        case READ:
          load(0, Type.getObjectType(className));
          load(1, Type.INT_TYPE);
          invokevirtual(Type.getInternalName(BrainFuckProgram.class), "read", "(I)V", false);
          break;
//          {
//          Label tryBegin = new Label();
//          Label tryEnd = new Label();
//          Label catchBegin = new Label();
//          Label catchEnd = new Label();
//
//          visitLabel(tryBegin);
//          load(0, Type.getObjectType(className));
//          getfield(className, DATA_FIELD_NAME, DATA_FIELD_DESC);
//          load(1, Type.INT_TYPE);
//          load(0, Type.getObjectType(className));
//          getfield(className, IN_FIELD_NAME, IN_FIELD_DESC);
//          invokevirtual("java/io/InputStream", "read", "()I", false);
//          cast(Type.INT_TYPE, Type.BYTE_TYPE);
//          astore(Type.BYTE_TYPE);
//          visitLabel(tryEnd);
//
//          goTo(catchEnd);
//
//          visitLabel(catchBegin);
//          store(2, Type.getType(IOException.class));
//          anew(Type.getType(RuntimeException.class));
//          dup();
//          anew(Type.getType(StringBuilder.class));
//          dup();
//          visitLdcInsn("Could not read input to ptr=");
//          invokespecial("java/lang/StringBuilder", "<init>", "()V", false);
//          load(1, Type.INT_TYPE);
//          invokevirtual("java/lang/StringBuilder", "append", "(I)Ljava/lang/StringBuilder;", false);
//          invokevirtual("java/lang/StringBuilder", "toString", "()Ljava/lang/String;", false);
//          load(2, Type.getType(IOException.class));
//          invokespecial("java/lang/RuntimeException", "<init>", "(Ljava/lang/String;Ljava/lang/Throwable;)V", false);
//          athrow();
//          visitLabel(catchEnd);
//          visitLocalVariable("readException", "Ljava/lang/IOException;", null, catchBegin, catchEnd, 2);
//          break;
//        }

        case WRITE:
          load(0, Type.getObjectType(className));
          getfield(className, OUT_FIELD_NAME, OUT_FIELD_DESC);
          load(2, Type.getType(byte[].class));
          load(1, Type.INT_TYPE);
          aload(Type.BYTE_TYPE);
          cast(Type.BYTE_TYPE, Type.CHAR_TYPE);
          invokevirtual("java/io/PrintStream", "print", "(C)V", false);
          break;

        case JZ:
          loopStartLabels.push(new Label());
          loopEndLabels.push(new Label());

          load(2, Type.getType(byte[].class));
          load(1, Type.INT_TYPE);
          aload(Type.BYTE_TYPE);
          ifeq(loopEndLabels.peek());
          visitLabel(loopStartLabels.peek());
          break;

        case JNZ:
          load(2, Type.getType(byte[].class));
          load(1, Type.INT_TYPE);
          aload(Type.BYTE_TYPE);
          ifne(loopStartLabels.pop());
          visitLabel(loopEndLabels.pop());
          break;

        case LOOP_ZERO:
          load(2, Type.getType(byte[].class));
          load(1, Type.INT_TYPE);
          iconst(0);
          astore(Type.BYTE_TYPE);
          break;

        case LOOP_MOVP:
          load(0, Type.getObjectType(className));
          load(1, Type.INT_TYPE);
          iconst(insn.getArg1());
          invokevirtual(Type.getInternalName(BrainFuckProgram.class), "loopMovp", "(II)I", false);
          store(1, Type.INT_TYPE);
          break;
//          {
//          Label begin = new Label();
//          Label end = new Label();
//
//          visitLabel(begin);
//          load(0, Type.getObjectType(className));
//          getfield(className, DATA_FIELD_NAME, DATA_FIELD_DESC);
//          load(1, Type.INT_TYPE);
//          aload(Type.BYTE_TYPE);
//          ifeq(end);
//          iinc(1, insn.getArg1());
//          goTo(begin);
//          visitLabel(end);
//          break;
//        }

        // (L+,mult,off) -> if(data[ptr] != 0) { data[ptr+off] += data[ptr] * mult; data[ptr] = 0 }
        case LOOP_MOVD:
          load(0, Type.getObjectType(className));
          load(1, Type.INT_TYPE);
          iconst(insn.getArg2());
          if(insn.getArg1() == 1) {
            invokevirtual(Type.getInternalName(BrainFuckProgram.class), "loopMovdA", "(II)V", false);
          } else if(insn.getArg1() == -1 ) {
            invokevirtual(Type.getInternalName(BrainFuckProgram.class), "loopMovdS", "(II)V", false);
          } else {
            iconst(insn.getArg1());
            invokevirtual(Type.getInternalName(BrainFuckProgram.class), "loopMovd", "(III)V", false);
          }
          break;
//        {
//          Label end = new Label();
//
//          load(2, Type.getType(byte[].class));
//          load(1, Type.INT_TYPE);
//          aload(Type.BYTE_TYPE);
//          ifeq(end);
//
//          load(2, Type.getType(byte[].class));
//
//          load(1, Type.INT_TYPE);
//          iconst(insn.getArg2());
//          add(Type.INT_TYPE);
//          dup2();
//          aload(Type.BYTE_TYPE);
//          load(2, Type.getType(byte[].class));
//          load(1, Type.INT_TYPE);
//          aload(Type.BYTE_TYPE);
//
//          if (insn.getArg1() != 1 && insn.getArg1() != -1) {
//            iconst(insn.getArg1());
//            mul(Type.INT_TYPE);
//          }
//          else {
//            iconst(1);
//          }
//          if (insn.getArg1() == -1) {
//            sub(Type.INT_TYPE);
//          }
//          else if (insn.getArg1() == 1) {
//            add(Type.INT_TYPE);
//          }
//
//          cast(Type.INT_TYPE, Type.BYTE_TYPE);
//          astore(Type.BYTE_TYPE);
//
//          load(0, Type.getObjectType(className));
//          getfield(className, DATA_FIELD_NAME, DATA_FIELD_DESC);
//          load(1, Type.INT_TYPE);
//          iconst(0);
//          astore(Type.BYTE_TYPE);
//
//          visitLabel(end);
//          break;
//        }

        default:
          throw new IllegalStateException("Not implemented opcode: " + insn);
      }
    }

    areturn(Type.VOID_TYPE);
    visitLabel(methodEnd);
    visitMaxs(-1, -1);
    visitLocalVariable("this", "L" + className + ";", null, methodStart, methodEnd, 0);
    visitLocalVariable("ptr", "I", null, methodStart, methodEnd, 1);
    visitLocalVariable("data", "[B", null, methodStart, methodEnd, 2);
  }
}