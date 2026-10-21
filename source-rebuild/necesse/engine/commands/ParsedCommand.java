package necesse.engine.commands;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ParsedCommand {
   private static final Pattern argsPattern = Pattern.compile("[^\\s\"']+|\"([^\"]*)\"|'([^']*)'");
   public final String fullCommand;
   public final String commandName;
   public final String[] args;

   public ParsedCommand(String fullCommand) {
      this.fullCommand = fullCommand;
      int nameSplit = fullCommand.indexOf(32);
      if (nameSplit == -1) {
         this.commandName = fullCommand;
         this.args = new String[0];
      } else {
         this.commandName = fullCommand.substring(0, nameSplit);
         String argsString = fullCommand.substring(nameSplit + 1);
         this.args = parseArgs(argsString);
      }
   }

   public static String[] parseArgs(String argsString) {
      if (argsString.isEmpty()) {
         return new String[]{""};
      }

      ArrayList<String> args = new ArrayList<>();
      Matcher matcher = argsPattern.matcher(argsString);

      while (matcher.find()) {
         if (matcher.group(1) != null) {
            args.add(matcher.group(1));
         } else if (matcher.group(2) != null) {
            args.add(matcher.group(2));
         } else {
            args.add(matcher.group());
         }
      }

      if (argsString.endsWith(" ")) {
         args.add("");
      }

      return args.toArray(new String[0]);
   }

   public static String wrapArgument(String arg) {
      char wrapper = 0;
      if (arg.contains(" ")) {
         if (arg.contains("\"")) {
            wrapper = '\'';
         } else {
            wrapper = '"';
         }
      } else if (arg.contains("\"")) {
         wrapper = '\'';
      } else if (arg.contains("'")) {
         wrapper = '"';
      }

      return wrapper != 0 ? wrapper + arg + wrapper : arg;
   }
}
