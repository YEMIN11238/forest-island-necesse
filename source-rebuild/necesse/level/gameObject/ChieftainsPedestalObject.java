package necesse.level.gameObject;

import java.awt.Color;
import java.awt.Rectangle;
import java.util.List;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.localization.Localization;
import necesse.engine.localization.message.LocalMessage;
import necesse.engine.registries.ItemRegistry;
import necesse.engine.util.GameRandom;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.objectEntity.ChieftainsPedestalObjectEntity;
import necesse.entity.objectEntity.ObjectEntity;
import necesse.entity.particle.Particle;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.texture.TextureDrawOptions;
import necesse.gfx.drawables.LevelSortedDrawable;
import necesse.gfx.drawables.OrderableDrawables;
import necesse.gfx.gameTexture.GameTexture;
import necesse.inventory.item.Item;
import necesse.inventory.item.toolItem.ToolType;
import necesse.level.maps.Level;
import necesse.level.maps.light.GameLight;

public class ChieftainsPedestalObject extends GameObject {
   public GameTexture texture;
   protected boolean hasRotation;
   protected int yOffset = -3;

   public ChieftainsPedestalObject() {
      super(new Rectangle(2, 5, 28, 22));
      this.toolType = ToolType.UNBREAKABLE;
      this.mapColor = new Color(186, 136, 46);
      this.isLightTransparent = true;
      this.lightLevel = 255;
      this.hoverHitbox = new Rectangle(0, -32, 32, 64);
      this.collision = new Rectangle(0, -8, 32, 32);
      this.lightHue = 166.0F;
      this.lightSat = 0.7F;
   }

   @Override
   public void loadTextures() {
      super.loadTextures();
      this.texture = GameTexture.fromFile("objects/chieftainspedestal");
   }

   @Override
   public void tickEffect(Level level, int layerID, int tileX, int tileY) {
      super.tickEffect(level, layerID, tileX, tileY);
      ChieftainsPedestalObjectEntity chieftainsPedestal = this.getCurrentObjectEntity(level, tileX, tileY, ChieftainsPedestalObjectEntity.class);
      if (chieftainsPedestal != null && chieftainsPedestal.getAnimationHeightProgress() >= 1.0F) {
         float heightMultiplier = chieftainsPedestal.getAnimationHeightProgress();
         if (heightMultiplier > 0.0F && heightMultiplier < 1.0F) {
            if (GameRandom.globalRandom.getChance(heightMultiplier)) {
               int startHeight = -8 + GameRandom.globalRandom.nextInt(16);
               level.entityManager
                  .addParticle(tileX * 32 + GameRandom.globalRandom.getIntBetween(0, 32), tileY * 32 + 32, Particle.GType.COSMETIC)
                  .sizeFades(8, 12)
                  .smokeColor()
                  .heightMoves(startHeight, startHeight + 32)
                  .lifeTime(1000);
            }
         } else if (GameRandom.globalRandom.getChance(0.25F)) {
            level.entityManager
               .addParticle(
                  tileX * 32 + GameRandom.globalRandom.getIntBetween(6, 28),
                  tileY * 32 + GameRandom.globalRandom.getIntBetween(10, 16) + 28,
                  GameRandom.globalRandom.getChance(0.75F) ? Particle.GType.CRITICAL : Particle.GType.COSMETIC
               )
               .movesConstant(GameRandom.globalRandom.getFloatBetween(-1.0F, 1.0F), GameRandom.globalRandom.getFloatBetween(-1.0F, 1.0F))
               .color(new Color(77, 209, 178))
               .sizeFades(10, 14)
               .heightMoves(48.0F, 70.0F)
               .lifeTime(2000);
         }
      }
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
      int drawY = camera.getTileDrawY(tileY) + 32;
      ChieftainsPedestalObjectEntity objectEntity = level.entityManager.getObjectEntity(tileX, tileY, ChieftainsPedestalObjectEntity.class);
      float heightProgress = 1.0F;
      if (objectEntity != null) {
         heightProgress = objectEntity.getAnimationHeightProgress();
      }

      int endY = (int)(heightProgress * this.texture.getHeight());
      final TextureDrawOptions drawOptions;
      if (this.hasRotation) {
         byte rotation = level.getObjectRotation(tileX, tileY);
         int spriteRes = this.texture.getWidth() / 4;
         int spriteX = rotation % 4;
         int xOffset = (spriteRes - 32) / 2;
         drawOptions = this.texture
            .initDraw()
            .section(spriteX * spriteRes, spriteX * spriteRes + spriteRes, 0, endY)
            .addObjectDamageOverlay(this, level, tileX, tileY)
            .light(light)
            .pos(drawX - xOffset, drawY + this.yOffset - endY);
      } else {
         int xOffset = (this.texture.getWidth() - 32) / 2;
         drawOptions = this.texture
            .initDraw()
            .section(0, this.texture.getWidth(), 0, endY)
            .addObjectDamageOverlay(this, level, tileX, tileY)
            .light(light)
            .pos(drawX - xOffset, drawY + this.yOffset - endY);
      }

      list.add(new LevelSortedDrawable(this, tileX, tileY) {
         @Override
         public int getSortY() {
            return 16;
         }

         @Override
         public void draw(TickManager tickManager) {
            drawOptions.draw();
         }
      });
   }

   @Override
   public void drawPreview(Level level, int tileX, int tileY, int rotation, float alpha, PlayerMob player, GameCamera camera) {
      int drawX = camera.getTileDrawX(tileX);
      int drawY = camera.getTileDrawY(tileY) - this.texture.getHeight() + 32;
      if (this.hasRotation) {
         int spriteWidth = this.texture.getWidth() / 4;
         int xOffset = (spriteWidth - 32) / 2;
         this.texture.initDraw().sprite(rotation % 4, 0, spriteWidth, this.texture.getHeight()).alpha(alpha).draw(drawX - xOffset, drawY + this.yOffset);
      } else {
         int xOffset = (this.texture.getWidth() - 32) / 2;
         this.texture.initDraw().alpha(alpha).draw(drawX - xOffset, drawY + this.yOffset);
      }
   }

   @Override
   public boolean canInteract(Level level, int x, int y, PlayerMob player) {
      ChieftainsPedestalObjectEntity objectEntity = this.getCurrentObjectEntity(level, x, y, ChieftainsPedestalObjectEntity.class);
      return objectEntity == null || objectEntity.shouldHaveCollision();
   }

   @Override
   public String getInteractTip(Level level, int x, int y, PlayerMob perspective, boolean debug) {
      return Localization.translate("controls", "activatetip");
   }

   @Override
   public void interact(Level level, int x, int y, PlayerMob player) {
      super.interact(level, x, y, player);
      ChieftainsPedestalObjectEntity currentObjectEntity = this.getCurrentObjectEntity(level, x, y, ChieftainsPedestalObjectEntity.class);
      if (currentObjectEntity != null && currentObjectEntity.canStartChieftainEvent()) {
         Item item = ItemRegistry.getItem("boneoffering");
         if (!player.isItemOnCooldown(item)) {
            if (player.getInv().removeItems(item, 1, false, false, false, false, "use") > 0) {
               player.startItemCooldown(item, 2000);
               if (level.isServer()) {
                  currentObjectEntity.startChieftainEvent();
               }
            } else if (level.isServer() && player.isServerClient()) {
               player.getServerClient().sendChatMessage(new LocalMessage("misc", "bossmissingitem"));
            }
         }
      }
   }

   @Override
   public ObjectEntity getNewObjectEntity(Level level, int x, int y) {
      return new ChieftainsPedestalObjectEntity(level, x, y);
   }

   @Override
   protected Rectangle getCollision(Level level, int x, int y, int rotation) {
      ChieftainsPedestalObjectEntity objectEntity = this.getCurrentObjectEntity(level, x, y, ChieftainsPedestalObjectEntity.class);
      return objectEntity != null && !objectEntity.shouldHaveCollision() ? new Rectangle() : super.getCollision(level, x, y, rotation);
   }

   @Override
   public boolean isSolid(Level level, int x, int y) {
      ChieftainsPedestalObjectEntity objectEntity = this.getCurrentObjectEntity(level, x, y, ChieftainsPedestalObjectEntity.class);
      return objectEntity != null && !objectEntity.shouldHaveCollision() ? false : super.isSolid(level, x, y);
   }
}
