package necesse.engine.localization.fileLanguage;

import java.util.HashMap;
import java.util.Map.Entry;
import java.util.function.BiConsumer;
import java.util.stream.Stream;
import necesse.engine.GameLog;
import necesse.engine.localization.AbstractTranslationElement;
import necesse.engine.localization.TranslationElement;
import necesse.engine.modLoader.LoadedMod;

public class TranslationCategory {
   public final String name;
   private final HashMap<String, AbstractTranslationElement> translations = new HashMap<>();
   private final HashMap<String, Boolean> sameTranslations = new HashMap<>();
   private final HashMap<String, Boolean> missingTranslations = new HashMap<>();

   public TranslationCategory(String name) {
      this.name = name;
   }

   public int getTotalTranslations() {
      return this.translations.size();
   }

   public void addTranslation(String debugName, String key, String translation, boolean sameAsEnglish, boolean missing, LoadedMod mod) {
      if (this.translations.containsKey(key)) {
         if (mod == null) {
            GameLog.warn.println(debugName + ": Overwrote duplicate translation " + this.name + "." + key);
         } else {
            System.out.println(debugName + ": " + this.name + "." + key + " localisation overwriting by mod " + mod.id);
         }
      }

      this.translations.put(key, new TranslationElement(translation));
      if (sameAsEnglish) {
         this.sameTranslations.put(key, true);
      }

      if (missing) {
         this.missingTranslations.put(key, true);
      }
   }

   public AbstractTranslationElement getTranslation(String key) {
      return this.translations.getOrDefault(key, TranslationElement.NULL_TRANSLATION);
   }

   public boolean isSameAsEnglish(String key) {
      return this.sameTranslations.getOrDefault(key, false);
   }

   public boolean isMissing(String key) {
      return this.missingTranslations.getOrDefault(key, false);
   }

   public boolean exists(String key) {
      return this.translations.containsKey(key) || this.sameTranslations.containsKey(key) || this.missingTranslations.containsKey(key);
   }

   public void printTranslations() {
      this.printTranslations("");
   }

   public void printTranslations(String prefix) {
      for (String key : this.translations.keySet()) {
         System.out.println(prefix + key + "=" + this.translations.get(key));
      }
   }

   public Stream<Entry<String, AbstractTranslationElement>> streamTranslations() {
      return this.translations.entrySet().stream();
   }

   public void forEachTranslations(BiConsumer<String, AbstractTranslationElement> action) {
      this.translations.forEach(action);
   }
}
