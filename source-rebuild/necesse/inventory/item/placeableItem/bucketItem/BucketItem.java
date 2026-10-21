package necesse.inventory.item.placeableItem.bucketItem;

import java.awt.geom.Line2D;
import java.util.ArrayList;
import necesse.engine.GameEvents;
import necesse.engine.events.players.ItemPlaceEvent;
import necesse.engine.input.Control;
import necesse.engine.localization.Localization;
import necesse.engine.network.gameNetworkData.GNDItemMap;
import necesse.engine.registries.ItemRegistry;
import necesse.engine.registries.TileRegistry;
import necesse.engine.sound.SoundEffect;
import necesse.engine.sound.SoundManager;
import necesse.engine.util.GameBlackboard;
import necesse.engine.util.GameMath;
import necesse.engine.window.GameWindow;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.buffs.BuffModifiers;
import necesse.entity.mobs.friendly.HusbandryMob;
import necesse.entity.mobs.itemAttacker.ItemAttackSlot;
import necesse.entity.mobs.itemAttacker.ItemAttackerMob;
import necesse.gfx.GameResources;
import necesse.gfx.Renderer;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.itemAttack.ItemAttackDrawOptions;
import necesse.gfx.gameTexture.GameSprite;
import necesse.gfx.gameTexture.GameTexture;
import necesse.gfx.gameTooltips.GameTooltipManager;
import necesse.gfx.gameTooltips.InputTooltip;
import necesse.gfx.gameTooltips.ListGameTooltips;
import necesse.gfx.gameTooltips.StringTooltips;
import necesse.gfx.gameTooltips.TooltipLocation;
import necesse.inventory.InventoryItem;
import necesse.inventory.PlayerInventorySlot;
import necesse.inventory.item.ItemCategory;
import necesse.inventory.item.ItemInteractAction;
import necesse.inventory.item.placeableItem.PlaceableItem;
import necesse.level.gameTile.GameTile;
import necesse.level.maps.Level;

public class BucketItem extends PlaceableItem implements ItemInteractAction {
   public BucketItem() {
      super(50, true);
      this.controllerIsTileBasedPlacing = true;
      this.setItemCategory("equipment", "tools", "misc");
      this.setItemCategory(ItemCategory.craftingManager, "equipment", "tools");
      this.keyWords.add("liquid");
      this.incinerationTimeMillis = 30000;
   }

   @Override
   public void loadItemTextures() {
      this.itemTexture = GameTexture.fromFile("tiles/bucket");
   }

   @Override
   public GameSprite getItemSprite(InventoryItem item, PlayerMob perspective) {
      return new GameSprite(this.itemTexture, 0, 0, 32);
   }

   @Override
   public String canPlace(Level level, int x, int y, PlayerMob player, Line2D playerPositionLine, InventoryItem item, GNDItemMap mapContent) {
      int tileX = GameMath.getTileCoordinate(x);
      int tileY = GameMath.getTileCoordinate(y);
      if (!level.isTileWithinBounds(tileX, tileY)) {
         return "outsidelevel";
      } else if (level.isProtected(tileX, tileY)) {
         return "protected";
      } else if (player.getPositionPoint().distance(tileX * 32 + 16, tileY * 32 + 16) > this.getPlaceRange(item, player)) {
         return "outofrange";
      } else if (!level.isLiquidTile(tileX, tileY)) {
         return "notliquid";
      } else {
         return level.liquidManager.getHeight(tileX, tileY) < -3 ? "deepsea" : null;
      }
   }

   @Override
   public InventoryItem onPlace(Level level, int x, int y, PlayerMob player, int seed, InventoryItem item, GNDItemMap mapContent) {
      int tileX = GameMath.getTileCoordinate(x);
      int tileY = GameMath.getTileCoordinate(y);
      ItemPlaceEvent event = new ItemPlaceEvent(level, tileX, tileY, player, item);
      GameEvents.triggerEvent(event);
      if (!event.isPrevented()) {
         GameTile beforeTile = level.getTile(tileX, tileY);
         level.setTile(tileX, tileY, TileRegistry.dirtID);
         level.tileLayer.setIsPlayerPlaced(tileX, tileY, true);
         if (level.isClient()) {
            SoundManager.playSound(GameResources.waterblob, SoundEffect.effect(tileX * 32 + 16, tileY * 32 + 16));
         } else {
            level.sendTileUpdatePacket(tileX, tileY);
            level.getLevelTile(tileX, tileY).checkAround();
            level.getLevelObject(tileX, tileY).checkAround();
         }

         InventoryItem tileBucket = new InventoryItem(ItemRegistry.getItem(beforeTile.getStringID()));
         if (item.getAmount() <= 1 && this.isSingleUse(player)) {
            return tileBucket;
         }

         player.getInv().addItemsDropRemaining(tileBucket, "addback", player, false, false);
         if (this.isSingleUse(player)) {
            item.setAmount(item.getAmount() - 1);
         }
      }

      return item;
   }

   @Override
   public void drawPlacePreview(Level level, int x, int y, GameCamera camera, PlayerMob player, InventoryItem item, PlayerInventorySlot slot) {
      if (this.canPlace(level, x, y, player, null, item, null) == null) {
         int tileX = GameMath.getTileCoordinate(x);
         int tileY = GameMath.getTileCoordinate(y);
         TileRegistry.getTile(TileRegistry.dirtID).drawPreview(level, tileX, tileY, 0.5F, player, camera);
      }
   }

   @Override
   public void setDrawAttackRotation(InventoryItem item, ItemAttackDrawOptions drawOptions, float attackDirX, float attackDirY, float attackProgress) {
      drawOptions.swingRotationInv(attackProgress);
   }

   @Override
   public float getAttackSpeedModifier(InventoryItem item, ItemAttackerMob attackerMob) {
      return super.getAttackSpeedModifier(item, attackerMob) * (attackerMob == null ? 1.0F : attackerMob.buffManager.getModifier(BuffModifiers.BUILDING_SPEED));
   }

   @Override
   public ListGameTooltips getTooltips(InventoryItem item, PlayerMob perspective, GameBlackboard blackboard) {
      ListGameTooltips tooltips = super.getTooltips(item, perspective, blackboard);
      tooltips.add(new StringTooltips(Localization.translate("itemtooltip", "buckettip"), 300));
      return tooltips;
   }

   @Override
   public boolean canMobInteract(Level level, Mob mob, ItemAttackerMob attackerMob, InventoryItem item) {
      return mob instanceof HusbandryMob && ((HusbandryMob)mob).canMilk(item) && mob.inInteractRange(attackerMob);
   }

   @Override
   public InventoryItem onMobInteract(
      Level level, Mob targetMob, ItemAttackerMob attackerMob, int attackHeight, InventoryItem item, ItemAttackSlot slot, int seed, GNDItemMap mapContent
   ) {
      if (targetMob instanceof HusbandryMob) {
         HusbandryMob hMob = (HusbandryMob)targetMob;
         if (hMob.canMilk(item)) {
            ArrayList<InventoryItem> products = new ArrayList<>();
            InventoryItem out = hMob.onMilk(item, products);
            if (!level.isClient()) {
               for (InventoryItem product : products) {
                  level.entityManager.pickups.add(product.getPickupEntity(level, hMob.x, hMob.y));
               }
            }

            return out;
         }
      }

      return item;
   }

   @Override
   public boolean getConstantInteract(InventoryItem item) {
      return true;
   }

   @Override
   public boolean onMouseHoverMob(InventoryItem item, GameCamera camera, PlayerMob perspective, Mob mob, GameBlackboard blackboard, boolean isDebug) {
      boolean sup = super.onMouseHoverMob(item, camera, perspective, mob, blackboard, isDebug);
      if (mob instanceof HusbandryMob && ((HusbandryMob)mob).canMilk(item)) {
         Renderer.setCursor(GameWindow.CURSOR.INTERACT);
         if (mob.inInteractRange(perspective)) {
            GameTooltipManager.addTooltip(new InputTooltip(Control.MOUSE2, Localization.translate("controls", "milktip")), TooltipLocation.INTERACT_FOCUS);
         } else {
            GameTooltipManager.addTooltip(new InputTooltip(Control.MOUSE2, Localization.translate("controls", "milktip"), 0.5F), TooltipLocation.INTERACT_FOCUS);
         }

         blackboard.set("preventInteractTip", true);
         return true;
      } else {
         return sup;
      }
   }
}
