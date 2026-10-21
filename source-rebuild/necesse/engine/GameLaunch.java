package necesse.engine;

import java.awt.Dimension;
import java.awt.Point;
import java.util.HashMap;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import necesse.engine.modLoader.DevModProvider;
import necesse.engine.modLoader.ModLoader;
import necesse.engine.platforms.Platform;
import necesse.engine.platforms.PlatformManager;
import necesse.engine.util.GameUtils;
import necesse.engine.world.WorldSettings;
import necesse.gfx.gameTexture.GameTexture;

public class GameLaunch {
   public static HashMap<String, String> launchOptions;
   public static String[] fullArgs = null;
   public static String fullLaunchParameters = null;
   public static boolean instantContinue = false;
   public static String instantHost = null;
   public static String instantLoad = null;
   public static String useCharacter = null;
   public static String instantConnect = null;
   public static String instantLobbyConnect = null;
   public static int launchMonitor = -1;
   public static Point launchWindowPosition = null;
   private static Dimension launchWindowSize = null;
   public static String chatMessage = null;
   public static int chatMessageDelaySeconds = 5;
   public static boolean runPeriodicGCRounds = false;
   private static final Pattern argsPattern = Pattern.compile("[^\\s\"']+|\"([^\"]*)\"|'([^']*)'");

   public static Dimension getLaunchWindowSize(int fullWindowWidth, int fullWindowHeight) {
      if (PlatformManager.getPlatform().getOperatingSystemFamily() == Platform.OperatingSystemFamily.Windows) {
         fullWindowHeight -= 50;
      }

      if (launchWindowSize == null) {
         launchWindowSize = new Dimension(fullWindowWidth / 2, fullWindowHeight / 2);
      } else {
         if (launchWindowSize.width < 0) {
            launchWindowSize.width = fullWindowWidth / Math.abs(launchWindowSize.width);
         }

         if (launchWindowSize.height < 0) {
            launchWindowSize.height = fullWindowHeight / Math.abs(launchWindowSize.height);
         }
      }

      return launchWindowSize;
   }

   public static String[] quoteArgs(String[] args) {
      String[] quoted = new String[args.length];

      for (int i = 0; i < args.length; i++) {
         String arg = args[i];
         if (!arg.contains(" ") || arg.startsWith("\"") && arg.endsWith("\"")) {
            quoted[i] = arg;
         } else {
            quoted[i] = "\"" + arg + "\"";
         }
      }

      return quoted;
   }

   public static HashMap<String, String> parseLaunchOptions(String[] args) {
      args = quoteArgs(args);
      String full = GameUtils.join(args, " ");
      int currentIndex = 0;
      HashMap<String, String> out = new HashMap<>();

      while (currentIndex < full.length()) {
         char current = full.charAt(currentIndex);
         if (current != '-' && current != '+') {
            int nextOption = Math.max(full.indexOf("-", currentIndex), full.indexOf("+", currentIndex));
            if (nextOption == -1) {
               break;
            }

            currentIndex = nextOption;
         } else {
            int nextSpace = full.indexOf(" ", currentIndex);
            if (nextSpace != -1) {
               String currentOption = full.substring(currentIndex + 1, nextSpace);
               currentIndex = nextSpace + 1;
               String arg = null;
               Matcher matcher = argsPattern.matcher(full.substring(currentIndex));
               if (matcher.find()) {
                  if (matcher.group(1) != null) {
                     arg = matcher.group(1);
                  } else if (matcher.group(2) != null) {
                     arg = matcher.group(2);
                  } else {
                     arg = matcher.group();
                  }
               }

               if (arg != null) {
                  if (!arg.startsWith("-") && !arg.startsWith("+")) {
                     out.put(currentOption, arg);
                     continue;
                  }

                  out.put(currentOption, "");
                  continue;
               }

               out.put(currentOption, "");
               break;
            }

            String currentOption = full.substring(currentIndex + 1);
            out.put(currentOption, "");
            break;
         }
      }

      return out;
   }

   public static HashMap<String, String> parseAndHandleLaunchOptions(String[] args) {
      fullArgs = args;
      HashMap<String, String> options = parseLaunchOptions(args);
      fullLaunchParameters = GameUtils.join(quoteArgs(args), " ");
      if (args.length > 0) {
         System.out.println("Launched game with arguments: " + fullLaunchParameters);
      }

      if (options.containsKey("dev")) {
         GlobalData.setDevMode();
         String authString = options.get("dev");
         if (!authString.isEmpty()) {
            try {
               long authentication = Long.parseLong(authString);
               if (authentication > 0L && authentication <= 500L) {
                  GameAuth.setTempAuth(authentication);
               } else {
                  System.err.println("Invalid authentication number: " + authentication);
                  System.exit(0);
               }
            } catch (NumberFormatException e) {
               System.err.println("Authentication argument must be a number.");
               System.exit(0);
            }
         }
      }

      if (options.containsKey("hiddencheats")) {
         WorldSettings.cheatsHidden = true;
      }

      if (options.containsKey("lowmemory")) {
         GlobalData.setLowMemoryMode();
      }

      if (options.containsKey("memorydebug")) {
         GameTexture.memoryDebug = true;
      }

      if (options.containsKey("mod")) {
         DevModProvider.devMod = options.get("mod");
      }

      if (options.containsKey("disablemods")) {
         ModLoader.disableMods = true;
      }

      if (options.containsKey("continue")) {
         instantContinue = true;
      }

      if (options.containsKey("load")) {
         instantLoad = options.get("load");
      }

      if (options.containsKey("host")) {
         instantHost = options.get("host");
      }

      if (options.containsKey("character")) {
         useCharacter = options.get("character");
      }

      if (options.containsKey("connect")) {
         instantConnect = options.get("connect");
      }

      if (options.containsKey("connect_lobby")) {
         instantLobbyConnect = options.get("connect_lobby");
      }

      if (options.containsKey("monitor")) {
         String param = options.get("monitor");
         if (!param.isEmpty()) {
            try {
               launchMonitor = Integer.parseInt(param);
               if (launchMonitor < 0) {
                  launchMonitor = -1;
                  throw new NumberFormatException("Monitor cannot be negative");
               }
            } catch (NumberFormatException e) {
               GameLog.warn.println("Monitor launch parameter is not a positive number: \"" + param + "\"");
            }
         } else {
            GameLog.warn.println("Monitor launch parameter cannot be empty");
         }
      }

      if (options.containsKey("windowpos")) {
         String windowPosParam = options.get("windowpos").toLowerCase(Locale.ENGLISH);
         if (!windowPosParam.isEmpty()) {
            String[] split = windowPosParam.split(",");
            if (split.length == 2) {
               try {
                  int x = Integer.parseInt(split[0].trim());
                  int y = Integer.parseInt(split[1].trim());
                  if (x < 0 || y < 0) {
                     throw new NumberFormatException("Window position cannot be negative");
                  }

                  launchWindowPosition = new Point(x, y);
               } catch (NumberFormatException e) {
                  GameLog.warn.println("Window position launch parameter is not valid: \"" + windowPosParam + "\"");
               }
            } else {
               GameLog.warn.println("Window position launch parameter is not valid: \"" + windowPosParam + "\"");
            }
         }

         if (options.containsKey("windowsize")) {
            String windowSizeParam = options.get("windowsize").toLowerCase(Locale.ENGLISH);
            if (!windowSizeParam.isEmpty()) {
               String[] split = windowSizeParam.split(",");
               if (split.length == 2) {
                  try {
                     int width = Integer.parseInt(split[0].trim());
                     int height = Integer.parseInt(split[1].trim());
                     launchWindowSize = new Dimension(width, height);
                  } catch (NumberFormatException e) {
                     GameLog.warn.println("Window size launch parameter is not valid: \"" + windowSizeParam + "\"");
                  }
               } else {
                  GameLog.warn.println("Window size launch parameter is not valid: \"" + windowSizeParam + "\"");
               }
            }
         }
      }

      chatMessage = options.getOrDefault("chatcommand", null);
      if (chatMessage != null && chatMessage.isEmpty()) {
         chatMessage = null;
      }

      if (options.containsKey("chatcommanddelay")) {
         String delayParam = options.get("chatcommanddelay");
         if (!delayParam.isEmpty()) {
            try {
               int delay = Integer.parseInt(delayParam);
               if (delay >= 0) {
                  chatMessageDelaySeconds = delay;
               } else {
                  GameLog.warn.println("Chat command delay cannot be negative");
               }
            } catch (NumberFormatException e) {
               GameLog.warn.println("Chat command delay launch parameter is not a positive number: \"" + delayParam + "\"");
            }
         } else {
            GameLog.warn.println("Chat command delay launch parameter cannot be empty");
         }
      }

      runPeriodicGCRounds = options.containsKey("runperiodicgc");
      return options;
   }
}
