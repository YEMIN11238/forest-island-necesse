package necesse.level.maps.levelData.settlementData;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.function.Supplier;
import necesse.engine.util.GameMath;
import necesse.engine.util.GameRandom;
import necesse.engine.util.TicketSystemList;

public class SettlementRaidLoadoutGenerator {
   public static ArrayList<SettlementRaidLoadout.Weapon> ALL_WEAPONS = new ArrayList<>();
   public static ArrayList<SettlementRaidLoadout.ArmorSet> ALL_ARMOR_SETS = new ArrayList<>();
   public static float MIN_POWER_CONSISTENCY_MODIFIER = 2.0F;
   public static float MAX_POWER_CONSISTENCY_MODIFIER = 10.0F;
   protected ArrayList<SettlementRaidLoadout.Weapon> weapons = ALL_WEAPONS;
   protected ArrayList<SettlementRaidLoadout.ArmorSet> armorSets = ALL_ARMOR_SETS;
   protected double maxWeaponValue = Double.MAX_VALUE;
   protected double maxArmorValue = Double.MAX_VALUE;
   protected double equipmentVariationModifier = MIN_POWER_CONSISTENCY_MODIFIER;
   protected HashSet<String> obtainedItems = null;
   protected int maxWeaponsInPool = 20;
   protected boolean maxWeaponsInPoolOnlyBelowMaxValue = true;
   protected int maxWeaponsValueOffset = 1000;
   protected int maxArmorsInPool = 20;
   protected boolean maxArmorsInPoolOnlyBelowMaxValue = true;
   protected int maxArmorsValueOffset = 1000;

   protected SettlementRaidLoadoutGenerator() {
   }

   public static SettlementRaidLoadoutGenerator init() {
      return new SettlementRaidLoadoutGenerator();
   }

   public SettlementRaidLoadoutGenerator weapons(ArrayList<SettlementRaidLoadout.Weapon> weapons) {
      this.weapons = weapons;
      return this;
   }

   public SettlementRaidLoadoutGenerator armorSets(ArrayList<SettlementRaidLoadout.ArmorSet> armorSets) {
      this.armorSets = armorSets;
      return this;
   }

   public SettlementRaidLoadoutGenerator maxWeaponValue(double maxWeaponValue) {
      this.maxWeaponValue = maxWeaponValue;
      return this;
   }

   public SettlementRaidLoadoutGenerator maxArmorValue(double maxArmorValue) {
      this.maxArmorValue = maxArmorValue;
      return this;
   }

   public SettlementRaidLoadoutGenerator equipmentVariationModifier(double equipmentVariationModifier) {
      this.equipmentVariationModifier = equipmentVariationModifier;
      return this;
   }

   public SettlementRaidLoadoutGenerator equipmentVariationCoefficient(double equipmentVariationCoefficient) {
      if (Double.isNaN(equipmentVariationCoefficient)) {
         equipmentVariationCoefficient = 0.0;
      }

      double consistencyModifier = GameMath.lerp(
         GameMath.limit(equipmentVariationCoefficient, 0.0, 1.0), MIN_POWER_CONSISTENCY_MODIFIER, MAX_POWER_CONSISTENCY_MODIFIER
      );
      return this.equipmentVariationModifier(consistencyModifier);
   }

   public SettlementRaidLoadoutGenerator obtainedItems(HashSet<String> obtainedItems) {
      this.obtainedItems = obtainedItems;
      return this;
   }

   public SettlementRaidLoadoutGenerator maxWeaponsInPool(int max) {
      this.maxWeaponsInPool = max;
      return this;
   }

   public SettlementRaidLoadoutGenerator maxWeaponsInPool(int max, boolean maxWeaponsInPoolOnlyBelowMaxValue) {
      this.maxWeaponsInPool = max;
      this.maxWeaponsInPoolOnlyBelowMaxValue = maxWeaponsInPoolOnlyBelowMaxValue;
      return this;
   }

   public SettlementRaidLoadoutGenerator maxWeaponsValueOffset(int max) {
      this.maxWeaponsValueOffset = max;
      return this;
   }

   public SettlementRaidLoadoutGenerator maxArmorsInPool(int max) {
      this.maxArmorsInPool = max;
      return this;
   }

   public SettlementRaidLoadoutGenerator maxArmorsInPool(int max, boolean maxArmorsInPoolOnlyBelowMaxValue) {
      this.maxArmorsInPool = max;
      this.maxArmorsInPoolOnlyBelowMaxValue = maxArmorsInPoolOnlyBelowMaxValue;
      return this;
   }

   public SettlementRaidLoadoutGenerator maxArmorsValueOffset(int max) {
      this.maxArmorsValueOffset = max;
      return this;
   }

   public ArrayList<SettlementRaidLoadout> generateLoadouts(GameRandom random, double totalValue, int maxLoadouts, String mobStringID) {
      return this.generateLoadouts(random, totalValue, maxLoadouts, () -> new SettlementRaidLoadout(mobStringID));
   }

   public ArrayList<SettlementRaidLoadout> generateLoadouts(
      GameRandom random, double totalValue, int maxLoadouts, Supplier<SettlementRaidLoadout> loadoutSupplier
   ) {
      double maxWeaponValue = Math.max(this.maxWeaponValue, 100.0);
      double maxArmorValue = Math.max(this.maxArmorValue, 100.0);
      double tempMaxWeaponValue = maxWeaponValue;
      if (maxArmorValue != Double.MAX_VALUE && maxWeaponValue < maxArmorValue / 2.0) {
         maxWeaponValue = maxArmorValue / 2.0;
      }

      if (tempMaxWeaponValue != Double.MAX_VALUE && maxArmorValue < tempMaxWeaponValue / 2.0) {
         maxArmorValue = tempMaxWeaponValue / 2.0;
      }

      ArrayList<SettlementRaidLoadout.Weapon> validWeapons = this.getValidWeapons(maxWeaponValue);
      ArrayList<SettlementRaidLoadout.ArmorSet> validArmorSets = this.getValidArmorSets(maxArmorValue);
      validWeapons.sort(Comparator.comparingDouble(SettlementRaidLoadout.Weapon::getValue).reversed());
      validArmorSets.sort(Comparator.comparingDouble(SettlementRaidLoadout.ArmorSet::getValue).reversed());
      maxWeaponValue = validWeapons.get(0).getValue();
      maxArmorValue = validArmorSets.get(0).getValue();
      TicketSystemList<SettlementRaidLoadout.Weapon> weaponTickets = new TicketSystemList<>();
      int weaponCounter = 0;
      double secondaryMaxWeaponValue = 0.0;

      for (SettlementRaidLoadout.Weapon weapon : validWeapons) {
         double value = weapon.getValue();
         if (maxWeaponValue - value > this.maxWeaponsValueOffset && weaponCounter > 5) {
            break;
         }

         int tickets = weapon.getTickets(this.equipmentVariationModifier, this.obtainedItems);
         if (tickets > 0) {
            weaponTickets.addObject(tickets, weapon);
            if (!this.maxWeaponsInPoolOnlyBelowMaxValue || value < maxWeaponValue) {
               weaponCounter++;
               secondaryMaxWeaponValue = Math.max(secondaryMaxWeaponValue, value);
            }

            if (weaponCounter >= this.maxWeaponsInPool && value < secondaryMaxWeaponValue) {
               break;
            }
         }
      }

      TicketSystemList<SettlementRaidLoadout.ArmorSet> armorTickets = new TicketSystemList<>();
      int armorCounter = 0;
      double secondaryMaxArmorValue = 0.0;

      for (SettlementRaidLoadout.ArmorSet armorSet : validArmorSets) {
         double value = armorSet.getValue();
         if (maxArmorValue - value > this.maxArmorsValueOffset && armorCounter > 3) {
            break;
         }

         int tickets = armorSet.getTickets(this.equipmentVariationModifier, this.obtainedItems);
         if (tickets > 0) {
            armorTickets.addObject(tickets, armorSet);
            if (!this.maxArmorsInPoolOnlyBelowMaxValue || value < maxArmorValue) {
               armorCounter++;
               secondaryMaxArmorValue = Math.max(secondaryMaxArmorValue, value);
            }

            if (armorCounter >= this.maxArmorsInPool && value < secondaryMaxArmorValue) {
               break;
            }
         }
      }

      boolean shouldRegenerate = false;
      if (weaponTickets.isEmpty()) {
         System.out.println("No valid weapons in raid loadout pool");
         if (this.weapons == ALL_WEAPONS) {
            weaponTickets.addObject(100, new SettlementRaidLoadout.Weapon("woodsword"));
            weaponTickets.addObject(100, new SettlementRaidLoadout.Weapon("woodbow"));
         } else {
            this.weapons = ALL_WEAPONS;
            shouldRegenerate = true;
         }
      }

      if (armorTickets.isEmpty()) {
         System.out.println("No valid armor sets in raid loadout pool");
         if (this.armorSets == ALL_ARMOR_SETS) {
            armorTickets.addObject(100, new SettlementRaidLoadout.ArmorSet("leatherhood", "leathershirt", "leatherboots"));
         } else {
            this.armorSets = ALL_ARMOR_SETS;
            shouldRegenerate = true;
         }
      }

      if (shouldRegenerate) {
         return this.generateLoadouts(random, totalValue, maxLoadouts, loadoutSupplier);
      }

      ArrayList<SettlementRaidLoadout> loadoutCollection = new ArrayList<>();
      double remainingValue = totalValue;

      while (remainingValue > 0.0 && loadoutCollection.size() < maxLoadouts) {
         SettlementRaidLoadout.Weapon weapon = weaponTickets.getRandomObject(random);
         SettlementRaidLoadout.ArmorSet armorSet = armorTickets.getRandomObject(random);
         remainingValue -= weapon.getValue();
         remainingValue -= armorSet.getValue();
         SettlementRaidLoadout loadout = loadoutSupplier.get();
         loadout.setWeapon(weapon);
         loadout.setArmorSet(armorSet);
         loadoutCollection.add(loadout);
      }

      return loadoutCollection;
   }

   protected ArrayList<SettlementRaidLoadout.Weapon> getValidWeapons(double maxWeaponValue) {
      ArrayList<SettlementRaidLoadout.Weapon> validWeapons = new ArrayList<>();

      for (SettlementRaidLoadout.Weapon weapon : this.weapons) {
         if (weapon.getValue() <= maxWeaponValue) {
            validWeapons.add(weapon);
         }
      }

      if (validWeapons.size() < 5) {
         int limit = 5 - validWeapons.size();
         this.weapons
            .stream()
            .sorted(Comparator.comparingDouble(SettlementRaidLoadout.Weapon::getValue))
            .skip(validWeapons.size())
            .limit(limit)
            .forEach(validWeapons::add);
      }

      return validWeapons;
   }

   protected ArrayList<SettlementRaidLoadout.ArmorSet> getValidArmorSets(double maxArmorValue) {
      ArrayList<SettlementRaidLoadout.ArmorSet> validArmorSets = new ArrayList<>();

      for (SettlementRaidLoadout.ArmorSet armorSet : this.armorSets) {
         if (armorSet.getValue() <= maxArmorValue) {
            validArmorSets.add(armorSet);
         }
      }

      if (validArmorSets.size() < 3) {
         int limit = 3 - validArmorSets.size();
         this.armorSets
            .stream()
            .sorted(Comparator.comparingDouble(SettlementRaidLoadout.ArmorSet::getValue))
            .skip(validArmorSets.size())
            .limit(limit)
            .forEach(validArmorSets::add);
      }

      return validArmorSets;
   }
}
