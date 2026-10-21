package necesse.entity.mobs.attackHandler;

import java.awt.geom.Line2D;
import necesse.engine.Settings;
import necesse.engine.input.Input;
import necesse.engine.input.controller.ControllerInput;
import necesse.engine.localization.Localization;
import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.engine.network.gameNetworkData.GNDItemMap;
import necesse.engine.network.server.ServerClient;
import necesse.engine.util.GameMath;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.itemAttacker.ItemAttackSlot;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.gameTooltips.GameTooltipManager;
import necesse.gfx.gameTooltips.StringTooltips;
import necesse.gfx.gameTooltips.TooltipLocation;
import necesse.inventory.InventoryItem;
import necesse.inventory.item.toolItem.TileDamageOption;
import necesse.inventory.item.toolItem.ToolDamageItem;
import necesse.level.maps.Level;

public class ToolDamageItemAttackHandler extends PlaceItemAttackHandler<ToolDamageItemAttackHandler.ItemDamagePosition> {
   public final ToolDamageItem toolDamageItem;
   private boolean isMobHitExclusive = false;
   private int animAttack = 0;

   public ToolDamageItemAttackHandler(
      PlayerMob player, ItemAttackSlot slot, int startLevelX, int startLevelY, int seed, ToolDamageItem toolDamageItem, GNDItemMap attackContentMap
   ) {
      super(player, slot, startLevelX, startLevelY, seed);
      this.toolDamageItem = toolDamageItem;
      if (attackContentMap != null) {
         this.runInitialPlace(attackContentMap);
      }
   }

   protected ToolDamageItemAttackHandler.ItemDamagePosition placeItem(
      InventoryItem item, int currentX, int currentY, Line2D playerPositionLine, GNDItemMap mapContent
   ) {
      Level level = this.attackerMob.getLevel();
      PlayerMob player = (PlayerMob)this.attackerMob;
      TileDamageOption tileDamageOption = null;
      Mob damagedMob;
      if (mapContent == null) {
         mapContent = new GNDItemMap();
         damagedMob = this.toolDamageItem.getMobHitOption(level, currentX, currentY, player, playerPositionLine, item);
         if (damagedMob != null) {
            this.toolDamageItem.setupAttackMapContentHitMob(mapContent, level, damagedMob);
         } else if (!this.isMobHitExclusive) {
            if (Settings.smartMining || Input.lastInputIsController && !ControllerInput.isCursorVisible()) {
               ToolDamageItem.SmartMineTarget hitTile = this.toolDamageItem.getFirstSmartHitTile(player.getLevel(), player, item, currentX, currentY);
               if (hitTile != null) {
                  tileDamageOption = hitTile.tileDamageOption;
                  currentX = hitTile.x * 32 + 16;
                  currentY = hitTile.y * 32 + 16;
               }
            }

            if (tileDamageOption == null) {
               tileDamageOption = this.toolDamageItem
                  .getTileDamageOption(level, currentX, currentY, player, playerPositionLine, item, this.getPlaceCooldown() > 0);
               if (tileDamageOption == null) {
                  tileDamageOption = new TileDamageOption(-1, GameMath.getTileCoordinate(currentX), GameMath.getTileCoordinate(currentY));
               }
            }

            this.toolDamageItem.setupAttackMapContentHitTile(mapContent, level, tileDamageOption);
         }
      } else {
         damagedMob = this.toolDamageItem.getMobHitFromMap(mapContent, level);
         if (damagedMob != null) {
            this.isMobHitExclusive = true;
         } else {
            tileDamageOption = this.toolDamageItem.getTileDamageOptionFromMap(mapContent, level);
         }
      }

      if ((damagedMob != null || tileDamageOption != null) && this.toolDamageItem.canAttack(level, currentX, currentY, player, item) == null) {
         int animAttack = this.animAttack++;
         InventoryItem newItem = this.toolDamageItem.runTileAttack(level, currentX, currentY, player, playerPositionLine, item, animAttack, mapContent);
         if (newItem.getAmount() <= 0) {
            this.slot.setItem(null);
         } else {
            this.slot.setItem(newItem);
         }

         return new ToolDamageItemAttackHandler.ItemDamagePosition(currentX, currentY, mapContent, animAttack);
      } else {
         return null;
      }
   }

   protected boolean placeServerItem(InventoryItem item, ToolDamageItemAttackHandler.ItemDamagePosition placePosition, Line2D playerPositionLine) {
      Level level = this.attackerMob.getLevel();
      PlayerMob player = (PlayerMob)this.attackerMob;
      if (this.toolDamageItem.canAttack(level, placePosition.placeX, placePosition.placeY, player, item) == null) {
         InventoryItem newItem = this.toolDamageItem
            .runTileAttack(
               level, placePosition.placeX, placePosition.placeY, player, playerPositionLine, item, placePosition.animAttack, placePosition.attackMapContent
            );
         if (newItem.getAmount() <= 0) {
            this.slot.setItem(null);
         } else {
            this.slot.setItem(newItem);
         }

         return true;
      } else {
         return false;
      }
   }

   protected void onServerPlaceInvalid(InventoryItem item, ToolDamageItemAttackHandler.ItemDamagePosition placePosition, Line2D playerPositionLine) {
      PlayerMob player = (PlayerMob)this.attackerMob;
      Level level = player.getLevel();
      ServerClient client = player.getServerClient();
      TileDamageOption tileDamageOption = this.toolDamageItem.getTileDamageOptionFromMap(placePosition.attackMapContent, level);
      if (tileDamageOption != null) {
         level.checkTileAndObjectsHashGNDMap(client, placePosition.attackMapContent, tileDamageOption.tileX, tileDamageOption.tileY, false);
      }
   }

   protected ToolDamageItemAttackHandler.ItemDamagePosition createPlacePositionFromPacket(PacketReader reader) {
      return new ToolDamageItemAttackHandler.ItemDamagePosition(reader);
   }

   @Override
   protected int getPlaceCooldown() {
      InventoryItem item = this.slot.getItem();
      return item != null ? this.toolDamageItem.getAttackHandlerDamageCooldown(item, this.attackerMob) : 200;
   }

   @Override
   protected void showAttackAndSendAttacker(int targetX, int targetY, InventoryItem item) {
      super.showAttackAndSendAttacker(targetX, targetY, item);
      this.toolDamageItem
         .startToolItemEventAbilityEvent(
            this.attackerMob.getLevel(), targetX, targetY, this.attackerMob, this.attackerMob.getCurrentAttackHeight(), item, this.seed
         );
   }

   @Override
   public void drawHUDItemSelected(GameCamera camera, Level level, PlayerMob player, InventoryItem item) {
      super.drawHUDItemSelected(camera, level, player, item);
      if (this.isMobHitExclusive) {
         GameTooltipManager.addTooltip(new StringTooltips(Localization.translate("itemtooltip", "tooltargetingmobs")), TooltipLocation.INTERACT_FOCUS);
      }
   }

   protected static class ItemDamagePosition extends PlaceItemAttackHandler.PlacePosition {
      public final int animAttack;

      public ItemDamagePosition(int placeX, int placeY, GNDItemMap attackMapContent, int animAttack) {
         super(placeX, placeY, attackMapContent);
         this.animAttack = animAttack;
      }

      public ItemDamagePosition(PacketReader reader) {
         super(reader);
         this.animAttack = reader.getNextInt();
      }

      @Override
      public void writePacket(PacketWriter writer) {
         super.writePacket(writer);
         writer.putNextInt(this.animAttack);
      }
   }
}
