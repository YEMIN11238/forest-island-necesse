package necesse.inventory.container.settlement.data;

import java.awt.Point;
import necesse.engine.Settings;
import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.engine.registries.SettlerRegistry;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.friendly.human.HumanMob;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.DrawOptions;
import necesse.gfx.drawOptions.DrawOptionsList;
import necesse.gfx.drawOptions.human.HumanDrawOptions;
import necesse.gfx.forms.presets.containerComponent.mob.DialogueForm;
import necesse.gfx.gameTexture.GameTexture;
import necesse.level.maps.Level;
import necesse.level.maps.levelData.settlementData.LevelSettler;
import necesse.level.maps.levelData.settlementData.SettlementBed;
import necesse.level.maps.levelData.settlementData.settler.CommandMob;
import necesse.level.maps.levelData.settlementData.settler.Settler;
import necesse.level.maps.levelData.settlementData.settler.SettlerMob;

public class SettlementSettlerData {
   public final int mobUniqueID;
   public final Settler settler;
   public Point bedPosition;
   private SettlerMob foundMob;
   public SettlerLooksData looksData;
   public SettlerActivityData activityData;
   public SettlerCommandData commandData;
   public SettlerOverviewData overviewData;
   public SettlerEquipmentData equipmentData;
   public SettlerDietData dietData;
   public SettlerGearData gearData;
   public SettlerRestrictZoneData restrictZoneData;
   public SettlerWorkPrioritiesData workPrioritiesData;

   public SettlementSettlerData(LevelSettler settler) {
      this.settler = settler.settler;
      this.mobUniqueID = settler.mobUniqueID;
      SettlementBed bed = settler.getBed();
      this.bedPosition = bed == null ? null : new Point(bed.tileX, bed.tileY);
   }

   public SettlementSettlerData(PacketReader reader) {
      int settlerNetworkID = reader.getNextShort();
      this.settler = SettlerRegistry.getSettler(settlerNetworkID);
      this.mobUniqueID = reader.getNextInt();
      if (reader.getNextBoolean()) {
         int bedTileX = reader.getNextInt();
         int bedTileY = reader.getNextInt();
         this.bedPosition = new Point(bedTileX, bedTileY);
      } else {
         this.bedPosition = null;
      }
   }

   public void writeContentPacket(PacketWriter writer) {
      writer.putNextShort((short)this.settler.getID());
      writer.putNextInt(this.mobUniqueID);
      writer.putNextBoolean(this.bedPosition != null);
      if (this.bedPosition != null) {
         writer.putNextInt(this.bedPosition.x);
         writer.putNextInt(this.bedPosition.y);
      }
   }

   public void updateFromOtherData(SettlementSettlerData data) {
      if (data.mobUniqueID != this.mobUniqueID) {
         throw new IllegalStateException("Cannot update settler data from data with different mob unique ID");
      }

      this.bedPosition = data.bedPosition;
   }

   public void updateFromClient(Level level) {
      SettlerMob mob = this.getSettlerMob(level);
      if (mob != null) {
         this.looksData = mob.createLooksData();
         this.gearData = mob.createGearData();
         this.activityData = new SettlerActivityData(mob.getCurrentActivity(), mob.hasCommandOrders());
         if (mob instanceof CommandMob) {
            this.commandData = new SettlerCommandData((CommandMob)mob);
         }
      }
   }

   public SettlerMob getSettlerMob(Level level) {
      if (this.foundMob == null || this.foundMob.getMob().removed() || this.foundMob.getMob().isDisposed()) {
         Mob mob = level.entityManager.mobs.get(this.mobUniqueID, false);
         if (mob instanceof SettlerMob) {
            this.foundMob = (SettlerMob)mob;
         } else {
            this.foundMob = null;
         }
      }

      return this.foundMob;
   }

   public String getSettlerName(Level level, String defaultValue) {
      if (level != null) {
         SettlerMob settlerMob = this.getSettlerMob(level);
         if (settlerMob != null) {
            return settlerMob.getSettlerName();
         }
      }

      return this.looksData != null ? this.looksData.settlerName : defaultValue;
   }

   public SettlerActivityData getSettlerActivityData(Level level) {
      if (level != null) {
         SettlerMob settlerMob = this.getSettlerMob(level);
         if (settlerMob != null) {
            return new SettlerActivityData(settlerMob.getCurrentActivity(), settlerMob.hasCommandOrders());
         }
      }

      return this.activityData;
   }

   public boolean selfManagesEquipment(Level level, boolean defaultValue) {
      if (level != null) {
         SettlerMob settlerMob = this.getSettlerMob(level);
         if (settlerMob != null) {
            return settlerMob.managesEquipmentThemself();
         }
      }

      return this.equipmentData != null ? this.equipmentData.selfManageEquipment : defaultValue;
   }

   public SettlerCommandData updateCommandData(Level level, boolean setUnknownToNull) {
      SettlerMob mob = this.getSettlerMob(level);
      if (mob instanceof CommandMob) {
         this.commandData = new SettlerCommandData((CommandMob)mob);
      } else if (setUnknownToNull) {
         this.commandData = null;
      }

      return this.commandData;
   }

   public boolean matchesSearch(String lowerCaseSearchString, Level level, PlayerMob perspective, SettlerSearchData... addons) {
      if (this.settler.getStringID().toLowerCase().contains(lowerCaseSearchString)) {
         return true;
      }

      if (this.settler.getGenericMobName().toLowerCase().contains(lowerCaseSearchString)) {
         return true;
      }

      SettlerMob mob = this.getSettlerMob(level);
      if (mob != null && mob.getSettlerName().toLowerCase().contains(lowerCaseSearchString)) {
         return true;
      }

      if (mob instanceof HumanMob) {
         HumanMob humanMob = (HumanMob)mob;

         for (DialogueForm.PersonalityData data : DialogueForm.getPersonalityData(humanMob)) {
            if (data.title.translate().toLowerCase().contains(lowerCaseSearchString)) {
               return true;
            }
         }
      }

      for (SettlerSearchData addon : addons) {
         if (addon != null && addon.matchesSearch(lowerCaseSearchString, mob, perspective)) {
            return true;
         }
      }

      return false;
   }

   public DrawOptions getIconDrawOptions(Level level, int size, int drawX, int drawY) {
      SettlerMob mob = this.getSettlerMob(level);
      if (mob != null) {
         return this.settler.getSettlerFaceDrawOptions(drawX, drawY, size, mob.getMob());
      } else if (this.looksData != null) {
         HumanDrawOptions humanOptions = new HumanDrawOptions(null, this.looksData.look, !this.looksData.customLook);
         this.settler.setDefaultArmor(humanOptions, this.looksData.settlerSeed, this.looksData.look, this.looksData.customLook);
         return Settler.getHumanFaceDrawOptions(humanOptions, size, drawX, drawY);
      } else {
         return this.settler.getSettlerIcon().initDraw().size(size).pos(drawX, drawY);
      }
   }

   public static DrawOptions getSettlerFlagDrawOptions(int midDrawX, int drawY, SettlementSettlerData.FaceDrawOptionsGetter faceDrawOptionsGetter) {
      DrawOptionsList options = new DrawOptionsList();
      GameTexture texture = Settings.UI.settler_house;
      options.add(texture.initDraw().pos(midDrawX - texture.getWidth() / 2, drawY));
      options.add(faceDrawOptionsGetter.getDrawOptions(32, midDrawX - 16, drawY + texture.getHeight() / 2 - 16));
      return options;
   }

   public static DrawOptions getSettlerFlagDrawOptionsTile(
      int tileX, int tileY, GameCamera camera, SettlementSettlerData.FaceDrawOptionsGetter faceDrawOptionsGetter
   ) {
      int drawX = camera.getTileDrawX(tileX) + 16;
      int drawY = camera.getTileDrawY(tileY);
      return drawX >= -64 && drawY >= -64 && drawX <= camera.getWidth() && drawY <= camera.getHeight()
         ? getSettlerFlagDrawOptions(drawX, drawY, faceDrawOptionsGetter)
         : () -> {};
   }

   public interface FaceDrawOptionsGetter {
      DrawOptions getDrawOptions(int var1, int var2, int var3);
   }
}
