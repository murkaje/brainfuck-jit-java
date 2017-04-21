package ee.murkaje.brainfuck;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class BrainFuckProgram {

  private static AtomicInteger counter = new AtomicInteger(0);

  public static int getNextId() {
    return counter.getAndIncrement();
  }

  // TODO: Generate bridge methods
  public byte[] data;
  public PrintStream out;

  public InputStream in;

  public BrainFuckProgram(PrintStream out, InputStream in) {
    data = new byte[4096];
    this.out = out;
    this.in = in;
  }

  public abstract void run();

  // Higher level operations pulled here so JVM can JIT them
  // Can essentially generate the same methods in the class as long as the main monolithic method gets broken up

  public void loopMovd(int ptr, int off, int mult) {
    if(data[ptr] != 0) {
      data[ptr + off] += mult * data[ptr];
      data[ptr] = 0;
    }
  }

  public void loopMovdA(int ptr, int off) {
    if(data[ptr] != 0) {
      data[ptr + off] += data[ptr];
      data[ptr] = 0;
    }
  }

  public void loopMovdS(int ptr, int off) {
    if(data[ptr] != 0) {
      data[ptr + off] -= data[ptr];
      data[ptr] = 0;
    }
  }

  public void read(int ptr) {
    try {
      data[ptr] = (byte) in.read();
    }
    catch (IOException e) {
      throw new RuntimeException("Could not read input", e);
    }
  }

  public int loopMovp(int ptr, int step) {
    while (data[ptr] != 0) {
      ptr += step;
    }
    return ptr;
  }
}
