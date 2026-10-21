package necesse.engine.platforms.steam;

import com.codedisaster.steamworks.SteamLibraryLoader;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.function.Predicate;
import necesse.engine.GlobalData;
import org.lwjgl.system.Configuration;
import org.lwjgl.system.Library;
import org.lwjgl.system.Platform;
import org.lwjgl.system.Platform.Architecture;

public class GameSteamLibraryLoader implements SteamLibraryLoader {
   @Override
   public void setLibraryPath(String libraryPath) {
      System.setProperty("org.lwjgl.librarypath", libraryPath);
   }

   @Override
   public boolean loadLibrary(String libraryName) {
      return this.loadLibrary(libraryName, false);
   }

   public boolean loadLibrary(String libraryName, boolean fallback) {
      Platform os = Platform.get();
      Architecture arch = Platform.getArchitecture();
      if (os == Platform.WINDOWS && arch == Architecture.X64 && !fallback) {
         libraryName = libraryName + "64";
      }

      boolean debugLWJGLAlreadyEnabled = Configuration.DEBUG_LOADER.get(false);

      try {
         if (!debugLWJGLAlreadyEnabled) {
            boolean previousDebugValue = Configuration.DEBUG.get(false);
            boolean previousDebugLoaderValue = Configuration.DEBUG_LOADER.get(false);
            Object previousDebugStream = Configuration.DEBUG_STREAM.get();
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            PrintStream printStream = new PrintStream(out, true, StandardCharsets.UTF_8.name());
            Configuration.DEBUG.set(true);
            Configuration.DEBUG_LOADER.set(true);
            Configuration.DEBUG_STREAM.set(printStream);
            this.tryLoadLibrary(libraryName, fallback, out);
            Configuration.DEBUG.set(previousDebugValue);
            Configuration.DEBUG_LOADER.set(previousDebugLoaderValue);
            if (previousDebugStream != null) {
               Configuration.DEBUG_STREAM.set(previousDebugStream);
            }

            printStream.close();
         } else {
            this.tryLoadLibrary(libraryName, fallback, null);
         }

         return true;
      } catch (UnsupportedEncodingException e) {
         throw new RuntimeException(e);
      }
   }

   private void tryLoadLibrary(String libraryName, boolean fallback, ByteArrayOutputStream debugStream) throws UnsupportedEncodingException {
      try {
         Library.loadSystem("com.codedisaster.steamworks", libraryName);
      } catch (Throwable t) {
         Throwable linkError = this.getThrowableCause(t, e -> e instanceof UnsatisfiedLinkError);
         if (linkError != null && !fallback) {
            System.err.println("Failed to load natives library \"" + libraryName + "\". Attempting fallback method...");
            t.printStackTrace();
            String nativesPath = Paths.get(GlobalData.rootPath(), "natives").toString();
            Configuration.SHARED_LIBRARY_EXTRACT_PATH.set(nativesPath);
            System.setProperty("com.codedisaster.steamworks.SharedLibraryExtractPath", nativesPath);
            this.loadLibrary(libraryName, true);
         } else if (debugStream != null) {
            throw new RuntimeException(
               "Failed to load Steam library: " + libraryName + " \nLWJGL debug output:\n" + debugStream.toString(StandardCharsets.UTF_8.name()), t
            );
         } else {
            throw new RuntimeException("Failed to load Steam library: " + libraryName, t);
         }
      }
   }

   private Throwable getThrowableCause(Throwable throwable, Predicate<Throwable> test) {
      if (test.test(throwable)) {
         return throwable;
      }

      Throwable cause = throwable.getCause();
      return cause != null && cause != throwable ? this.getThrowableCause(cause, test) : null;
   }
}
