package necesse.engine.localization;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import necesse.engine.GameLog;
import necesse.engine.GlobalData;
import necesse.engine.localization.fileLanguage.FileLanguage;
import necesse.engine.localization.fileLanguage.Translation;
import necesse.engine.localization.message.LocalMessage;
import necesse.engine.modLoader.LoadedMod;
import necesse.engine.modLoader.ModLoader;
import necesse.gfx.gameFont.FontManager;

public class Localization {
   private static final Object LOCK = new Object();
   private static final ArrayList<Language> officialLanguages = new ArrayList<>();
   private static final ArrayList<Language> languages = new ArrayList<>();
   public static final Language NoTranslation = registerLanguage(new Language("no_translation", null, "No translation", "No translation", null) {
      @Override
      public AbstractTranslationElement getTranslation(String category, String key) {
         return new DebugTranslationElement(category, key);
      }

      @Override
      public boolean isMissing(String category, String key) {
         return false;
      }

      @Override
      public boolean translationExists(String category, String key) {
         return false;
      }

      @Override
      public boolean isSameAsEnglish(String category, String key) {
         return false;
      }

      @Override
      public int countTranslationWords() {
         return 0;
      }

      @Override
      public void exportTranslationCSV(String path) {
      }

      @Override
      public void importTranslationCSV(String csvFilePath) {
      }

      @Override
      public boolean isDebugOnly() {
         return true;
      }

      @Override
      public String getCharactersUsed() {
         return "";
      }
   }, true);
   public static final Translation EnglishTranslation = new Translation("en.lang");
   public static final Language English = registerLanguage(new FileLanguage("en", "english", EnglishTranslation, "English", "English"), true);
   public static final Language ChineseSimplified = registerLanguage(
      new FileLanguage("zh-CN", "schinese", new Translation("zh-CN.lang", EnglishTranslation), "Chinese - Simplified", "简体中文"), false
   );
   public static final Language ChineseTraditional = registerLanguage(
      new FileLanguage("zh-TW", "tchinese", new Translation("zh-TW.lang", EnglishTranslation), "Chinese - Traditional", "繁體中文"), false
   );
   public static final Language Russian = registerLanguage(
      new FileLanguage("ru", "russian", new Translation("ru.lang", EnglishTranslation), "Russian", "Русский"), false
   );
   public static final Language BrazilianPortuguese = registerLanguage(
      new FileLanguage("pt-BR", "brazilian", new Translation("pt-BR.lang", EnglishTranslation), "Portuguese - Brazil", "Português - Brasil"), false
   );
   public static final Language Spanish = registerLanguage(
      new FileLanguage("es", "spanish", new Translation("es.lang", EnglishTranslation), "Spanish", "Español"), false
   );
   public static final Language German = registerLanguage(
      new FileLanguage("de", "german", new Translation("de.lang", EnglishTranslation), "German", "Deutsch"), false
   );
   public static final Language Polish = registerLanguage(
      new FileLanguage("pl", "polish", new Translation("pl.lang", EnglishTranslation), "Polish", "Polski"), false
   );
   public static final Language Czech = registerLanguage(
      new FileLanguage("cs", "czech", new Translation("cs.lang", EnglishTranslation), "Czech", "Čeština"), false
   );
   public static final Language Turkish = registerLanguage(
      new FileLanguage("tr", "turkish", new Translation("tr.lang", EnglishTranslation), "Turkish", "Türkçe"), false
   );
   public static final Language Japanese = registerLanguage(
      new FileLanguage("ja", "japanese", new Translation("ja.lang", EnglishTranslation), "Japanese", "日本語"), false
   );
   public static final Language French = registerLanguage(
      new FileLanguage("fr", "french", new Translation("fr.lang", EnglishTranslation), "French", "Français"), false
   );
   public static final Language Ukrainian = registerLanguage(
      new FileLanguage("uk", "ukrainian", new Translation("uk.lang", EnglishTranslation), "Ukrainian", "Українська"), false
   );
   public static final Language Danish = registerLanguage(
      new FileLanguage("da", "danish", new Translation("da.lang", EnglishTranslation), "Danish", "Dansk"), false
   );
   public static final Language Swedish = registerLanguage(
      new FileLanguage("se", "swedish", new Translation("se.lang", EnglishTranslation), "Swedish", "Svenska"), false
   );
   public static final Language Norwegian = registerLanguage(
      new FileLanguage("no", "norwegian", new Translation("no.lang", EnglishTranslation), "Norwegian", "Norsk"), false
   );
   public static final Language Hungarian = registerLanguage(
      new FileLanguage("hu", "hungarian", new Translation("hu.lang", EnglishTranslation), "Hungarian", "Magyar"), false
   );
   public static final Language Italian = registerLanguage(
      new FileLanguage("it", "italian", new Translation("it.lang", EnglishTranslation), "Italian", "Italiano"), false
   );
   public static final Language Korean = registerLanguage(
      new FileLanguage("kr", "koreana", new Translation("kr.lang", EnglishTranslation), "Korean", "한국어"), false
   );
   public static final Language Thai = registerLanguage(new FileLanguage("th", "thai", new Translation("th.lang", EnglishTranslation), "Thai", "ไทย"), false);
   public static final Language Vietnamese = registerLanguage(
      new FileLanguage("vi", "vietnamese", new Translation("vi.lang", EnglishTranslation), "Vietnamese", "Tiếng Việt"), false
   );
   public static final Language Indonesian = registerLanguage(
      new FileLanguage("id", "indonesian", new Translation("id.lang", EnglishTranslation), "Indonesian", "Indonesia"), false
   );
   public static final Language Lithuanian = registerLanguage(
      new FileLanguage("lt", "lithuanian", new Translation("lt.lang", EnglishTranslation), "Lithuanian", "Lietuviškai"), false
   );
   public static final Language Dutch = registerLanguage(
      new FileLanguage("nl", "dutch", new Translation("nl.lang", EnglishTranslation), "Dutch", "Nederlands"), false
   );
   public static final Language Finnish = registerLanguage(
      new FileLanguage("fi", "finnish", new Translation("fi.lang", EnglishTranslation), "Finnish", "Suomi"), false
   );
   public static final Language Croatian = registerLanguage(
      new FileLanguage("hr", "croatian", new Translation("hr.lang", EnglishTranslation), "Croatian", "Hrvatski"), false
   );
   public static final Language Catalan = registerLanguage(
      new FileLanguage("ca", "catalan", new Translation("ca.lang", EnglishTranslation), "Catalan", "Català"), false
   );
   public static final Language Arabic = registerLanguage(
      new FileLanguage("ar", "arabic", new Translation("ar.lang", EnglishTranslation), "Arabic", "العربية"), false
   );
   public static final Language Bulgarian = registerLanguage(
      new FileLanguage("bg", "bulgarian", new Translation("bg.lang", EnglishTranslation), "Bulgarian", "Български"), false
   );
   public static final Language defaultLang;
   private static Language currentLang;
   private static final LinkedList<LocalizationChangeListener> nextListeners;
   private static final LinkedList<LocalizationChangeListener> listeners;
   private static final HashMap<Integer, Long> warnTimers;
   private static final int warnCooldown = 10000;

   public static <T extends Language> T registerLanguage(T language) {
      return registerLanguage(language, false);
   }

   private static <T extends Language> T registerLanguage(T language, boolean isOfficial) {
      if (!language.stringID.matches("[a-zA-Z0-9_\\-]+")) {
         throw new IllegalArgumentException("Tried to register language with invalid stringID: \"" + language.stringID + "\"");
      }

      if (languages.stream().anyMatch(l -> l.stringID.equals(language.stringID))) {
         throw new IllegalArgumentException("Tried to register language with duplicate stringID: \"" + language.stringID + "\"");
      }

      if (isOfficial) {
         officialLanguages.add(language);
      }

      languages.add(language);
      return language;
   }

   public static void reloadLanguageFiles() {
      for (Language language : languages) {
         language.reload(ModLoader.getEnabledMods());
         language.updateUnique();
      }

      updateListeners();
   }

   public static Language getLanguageStringID(String stringID) {
      return languages.stream().filter(l -> l.stringID.equals(stringID)).findFirst().orElse(null);
   }

   public static boolean isOfficial(Language language) {
      return officialLanguages.stream().filter(l -> l == language).findFirst().orElse(null) != null;
   }

   public static Language getCurrentLang() {
      return currentLang;
   }

   private static void warn(String category, String translationKey) {
      int hash = (category + "." + translationKey).hashCode();
      long timer = 0L;
      if (warnTimers.containsKey(hash)) {
         timer = warnTimers.get(hash);
      }

      if (timer < System.currentTimeMillis()) {
         GameLog.warn.println("Translation of " + category + "." + translationKey + " is not found.");
         warnTimers.put(hash, System.currentTimeMillis() + 10000L);
      }
   }

   public static void setCurrentLang(Language lang) {
      setCurrentLang(lang, true);
   }

   private static void setCurrentLang(Language lang, boolean updateListeners) {
      if (getCurrentLang() != lang) {
         currentLang = lang;
         GameLog.debug.println("Changed current language to " + lang.stringID);
         if (FontManager.bit != null && lang != defaultLang) {
            long time = System.currentTimeMillis();
            FontManager.updateFont(lang);
            GameLog.debug.println("Font update took " + (System.currentTimeMillis() - time) + " ms");
         }

         if (updateListeners) {
            updateListeners();
         }
      }
   }

   private static void updateListeners() {
      synchronized (LOCK) {
         listeners.addAll(nextListeners);
         nextListeners.clear();
         cleanListeners();

         for (LocalizationChangeListener listener : listeners) {
            listener.onChange(currentLang);
         }
      }
   }

   public static void loadModsLanguage() {
      for (Language language : languages) {
         for (LoadedMod mod : ModLoader.getEnabledMods()) {
            language.loadMod(mod);
         }
      }
   }

   public static Language[] getLanguages() {
      return languages.toArray(new Language[0]);
   }

   public static void cleanListeners() {
      synchronized (LOCK) {
         listeners.removeIf(LocalizationChangeListener::isDisposed);
         nextListeners.removeIf(LocalizationChangeListener::isDisposed);
      }
   }

   public static int getListenersSize() {
      return listeners.size() + nextListeners.size();
   }

   public static LocalizationChangeListener addListener(LocalizationChangeListener listener) {
      synchronized (LOCK) {
         nextListeners.add(listener);
         return listener;
      }
   }

   public static AbstractTranslationElement getTranslation(Language language, String category, String translationKey, boolean debug) {
      AbstractTranslationElement translation = language.getTranslation(category, translationKey);
      if (translation == AbstractTranslationElement.NULL_TRANSLATION) {
         translation = defaultLang.getTranslation(category, translationKey);
         if (translation == AbstractTranslationElement.NULL_TRANSLATION) {
            if (debug) {
               warn(category, translationKey);
            }

            return new DebugTranslationElement(category, translationKey);
         }
      }

      return translation;
   }

   public static String translate(Language language, String category, String translationKey, boolean debug) {
      return getTranslation(language, category, translationKey, debug).translation;
   }

   public static String translate(Language language, String category, String translationKey) {
      return translate(language, category, translationKey, true);
   }

   public static String translate(String category, String translationKey, boolean debug) {
      return translate(getCurrentLang(), category, translationKey, debug);
   }

   public static String translate(String category, String translationKey) {
      return translate(category, translationKey, true);
   }

   public static String translate(String category, String key, String replaceKey, String replacement) {
      return new LocalMessage(category, key, replaceKey, replacement).translate();
   }

   public static String translate(String category, String key, String replaceKey, Object replacement) {
      return new LocalMessage(category, key, replaceKey, replacement).translate();
   }

   public static String translate(String category, String key, String... replacements) {
      return new LocalMessage(category, key, replacements).translate();
   }

   public static String translate(String category, String key, Object... replacements) {
      return new LocalMessage(category, key, replacements).translate();
   }

   static {
      String[] ignoredFiles = new String[]{".DS_Store", ".gitignore"};
      Pattern namePattern = Pattern.compile("([a-zA-Z0-9-_]+)\\.lang");
      File[] files = new File(GlobalData.rootPath() + "locale/").listFiles();
      if (files != null) {
         for (File file : files) {
            if (!Arrays.stream(ignoredFiles).anyMatch(s -> s.equals(file.getName()))) {
               Matcher matcher = namePattern.matcher(file.getName());
               if (file.isFile() && matcher.find()) {
                  String stringID = matcher.group(1);
                  if (!languages.stream().anyMatch(l -> l.stringID.equals(stringID))) {
                     String err = FileLanguage.loadFileLanguage(stringID, file.getName());
                     if (err != null) {
                        GameLog.warn.println("Error loading " + file.getName() + ": " + err);
                     } else {
                        System.out.println("Registered unsupported language " + stringID);
                     }
                  }
               } else {
                  GameLog.warn.println("Locale file " + file.getName() + " does not match name requirements");
               }
            }
         }
      }

      defaultLang = English;
      currentLang = defaultLang;
      nextListeners = new LinkedList<>();
      listeners = new LinkedList<>();
      warnTimers = new HashMap<>();
   }
}
