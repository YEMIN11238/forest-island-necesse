package necesse.level.gameObject;

import java.awt.Color;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.List;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.localization.Localization;
import necesse.engine.localization.message.LocalMessage;
import necesse.engine.network.gameNetworkData.GNDItemMap;
import necesse.engine.network.packet.PacketShowAttack;
import necesse.engine.network.server.ServerClient;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.objectEntity.EggNestObjectEntity;
import necesse.entity.objectEntity.ObjectEntity;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.DrawOptions;
import necesse.gfx.drawables.LevelSortedDrawable;
import necesse.gfx.drawables.OrderableDrawables;
import necesse.gfx.gameTexture.GameTexture;
import necesse.gfx.gameTooltips.ListGameTooltips;
import necesse.inventory.InventoryItem;
import necesse.inventory.PlayerInventorySlot;
import necesse.inventory.item.Item;
import necesse.inventory.item.SwingSpriteAttackItem;
import necesse.inventory.item.placeableItem.consumableItem.food.EggItemInterface;
import necesse.inventory.item.toolItem.ToolType;
import necesse.level.maps.Level;
import necesse.level.maps.light.GameLight;

public class EggNestObject extends GameObject implements EggNestObjectInterface {
   public GameTexture texture;

   public EggNestObject() {
      super(new Rectangle());
      this.mapColor = new Color(243, 169, 35);
      this.displayMapTooltip = true;
      this.objectHealth = 50;
      this.toolType = ToolType.ALL;
      this.rarity = Item.Rarity.COMMON;
      this.isLightTransparent = true;
      this.setItemCategory("objects", "misc");
      this.setCraftingCategory("objects", "misc");
   }

   @Override
   public void loadTextures() {
      super.loadTextures();
      this.texture = GameTexture.fromFile("objects/eggnest");
   }

   @Override
   public void addDrawables(
      List<LevelSortedDrawable> list,
      OrderableDrawables tileList,
      Level level,
      int tileX,
      int tileY,
      TickManager tickManager,
      GameCamera camera,
      PlayerMob perspective
   ) {
      GameLight light = level.getLightLevel(tileX, tileY);
      int drawX = camera.getTileDrawX(tileX);
      int drawY = camera.getTileDrawY(tileY);
      EggNestObjectEntity objectEntity = this.getCurrentObjectEntity(level, tileX, tileY, EggNestObjectEntity.class);
      Point sprite = new Point(0, 0);
      if (objectEntity != null) {
         if (objectEntity.hasEgg()) {
            sprite = new Point(1, 0);
         } else if (objectEntity.hasRecentlyHatched()) {
            sprite = new Point(2, 0);
         }
      }

      final DrawOptions baseOptions = this.texture
         .initDraw()
         .sprite(sprite.x, sprite.y, 32)
         .addObjectDamageOverlay(this, level, tileX, tileY)
         .light(light)
         .pos(drawX, drawY - (this.texture.getHeight() - 32));
      list.add(new LevelSortedDrawable(this, tileX, tileY) {
         @Override
         public int getSortY() {
            return 16;
         }

         @Override
         public void draw(TickManager tickManager) {
            baseOptions.draw();
         }
      });
   }

   @Override
   public void drawPreview(Level level, int tileX, int tileY, int rotation, float alpha, PlayerMob player, GameCamera camera) {
      int drawX = camera.getTileDrawX(tileX);
      int drawY = camera.getTileDrawY(tileY);
      this.texture.initDraw().sprite(0, 0, 32).alpha(alpha).draw(drawX, drawY - (this.texture.getHeight() - 32));
   }

   @Override
   public void interact(Level level, int x, int y, PlayerMob player) {
      if (player != null) {
         ServerClient serverClient = player.isServerClient() ? player.getServerClient() : null;
         EggNestObjectEntity objectEntity = this.getCurrentObjectEntity(level, x, y, EggNestObjectEntity.class);
         if (objectEntity != null) {
            int textX = x * 32 + 16;
            int textY = y * 32 + 32;
            if (!objectEntity.hasEgg()) {
               PlayerInventorySlot selectedSlot = player.getSelectedItemSlot();
               InventoryItem selectedItem = selectedSlot.getItem(player.getInv());
               if (selectedItem != null && selectedItem.item instanceof EggItemInterface) {
                  if (level.isServer()) {
                     objectEntity.placeEgg(selectedItem.copy(1));
                     selectedItem.setAmount(selectedItem.getAmount() - 1);
                     if (selectedItem.getAmount() <= 0) {
                        selectedSlot.setItem(player.getInv(), null);
                     }

                     selectedSlot.markDirty(player.getInv());
                  }

                  InventoryItem attackItem = SwingSpriteAttackItem.setup(new InventoryItem("swingspriteattack"), selectedItem.copy(1), false);
                  int attackX = x * 32 + 16;
                  int attackY = y * 32 + 16;
                  int attackSeed = 0;
                  GNDItemMap attackMap = new GNDItemMap();
                  player.showItemAttack(attackItem, attackX, attackY, 0, attackSeed, attackMap);
                  if (serverClient != null) {
                     serverClient.getServer()
                        .network
                        .sendToClientsWithEntityExcept(
                           new PacketShowAttack(player, attackItem, attackX, attackY, 0, attackSeed, attackMap), player, serverClient
                        );
                  }
               }
            } else if (serverClient != null) {
               if (objectEntity.isFertilized()) {
                  serverClient.sendUniqueFloatText(textX, textY, new LocalMessage("ui", "egghatchtip"), "inspect", 6000);
               } else {
                  serverClient.sendUniqueFloatText(textX, textY, new LocalMessage("ui", "eggnotfertilized"), "inspect", 6000);
               }
            }
         }
      }
   }

   @Override
   public boolean canInteract(Level level, int x, int y, PlayerMob player) {
      if (player != null) {
         InventoryItem selectedItem = player.getSelectedItem();
         if (selectedItem != null && selectedItem.item instanceof EggItemInterface) {
            return true;
         }
      }

      EggNestObjectEntity objectEntity = this.getCurrentObjectEntity(level, x, y, EggNestObjectEntity.class);
      return objectEntity != null && objectEntity.hasEgg();
   }

   @Override
   public String getInteractTip(Level level, int x, int y, PlayerMob perspective, boolean debug) {
      if (perspective != null) {
         EggNestObjectEntity objectEntity = this.getCurrentObjectEntity(level, x, y, EggNestObjectEntity.class);
         if (objectEntity != null && !objectEntity.hasEgg()) {
            InventoryItem selectedItem = perspective.getSelectedItem();
            if (selectedItem != null && selectedItem.item instanceof EggItemInterface) {
               return Localization.translate("ui", "placeeggtip");
            }
         }
      }

      return Localization.translate("controls", "inspecttip");
   }

   @Override
   public ObjectEntity getNewObjectEntity(Level level, int x, int y) {
      return new EggNestObjectEntity(level, x, y);
   }

   @Override
   public ListGameTooltips getItemTooltips(InventoryItem item, PlayerMob perspective) {
      ListGameTooltips tooltips = super.getItemTooltips(item, perspective);
      tooltips.add(Localization.translate("itemtooltip", "eggnesttip1"));
      tooltips.add(Localization.translate("itemtooltip", "eggnesttip2"));
      return tooltips;
   }

   @Override
   public ProcessObjectHandler getLayEggHandler(Level level, int tileX, int tileY) {
      final EggNestObjectEntity objectEntity = this.getCurrentObjectEntity(level, tileX, tileY, EggNestObjectEntity.class);
      return objectEntity != null ? new ProcessObjectHandler(tileX, tileY, objectEntity.layEggOrFertilizeReservable) {
         @Override
         public boolean canProcess() {
            return !objectEntity.hasEgg();
         }

         @Override
         public void process() {
            objectEntity.placeEgg(new InventoryItem("egg"));
            objectEntity.fertilize();
         }

         @Override
         public boolean isValid() {
            return !objectEntity.removed();
         }

         @Override
         public int getTimeItTakesInMilliseconds() {
            return 8000;
         }
      } : null;
   }

   @Override
   public ProcessObjectHandler getFertilizeEggHandler(Level level, int tileX, int tileY) {
      final EggNestObjectEntity objectEntity = this.getCurrentObjectEntity(level, tileX, tileY, EggNestObjectEntity.class);
      return objectEntity != null ? new ProcessObjectHandler(tileX, tileY, objectEntity.layEggOrFertilizeReservable) {
         @Override
         public boolean canProcess() {
            return objectEntity.hasEgg() && !objectEntity.isFertilized();
         }

         @Override
         public void process() {
            objectEntity.fertilize();
         }

         @Override
         public boolean isValid() {
            return !objectEntity.removed();
         }

         @Override
         public int getTimeItTakesInMilliseconds() {
            return 8000;
         }
      } : null;
   }
}
