package necesse.engine.quest;

import java.awt.Color;
import java.awt.Rectangle;
import java.util.ArrayList;
import necesse.engine.Settings;
import necesse.engine.achievements.Achievement;
import necesse.engine.localization.Localization;
import necesse.engine.localization.message.GameMessage;
import necesse.engine.network.NetworkClient;
import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.engine.network.server.ServerClient;
import necesse.engine.registries.ItemRegistry;
import necesse.engine.save.LoadData;
import necesse.engine.save.SaveData;
import necesse.engine.save.levelData.ItemSave;
import necesse.engine.util.GameUtils;
import necesse.entity.mobs.Mob;
import necesse.gfx.drawOptions.DrawOptionsBox;
import necesse.gfx.drawOptions.DrawOptionsList;
import necesse.gfx.drawOptions.StringDrawOptions;
import necesse.gfx.gameFont.FontOptions;
import necesse.inventory.item.Item;
import necesse.inventory.item.ObtainTip;
import necesse.inventory.item.questItem.QuestItem;
import necesse.inventory.lootTable.LootTable;
import necesse.level.maps.Level;
import necesse.level.maps.biomes.FishingLootTable;
import necesse.level.maps.biomes.FishingSpot;
import necesse.level.maps.biomes.MobSpawnTable;

public abstract class DeliverItemsQuest extends Quest {
   protected ArrayList<DeliverItemsQuest.ItemObjective> objectives = new ArrayList<>();

   public DeliverItemsQuest() {
   }

   public DeliverItemsQuest(DeliverItemsQuest.ItemObjective firstObjective, DeliverItemsQuest.ItemObjective... extraObjectives) {
      this.objectives = new ArrayList<>();
      if (firstObjective != null) {
         this.objectives.add(firstObjective);
      }

      for (DeliverItemsQuest.ItemObjective extraObjective : extraObjectives) {
         if (extraObjective != null) {
            DeliverItemsQuest.ItemObjective prevItem = this.objectives.stream().filter(o -> o.item == extraObjective.item).findFirst().orElse(null);
            if (prevItem != null) {
               prevItem.itemsAmount = prevItem.itemsAmount + extraObjective.itemsAmount;
            } else {
               this.objectives.add(extraObjective);
            }
         }
      }

      if (this.objectives.isEmpty()) {
         throw new IllegalArgumentException("Must have at least one objective");
      }
   }

   public DeliverItemsQuest(String itemStringID, int itemsAmount) {
      this(new DeliverItemsQuest.ItemObjective(itemStringID, itemsAmount));
   }

   @Override
   public void addSaveData(SaveData save) {
      super.addSaveData(save);

      for (DeliverItemsQuest.ItemObjective objective : this.objectives) {
         SaveData objectiveData = new SaveData("objective");
         objectiveData.addUnsafeString("itemStringID", objective.item.getStringID());
         objectiveData.addInt("itemsAmount", objective.itemsAmount);
         save.addSaveData(objectiveData);
      }
   }

   @Override
   public void applyLoadData(LoadData save) {
      super.applyLoadData(save);
      this.objectives.clear();
      String itemStringID = save.getUnsafeString("itemStringID", null, false);
      if (itemStringID != null) {
         Item item = ItemSave.loadItem(itemStringID);
         if (item == null) {
            throw new IllegalArgumentException("Could not find item with id " + itemStringID);
         }

         int itemsAmount = save.getInt("itemsAmount", 1);
         this.objectives.add(new DeliverItemsQuest.ItemObjective(itemStringID, itemsAmount));
      }

      for (LoadData objectiveData : save.getLoadDataByName("objective")) {
         String itemStringIDx = objectiveData.getUnsafeString("itemStringID", null, false);
         if (itemStringIDx != null) {
            Item item = ItemSave.loadItem(itemStringIDx);
            if (item == null) {
               throw new IllegalArgumentException("Could not find item with id " + itemStringIDx);
            }

            int itemsAmount = objectiveData.getInt("itemsAmount", 1);
            this.objectives.add(new DeliverItemsQuest.ItemObjective(itemStringIDx, itemsAmount));
         }
      }

      if (this.objectives.isEmpty()) {
         throw new IllegalStateException("Could not find any objectives");
      }
   }

   @Override
   public void setupSpawnPacket(PacketWriter writer) {
      super.setupSpawnPacket(writer);
      writer.putNextShortUnsigned(this.objectives.size());

      for (DeliverItemsQuest.ItemObjective objective : this.objectives) {
         writer.putNextShortUnsigned(objective.item.getID());
         writer.putNextInt(objective.itemsAmount);
      }
   }

   @Override
   public void applySpawnPacket(PacketReader reader) {
      super.applySpawnPacket(reader);
      this.objectives.clear();
      int total = reader.getNextShortUnsigned();

      for (int i = 0; i < total; i++) {
         int itemID = reader.getNextShortUnsigned();
         int itemsAmount = reader.getNextInt();
         this.objectives.add(new DeliverItemsQuest.ItemObjective(itemID, itemsAmount));
      }
   }

   @Override
   public void tick(ServerClient client) {
   }

   @Override
   public boolean canComplete(NetworkClient client) {
      if (client.playerMob != null) {
         for (DeliverItemsQuest.ItemObjective objective : this.objectives) {
            if (objective.itemsAmount > client.playerMob.getInv().main.getAmount(client.playerMob.getLevel(), client.playerMob, objective.item, "deliverquest")
               )
             {
               return false;
            }
         }

         return true;
      } else {
         return false;
      }
   }

   @Override
   public void complete(ServerClient client) {
      super.complete(client);

      for (DeliverItemsQuest.ItemObjective objective : this.objectives) {
         client.playerMob.getInv().main.removeItems(client.playerMob.getLevel(), client.playerMob, objective.item, objective.itemsAmount, "deliverquest");
      }
   }

   @Override
   public GameMessage getDescription() {
      return null;
   }

   @Override
   public DrawOptionsBox getProgressDrawBox(NetworkClient client, final int x, final int y, final int width, Color textColor, boolean outlined) {
      final DrawOptionsList drawOptions = new DrawOptionsList();
      int currentHeight = 0;

      for (DeliverItemsQuest.ItemObjective objective : this.objectives) {
         int currentItems = 0;
         if (client != null) {
            currentItems = Math.min(
               objective.itemsAmount, client.playerMob.getInv().main.getAmount(client.playerMob.getLevel(), client.playerMob, objective.item, "deliverquest")
            );
         }

         float progress = objective.itemsAmount == 0 ? 1.0F : (float)currentItems / objective.itemsAmount;
         String deliverStr = Localization.translate("quests", "deliveritem", "item", ItemRegistry.getDisplayName(objective.item.getID()));
         FontOptions deliverFontOptions = new FontOptions(16).outline(outlined);
         if (textColor != null) {
            deliverFontOptions.color(textColor);
         }

         drawOptions.add(new StringDrawOptions(deliverFontOptions, deliverStr).pos(x, y + currentHeight));
         currentHeight += 16;
         if (objective.item instanceof ObtainTip) {
            GameMessage obtainTip = ((ObtainTip)objective.item).getObtainTip();
            if (obtainTip != null) {
               FontOptions obtainFontOptions = new FontOptions(12).outline(outlined);
               if (textColor != null) {
                  obtainFontOptions.color(textColor);
               }

               String obtainStr = obtainTip.translate();

               for (String line : GameUtils.breakString(obtainStr, obtainFontOptions, width)) {
                  drawOptions.add(new StringDrawOptions(obtainFontOptions, line).pos(x, y + currentHeight + 2));
                  currentHeight += 12;
               }

               currentHeight += 2;
            }
         }

         Color col = progress == 1.0F ? Settings.UI.successTextColor : Settings.UI.errorTextColor;
         FontOptions progressFontOptions = new FontOptions(16).outline(outlined).color(col);
         DrawOptionsBox progressBox = Achievement.getProgressbarTextDrawBox(
            x,
            y + currentHeight,
            width,
            5,
            progress,
            Settings.UI.progressBarOutline,
            Settings.UI.progressBarFill,
            currentItems + "/" + objective.itemsAmount,
            progressFontOptions
         );
         drawOptions.add(progressBox);
         currentHeight += progressBox.getBoundingBox().height;
      }

      final int finalHeight = currentHeight;
      return new DrawOptionsBox() {
         @Override
         public Rectangle getBoundingBox() {
            return new Rectangle(x, y, width, finalHeight);
         }

         @Override
         public void draw() {
            drawOptions.draw();
         }
      };
   }

   @Override
   public MobSpawnTable getExtraCritterSpawnTable(ServerClient client, Level level) {
      for (DeliverItemsQuest.ItemObjective objective : this.objectives) {
         if (objective.item instanceof QuestItem) {
            return ((QuestItem)objective.item).getExtraCritterSpawnTable(client, level);
         }
      }

      return super.getExtraCritterSpawnTable(client, level);
   }

   @Override
   public MobSpawnTable getExtraMobSpawnTable(ServerClient client, Level level) {
      for (DeliverItemsQuest.ItemObjective objective : this.objectives) {
         if (objective.item instanceof QuestItem) {
            return ((QuestItem)objective.item).getExtraMobSpawnTable(client, level);
         }
      }

      return super.getExtraMobSpawnTable(client, level);
   }

   @Override
   public FishingLootTable getExtraFishingLoot(ServerClient client, FishingSpot spot) {
      for (DeliverItemsQuest.ItemObjective objective : this.objectives) {
         if (objective.item instanceof QuestItem) {
            return ((QuestItem)objective.item).getExtraFishingLoot(client, spot);
         }
      }

      return super.getExtraFishingLoot(client, spot);
   }

   @Override
   public LootTable getExtraMobDrops(ServerClient client, Mob mob) {
      for (DeliverItemsQuest.ItemObjective objective : this.objectives) {
         if (objective.item instanceof QuestItem) {
            return ((QuestItem)objective.item).getExtraMobDrops(client, mob);
         }
      }

      return super.getExtraMobDrops(client, mob);
   }

   public static class ItemObjective {
      protected Item item;
      protected int itemsAmount;

      public ItemObjective(int itemID, int itemsAmount) {
         this.item = ItemRegistry.getItem(itemID);
         if (this.item == null) {
            throw new IllegalArgumentException("Could not find item with id " + itemID);
         }

         this.itemsAmount = itemsAmount;
      }

      public ItemObjective(String itemStringID, int itemsAmount) {
         this.item = ItemRegistry.getItem(itemStringID);
         if (this.item == null) {
            throw new IllegalArgumentException("Could not find item with id " + itemStringID);
         }

         this.itemsAmount = itemsAmount;
      }
   }
}
