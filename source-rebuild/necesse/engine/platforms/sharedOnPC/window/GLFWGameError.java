package necesse.engine.platforms.sharedOnPC.window;

import java.io.PrintStream;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.IntStream;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.system.APIUtil;

public class GLFWGameError {
   private static final Map<Integer, String> ERROR_CODES = APIUtil.apiClassTokens((field, value) -> 65536 < value && value < 131072, null, GLFW.class);
   private static final Object throwLock = new Object();
   private static int[] handledCodes;
   public final int errorCode;
   public final String errorName;
   public final long errorDescriptionPointer;
   public final String errorDescription;
   public final StackTraceElement[] stackTrace;
   protected boolean caught = false;

   public static void throwError(int errorCode, long errorDescriptionPointer) {
      synchronized (throwLock) {
         GLFWGameError error = new GLFWGameError(errorCode, errorDescriptionPointer);
         if (handledCodes == null || handledCodes.length != 0 && !IntStream.of(handledCodes).anyMatch(code -> error.errorCode == code)) {
            error.print();
         } else {
            throw new GLFWGameError.GLFWGameErrorException(error);
         }
      }
   }

   private GLFWGameError(int errorCode, long errorDescriptionPointer) {
      this.errorCode = errorCode;
      this.errorName = ERROR_CODES.getOrDefault(errorCode, "GLFW_UNKNOWN_ERROR");
      this.errorDescriptionPointer = errorDescriptionPointer;
      this.errorDescription = errorDescriptionPointer == 0L ? "No description" : GLFWErrorCallback.getDescription(errorDescriptionPointer);
      StackTraceElement[] fullStackTrace = Thread.currentThread().getStackTrace();
      this.stackTrace = Arrays.copyOfRange(fullStackTrace, 6, fullStackTrace.length);
   }

   public void print(PrintStream stream) {
      stream.println(this.errorName + ": " + this.errorDescription);

      for (StackTraceElement e : this.stackTrace) {
         stream.println("\t" + e.toString());
      }
   }

   public void print() {
      this.print(System.err);
   }

   public static void tryGLFWError(GLFWGameError.Handler handler) {
      synchronized (throwLock) {
         try {
            handledCodes = handler.errorCodes;
            handler.run();
         } catch (GLFWGameError.GLFWGameErrorException e) {
            handler.onCatch(e.error);
         } finally {
            handledCodes = null;
            handler.onFinally();
         }
      }
   }

   public static <T> T tryGLFWError(GLFWGameError.Supplier<T> handler) {
      synchronized (throwLock) {
         Object var3;
         try {
            handledCodes = handler.errorCodes;
            return handler.run();
         } catch (GLFWGameError.GLFWGameErrorException e) {
            var3 = handler.onCatch(e.error);
         } finally {
            handledCodes = null;
            handler.onFinally();
         }

         return (T)var3;
      }
   }

   private static class GLFWGameErrorException extends RuntimeException {
      public final GLFWGameError error;

      public GLFWGameErrorException(GLFWGameError error) {
         this.error = error;
      }
   }

   public abstract static class Handler {
      protected final int[] errorCodes;

      public Handler(int... errorCodes) {
         this.errorCodes = errorCodes;
      }

      public abstract void run();

      public abstract void onCatch(GLFWGameError var1);

      public void onFinally() {
      }
   }

   public abstract static class Supplier<T> {
      protected final int[] errorCodes;

      public Supplier(int... errorCodes) {
         this.errorCodes = errorCodes;
      }

      public abstract T run();

      public abstract T onCatch(GLFWGameError var1);

      public void onFinally() {
      }
   }
}
