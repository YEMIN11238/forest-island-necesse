package necesse.engine.localization;

import java.util.function.Function;

public class DebugTranslationElement extends AbstractTranslationElement {
   public DebugTranslationElement(String category, String key) {
      super(category + "." + key);
   }

   @Override
   public boolean hasReplacementKey(String key) {
      return false;
   }

   @Override
   public String getReplacedTranslation(Function<String, String> keyToReplacement) {
      return this.translation;
   }
}
