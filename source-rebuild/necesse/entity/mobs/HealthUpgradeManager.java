package necesse.entity.mobs;

import java.util.ArrayList;
import java.util.HashMap;
import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.engine.save.LoadData;
import necesse.engine.save.SaveData;

public class HealthUpgradeManager {
   private final ArrayList<HealthUpgradeManager.HealthUpgrade> healthUpgrades = new ArrayList<>();
   private final HashMap<String, Integer> healthUpgradesIndexes = new HashMap<>();
   private int currentCalculatedHealthUpgrade = 0;
   private boolean isDirty = true;

   protected int registerHealthUpgrade(String itemStringID, HealthUpgradeManager.HealthUpgrade upgrade) {
      int index = this.healthUpgradesIndexes.getOrDefault(itemStringID, -1);
      if (index != -1) {
         this.healthUpgrades.set(index, upgrade);
         upgrade.stringID = itemStringID;
         upgrade.index = index;
         return index;
      } else {
         int nextIndex = this.healthUpgrades.size();
         this.healthUpgrades.add(upgrade);
         upgrade.stringID = itemStringID;
         upgrade.index = nextIndex;
         this.healthUpgradesIndexes.put(itemStringID, nextIndex);
         return nextIndex;
      }
   }

   protected int getHealthUpgradeID(String itemStringID) {
      return this.healthUpgradesIndexes.getOrDefault(itemStringID, -1);
   }

   protected String getHealthUpgradeStringID(int id) {
      return this.healthUpgrades.get(id).stringID;
   }

   protected HealthUpgradeManager.HealthUpgrade getHealthUpgrade(String itemStringID) {
      int id = this.getHealthUpgradeID(itemStringID);
      return id == -1 ? null : this.healthUpgrades.get(id);
   }

   public HealthUpgradeManager() {
      this.registerHealthUpgrade("demonheart", new HealthUpgradeManager.HealthUpgrade(1, 50));
      this.registerHealthUpgrade("spiderheart", new HealthUpgradeManager.HealthUpgrade(1, 50));
      this.registerHealthUpgrade("runicheart", new HealthUpgradeManager.HealthUpgrade(1, 50));
      this.registerHealthUpgrade("guardianheart", new HealthUpgradeManager.HealthUpgrade(1, 50));
      this.registerHealthUpgrade("lifeelixir", new HealthUpgradeManager.HealthUpgrade(10, 10));
      this.registerHealthUpgrade("cryoheart", new HealthUpgradeManager.HealthUpgrade(1, 50));
      this.registerHealthUpgrade("wardenheart", new HealthUpgradeManager.HealthUpgrade(1, 50));
      this.registerHealthUpgrade("greaterlifeelixir", new HealthUpgradeManager.HealthUpgrade(10, 10));
   }

   public void addSaveData(SaveData save, String name) {
      SaveData healthUpgradesSave = new SaveData(name);

      for (HealthUpgradeManager.HealthUpgrade upgrade : this.healthUpgrades) {
         int currentUpgrade = upgrade.currentUpgrade;
         if (currentUpgrade != 0) {
            healthUpgradesSave.addInt(upgrade.stringID, currentUpgrade);
         }
      }

      save.addSaveData(healthUpgradesSave);
   }

   public void readLoadData(LoadData save, String name, PlayerMob player) {
      LoadData healthUpgradesSave = save.getFirstLoadDataByName(name);
      if (healthUpgradesSave != null) {
         for (String key : this.healthUpgradesIndexes.keySet()) {
            HealthUpgradeManager.HealthUpgrade upgrade = this.getHealthUpgrade(key);
            upgrade.currentUpgrade = healthUpgradesSave.getInt(key, upgrade.currentUpgrade, false);
         }
      } else {
         this.migrateHealthUpgrades(player, player.getMaxHealthFlat());
      }

      this.isDirty = true;
   }

   public void writePacket(PacketWriter writer) {
      for (HealthUpgradeManager.HealthUpgrade healthUpgrade : this.healthUpgrades) {
         writer.putNextShortUnsigned(healthUpgrade.currentUpgrade);
      }
   }

   public void readPacket(PacketReader reader) {
      for (HealthUpgradeManager.HealthUpgrade healthUpgrade : this.healthUpgrades) {
         healthUpgrade.currentUpgrade = reader.getNextShortUnsigned();
      }

      this.isDirty = true;
   }

   public void addUpgrade(String itemStringID, int upgradeIncrement) {
      HealthUpgradeManager.HealthUpgrade healthUpgrade = this.getHealthUpgrade(itemStringID);
      if (healthUpgrade != null) {
         healthUpgrade.currentUpgrade = Math.min(healthUpgrade.maxUpgrades, healthUpgrade.currentUpgrade + upgradeIncrement);
         this.isDirty = true;
      }
   }

   public boolean canUpgrade(String itemStringID) {
      HealthUpgradeManager.HealthUpgrade healthUpgrade = this.getHealthUpgrade(itemStringID);
      return healthUpgrade == null ? false : healthUpgrade.currentUpgrade < healthUpgrade.maxUpgrades;
   }

   public int getCurrentUpgrade(String itemStringID) {
      HealthUpgradeManager.HealthUpgrade healthUpgrade = this.getHealthUpgrade(itemStringID);
      return healthUpgrade == null ? 0 : healthUpgrade.currentUpgrade;
   }

   public int getMaxUpgrades(String itemStringID) {
      HealthUpgradeManager.HealthUpgrade healthUpgrade = this.getHealthUpgrade(itemStringID);
      return healthUpgrade == null ? 0 : healthUpgrade.maxUpgrades;
   }

   public int getCurrentMaxHealthIncrease() {
      if (this.isDirty) {
         this.currentCalculatedHealthUpgrade = 0;

         for (HealthUpgradeManager.HealthUpgrade upgrade : this.healthUpgrades) {
            this.currentCalculatedHealthUpgrade = this.currentCalculatedHealthUpgrade + upgrade.currentUpgrade * upgrade.healthPerUpgrade;
         }

         this.isDirty = false;
      }

      return this.currentCalculatedHealthUpgrade;
   }

   public void migrateHealthUpgrades(PlayerMob player, int flatHealth) {
      for (HealthUpgradeManager.HealthUpgrade healthUpgrade : this.healthUpgrades) {
         if (flatHealth <= 100) {
            break;
         }

         while (healthUpgrade.currentUpgrade < healthUpgrade.maxUpgrades && flatHealth >= 100 + healthUpgrade.healthPerUpgrade) {
            healthUpgrade.currentUpgrade++;
            flatHealth -= healthUpgrade.healthPerUpgrade;
         }
      }

      player.setMaxHealth(flatHealth);
      this.isDirty = true;
   }

   public void resetHealthUpgrades() {
      for (HealthUpgradeManager.HealthUpgrade upgrade : this.healthUpgrades) {
         upgrade.currentUpgrade = 0;
      }

      this.isDirty = true;
   }

   protected static class HealthUpgrade {
      public int currentUpgrade;
      public final int maxUpgrades;
      public final int healthPerUpgrade;
      protected String stringID;
      protected int index;

      public HealthUpgrade(int maxUpgrades, int healthPerUpgrade) {
         this.maxUpgrades = maxUpgrades;
         this.healthPerUpgrade = healthPerUpgrade;
      }
   }
}
