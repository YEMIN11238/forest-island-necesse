package necesse.level.maps.incursion;

import java.util.HashSet;
import java.util.stream.Collectors;
import necesse.engine.GameLog;
import necesse.engine.incursionPerkTree.IncursionPerk;
import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.engine.playerStats.EmptyStats;
import necesse.engine.registries.IncursionPerksRegistry;
import necesse.engine.save.LoadData;
import necesse.engine.save.SaveData;

public class AltarData {
   public HashSet<Integer> obtainedPerkIDs = new HashSet<>();
   public HashSet<Integer> tabletsRewardedFromPerksIDs = new HashSet<>();
   public AltarStats altarStats = new AltarStats(EmptyStats.Mode.READ_AND_WRITE);

   public void addSaveData(SaveData save) {
      HashSet<String> obtainedPerksStringIDs = this.obtainedPerkIDs
         .stream()
         .map(IncursionPerksRegistry::getPerkStringID)
         .collect(Collectors.toCollection(HashSet::new));
      save.addStringHashSet("obtainedPerks", obtainedPerksStringIDs);
      HashSet<String> tabletsRewardedIDs = this.tabletsRewardedFromPerksIDs
         .stream()
         .map(IncursionPerksRegistry::getPerkStringID)
         .collect(Collectors.toCollection(HashSet::new));
      save.addStringHashSet("tabletsRewardedFromPerks", tabletsRewardedIDs);
      SaveData statsSave = new SaveData("stats");
      this.altarStats.addSaveData(statsSave);
      save.addSaveData(statsSave);
   }

   public void applyLoadData(LoadData save) {
      HashSet<String> obtainedPerksStringIDs = save.getStringHashSet("obtainedPerks", null);
      if (obtainedPerksStringIDs != null) {
         for (String stringID : obtainedPerksStringIDs) {
            int perkID = IncursionPerksRegistry.getPerkID(stringID);
            if (perkID != -1) {
               this.obtainedPerkIDs.add(perkID);
            } else {
               GameLog.warn.println("Could not load incursion perk with string id: " + stringID);
            }
         }
      }

      HashSet<String> tabletsRewardedIDs = save.getStringHashSet("tabletsRewardedFromPerks", null, false);
      if (tabletsRewardedIDs != null) {
         for (String stringID : tabletsRewardedIDs) {
            int perkID = IncursionPerksRegistry.getPerkID(stringID);
            if (perkID != -1) {
               this.tabletsRewardedFromPerksIDs.add(perkID);
            } else {
               GameLog.warn.println("Could not load incursion perk with string id: " + stringID);
            }
         }
      }

      LoadData statsSave = save.getFirstLoadDataByName("stats");
      if (statsSave != null) {
         this.altarStats.applyLoadData(statsSave);
      }
   }

   public void writePacket(PacketWriter writer) {
      writer.putNextInt(this.obtainedPerkIDs.size());

      for (Integer obtainedPerkID : this.obtainedPerkIDs) {
         writer.putNextInt(obtainedPerkID);
      }

      writer.putNextInt(this.tabletsRewardedFromPerksIDs.size());

      for (Integer tabletsRewardedFromPerkID : this.tabletsRewardedFromPerksIDs) {
         writer.putNextInt(tabletsRewardedFromPerkID);
      }

      this.altarStats.setupContentPacket(writer);
   }

   public void applyPacket(PacketReader reader) {
      this.obtainedPerkIDs.clear();
      int obtainedPerksSize = reader.getNextInt();

      for (int i = 0; i < obtainedPerksSize; i++) {
         this.obtainedPerkIDs.add(reader.getNextInt());
      }

      this.tabletsRewardedFromPerksIDs.clear();
      int tabletsRewardedSize = reader.getNextInt();

      for (int i = 0; i < tabletsRewardedSize; i++) {
         this.tabletsRewardedFromPerksIDs.add(reader.getNextInt());
      }

      this.altarStats.applyContentPacket(reader);
   }

   public void init() {
   }

   public String getDebugString() {
      return "to-do add string";
   }

   public void obtainPerk(IncursionPerk perk) {
      this.obtainedPerkIDs.add(perk.getID());
   }

   public void removePerk(IncursionPerk perk) {
      this.obtainedPerkIDs.remove(perk.getID());
   }

   public boolean hasPerk(IncursionPerk perk) {
      return this.obtainedPerkIDs.contains(perk.getID());
   }

   public void addPerkIDToTabletsRewardedList(IncursionPerk perk) {
      this.tabletsRewardedFromPerksIDs.add(perk.getID());
   }

   public boolean tabletHasBeenRewardedAlready(IncursionPerk perk) {
      return this.tabletsRewardedFromPerksIDs.contains(perk.getID());
   }

   public int getAltarTier() {
      int tier = IncursionData.MINIMUM_TIER - 1;

      for (Integer obtainedPerkID : this.obtainedPerkIDs) {
         int currentPerkTier = IncursionPerksRegistry.getPerk(obtainedPerkID).tier;
         if (currentPerkTier > tier) {
            tier = currentPerkTier;
         }
      }

      return Math.min(tier, IncursionData.TABLET_TIER_UPGRADE_CAP);
   }

   public int getAltarTierAfterPerkRespec(IncursionPerk perkBeingRespecced) {
      int tier = IncursionData.MINIMUM_TIER - 1;

      for (Integer obtainedPerkID : this.obtainedPerkIDs) {
         if (IncursionPerksRegistry.getPerk(obtainedPerkID) != perkBeingRespecced) {
            int currentPerkTier = IncursionPerksRegistry.getPerk(obtainedPerkID).tier;
            if (currentPerkTier > tier) {
               tier = currentPerkTier;
            }
         }
      }

      return Math.min(tier, IncursionData.TABLET_TIER_UPGRADE_CAP);
   }

   public boolean areTherePerksLeftAfterRespecOnThisTier(IncursionPerk perkBeingRespecced, int tier) {
      for (Integer obtainedPerkID : this.obtainedPerkIDs) {
         IncursionPerk perk = IncursionPerksRegistry.getPerk(obtainedPerkID);
         if (perkBeingRespecced != perk && IncursionPerksRegistry.getPerksAtTier(tier).contains(perk)) {
            return true;
         }
      }

      return false;
   }

   public void addCompletedIncursion(IncursionData data) {
      if (data instanceof BiomeMissionIncursionData) {
         this.altarStats.completed_incursions.add((BiomeMissionIncursionData)data);
      }
   }

   public int getCompletedIncursionsAtSpecificTierOrHigher(int tier) {
      return this.altarStats.completed_incursions.getTotalTiersAbove(tier, true);
   }

   public boolean checkForSpecificIncursionCompletedAtSpecificTierOrAbove(int incursionBiomeID, int tier) {
      return this.altarStats.completed_incursions.getData(incursionBiomeID).getTotalTiersAbove(tier, true) > 0;
   }

   public boolean checkForAmountOfIncursionsCompletedAtSpecificTierOrAbove(int tier, int amount) {
      return this.altarStats.completed_incursions.getTotalTiersAbove(tier, true) >= amount;
   }

   public boolean isSameAltarData(AltarData other) {
      return other.obtainedPerkIDs == this.obtainedPerkIDs;
   }

   public AltarData makeCopy() {
      AltarData altarData = new AltarData();
      altarData.obtainedPerkIDs = this.obtainedPerkIDs;
      return altarData;
   }
}
