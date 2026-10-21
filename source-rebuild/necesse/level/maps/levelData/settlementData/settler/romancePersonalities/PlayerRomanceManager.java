package necesse.level.maps.levelData.settlementData.settler.romancePersonalities;

import java.util.ArrayList;
import necesse.engine.network.server.ServerClient;
import necesse.engine.registries.RomanceActivitiesRegistry;
import necesse.engine.save.LoadData;
import necesse.engine.save.LoadDataException;
import necesse.engine.save.SaveData;
import necesse.engine.util.GameMath;
import necesse.engine.util.GameRandom;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.MobWasKilledEvent;
import necesse.gfx.forms.components.FormExpressionWheel;
import necesse.inventory.recipe.Recipe;
import necesse.level.gameObject.GameObject;
import necesse.level.maps.levelData.settlementData.settler.romancePersonalities.romanceActivities.RomanceActivity;

public class PlayerRomanceManager {
   public final SettlerRomancePersonality romancePersonality;
   public final long clientAuth;
   public int romanceLevel;
   public int romanceActivityCooldownEndDay;
   public int giftFromLifePartnerCooldownEndDay;
   public ArrayList<RomanceActivity> romanceActivities = new ArrayList<>();

   public PlayerRomanceManager(SettlerRomancePersonality romancePersonality, long clientAuth, LoadData save) {
      this.romancePersonality = romancePersonality;
      this.clientAuth = clientAuth;
      this.applyLoadData(save);
   }

   public PlayerRomanceManager(SettlerRomancePersonality romancePersonality, long clientAuth) {
      this.romancePersonality = romancePersonality;
      this.clientAuth = clientAuth;
      this.romanceActivities = romancePersonality.generateRomanceActivities(this);
   }

   public void addSaveData(SaveData save) {
      save.addInt("romanceLevel", this.romanceLevel);
      save.addInt("romanceActivityCooldownEndDay", this.romanceActivityCooldownEndDay);
      save.addInt("giftFromLifePartnerCooldownEndDay", this.giftFromLifePartnerCooldownEndDay);

      for (RomanceActivity romanceActivity : this.romanceActivities) {
         SaveData romanceActivityData = new SaveData("romanceActivity");
         romanceActivityData.addSafeString("activityStringID", romanceActivity.getStringID());
         romanceActivity.addSaveData(romanceActivityData);
         save.addSaveData(romanceActivityData);
      }
   }

   public void applyLoadData(LoadData save) {
      this.romanceLevel = save.getInt("romanceLevel");
      this.romanceActivityCooldownEndDay = save.getInt("romanceActivityCooldownEndDay", 0, false);
      this.giftFromLifePartnerCooldownEndDay = save.getInt("giftFromLifePartnerCooldownEndDay", 0, false);
      this.romanceActivities = new ArrayList<>();

      try {
         for (LoadData romanceActivityData : save.getLoadDataByName("romanceActivity")) {
            String activityStringID = romanceActivityData.getSafeString("activityStringID", null, false);
            if (activityStringID == null) {
               throw new LoadDataException("Romance activity string ID not found in save data.");
            }

            RomanceActivity romanceActivity = RomanceActivitiesRegistry.getNewRomanceActivity(activityStringID, this);
            if (romanceActivity == null) {
               throw new LoadDataException("Romance activity with string ID " + activityStringID + " not found in registry.");
            }

            romanceActivity.applyLoadData(romanceActivityData);
            this.romanceActivities.add(romanceActivity);
         }
      } catch (Exception e) {
         System.err
            .println(
               "Error loading romance activities for settler " + this.romancePersonality.mob + " with clientAuth " + this.clientAuth + ": " + e.getMessage()
            );
         e.printStackTrace();
         this.romanceActivities = this.romancePersonality.generateRomanceActivities(this);
      }
   }

   public void increaseRomanceLevel(int cooldownInDaysBeforeNextActivity) {
      this.startActivityCooldown(cooldownInDaysBeforeNextActivity);
      this.romanceLevel++;
      this.romancePersonality.spawnSettlerHeartParticlesAndHappyExpression();
   }

   public void increaseRomanceLevel() {
      int cooldownInDays = GameRandom.globalRandom.getIntBetween(1, 3);
      this.increaseRomanceLevel(cooldownInDays);
   }

   public void increaseRomanceLevelWithCustomExpression(FormExpressionWheel.Expression expression) {
      this.startActivityCooldown(GameRandom.globalRandom.getIntBetween(1, 3));
      this.romanceLevel++;
      this.romancePersonality.mob.startExpressionAbility.runAndSend(expression, 8000);
   }

   public void startActivityCooldown(int cooldownInDays) {
      this.romanceActivityCooldownEndDay = this.romancePersonality.mob.getWorldEntity().getDay() + cooldownInDays;
   }

   public boolean isActivityOnCooldown() {
      int currentDay = this.romancePersonality.mob.getWorldEntity().getDay();
      return currentDay < this.romanceActivityCooldownEndDay;
   }

   public boolean isGiftFromPartnerOnCooldown() {
      int currentDay = this.romancePersonality.mob.getWorldEntity().getDay();
      return currentDay < this.giftFromLifePartnerCooldownEndDay;
   }

   public void startGiftCooldown(int cooldownInDays) {
      this.giftFromLifePartnerCooldownEndDay = this.romancePersonality.mob.getWorldEntity().getDay() + cooldownInDays;
   }

   public RomanceActivity getCurrentRomanceActivity() {
      if (this.romancePersonality.isInRelationshipWith(this.clientAuth)) {
         return null;
      }

      int romanceLevelToCheck = Math.max(0, this.romanceLevel - 1);
      return this.romanceActivities.get(GameMath.limit(romanceLevelToCheck, 0, this.romanceActivities.size() - 1));
   }

   public void serverTickInAdventureParty(ServerClient client) {
      RomanceActivity currentActivity = this.getCurrentRomanceActivity();
      if (currentActivity != null) {
         currentActivity.serverTickInAdventureParty(client);
      }
   }

   public void onMobKilled(ServerClient client, MobWasKilledEvent event) {
      RomanceActivity currentActivity = this.getCurrentRomanceActivity();
      if (currentActivity != null) {
         currentActivity.onMobKilled(client, event);
      }
   }

   public void onAnimalCaptured(ServerClient client, Mob animalCaptured) {
      RomanceActivity currentActivity = this.getCurrentRomanceActivity();
      if (currentActivity != null) {
         currentActivity.onAnimalCaptured(client, animalCaptured);
      }
   }

   public void onCritterNetted(ServerClient client, Mob critterNetted) {
      RomanceActivity currentActivity = this.getCurrentRomanceActivity();
      if (currentActivity != null) {
         currentActivity.onCritterNetted(client, critterNetted);
      }
   }

   public void onAnimalReleased(ServerClient client, Mob animalReleased) {
      RomanceActivity currentActivity = this.getCurrentRomanceActivity();
      if (currentActivity != null) {
         currentActivity.onAnimalReleased(client, animalReleased);
      }
   }

   public void onCraftedRecipe(ServerClient client, Recipe recipe, int amount) {
      RomanceActivity currentActivity = this.getCurrentRomanceActivity();
      if (currentActivity != null) {
         currentActivity.onCraftedRecipe(client, recipe, amount);
      }
   }

   public void onObjectPlaced(ServerClient client, GameObject object, int tileX, int tileY, int rotation) {
      RomanceActivity currentActivity = this.getCurrentRomanceActivity();
      if (currentActivity != null) {
         currentActivity.onObjectPlaced(client, object, tileX, tileY, rotation);
      }
   }

   public String getDebugServerTooltips(String clientName) {
      RomanceActivity currentActivity = this.getCurrentRomanceActivity();
      return clientName + " - romanceLevel: " + this.romanceLevel + ", currentActivity: " + (currentActivity == null ? "null" : currentActivity.getStringID());
   }
}
