package necesse.engine.registries;

import java.util.ArrayList;
import necesse.engine.GameLog;
import necesse.engine.util.GameRandom;
import necesse.entity.levelEvent.incursionModifiers.AlchemicalInterferenceIncursionModifier;
import necesse.entity.levelEvent.incursionModifiers.CrawlmageddonIncursionModifier;
import necesse.entity.levelEvent.incursionModifiers.ExplosiveIncursionModifier;
import necesse.entity.levelEvent.incursionModifiers.FlamelingsIncursionModifier;
import necesse.entity.levelEvent.incursionModifiers.FrenzyIncursionModifier;
import necesse.entity.levelEvent.incursionModifiers.StormingIncursionModifier;
import necesse.entity.levelEvent.incursionModifiers.TremorsIncursionModifier;
import necesse.level.maps.incursion.UniqueIncursionModifier;

public class UniqueIncursionModifierRegistry extends GameRegistry<UniqueIncursionModifier> {
   public static final UniqueIncursionModifierRegistry instance = new UniqueIncursionModifierRegistry();

   private UniqueIncursionModifierRegistry() {
      super("UniqueIncursionModifier", 32766);
   }

   @Override
   public void registerCore() {
      registerUniqueModifier("alchemicalinterference", new AlchemicalInterferenceIncursionModifier());
      registerUniqueModifier("crawlmageddon", new CrawlmageddonIncursionModifier());
      registerUniqueModifier("frenzy", new FrenzyIncursionModifier());
      registerUniqueModifier("tremors", new TremorsIncursionModifier());
      registerUniqueModifier("storming", new StormingIncursionModifier());
      registerUniqueModifier("explosive", new ExplosiveIncursionModifier());
      registerUniqueModifier("flamelings", new FlamelingsIncursionModifier());
   }

   protected void onRegister(UniqueIncursionModifier object, int id, String stringID, boolean isReplace) {
   }

   @Override
   protected void onRegistryClose() {
   }

   public static int registerUniqueModifier(String stringID, UniqueIncursionModifier modifier) {
      return instance.register(stringID, modifier);
   }

   public static Iterable<UniqueIncursionModifier> getIncursionModifiers() {
      return instance.getElements();
   }

   public static UniqueIncursionModifier getRandomIncursionModifier(GameRandom seededRandom, ArrayList<UniqueIncursionModifier> usedModifiers) {
      Iterable<UniqueIncursionModifier> incursionModifiers = getIncursionModifiers();
      ArrayList<UniqueIncursionModifier> unusedModifiers = new ArrayList<>();

      for (UniqueIncursionModifier incursionModifier : incursionModifiers) {
         if (!usedModifiers.contains(incursionModifier)) {
            unusedModifiers.add(incursionModifier);
         }
      }

      if (unusedModifiers.isEmpty()) {
         GameLog.warn.println("No unused modifier found. Adding a random modifier instead.");
         return (UniqueIncursionModifier)(Object)seededRandom.getOneOf(incursionModifiers);
      } else {
         return seededRandom.getOneOf(unusedModifiers);
      }
   }
}
