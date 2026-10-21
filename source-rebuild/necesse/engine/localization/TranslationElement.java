package necesse.engine.localization;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TranslationElement extends AbstractTranslationElement {
   public static final Pattern REPLACEMENTS_PATTERN = Pattern.compile("<([^>]+)>");
   private final ArrayList<TranslationElement.KeyIndex> keyIndexes = new ArrayList<>();
   private final HashSet<String> existingKeys = new HashSet<>();

   public TranslationElement(String translation) {
      super(translation);
      Matcher matcher = REPLACEMENTS_PATTERN.matcher(translation);
      int currentIndex = 0;

      while (matcher.find(currentIndex)) {
         int startIndex = matcher.start();
         String fullKey = matcher.group(0);
         currentIndex = startIndex + fullKey.length();
         String key = fullKey.substring(1, fullKey.length() - 1);
         this.keyIndexes.add(new TranslationElement.KeyIndex(key, startIndex));
         this.existingKeys.add(key);
      }
   }

   public void printDebug() {
      System.out.println("Translation: " + this.translation);
      System.out.println("Replacements:");

      for (TranslationElement.KeyIndex replacement : this.keyIndexes) {
         System.out.println("\t\"" + replacement.key + "\": " + replacement.index);
      }
   }

   @Override
   public boolean hasReplacementKey(String key) {
      return this.existingKeys.contains(key);
   }

   @Override
   public String getReplacedTranslation(Function<String, String> keyToReplacement) {
      String translation = this.translation;
      int indexOffset = 0;

      for (TranslationElement.KeyIndex ki : this.keyIndexes) {
         String replacement = keyToReplacement.apply(ki.key);
         if (replacement != null) {
            translation = translation.substring(0, ki.index + indexOffset) + replacement + translation.substring(ki.index + indexOffset + ki.key.length() + 2);
            indexOffset += replacement.length() - (ki.key.length() + 2);
         }
      }

      return translation;
   }

   private static class KeyIndex {
      public final String key;
      public final int index;

      public KeyIndex(String key, int index) {
         this.key = key;
         this.index = index;
      }
   }
}
