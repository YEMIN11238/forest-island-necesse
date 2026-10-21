package necesse.engine.world;

import java.util.List;

public class RecentJVMECrashException extends RuntimeException {
   public RecentJVMECrashException(List<String> hs_err_log) {
      super(getMessage(hs_err_log));
   }

   private static String getMessage(List<String> hs_err_log) {
      return "\n\tA recent crash was detected at startup:\n\n"
         + getErrorType(hs_err_log, "\t")
         + "\n\tProblematic frame: "
         + getProblematicFrame(hs_err_log)
         + "\n\tCurrent thread: "
         + getCurrentThread(hs_err_log)
         + "\n\n\tJava frames:\n"
         + getJavaFrames(hs_err_log, "\t")
         + "\n\tFull log file: \n\t\t"
         + String.join("\n\t\t", hs_err_log)
         + "\n";
   }

   private static String getErrorType(List<String> hs_err_log, String prefix) {
      boolean foundStart = false;
      StringBuilder out = new StringBuilder();

      for (int i = 0; i < hs_err_log.size(); i++) {
         String line = hs_err_log.get(i);
         if (!foundStart && line.startsWith("# A fatal error has been detected by the Java Runtime Environment:")) {
            foundStart = true;
            i++;
         } else if (foundStart) {
            if (!line.contains("#")) {
               break;
            }

            if (line.replace("#", "").trim().isEmpty()) {
               return out.toString();
            }

            out.append(prefix).append(line.replace("#", "").trim()).append("\n");
         }
      }

      return prefix + "(Error parsing log file, could not find error type)\n";
   }

   private static String getProblematicFrame(List<String> hs_err_log) {
      boolean foundStart = false;
      StringBuilder out = new StringBuilder();

      for (String line : hs_err_log) {
         if (!foundStart && line.startsWith("# Problematic frame:")) {
            foundStart = true;
         } else if (foundStart) {
            if (line.contains("#")) {
               out.append("\t").append(line.replace("#", "").trim()).append("\n");
               return out.toString();
            }
            break;
         }
      }

      return "(Unknown frame)";
   }

   private static String getCurrentThread(List<String> hs_err_log) {
      for (String line : hs_err_log) {
         int indexOfCurrentThread = line.indexOf("Current thread");
         if (indexOfCurrentThread != -1) {
            return line.substring(indexOfCurrentThread + "Current thread".length()).trim();
         }
      }

      return "(Error parsing log file, could not find current thread)";
   }

   private static String getJavaFrames(List<String> hs_err_log, String prefix) {
      boolean foundStart = false;
      StringBuilder out = new StringBuilder();

      for (String line : hs_err_log) {
         if (!foundStart && line.startsWith("Java frames:")) {
            foundStart = true;
         } else if (foundStart) {
            if (!line.startsWith("j ") && !line.startsWith("J ") && !line.startsWith("V ") && !line.startsWith("v ") && !line.startsWith("C ")) {
               return out.toString();
            }

            out.append(prefix).append(line.trim()).append("\n");
         }
      }

      return prefix + "(Could not find java frames)\n";
   }
}
