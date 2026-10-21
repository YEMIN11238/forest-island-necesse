package necesse.engine.localization.fileLanguage;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map.Entry;
import java.util.function.Consumer;
import java.util.jar.JarEntry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import necesse.engine.GlobalData;
import necesse.engine.localization.AbstractTranslationElement;
import necesse.engine.localization.Localization;
import necesse.engine.localization.TranslationElement;
import necesse.engine.modLoader.LoadedMod;
import necesse.engine.util.GameUtils;
import necesse.gfx.fairType.TypeParsers;
import necesse.gfx.fairType.parsers.TypeItemParser;
import necesse.gfx.gameTooltips.ListGameTooltips;
import necesse.gfx.gameTooltips.SpacerGameTooltip;

public class Translation {
   public static String MISSING_TRANSLATION_PREPEND = "MISSING_TRANSLATION:";
   public static String SAME_TRANSLATION_PREPEND = "SAME_TRANSLATION:";
   public final String fileName;
   private final Translation compareCoverage;
   private int totalUniqueTranslations;
   private final HashMap<String, LinkedList<Translation.LineData>> linesBeforeCategory = new HashMap<>();
   private final HashMap<String, LinkedList<Translation.LineData>> linesBeforeTranslation = new HashMap<>();
   private final HashMap<String, LinkedList<Translation.LineData>> linesAfterCategory = new HashMap<>();
   private final HashMap<String, LinkedList<Translation.LineData>> linesAfterTranslation = new HashMap<>();
   private final ArrayList<Translation.LineData> lines = new ArrayList<>();
   private HashMap<String, TranslationCategory> categories;

   public Translation(String fileName, Translation compareCoverage) {
      this.fileName = fileName;
      this.compareCoverage = compareCoverage;
      this.loadLanguageFile();
   }

   public Translation(String fileName) {
      this(fileName, null);
   }

   public void loadLanguageFile() {
      this.categories = new HashMap<>();

      try {
         File file = new File(GlobalData.rootPath() + "locale/" + "/" + this.fileName);
         InputStreamReader isr = new InputStreamReader(Files.newInputStream(file.toPath()), GlobalData.charset);
         BufferedReader br = new BufferedReader(isr);
         this.loadLanguageFile(br, null);
         br.close();
      } catch (IOException e) {
         e.printStackTrace();
      }
   }

   public void loadModLanguageFile(LoadedMod mod) {
      try {
         Enumeration<JarEntry> entries = mod.jarFile.entries();

         while (entries.hasMoreElements()) {
            JarEntry entry = entries.nextElement();
            String path = entry.getName();
            if (path.startsWith("resources/") && path.endsWith(this.fileName)) {
               InputStreamReader isr = new InputStreamReader(mod.jarFile.getInputStream(entry), GlobalData.charset);
               BufferedReader br = new BufferedReader(isr);
               this.loadLanguageFile(br, mod);
               br.close();
               break;
            }
         }
      } catch (IOException e) {
         System.err.println("Could not load mod " + mod.id + " language file " + this.fileName);
         e.printStackTrace();
      }
   }

   private void loadLanguageFile(BufferedReader br, LoadedMod mod) throws IOException {
      TranslationCategory currentCategory = this.categories.get("null");
      if (currentCategory == null) {
         currentCategory = new TranslationCategory("null");
      }

      LinkedList<Translation.LineData> miscLines = new LinkedList<>();
      boolean setCategoryLastLine = false;
      Translation.SetTranslationLineData lastTranslation = null;

      String line;
      while ((line = br.readLine()) != null) {
         if (line.length() == 0) {
            if (setCategoryLastLine) {
               if (!miscLines.isEmpty()) {
                  this.linesAfterCategory.put(currentCategory.name, miscLines);
               }

               miscLines = new LinkedList<>();
            } else if (lastTranslation != null) {
               if (!miscLines.isEmpty()) {
                  this.linesAfterTranslation.put(lastTranslation.category + "." + lastTranslation.key, miscLines);
               }

               miscLines = new LinkedList<>();
            }

            miscLines.add(new Translation.LineData(Translation.LineType.EMPTY, line));
            this.lines.add(new Translation.LineData(Translation.LineType.EMPTY, line));
            setCategoryLastLine = false;
            lastTranslation = null;
         } else if (line.startsWith("//")) {
            miscLines.add(new Translation.LineData(Translation.LineType.COMMENT, line));
            this.lines.add(new Translation.LineData(Translation.LineType.COMMENT, line));
         } else {
            if (line.startsWith("[")) {
               int endCat = line.indexOf("]");
               if (endCat != -1) {
                  if (currentCategory.getTotalTranslations() > 0) {
                     this.categories.put(currentCategory.name, currentCategory);
                  }

                  String newCategoryName = line.substring(1, endCat);
                  if (this.categories.containsKey(newCategoryName)) {
                     currentCategory = this.categories.get(newCategoryName);
                  } else {
                     currentCategory = new TranslationCategory(newCategoryName);
                  }

                  if (!miscLines.isEmpty()) {
                     this.linesBeforeCategory.put(newCategoryName, miscLines);
                  }

                  miscLines = new LinkedList<>();
                  this.lines.add(new Translation.SetCategoryLineData(line, newCategoryName));
                  lastTranslation = null;
                  setCategoryLastLine = true;
                  continue;
               }
            }

            int equalIndex = line.indexOf(61);
            if (equalIndex == -1) {
               this.lines.add(new Translation.LineData(Translation.LineType.UNKNOWN, line));
            } else {
               String key = line.substring(0, equalIndex);
               boolean sameTranslation = false;
               boolean missingTranslation = false;
               if (key.startsWith(MISSING_TRANSLATION_PREPEND)) {
                  key = key.substring(MISSING_TRANSLATION_PREPEND.length());
                  missingTranslation = true;
               } else if (key.startsWith(SAME_TRANSLATION_PREPEND)) {
                  sameTranslation = true;
                  key = key.substring(SAME_TRANSLATION_PREPEND.length());
               }

               if (currentCategory.name.equals("lang")) {
                  sameTranslation = true;
               }

               String translation = line.substring(equalIndex + 1);
               translation = translation.replace("\\n", "\n");
               translation = translation.replace("[newline]", "\n");
               currentCategory.addTranslation(this.fileName, key, translation, sameTranslation, missingTranslation, mod);
               if (this.compareCoverage == null) {
                  this.totalUniqueTranslations++;
               } else {
                  String compareTranslation = this.compareCoverage.getTranslation(currentCategory.name, key).translation;
                  if (compareTranslation != null && (sameTranslation || !compareTranslation.equals(translation))) {
                     this.totalUniqueTranslations++;
                  }
               }

               if (!miscLines.isEmpty()) {
                  this.linesBeforeTranslation.put(currentCategory.name + "." + key, miscLines);
               }

               miscLines = new LinkedList<>();
               this.lines.add(lastTranslation = new Translation.SetTranslationLineData(line, currentCategory.name, key, translation));
               setCategoryLastLine = false;
            }
         }
      }

      if (currentCategory.getTotalTranslations() > 0) {
         this.categories.put(currentCategory.name, currentCategory);
      }
   }

   public TranslationCategory getCategory(String category) {
      return this.categories.get(category);
   }

   public AbstractTranslationElement getTranslation(String category, String key) {
      TranslationCategory cat = this.getCategory(category);
      return cat != null ? cat.getTranslation(key) : AbstractTranslationElement.NULL_TRANSLATION;
   }

   public boolean isMissing(String category, String key) {
      TranslationCategory cat = this.categories.get(category);
      return cat != null ? cat.isMissing(key) : true;
   }

   public boolean exists(String category, String key) {
      TranslationCategory cat = this.categories.get(category);
      return cat != null ? cat.exists(key) : true;
   }

   public boolean isSameAsEnglish(String category, String key) {
      TranslationCategory cat = this.categories.get(category);
      return cat != null ? cat.isSameAsEnglish(key) : false;
   }

   public Stream<Translation.TranslationData> streamTranslations(boolean acceptSameTranslations, boolean acceptMissingTranslations) {
      return this.categories.entrySet().stream().flatMap(c -> {
         Stream<Entry<String, AbstractTranslationElement>> translationStream = c.getValue().streamTranslations();
         if (!acceptSameTranslations) {
            translationStream = translationStream.filter(t -> !((TranslationCategory)c.getValue()).isSameAsEnglish(t.getKey()));
         }

         if (!acceptMissingTranslations) {
            translationStream = translationStream.filter(t -> !((TranslationCategory)c.getValue()).isMissing(t.getKey()));
         }

         return translationStream.map(t -> new Translation.TranslationData((String)c.getKey(), t.getKey(), t.getValue().translation));
      });
   }

   public Stream<Translation.TranslationData> streamStrippedTranslations(boolean acceptSameTranslations, boolean acceptMissingTranslations) {
      return this.streamTranslations(acceptSameTranslations, acceptMissingTranslations)
         .map(
            t -> {
               String stripped = t.translation
                  .replaceAll(TranslationElement.REPLACEMENTS_PATTERN.pattern(), "")
                  .replaceAll("§([!0-9a-z]|#([0-9a-fA-F]{6}|[0-9a-fA-F]{3}))", "")
                  .replace("\\n", " ")
                  .replaceAll(TypeParsers.INPUT_PATTERN.pattern(), " ")
                  .replaceAll(TypeItemParser.ITEM_PATTERN.pattern(), " ")
                  .replaceAll(TypeItemParser.ITEMS_PATTERN.pattern(), " ")
                  .replaceAll("\\s+", " ")
                  .trim();
               return new Translation.TranslationData(t.category, t.key, stripped);
            }
         );
   }

   public void forEachTranslation(Consumer<Translation.TranslationData> action, boolean acceptSameTranslations, boolean acceptMissingTranslations) {
      this.categories.forEach((cName, c) -> c.forEachTranslations((k, t) -> {
         if ((acceptSameTranslations || !c.isSameAsEnglish(k)) && (acceptMissingTranslations || !c.isMissing(k))) {
            action.accept(new Translation.TranslationData(cName, k, t.translation));
         }
      }));
   }

   public void debugReload() {
      this.loadLanguageFile();
   }

   public void printTranslations() {
      System.out.println("Translations in file:");
      System.out.println(this.fileName);
      System.out.println("--------------------------------");

      for (String category : this.categories.keySet()) {
         System.out.println("[" + category + "]");
         this.categories.get(category).printTranslations(" ");
      }
   }

   public void addCoverageTooltips(ListGameTooltips tooltips) {
      if (this.compareCoverage != null) {
         tooltips.add(new SpacerGameTooltip(5));
         double percent = (double)this.totalUniqueTranslations / this.compareCoverage.totalUniqueTranslations;
         int percentDisplay = (int)Math.floor(percent * 100.0);
         tooltips.add(Localization.translate("settingsui", "translationcoverage", "percent", percentDisplay + "%"));
      }
   }

   public void fixAndPrintLanguageFile(Translation other, String filePath, boolean addMissingTranslations) {
      ArrayList<String> fixedLines = new ArrayList<>();

      for (int i = 0; i < this.lines.size(); i++) {
         Translation.LineData myLine = this.lines.get(i);
         if (myLine.type != Translation.LineType.COMMENT && myLine.type != Translation.LineType.EMPTY && myLine.type != Translation.LineType.UNKNOWN) {
            if (myLine.type == Translation.LineType.SET_CATEGORY) {
               Translation.SetCategoryLineData categoryLine = (Translation.SetCategoryLineData)myLine;
               LinkedList<Translation.LineData> linesBefore = other.linesBeforeCategory.get(categoryLine.category);
               if (linesBefore == null) {
                  linesBefore = this.linesBeforeCategory.getOrDefault(categoryLine.category, new LinkedList<>());
               }

               for (Translation.LineData line : linesBefore) {
                  fixedLines.add(line.fullLine);
               }

               fixedLines.add(myLine.fullLine);
               LinkedList<Translation.LineData> linesAfter = other.linesAfterCategory.get(categoryLine.category);
               if (linesAfter == null) {
                  linesAfter = this.linesAfterCategory.getOrDefault(categoryLine.category, new LinkedList<>());
               }

               for (Translation.LineData line : linesAfter) {
                  fixedLines.add(line.fullLine);
               }
            } else if (myLine.type == Translation.LineType.SET_TRANSLATION) {
               Translation.SetTranslationLineData myTranslationLine = (Translation.SetTranslationLineData)myLine;
               LinkedList<Translation.LineData> linesBefore = other.linesBeforeTranslation.get(myTranslationLine.category + "." + myTranslationLine.key);
               if (linesBefore == null) {
                  linesBefore = this.linesAfterTranslation.getOrDefault(myTranslationLine.category + "." + myTranslationLine.key, new LinkedList<>());
               }

               for (Translation.LineData line : linesBefore) {
                  fixedLines.add(line.fullLine);
               }

               String otherTranslation = other.getTranslation(myTranslationLine.category, myTranslationLine.key).translation;
               if (otherTranslation == null
                  || !other.isSameAsEnglish(myTranslationLine.category, myTranslationLine.key) && myTranslationLine.translation.equals(otherTranslation)) {
                  if (addMissingTranslations) {
                     fixedLines.add(MISSING_TRANSLATION_PREPEND + myTranslationLine.fullLine);
                  }
               } else {
                  String myTranslation = myTranslationLine.translation;
                  boolean partOfLang = myTranslationLine.category.equals("lang");
                  if (partOfLang) {
                     fixedLines.add(myTranslationLine.key + "=" + otherTranslation.replace("\n", "\\n"));
                  } else {
                     HashSet<String> myReplacementKeys = new HashSet<>();
                     HashSet<String> otherReplacementKeys = new HashSet<>();
                     GameUtils.forEachMatcherResult(TranslationElement.REPLACEMENTS_PATTERN, myTranslation, result -> myReplacementKeys.add(result.group(1)));
                     GameUtils.forEachMatcherResult(
                        TranslationElement.REPLACEMENTS_PATTERN, otherTranslation, result -> otherReplacementKeys.add(result.group(1))
                     );
                     if (!myReplacementKeys.equals(otherReplacementKeys)) {
                        if (addMissingTranslations) {
                           fixedLines.add(MISSING_TRANSLATION_PREPEND + myTranslationLine.fullLine);
                        }
                     } else {
                        boolean sameTranslation = other.isSameAsEnglish(myTranslationLine.category, myTranslationLine.key);
                        String fullLine = (sameTranslation ? SAME_TRANSLATION_PREPEND : "")
                           + myTranslationLine.key
                           + "="
                           + otherTranslation.replace("\n", "\\n");
                        fixedLines.add(fullLine);
                     }
                  }
               }

               LinkedList<Translation.LineData> linesAfter = other.linesAfterTranslation.get(myTranslationLine.category + "." + myTranslationLine.key);
               if (linesAfter == null) {
                  linesAfter = this.linesAfterTranslation.getOrDefault(myTranslationLine.category + "." + myTranslationLine.key, new LinkedList<>());
               }

               for (Translation.LineData line : linesAfter) {
                  fixedLines.add(line.fullLine);
               }
            }
         }
      }

      try {
         BufferedWriter writer = Files.newBufferedWriter(Paths.get(filePath), GlobalData.charset);

         try {
            Iterator<String> it = fixedLines.iterator();

            while (it.hasNext()) {
               writer.write(it.next());
               if (it.hasNext()) {
                  writer.newLine();
               }
            }
         } catch (Throwable var17) {
            if (writer != null) {
               try {
                  writer.close();
               } catch (Throwable var16) {
                  var17.addSuppressed(var16);
               }
            }

            throw var17;
         }

         if (writer != null) {
            writer.close();
         }
      } catch (IOException e) {
         e.printStackTrace();
      }
   }

   public int countTranslationWords() {
      return this.streamStrippedTranslations(true, false).mapToInt(t -> t.translation.split(" ").length).sum();
   }

   public void exportTranslationCSV(String path) {
      File file = new File(path);

      try {
         GameUtils.mkDirs(file);
         OutputStreamWriter os = new FileWriter(file);
         this.forEachTranslation(
            data -> {
               try {
                  os.append("\"")
                     .append(data.category)
                     .append(".")
                     .append(data.key)
                     .append("\"")
                     .append(",")
                     .append("\"")
                     .append(data.translation.replace("\n", "\\n"))
                     .append("\"")
                     .append("\n");
               } catch (IOException ex) {
                  throw new RuntimeException(ex);
               }
            },
            true,
            false
         );
         os.close();
      } catch (IOException e) {
         e.printStackTrace();
      }
   }

   public void importTranslationCSV(String langFilePath, String csvFilePath) {
      HashMap<String, TranslationCategory> csvCategories = new HashMap<>();
      Pattern regex = Pattern.compile("(?:^\"|,\")(\"\"|[\\w\\W]*?)(?=\",|\"$)|(?:^(?!\")|,(?!\"))([^,]*?)(?=$|,)|(\\r\\n|\\n)");

      try {
         BufferedReader reader = Files.newBufferedReader(Paths.get(csvFilePath), GlobalData.charset);

         String line;
         try {
            while ((line = reader.readLine()) != null) {
               while (!line.endsWith("\"")) {
                  String readLine = reader.readLine();
                  if (readLine == null) {
                     break;
                  }

                  line = line + "\\n" + readLine;
               }

               Matcher matcher = regex.matcher(line);
               if (matcher.find()) {
                  String key = matcher.group(1);
                  if (matcher.find()) {
                     String translation = matcher.group(1);
                     translation = translation.replace("\"\"", "\"");
                     int split = key.indexOf(".");
                     String category;
                     if (split != -1) {
                        category = key.substring(0, split);
                        key = key.substring(split + 1);
                     } else {
                        category = "null";
                     }

                     String finalKey = key;
                     String finalTranslation = translation;
                     csvCategories.compute(category, (s, last) -> {
                        if (last == null) {
                           last = new TranslationCategory(category);
                        }

                        last.addTranslation(this.fileName, finalKey, finalTranslation, false, false, null);
                        return (TranslationCategory)last;
                     });
                  } else {
                     System.out.println("COULD NOT FIND TRANSLATION IN " + line + " FOR " + langFilePath);
                  }
               } else {
                  System.out.println("COULD NOT FIND KEY IN " + line + " FOR " + langFilePath);
               }
            }
         } catch (Throwable var18) {
            if (reader != null) {
               try {
                  reader.close();
               } catch (Throwable var14) {
                  var18.addSuppressed(var14);
               }
            }

            throw var18;
         }

         if (reader != null) {
            reader.close();
         }
      } catch (IOException e) {
         throw new RuntimeException(e);
      }

      ArrayList<String> fixedLines = new ArrayList<>();

      for (int i = 0; i < this.lines.size(); i++) {
         Translation.LineData myLine = this.lines.get(i);
         if (myLine.type == Translation.LineType.SET_TRANSLATION) {
            Translation.SetTranslationLineData myTranslationLine = (Translation.SetTranslationLineData)myLine;
            TranslationCategory csvCategory = csvCategories.get(myTranslationLine.category);
            if (csvCategory != null) {
               String csvTranslation = csvCategory.getTranslation(myTranslationLine.key).translation;
               if (csvTranslation == null
                  || csvTranslation.equals(myTranslationLine.translation) && !this.categories.get(myTranslationLine.category).isMissing(myTranslationLine.key)) {
                  fixedLines.add(myTranslationLine.fullLine);
               } else {
                  fixedLines.add(myTranslationLine.key + "=" + csvTranslation);
               }
            } else {
               fixedLines.add(myTranslationLine.fullLine);
            }
         } else {
            fixedLines.add(myLine.fullLine);
         }
      }

      try {
         BufferedWriter writer = Files.newBufferedWriter(Paths.get(langFilePath), GlobalData.charset);

         try {
            Iterator<String> it = fixedLines.iterator();

            while (it.hasNext()) {
               writer.write(it.next());
               if (it.hasNext()) {
                  writer.newLine();
               }
            }
         } catch (Throwable var16) {
            if (writer != null) {
               try {
                  writer.close();
               } catch (Throwable var15) {
                  var16.addSuppressed(var15);
               }
            }

            throw var16;
         }

         if (writer != null) {
            writer.close();
         }
      } catch (IOException e) {
         e.printStackTrace();
      }
   }

   private static class LineData {
      public final Translation.LineType type;
      public final String fullLine;

      public LineData(Translation.LineType type, String fullLine) {
         this.type = type;
         this.fullLine = fullLine;
      }
   }

   private enum LineType {
      SET_CATEGORY(Translation.SetCategoryLineData.class),
      SET_TRANSLATION(Translation.SetTranslationLineData.class),
      COMMENT(Translation.LineData.class),
      EMPTY(Translation.LineData.class),
      UNKNOWN(Translation.LineData.class);

      public final Class<? extends Translation.LineData> typeClass;

      LineType(Class<? extends Translation.LineData> typeClass) {
         this.typeClass = typeClass;
      }
   }

   private static class SetCategoryLineData extends Translation.LineData {
      public final String category;

      public SetCategoryLineData(String fullLine, String category) {
         super(Translation.LineType.SET_CATEGORY, fullLine);
         this.category = category;
      }
   }

   private static class SetTranslationLineData extends Translation.LineData {
      public final String category;
      public final String key;
      public final String translation;

      public SetTranslationLineData(String fullLine, String category, String key, String translation) {
         super(Translation.LineType.SET_TRANSLATION, fullLine);
         this.category = category;
         this.key = key;
         this.translation = translation;
      }
   }

   public static class TranslationData {
      public final String category;
      public final String key;
      public final String translation;

      public TranslationData(String category, String key, String translation) {
         this.category = category;
         this.key = key;
         this.translation = translation;
      }
   }
}
