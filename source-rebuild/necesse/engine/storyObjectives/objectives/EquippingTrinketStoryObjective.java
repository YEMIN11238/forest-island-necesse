package necesse.engine.storyObjectives.objectives;

import necesse.engine.GlobalData;
import necesse.engine.input.Control;
import necesse.engine.localization.message.LocalMessage;
import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.engine.network.gameNetworkData.GNDItemMap;
import necesse.engine.registries.BuffRegistry;
import necesse.engine.registries.ItemRegistry;
import necesse.engine.save.LoadData;
import necesse.engine.save.SaveData;
import necesse.engine.state.MainGame;
import necesse.engine.storyObjectives.StoryObjective;
import necesse.engine.storyObjectives.StoryObjectiveManager;
import necesse.engine.util.GameMath;
import necesse.entity.mobs.PlayerMob;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.DrawOptions;
import necesse.gfx.fairType.FairTypeDrawOptions;
import necesse.gfx.fairType.TypeParsers;
import necesse.gfx.forms.components.FormFlow;
import necesse.gfx.forms.components.FormStoryObjectiveComponent;
import necesse.gfx.forms.components.containerSlot.FormContainerSlot;
import necesse.inventory.InventoryItem;
import necesse.inventory.lootTable.LootTable;
import necesse.inventory.lootTable.lootItem.LootItem;

public class EquippingTrinketStoryObjective extends StoryObjective {
   public static LootTable reward = new LootTable(new LootItem("woodboomerang", new GNDItemMap().clearItem("enchantment")));
   public static int SECONDS_TO_USE_LEATHER_DASHERS = 2;
   public boolean hasTrinket;
   public boolean hasEquippedTrinket;
   public float leatherDashersUseBuffer;

   public EquippingTrinketStoryObjective(StoryObjectiveManager manager) {
      super(manager);
   }

   @Override
   public void addSaveData(SaveData save) {
      super.addSaveData(save);
      if (this.hasTrinket) {
         save.addBoolean("hasTrinket", this.hasTrinket);
      }

      if (this.hasEquippedTrinket) {
         save.addBoolean("hasEquippedTrinket", this.hasEquippedTrinket);
      }
   }

   @Override
   public void applyLoadData(LoadData save) {
      super.applyLoadData(save);
      this.hasTrinket = save.getBoolean("hasTrinket", this.hasTrinket, false);
      this.hasEquippedTrinket = save.getBoolean("hasEquippedTrinket", this.hasEquippedTrinket, false);
   }

   @Override
   public void writePacket(PacketWriter writer) {
      super.writePacket(writer);
      writer.putNextBoolean(this.hasTrinket);
      writer.putNextBoolean(this.hasEquippedTrinket);
      writer.putNextFloat(this.leatherDashersUseBuffer);
   }

   @Override
   public void readPacket(PacketReader reader) {
      super.readPacket(reader);
      this.hasTrinket = reader.getNextBoolean();
      this.hasEquippedTrinket = reader.getNextBoolean();
      this.leatherDashersUseBuffer = reader.getNextFloat();
   }

   @Override
   public void serverTickCurrentObjective() {
      super.serverTickCurrentObjective();
      int leatherDashersID = ItemRegistry.getItemID("leatherdashers");
      if (!this.hasTrinket) {
         boolean hasLeatherDashersTrinket = this.getPlayer()
            .getInv()
            .hasAnyItem(false, false, false, false, "tutorial", item -> item.item.getID() == leatherDashersID);
         if (hasLeatherDashersTrinket) {
            this.hasTrinket = true;
            this.markDirty();
         }
      } else if (!this.hasEquippedTrinket) {
         InventoryItem trinketItem = this.getPlayer().getInv().equipment.getSelectedEquipmentSlot(1).getItem();
         if (trinketItem != null) {
            boolean hasEquippedLeatherDashers = trinketItem.item.getID() == leatherDashersID;
            if (hasEquippedLeatherDashers) {
               this.hasEquippedTrinket = true;
               this.markDirty();
            }
         }
      } else if (!this.isCompleted()) {
         PlayerMob player = this.getPlayer();
         if (player.buffManager.hasBuff(BuffRegistry.LEATHER_DASHERS_ACTIVE) && (player.dx != 0.0F || player.dy != 0.0F)) {
            this.leatherDashersUseBuffer += 50.0F;
            if (this.leatherDashersUseBuffer >= SECONDS_TO_USE_LEATHER_DASHERS * 1000) {
               this.markCompleted();
            }
         } else {
            this.leatherDashersUseBuffer = Math.max(0.0F, this.leatherDashersUseBuffer - 50.0F);
         }
      }
   }

   @Override
   public void clientTickCurrentObjective() {
      super.clientTickCurrentObjective();
      if (this.hasTrinket && this.hasEquippedTrinket && !this.isCompleted()) {
         PlayerMob player = this.getPlayer();
         if (!player.buffManager.hasBuff(BuffRegistry.LEATHER_DASHERS_ACTIVE) || player.dx == 0.0F && player.dy == 0.0F) {
            this.leatherDashersUseBuffer = Math.max(0.0F, this.leatherDashersUseBuffer - 50.0F);
         } else {
            this.leatherDashersUseBuffer += 50.0F;
         }
      }
   }

   @Override
   public DrawOptions getCurrentObjectiveLevelHudDrawOptions(GameCamera camera, PlayerMob perspective) {
      if (!this.isCompleted() && this.hasTrinket && !this.hasEquippedTrinket && !perspective.isInventoryExtended()) {
         LocalMessage openInvMessage = new LocalMessage("objectives", "openinv", "key", TypeParsers.getInputParseString(Control.INVENTORY));
         return StoryObjective.getLevelTextDrawOptions(openInvMessage, perspective.getDrawX(), perspective.getDrawY() - 48, camera, perspective, false);
      } else {
         return super.getCurrentObjectiveLevelHudDrawOptions(camera, perspective);
      }
   }

   @Override
   public void drawCurrentObjectiveOverForms(MainGame mainGame, PlayerMob perspective) {
      super.drawCurrentObjectiveOverForms(mainGame, perspective);
      if (!this.isCompleted()
         && this.hasTrinket
         && !this.hasEquippedTrinket
         && perspective.isInventoryExtended()
         && !perspective.getWorldSettings().creativeMode) {
         FormContainerSlot trinketSlot = ((MainGame)GlobalData.getCurrentState()).formManager.activeTrinketSlot;
         FairTypeDrawOptions text = getTextDrawOptions(getObjectiveText(new LocalMessage("objectives", "equiptrinkethere").translate()));
         getTextDrawOptions(text, null, trinketSlot, trinketSlot.getBoundingBox().width / 2 + 4, 5, true).draw();
      }
   }

   @Override
   public LootTable getRewards() {
      return reward;
   }

   @Override
   public void setupFormComponent(FormStoryObjectiveComponent form, FormFlow flow) {
      form.addTitle(flow, new LocalMessage("objectives", "equippingtrinkettitle"));
      form.addObjective(
         flow,
         1,
         new LocalMessage(
            "objectives",
            "obtaintrinket",
            "eldericon",
            TypeParsers.getMobParseString("elderhuman"),
            "trinket",
            TypeParsers.getItemAndNameParseString("leatherdashers"),
            "workstation",
            TypeParsers.getItemAndNameParseString("workstationduo"),
            "elder",
            TypeParsers.getMobAndNameParseString("elderhuman")
         ),
         this.isCompleted() || this.hasTrinket
      );
      if (this.isCompleted() || this.hasTrinket) {
         form.addObjective(
            flow,
            2,
            new LocalMessage("objectives", "equiptrinket", "trinket", TypeParsers.getItemAndNameParseString("leatherdashers")),
            this.isCompleted() || this.hasEquippedTrinket
         );
         if (this.isCompleted() || this.hasEquippedTrinket) {
            LocalMessage useTrinketMessage = new LocalMessage(
               "objectives",
               "usetrinket",
               "trinket",
               TypeParsers.getItemAndNameParseString("leatherdashers"),
               "key",
               TypeParsers.getInputParseString(Control.TRINKET_ABILITY)
            );
            form.addObjective(flow, 3, useTrinketMessage, this.isCompleted());
            if (!this.isCompleted()) {
               flow.next(8);
               form.addProgressbar(
                  flow, 40, () -> GameMath.limit((double)this.leatherDashersUseBuffer / (SECONDS_TO_USE_LEATHER_DASHERS * 1000), 0.0, 1.0), () -> null
               );
            }
         }
      }

      if (this.isCompleted()) {
         form.addObjective(flow, 4, new LocalMessage("objectives", "elderclaimreward", "elder", TypeParsers.getMobAndNameParseString("elderhuman")), false);
      }

      form.addRewardsText(flow);
   }
}
