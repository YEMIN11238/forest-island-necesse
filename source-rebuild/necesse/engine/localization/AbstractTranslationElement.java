package necesse.engine.localization;

import java.util.function.Function;

public abstract class AbstractTranslationElement {
   public static final AbstractTranslationElement NULL_TRANSLATION = new AbstractTranslationElement(null) {
      @Override
      public boolean hasReplacementKey(String key) {
         return false;
      }

      @Override
      public String getReplacedTranslation(Function<String, String> keyToReplacement) {
         return null;
      }
   };
   public final String translation;

   public AbstractTranslationElement(String translation) {
      this.translation = translation;
   }

   public abstract boolean hasReplacementKey(String var1);

   public abstract String getReplacedTranslation(Function<String, String> var1);
}
