package necesse.level.gameObject;

import java.awt.Color;
import java.awt.Rectangle;
import java.util.List;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.network.server.ServerClient;
import necesse.engine.registries.ObjectRegistry;
import necesse.engine.util.GameRandom;
import necesse.engine.util.GameUtils;
import necesse.entity.levelEvent.mobAbilityLevelEvent.CoreKeeperCoreChatLevelEvent;
import necesse.entity.mobs.Attacker;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.PlayerMob;
import necesse.gfx.GameColor;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.DrawOptions;
import necesse.gfx.drawables.LevelSortedDrawable;
import necesse.gfx.drawables.OrderableDrawables;
import necesse.gfx.gameTexture.GameSprite;
import necesse.gfx.gameTooltips.ListGameTooltips;
import necesse.gfx.gameTooltips.StringTooltips;
import necesse.inventory.InventoryItem;
import necesse.inventory.item.Item;
import necesse.inventory.item.toolItem.ToolType;
import necesse.level.maps.Level;
import necesse.level.maps.light.GameLight;

public class TheCoreStatueObject extends StaticMultiObject {
   protected TheCoreStatueObject(String texturePath, int multiX, int multiY, int multiWidth, int multiHeight, int[] multiIDs, Rectangle fullCollision) {
      super(multiX, multiY, multiWidth, multiHeight, multiIDs, fullCollision, "statues/" + texturePath);
      this.stackSize = 1;
      this.rarity = Item.Rarity.UNIQUE;
      this.mapColor = new Color(32, 137, 155);
      this.objectHealth = 1000;
      this.toolType = ToolType.NONE;
      this.lightLevel = 75;
      this.lightHue = 220.0F;
      this.lightSat = 0.5F;
      this.isLightTransparent = true;
      this.hoverHitbox = new Rectangle(0, 0, 32, 32);
      this.setItemCategory("objects", "landscaping", "masonry");
      this.setCraftingCategory("objects", "landscaping", "masonry");
   }

   @Override
   public ListGameTooltips getItemTooltips(InventoryItem item, PlayerMob perspective) {
      ListGameTooltips itemTooltips = super.getItemTooltips(item, perspective);
      itemTooltips.add(new StringTooltips("If ever thou findst your way hither again, let us be a force for brilliance", GameColor.ITEM_EPIC, 350));
      itemTooltips.add(GameUtils.getDevQuoteTooltip("A tribute to our friends from the Core Keeper team", null));
      return itemTooltips;
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
      int spriteX = GameUtils.getAnim(level.getTime(), 4, 1000);
      GameSprite baseSprite = new GameSprite(this.texture, spriteX, 0, 192, 224, 192, 224);
      GameSprite eggSprite = new GameSprite(this.texture, spriteX, 1, 192, 224, 192, 224);
      GameSprite rootsSprite1 = new GameSprite(this.texture, spriteX, 2, 192, 224, 192, 224);
      GameSprite effectsSprite = new GameSprite(this.texture, spriteX, 3, 192, 224, 192, 224);
      GameSprite rootsSprite2 = new GameSprite(this.texture, spriteX, 4, 192, 224, 192, 224);
      GameLight light = level.getLightLevel(tileX, tileY);
      final DrawOptions baseOptions = this.getMultiTextureDrawOptions(baseSprite, level, tileX, tileY, camera, light);
      final DrawOptions eggOptions = this.getMultiTextureDrawOptions(eggSprite, level, tileX, tileY, camera, light.minLevelCopy(112.5F));
      final DrawOptions roots1Options = this.getMultiTextureDrawOptions(rootsSprite1, level, tileX, tileY, camera, light);
      final DrawOptions effectsOptions = this.getMultiTextureDrawOptions(effectsSprite, level, tileX, tileY, camera, light.minLevelCopy(150.0F));
      final DrawOptions roots2Options = this.getMultiTextureDrawOptions(rootsSprite2, level, tileX, tileY, camera, light);
      list.add(new LevelSortedDrawable(this, tileX, tileY) {
         @Override
         public int getSortY() {
            return TheCoreStatueObject.this.multiY == 0 ? 30 : 16;
         }

         @Override
         public void draw(TickManager tickManager) {
            baseOptions.draw();
            eggOptions.draw();
            roots1Options.draw();
            effectsOptions.draw();
            roots2Options.draw();
         }
      });
   }

   @Override
   public void drawPreview(Level level, int tileX, int tileY, int rotation, float alpha, PlayerMob player, GameCamera camera) {
      int spriteX = GameUtils.getAnim(level.getTime(), 4, 1000);
      GameSprite baseSprite = new GameSprite(this.texture, spriteX, 0, 192, 224, 192, 224);
      GameSprite eggSprite = new GameSprite(this.texture, spriteX, 1, 192, 224, 192, 224);
      GameSprite rootsSprite1 = new GameSprite(this.texture, spriteX, 2, 192, 224, 192, 224);
      GameSprite effectsSprite = new GameSprite(this.texture, spriteX, 3, 192, 224, 192, 224);
      GameSprite rootsSprite2 = new GameSprite(this.texture, spriteX, 4, 192, 224, 192, 224);
      this.drawMultiTexturePreview(baseSprite, tileX, tileY, alpha, camera);
      this.drawMultiTexturePreview(eggSprite, tileX, tileY, alpha, camera);
      this.drawMultiTexturePreview(rootsSprite1, tileX, tileY, alpha, camera);
      this.drawMultiTexturePreview(effectsSprite, tileX, tileY, alpha, camera);
      this.drawMultiTexturePreview(rootsSprite2, tileX, tileY, alpha, camera);
   }

   @Override
   public boolean onDamaged(
      Level level, int layerID, int x, int y, int damage, Attacker attacker, ServerClient client, boolean showEffect, int mouseX, int mouseY
   ) {
      if (attacker == null) {
         return super.onDamaged(level, layerID, x, y, damage, attacker, client, showEffect, mouseX, mouseY);
      }

      if (!level.isClient()) {
         Mob attackOwner = attacker.getAttackOwner();
         if (attackOwner != null && attackOwner.isPlayer && !attackOwner.isOnGenericCooldown("coredamagethought")) {
            attackOwner.startGenericCooldown("coredamagethought", 4500L);
            level.entityManager.events.add(new CoreKeeperCoreChatLevelEvent(attackOwner, GameRandom.globalRandom));
         }
      }

      return super.onDamaged(level, layerID, x, y, damage, attacker, client, showEffect, mouseX, mouseY);
   }

   public static int[] registerTheCoreStatue(String texturePath, boolean isObtainable) {
      int[] ids = new int[36];
      Rectangle collision = new Rectangle(0, 26, 192, 150);
      ids[0] = ObjectRegistry.registerObject(texturePath, new TheCoreStatueObject(texturePath, 0, 0, 6, 6, ids, collision), 0.0F, isObtainable);
      ids[1] = ObjectRegistry.registerObject(texturePath + "2", new TheCoreStatueObject(texturePath, 1, 0, 6, 6, ids, collision), 0.0F, false);
      ids[2] = ObjectRegistry.registerObject(texturePath + "3", new TheCoreStatueObject(texturePath, 2, 0, 6, 6, ids, collision), 0.0F, false);
      ids[3] = ObjectRegistry.registerObject(texturePath + "4", new TheCoreStatueObject(texturePath, 3, 0, 6, 6, ids, collision), 0.0F, false);
      ids[4] = ObjectRegistry.registerObject(texturePath + "5", new TheCoreStatueObject(texturePath, 4, 0, 6, 6, ids, collision), 0.0F, false);
      ids[5] = ObjectRegistry.registerObject(texturePath + "6", new TheCoreStatueObject(texturePath, 5, 0, 6, 6, ids, collision), 0.0F, false);
      ids[6] = ObjectRegistry.registerObject(texturePath + "7", new TheCoreStatueObject(texturePath, 0, 1, 6, 6, ids, collision), 0.0F, false);
      ids[7] = ObjectRegistry.registerObject(texturePath + "8", new TheCoreStatueObject(texturePath, 1, 1, 6, 6, ids, collision), 0.0F, false);
      ids[8] = ObjectRegistry.registerObject(texturePath + "9", new TheCoreStatueObject(texturePath, 2, 1, 6, 6, ids, collision), 0.0F, false);
      ids[9] = ObjectRegistry.registerObject(texturePath + "10", new TheCoreStatueObject(texturePath, 3, 1, 6, 6, ids, collision), 0.0F, false);
      ids[10] = ObjectRegistry.registerObject(texturePath + "11", new TheCoreStatueObject(texturePath, 4, 1, 6, 6, ids, collision), 0.0F, false);
      ids[11] = ObjectRegistry.registerObject(texturePath + "12", new TheCoreStatueObject(texturePath, 5, 1, 6, 6, ids, collision), 0.0F, false);
      ids[12] = ObjectRegistry.registerObject(texturePath + "13", new TheCoreStatueObject(texturePath, 0, 2, 6, 6, ids, collision), 0.0F, false);
      ids[13] = ObjectRegistry.registerObject(texturePath + "14", new TheCoreStatueObject(texturePath, 1, 2, 6, 6, ids, collision), 0.0F, false);
      ids[14] = ObjectRegistry.registerObject(texturePath + "15", new TheCoreStatueObject(texturePath, 2, 2, 6, 6, ids, collision), 0.0F, false);
      ids[15] = ObjectRegistry.registerObject(texturePath + "16", new TheCoreStatueObject(texturePath, 3, 2, 6, 6, ids, collision), 0.0F, false);
      ids[16] = ObjectRegistry.registerObject(texturePath + "17", new TheCoreStatueObject(texturePath, 4, 2, 6, 6, ids, collision), 0.0F, false);
      ids[17] = ObjectRegistry.registerObject(texturePath + "18", new TheCoreStatueObject(texturePath, 5, 2, 6, 6, ids, collision), 0.0F, false);
      ids[18] = ObjectRegistry.registerObject(texturePath + "19", new TheCoreStatueObject(texturePath, 0, 3, 6, 6, ids, collision), 0.0F, false);
      ids[19] = ObjectRegistry.registerObject(texturePath + "20", new TheCoreStatueObject(texturePath, 1, 3, 6, 6, ids, collision), 0.0F, false);
      ids[20] = ObjectRegistry.registerObject(texturePath + "21", new TheCoreStatueObject(texturePath, 2, 3, 6, 6, ids, collision), 0.0F, false);
      ids[21] = ObjectRegistry.registerObject(texturePath + "22", new TheCoreStatueObject(texturePath, 3, 3, 6, 6, ids, collision), 0.0F, false);
      ids[22] = ObjectRegistry.registerObject(texturePath + "23", new TheCoreStatueObject(texturePath, 4, 3, 6, 6, ids, collision), 0.0F, false);
      ids[23] = ObjectRegistry.registerObject(texturePath + "24", new TheCoreStatueObject(texturePath, 5, 3, 6, 6, ids, collision), 0.0F, false);
      ids[24] = ObjectRegistry.registerObject(texturePath + "25", new TheCoreStatueObject(texturePath, 0, 4, 6, 6, ids, collision), 0.0F, false);
      ids[25] = ObjectRegistry.registerObject(texturePath + "26", new TheCoreStatueObject(texturePath, 1, 4, 6, 6, ids, collision), 0.0F, false);
      ids[26] = ObjectRegistry.registerObject(texturePath + "27", new TheCoreStatueObject(texturePath, 2, 4, 6, 6, ids, collision), 0.0F, false);
      ids[27] = ObjectRegistry.registerObject(texturePath + "28", new TheCoreStatueObject(texturePath, 3, 4, 6, 6, ids, collision), 0.0F, false);
      ids[28] = ObjectRegistry.registerObject(texturePath + "29", new TheCoreStatueObject(texturePath, 4, 4, 6, 6, ids, collision), 0.0F, false);
      ids[29] = ObjectRegistry.registerObject(texturePath + "30", new TheCoreStatueObject(texturePath, 5, 4, 6, 6, ids, collision), 0.0F, false);
      ids[30] = ObjectRegistry.registerObject(texturePath + "31", new TheCoreStatueObject(texturePath, 0, 5, 6, 6, ids, collision), 0.0F, false);
      ids[31] = ObjectRegistry.registerObject(texturePath + "32", new TheCoreStatueObject(texturePath, 1, 5, 6, 6, ids, collision), 0.0F, false);
      ids[32] = ObjectRegistry.registerObject(texturePath + "33", new TheCoreStatueObject(texturePath, 2, 5, 6, 6, ids, collision), 0.0F, false);
      ids[33] = ObjectRegistry.registerObject(texturePath + "34", new TheCoreStatueObject(texturePath, 3, 5, 6, 6, ids, collision), 0.0F, false);
      ids[34] = ObjectRegistry.registerObject(texturePath + "35", new TheCoreStatueObject(texturePath, 4, 5, 6, 6, ids, collision), 0.0F, false);
      ids[35] = ObjectRegistry.registerObject(texturePath + "36", new TheCoreStatueObject(texturePath, 5, 5, 6, 6, ids, collision), 0.0F, false);
      return ids;
   }
}
