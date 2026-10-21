package necesse.engine.save;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.zip.DataFormatException;
import necesse.engine.gameLoop.tickManager.Performance;
import necesse.engine.gameLoop.tickManager.PerformanceTimerManager;
import necesse.engine.gameLoop.tickManager.PerformanceWrapper;
import necesse.engine.util.GameUtils;
import necesse.engine.world.WorldFile;

public class SaveComponent {
   public static final int TYPE_UNKNOWN = 0;
   public static final int TYPE_DATA = 1;
   public static final int TYPE_ARRAY = 2;
   public static final char[][] escapeUnsafeCharacters = new char[][]{
      {'{', '{'},
      {'}', '}'},
      {'[', '['},
      {']', ']'},
      {'"', '"'},
      {'\'', '\''},
      {'/', '/'},
      {',', ','},
      {'=', '='},
      {'\n', 'n'},
      {'\r', 'r'},
      {'\t', 't'},
      {'\b', 'b'},
      {'\\', '\\'}
   };
   private PerformanceTimerManager performanceManager;
   private static final char[][] dataSplitters = new char[][]{{'[', ']'}, {'"', '"'}, {'\'', '\''}};
   private String name;
   private String data;
   private final String comment;
   private List<SaveComponent> components;
   private int type;
   private boolean isCompiled;
   private String script;

   public static String toSafeData(String data) {
      for (int i = escapeUnsafeCharacters.length - 1; i >= 0; i--) {
         data = data.replace(Character.toString(escapeUnsafeCharacters[i][0]), "\\" + escapeUnsafeCharacters[i][1]);
      }

      return data;
   }

   public static String fromSafeData(String data) {
      for (int i = 0; i < data.length(); i++) {
         char c = data.charAt(i);
         if (c == '\\' && i < data.length() - 1) {
            char nextC = data.charAt(i + 1);
            char replace = 0;

            for (char[] parser : escapeUnsafeCharacters) {
               if (parser[1] == nextC) {
                  replace = parser[0];
                  break;
               }
            }

            if (replace != 0) {
               data = data.substring(0, i) + replace + data.substring(i + 2);
            } else {
               i++;
            }
         }
      }

      return data;
   }

   public SaveComponent(String name) {
      this.name = name;
      this.components = new ArrayList<>();
      this.comment = "";
      this.type = 2;
      this.isCompiled = true;
   }

   public SaveComponent(String name, String comment) {
      this.name = name;
      this.components = new ArrayList<>();
      this.comment = comment;
      this.type = 2;
      this.isCompiled = true;
   }

   public SaveComponent(String name, Object data, String comment) {
      this.name = name;
      this.data = data.toString();
      this.comment = comment;
      this.type = 1;
      this.isCompiled = true;
   }

   public SaveComponent(PerformanceTimerManager performanceManager, String script, boolean compileThis, boolean compileAll) {
      this.performanceManager = performanceManager;
      this.script = script;
      this.isCompiled = false;
      this.comment = "";
      this.type = 0;
      if (compileThis) {
         this.compile(compileAll);
      }
   }

   public String getName() {
      if (!this.isCompiled) {
         this.compile(false);
      }

      return this.name;
   }

   public String getData() {
      if (!this.isCompiled) {
         this.compile(false);
      }

      return this.data;
   }

   public int getType() {
      if (!this.isCompiled) {
         this.compile(false);
      }

      return this.type;
   }

   public List<SaveComponent> getComponents() {
      if (!this.isCompiled) {
         this.compile(false);
      }

      return this.components;
   }

   public int getSize() {
      return this.type == 2 ? this.components.size() : 1;
   }

   public boolean isEmpty() {
      return this.type == 2 && this.components.size() == 0;
   }

   public void addData(String name, Object data, String comment) {
      this.components.add(new SaveComponent(name, data, comment));
   }

   public void addData(String name, Object data) {
      this.addData(name, data, "");
   }

   public void addData(String name, Object data, String comment, int index) {
      this.components.add(index, new SaveComponent(name, data, comment));
   }

   public void addData(String name, Object data, int index) {
      this.addData(name, data, "", index);
   }

   public void addComponent(SaveComponent component) {
      this.components.add(component);
   }

   public List<SaveComponent> getComponentsByName(String name) {
      if (!this.isCompiled) {
         this.compile(false);
      }

      return this.getComponents().stream().filter(c -> c.getName().equals(name)).collect(Collectors.toList());
   }

   public SaveComponent getFirstComponentByName(String name) {
      if (!this.isCompiled) {
         this.compile(false);
      }

      return this.getComponents().stream().filter(c -> c.getName().equals(name)).findFirst().orElse(null);
   }

   public String getFirstDataByName(String name) {
      return this.getFirstComponentByName(name).getData();
   }

   public boolean hasComponentByName(String name) {
      return this.getFirstComponentByName(name) != null;
   }

   public void removeComponentsByName(String name) {
      if (!this.isCompiled) {
         this.compile(false);
      }

      for (int i = 0; i < this.components.size(); i++) {
         if (this.components.get(i).getName().equals(name)) {
            this.components.remove(i);
            i--;
         }
      }
   }

   public boolean removeFirstComponentByName(String name) {
      if (!this.isCompiled) {
         this.compile(false);
      }

      for (int i = 0; i < this.components.size(); i++) {
         if (this.components.get(i).getName().equals(name)) {
            this.components.remove(i);
            return true;
         }
      }

      return false;
   }

   public void clearComponents() {
      if (!this.isCompiled) {
         this.compile(false);
      }

      this.components.clear();
   }

   private String getScript(String prefix, String suffix, boolean compressed, Set<SaveComponent> blackList) {
      if (blackList.contains(this)) {
         throw new IllegalArgumentException("Script recursive");
      }

      blackList.add(this);
      String newLine = compressed ? "" : "\n";
      String tab = compressed ? "" : "\t";
      String equals = compressed ? "=" : " = ";
      String comment = compressed ? "" : (this.comment.isEmpty() ? "" : " // " + this.comment);
      String name = this.getName();
      if (!this.isCompiled) {
         return prefix + this.script + suffix + newLine;
      }

      StringBuilder script = new StringBuilder();
      if (this.type == 1) {
         String data = this.getData();
         if (name.equals("")) {
            script.append(prefix).append(data).append(suffix).append(compressed ? "" : comment).append(newLine);
         } else {
            script.append(prefix).append(name).append(equals).append(data).append(suffix).append(compressed ? "" : comment).append(newLine);
         }
      } else {
         script.append(prefix).append(name).append(name.equals("") ? "" : equals).append("{").append(comment).append(newLine);

         for (int i = 0; i < this.components.size(); i++) {
            if (this.components.get(i).getType() == 2) {
               script.append(this.components.get(i).getScript(prefix + tab, suffix, compressed, blackList));
               if (i == this.components.size() - 1) {
                  script = new StringBuilder(script.substring(0, script.lastIndexOf(",")) + script.substring(script.lastIndexOf(",") + 1, script.length()));
               }
            } else {
               script.append(this.components.get(i).getScript(prefix + tab, i == this.components.size() - 1 ? "" : suffix, compressed, blackList));
            }
         }

         script.append(prefix).append("}").append(suffix).append(newLine);
      }

      return script.toString();
   }

   public String getScript(boolean compressed) {
      String script = this.getScript("", ",", compressed, new HashSet<>());
      int endComma = script.lastIndexOf(",");
      if (endComma != -1) {
         script = script.substring(0, endComma);
      }

      return script;
   }

   public String getScript() {
      return this.getScript(false);
   }

   public void printScript() {
      System.out.println(this.getScript());
   }

   public void printDebug() {
      this.printDebug("");
   }

   private void printDebug(String prefix) {
      switch (this.getType()) {
         case 1:
            System.out.println(prefix + "[DATA] \"" + this.getName() + "\" = \"" + this.getData() + "\"");
            break;
         case 2:
            System.out.println(prefix + "[ARRAY] \"" + this.getName() + "\" = {");

            for (SaveComponent child : this.getComponents()) {
               child.printDebug(prefix + "\t");
            }

            System.out.println(prefix + "}");
            break;
         default:
            System.out.println(prefix + "[UNKNOWN] \"" + this.getName() + "\" = \"" + this.getData() + "\"");
      }
   }

   public static void saveScriptRaw(WorldFile file, SaveComponent save, boolean compressed) throws IOException {
      byte[] data = GameUtils.getStringBytes(save.getScript(compressed));
      if (compressed) {
         data = GameUtils.compressData(data);
      }

      file.write(data);
   }

   public static void saveScript(WorldFile file, SaveComponent save, boolean compressed) {
      try {
         saveScriptRaw(file, save, compressed);
      } catch (IllegalArgumentException e) {
         System.err.println("Could not create folder to save script: " + file.toString());
         e.printStackTrace();
      } catch (IOException e) {
         e.printStackTrace();
      }
   }

   public static void saveScript(WorldFile file, SaveComponent save) {
      saveScript(file, save, false);
   }

   public void saveScript(WorldFile file) {
      saveScript(file, this);
   }

   public static SaveComponent loadScriptRaw(WorldFile file, boolean isCompressed) throws IOException, DataFormatException {
      return loadScriptRaw(null, file, isCompressed);
   }

   public static SaveComponent loadScriptRaw(PerformanceTimerManager performanceManager, WorldFile file, boolean isCompressed) throws IOException, DataFormatException {
      try {
         byte[] data = file.read();
         if (isCompressed) {
            data = GameUtils.decompressData(data);
         }

         return loadScript(performanceManager, GameUtils.getStringFromBytes(data));
      } catch (IOException | DataFormatException e) {
         throw e;
      } catch (Exception e) {
         throw new RuntimeException("Could not load file " + file.getFileName(), e);
      }
   }

   public static SaveComponent loadScript(WorldFile file, boolean isCompressed) {
      return loadScript(null, file, isCompressed);
   }

   public static SaveComponent loadScript(PerformanceTimerManager performanceManager, WorldFile file, boolean isCompressed) {
      try {
         return loadScriptRaw(performanceManager, file, isCompressed);
      } catch (IOException e) {
         e.printStackTrace();
      } catch (DataFormatException e) {
         System.err.println("Could not decompress script file: " + file.toString());
         e.printStackTrace();
      }

      return null;
   }

   public static SaveComponent loadScript(WorldFile file) {
      return loadScript(null, file);
   }

   public static SaveComponent loadScript(PerformanceTimerManager performanceManager, WorldFile file) {
      return loadScript(performanceManager, file, false);
   }

   public static void saveScriptRaw(File file, SaveComponent save, boolean compressed) throws IOException {
      byte[] data = GameUtils.getStringBytes(save.getScript(compressed));
      if (compressed) {
         data = GameUtils.compressData(data);
      }

      GameUtils.saveByteFile(data, file);
   }

   public static void saveScript(File file, SaveComponent save, boolean compressed) {
      try {
         saveScriptRaw(file, save, compressed);
      } catch (IllegalArgumentException e) {
         System.err.println("Could not create folder to save script: " + file.getPath());
         e.printStackTrace();
      } catch (IOException e) {
         e.printStackTrace();
      }
   }

   public static void saveScript(File file, SaveComponent save) {
      saveScript(file, save, false);
   }

   public void saveScript(File file) {
      saveScript(file, this);
   }

   public static SaveComponent loadScriptRaw(File file, boolean isCompressed) throws IOException, DataFormatException {
      return loadScriptRaw(null, file, isCompressed);
   }

   public static SaveComponent loadScriptRaw(PerformanceTimerManager performanceManager, File file, boolean isCompressed) throws IOException, DataFormatException {
      try {
         byte[] data = GameUtils.loadByteFile(file);
         if (isCompressed) {
            data = GameUtils.decompressData(data);
         }

         return loadScript(performanceManager, GameUtils.getStringFromBytes(data));
      } catch (IOException | DataFormatException e) {
         throw e;
      } catch (Exception e) {
         throw new RuntimeException("Could not load file " + file.getName(), e);
      }
   }

   public static SaveComponent loadScript(File file, boolean isCompressed) {
      return loadScript(null, file, isCompressed);
   }

   public static SaveComponent loadScript(PerformanceTimerManager performanceManager, File file, boolean isCompressed) {
      try {
         return loadScriptRaw(performanceManager, file, isCompressed);
      } catch (IOException e) {
         e.printStackTrace();
      } catch (DataFormatException e) {
         System.err.println("Could not decompress script file: " + file.getPath());
         e.printStackTrace();
      }

      return null;
   }

   public static SaveComponent loadScript(File file) {
      return loadScript(null, file);
   }

   public static SaveComponent loadScript(PerformanceTimerManager performanceManager, File file) {
      return loadScript(performanceManager, file, false);
   }

   public static SaveComponent loadScript(String script) {
      return loadScript(null, script);
   }

   public static SaveComponent loadScript(PerformanceTimerManager performanceManager, String script) {
      try {
         return loadScript(performanceManager, new BufferedReader(new StringReader(script)));
      } catch (IOException e) {
         e.printStackTrace();
         return null;
      }
   }

   private static SaveComponent loadScript(PerformanceTimerManager performanceManager, BufferedReader br) throws IOException {
      StringBuilder src = new StringBuilder();

      String line;
      while ((line = br.readLine()) != null) {
         int comment = line.indexOf("//");
         if (comment != -1) {
            line = line.substring(0, comment);
         }

         line = line.trim();
         if (line.length() != 0) {
            src.append(line);
            if (line.charAt(line.length() - 1) != ',') {
               src.append(",");
            }
         }
      }

      br.close();
      return new SaveComponent(performanceManager, src.toString(), true, false);
   }

   public static int getSectionStop(String script, char startChar, char endChar, int startIndex) {
      int startAt = indexOf(script, startChar, startIndex);
      if (startAt == -1) {
         throw new SaveSyntaxException("SYNTAX ERROR: Missing section start \"" + startChar + "\" at " + script);
      }

      int open = 1;

      while (true) {
         int endCharIndex = indexOf(script, endChar, startAt + 1);
         if (endCharIndex == -1) {
            throw new SaveSyntaxException("SYNTAX ERROR: Missing section stop \"" + endChar + "\" at " + script);
         }

         int startCharIndex = indexOf(script, startChar, startAt + 1);
         if (startCharIndex != -1 && startCharIndex <= endCharIndex) {
            open++;
            startAt = startCharIndex;
         } else {
            if (--open == 0) {
               return endCharIndex;
            }

            startAt = endCharIndex;
         }
      }
   }

   public static int indexOf(String script, char character, int startIndex) {
      int index = script.indexOf(character, startIndex);
      if (index >= 0) {
         return isCharEscaped(script, index) ? indexOf(script, character, index + 1) : index;
      } else {
         return -1;
      }
   }

   private static boolean isCharEscaped(String script, int charIndex) {
      boolean escaped = false;

      while (charIndex > 0 && script.charAt(charIndex - 1) == '\\') {
         escaped = !escaped;
         charIndex--;
      }

      return escaped;
   }

   private static int indexOfValid(String script, char character, int startIndex) {
      int index = script.indexOf(character, startIndex);
      if (index >= 0) {
         return isCharEscaped(script, index) ? indexOfValid(script, character, index + 1) : index;
      } else {
         throw new SaveSyntaxException("SYNTAX ERROR: Missing valid data section \"" + character + "\" character at " + script);
      }
   }

   private static int getDataSectionStop(String script, char startChar, char endChar, int startIndex) {
      int start = indexOfValid(script, startChar, startIndex);
      if (start == -1) {
         throw new SaveSyntaxException("SYNTAX ERROR: Missing start data section \"" + startChar + "\" character at " + script);
      } else {
         int end = indexOfValid(script, endChar, start + 1);
         if (end == -1) {
            throw new SaveSyntaxException("SYNTAX ERROR: Missing end data section \"" + endChar + "\" character at " + script);
         } else {
            return end + 1;
         }
      }
   }

   private void compile(boolean compileAll) {
      if (!this.isCompiled) {
         PerformanceWrapper typeTimer = Performance.wrapTimer(this.performanceManager, "getType");
         int arrayStart = indexOf(this.script, '{', 0);
         int contentEnd = indexOf(this.script, ',', 0);
         typeTimer.end();
         if (arrayStart != -1 && (contentEnd == -1 || contentEnd >= arrayStart)) {
            PerformanceWrapper arrayTimer = Performance.wrapTimer(this.performanceManager, "parseArrayType");
            this.type = 2;
            this.components = new ArrayList<>();
            PerformanceWrapper contentDefineTimer = Performance.wrapTimer(this.performanceManager, "contentDefine");
            int mid = indexOf(this.script, '=', 0);
            int arrayStop = getSectionStop(this.script, '{', '}', 0);
            String content = this.script.substring(arrayStart + 1, arrayStop);
            if (mid != -1 && mid < arrayStart) {
               this.name = this.script.substring(0, mid).trim();
            } else {
               this.name = this.script.substring(0, arrayStart).trim();
            }

            contentDefineTimer.end();
            int contentStartIndex = 0;

            while (true) {
               PerformanceWrapper arrayStartTimer = Performance.wrapTimer(this.performanceManager, "arrayStart");
               arrayStart = indexOf(content, '{', contentStartIndex);
               contentEnd = indexOf(content, ',', contentStartIndex);
               arrayStartTimer.end();
               if (arrayStart != -1 && contentEnd > arrayStart) {
                  PerformanceWrapper contentEndTimer = Performance.wrapTimer(this.performanceManager, "contentEnd");
                  contentEnd = indexOf(content, ',', getSectionStop(content, '{', '}', contentStartIndex));
                  contentEndTimer.end();
               } else {
                  int dataStart = -1;
                  int dataEnd = -1;
                  char dataStartChar = '[';
                  char dataEndChar = ']';
                  PerformanceWrapper dataSplittersTimer = Performance.wrapTimer(this.performanceManager, "dataSplitters");

                  for (char[] dataSplitter : dataSplitters) {
                     int nextDataStart = indexOf(content, dataSplitter[0], contentStartIndex);
                     int nextDataEnd = indexOf(content, dataSplitter[1], contentStartIndex);
                     if (dataStart == -1 || nextDataStart != -1 && nextDataEnd != -1 && nextDataStart < dataStart) {
                        dataStart = nextDataStart;
                        dataEnd = nextDataEnd;
                        dataStartChar = dataSplitter[0];
                        dataEndChar = dataSplitter[1];
                     }
                  }

                  dataSplittersTimer.end();
                  if (dataStart != -1 && dataEnd != -1 && contentEnd > dataStart) {
                     PerformanceWrapper contentEndTimer = Performance.wrapTimer(this.performanceManager, "contentEnd");
                     contentEnd = indexOf(content, ',', getDataSectionStop(content, dataStartChar, dataEndChar, contentStartIndex));
                     contentEndTimer.end();
                  }
               }

               PerformanceWrapper trimEndTimer = Performance.wrapTimer(this.performanceManager, "trimEnd1");
               if (contentEnd == -1) {
                  contentEnd = content.length();
               }

               String data = content.substring(contentStartIndex, contentEnd).trim();
               trimEndTimer.end();
               if (data.length() != 0) {
                  arrayTimer.end();
                  this.components.add(new SaveComponent(this.performanceManager, data, compileAll, compileAll));
                  arrayTimer = Performance.wrapTimer(this.performanceManager, "parseArrayType");
               }

               if (contentEnd == content.length()) {
                  arrayTimer.end();
                  break;
               }

               PerformanceWrapper trimEndTimer2 = Performance.wrapTimer(this.performanceManager, "trimEnd2");
               contentStartIndex = contentEnd + 1;
               trimEndTimer2.end();
               PerformanceWrapper trimEndTimer3 = Performance.wrapTimer(this.performanceManager, "trimEnd3");
               int length = content.length();

               while (contentStartIndex < length && content.charAt(contentStartIndex) <= ' ') {
                  contentStartIndex++;
               }

               trimEndTimer3.end();
            }
         } else {
            Performance.record(this.performanceManager, "parseDataType", () -> {
               this.type = 1;
               this.components = new ArrayList<>();
               int midx = indexOf(this.script, '=', 0);
               boolean trimEnd = this.script.endsWith(",") && !isCharEscaped(this.script, this.script.length() - 1);
               if (midx == -1) {
                  this.name = "";
                  this.data = this.script.substring(0, this.script.length() - (trimEnd ? 1 : 0));
               } else {
                  this.name = this.script.substring(0, midx).trim();
                  this.data = this.script.substring(midx + 1, this.script.length() - (trimEnd ? 1 : 0)).trim();
               }
            });
         }

         this.isCompiled = true;
         this.script = null;
      }
   }
}
