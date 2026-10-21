package necesse.level.maps.levelData.settlementData;

import java.awt.Point;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import necesse.inventory.InventoryItem;
import necesse.inventory.item.ItemAttackerWeaponItem;
import necesse.inventory.item.armorItem.ArmorItem;

public class SettlementWealthCounter {
   protected SettlementWealthCounter.Counter total = new SettlementWealthCounter.Counter(false);
   protected SettlementWealthCounter.Counter storage = new SettlementWealthCounter.Counter(false);
   protected SettlementWealthCounter.Counter settlers = new SettlementWealthCounter.Counter(true);
   protected SettlementWealthCounter.Counter players = new SettlementWealthCounter.Counter(false);
   protected HashSet<Point> storageTiles = new HashSet<>();

   public double getTotalWealth() {
      return this.total.totalWealth;
   }

   public double getBestArmorValue() {
      return this.total.bestArmorValue;
   }

   public double getBestWeaponValue() {
      return this.total.bestWeaponValue;
   }

   public double getTotalEquippedArmorValue() {
      return this.settlers.totalArmorValue + this.players.totalArmorValue;
   }

   public double getTotalEquippedWeaponValue() {
      return this.settlers.totalWeaponValue + this.players.totalWeaponValue;
   }

   public HashSet<Point> getStorageTiles() {
      return this.storageTiles;
   }

   public void addStoredItem(int tileX, int tileY, InventoryItem invItem) {
      if (tileX != Integer.MIN_VALUE && tileY != Integer.MIN_VALUE) {
         this.storageTiles.add(new Point(tileX, tileY));
      }

      this.addItem(invItem, this.total, this.storage);
   }

   public void addSettlerItem(InventoryItem invItem) {
      this.addItem(invItem, this.total, this.settlers);
   }

   public void addPlayerItem(InventoryItem invItem) {
      this.addItem(invItem, this.total, this.players);
   }

   protected void addItem(InventoryItem invItem, SettlementWealthCounter.Counter... counters) {
      if (invItem != null) {
         for (SettlementWealthCounter.Counter counter : counters) {
            counter.totalWealth = counter.totalWealth + invItem.getBrokerValue();
            counter.totalItems = counter.totalItems + invItem.getAmount();
         }

         if (invItem.item.isArmorItem()) {
            ArmorItem armorItem = (ArmorItem)invItem.item;
            if (!armorItem.isCosmetic) {
               float flatValue = armorItem.getSettlerEquipmentValueFlat(invItem);
               float totalValue = armorItem.getSettlerEquipmentValue(invItem, null);

               for (SettlementWealthCounter.Counter counter : counters) {
                  if (counter.armorValues != null) {
                     counter.armorValues.add(totalValue);
                  }

                  counter.totalArmorValue += totalValue;
                  if (counter.bestArmorValue < flatValue) {
                     counter.bestArmorValue = flatValue;
                     counter.bestArmorItem = invItem;
                  }

                  counter.totalArmorCount++;
               }
            }
         }

         if (invItem.item instanceof ItemAttackerWeaponItem) {
            ItemAttackerWeaponItem weaponItem = (ItemAttackerWeaponItem)invItem.item;
            float flatValue = weaponItem.getItemAttackerWeaponValueFlat(invItem);
            float totalValue = weaponItem.getItemAttackerWeaponValue(null, invItem);

            for (SettlementWealthCounter.Counter counter : counters) {
               if (counter.weaponValues != null) {
                  counter.weaponValues.add(totalValue);
               }

               counter.totalWeaponValue += totalValue;
               if (counter.bestWeaponValue < flatValue) {
                  counter.bestWeaponValue = flatValue;
                  counter.bestWeaponItem = invItem;
               }

               counter.totalWeaponCount++;
            }
         }
      }
   }

   public double getUnequippedWealth() {
      return this.getTotalWealth() - this.getEquippedWealth();
   }

   public double getEquippedWealth() {
      return this.getTotalEquippedArmorValue() + this.getTotalEquippedWeaponValue();
   }

   public double getSettlersGearCoefficientOfVariation() {
      return (this.settlers.getArmorValuesCV() + this.settlers.getWeaponValuesCV()) / 2.0;
   }

   public void printDebug() {
      this.total.printDebug("Total");
      this.storage.printDebug("Storage");
      this.settlers.printDebug("Settlers");
      this.players.printDebug("Players");
   }

   protected static class Counter {
      protected double totalWealth;
      protected int totalItems;
      protected ArrayList<Float> armorValues = null;
      protected double totalArmorValue;
      protected double bestArmorValue;
      protected InventoryItem bestArmorItem;
      protected int totalArmorCount;
      protected ArrayList<Float> weaponValues = null;
      protected double totalWeaponValue;
      protected double bestWeaponValue;
      protected InventoryItem bestWeaponItem;
      protected int totalWeaponCount;

      public Counter(boolean canCalculateDeviation) {
         if (canCalculateDeviation) {
            this.armorValues = new ArrayList<>();
            this.weaponValues = new ArrayList<>();
         }
      }

      private double getStandardDeviation(ArrayList<Float> values, double sum) {
         if (values == null) {
            return Double.NaN;
         }

         double mean = sum / values.size();
         double variance = 0.0;
         Iterator var8 = values.iterator();

         while (var8.hasNext()) {
            double value = ((Float)var8.next()).floatValue();
            variance += Math.pow(value - mean, 2.0);
         }

         variance /= values.size();
         return Math.sqrt(variance);
      }

      public double getArmorValuesStandardDeviation() {
         return this.getStandardDeviation(this.armorValues, this.totalArmorValue);
      }

      public double getWeaponValuesStandardDeviation() {
         return this.getStandardDeviation(this.weaponValues, this.totalWeaponValue);
      }

      public double getArmorValuesCV() {
         return this.armorValues == null ? Double.NaN : this.getArmorValuesStandardDeviation() / (this.totalArmorValue / this.armorValues.size());
      }

      public double getWeaponValuesCV() {
         return this.weaponValues == null ? Double.NaN : this.getWeaponValuesStandardDeviation() / (this.totalWeaponValue / this.weaponValues.size());
      }

      public void printDebug(String prefix) {
         System.out.println(prefix + " wealth: " + (int)this.totalWealth + " (" + this.totalItems + " items)");
         System.out
            .println(
               prefix
                  + " armor value: "
                  + (int)this.totalArmorValue
                  + " ("
                  + this.totalArmorCount
                  + "), Best: "
                  + (int)this.bestArmorValue
                  + " ("
                  + this.bestArmorItem
                  + "), CV: "
                  + this.getArmorValuesCV()
            );
         System.out
            .println(
               prefix
                  + " weapon value: "
                  + (int)this.totalWeaponValue
                  + " ("
                  + this.totalWeaponCount
                  + ") Best: "
                  + (int)this.bestWeaponValue
                  + " ("
                  + this.bestWeaponItem
                  + "), CV: "
                  + this.getWeaponValuesCV()
            );
      }
   }
}
