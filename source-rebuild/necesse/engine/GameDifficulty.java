package necesse.engine;

import java.util.ArrayList;
import java.util.function.Supplier;
import necesse.engine.localization.Language;
import necesse.engine.localization.Localization;
import necesse.engine.localization.message.GameMessage;
import necesse.engine.localization.message.LocalMessage;
import necesse.gfx.ui.ButtonIcon;

public enum GameDifficulty {
   CASUAL(
      0.4F,
      0.25F,
      1.0F,
      0.6F,
      0.9F,
      new LocalMessage("ui", "diffcasual"),
      new LocalMessage("ui", "diffcasualdesc"),
      "diffcasualeffect",
      () -> Settings.UI.difficulty_casual_background,
      () -> Settings.UI.difficulty_casual
   ),
   ADVENTURE(
      0.7F,
      0.5F,
      1.0F,
      0.85F,
      1.0F,
      new LocalMessage("ui", "diffadventure"),
      new LocalMessage("ui", "diffadventuredesc"),
      "diffadventureeffect",
      () -> Settings.UI.difficulty_adventure_background,
      () -> Settings.UI.difficulty_adventure
   ),
   CLASSIC(
      1.0F,
      0.75F,
      1.0F,
      1.0F,
      1.0F,
      new LocalMessage("ui", "diffclassic"),
      new LocalMessage("ui", "diffclassicdesc"),
      "diffclassiceffect",
      () -> Settings.UI.difficulty_classic_background,
      () -> Settings.UI.difficulty_classic
   ),
   HARD(
      1.2F,
      1.0F,
      0.75F,
      1.25F,
      1.2F,
      new LocalMessage("ui", "diffhard"),
      new LocalMessage("ui", "diffharddesc"),
      "diffhardeffect",
      () -> Settings.UI.difficulty_hard_background,
      () -> Settings.UI.difficulty_hard
   ),
   BRUTAL(
      1.6F,
      1.25F,
      0.4F,
      1.5F,
      1.5F,
      new LocalMessage("ui", "diffbrutal"),
      new LocalMessage("ui", "diffbrutaldesc"),
      "diffbrualeffect",
      () -> Settings.UI.difficulty_brutal_background,
      () -> Settings.UI.difficulty_brutal
   );

   public final GameMessage displayName;
   public final GameMessage description;
   public final String effectsMessagePrefix;
   public final float damageTakenModifier;
   public final float raiderDamageModifier;
   public final float knockbackGivenModifier;
   public final float enemySpawnRateModifier;
   public final float enemySpawnCapModifier;
   public final Supplier<ButtonIcon> buttonIconBackgroundSupplier;
   public final Supplier<ButtonIcon> buttonIconForegroundSupplier;

   GameDifficulty(
      float damageTakenModifier,
      float raiderDamageModifier,
      float knockbackGivenModifier,
      float enemySpawnRateModifier,
      float enemySpawnCapModifier,
      GameMessage displayName,
      GameMessage description,
      String effectsMessagePrefix,
      Supplier<ButtonIcon> buttonIconBackgroundSupplier,
      Supplier<ButtonIcon> buttonIconForegroundSupplier
   ) {
      this.damageTakenModifier = damageTakenModifier;
      this.raiderDamageModifier = raiderDamageModifier;
      this.knockbackGivenModifier = knockbackGivenModifier;
      this.enemySpawnRateModifier = enemySpawnRateModifier;
      this.enemySpawnCapModifier = enemySpawnCapModifier;
      this.displayName = displayName;
      this.description = description;
      this.effectsMessagePrefix = effectsMessagePrefix;
      this.buttonIconBackgroundSupplier = buttonIconBackgroundSupplier;
      this.buttonIconForegroundSupplier = buttonIconForegroundSupplier;
   }

   public ArrayList<GameMessage> getEffectMessages() {
      ArrayList<GameMessage> messages = new ArrayList<>();
      Language lang = Localization.getCurrentLang();

      for (int i = 1;
         i <= 50
            && (lang.translationExists("ui", this.effectsMessagePrefix + i) || Localization.EnglishTranslation.exists("ui", this.effectsMessagePrefix + i));
         i++
      ) {
         messages.add(new LocalMessage("ui", this.effectsMessagePrefix + i));
      }

      return messages;
   }
}
