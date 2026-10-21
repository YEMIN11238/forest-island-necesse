package necesse.level.gameObject.container;

import java.awt.Color;
import java.awt.Rectangle;
import java.util.List;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.localization.Localization;
import necesse.engine.registries.ContainerRegistry;
import necesse.engine.sound.SoundSettings;
import necesse.engine.sound.SoundSettingsRegistry;
import necesse.engine.util.GameRandom;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.objectEntity.CampfireObjectEntity;
import necesse.entity.objectEntity.FueledInventoryObjectEntity;
import necesse.entity.objectEntity.ObjectEntity;
import necesse.entity.particle.Particle;
import necesse.entity.particle.ParticleOption;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.texture.TextureDrawOptions;
import necesse.gfx.drawables.LevelSortedDrawable;
import necesse.gfx.drawables.OrderableDrawables;
import necesse.gfx.gameTexture.GameTexture;
import necesse.gfx.gameTooltips.ListGameTooltips;
import necesse.inventory.InventoryItem;
import necesse.inventory.container.object.OEInventoryContainer;
import necesse.inventory.item.toolItem.ToolType;
import necesse.level.gameObject.GameObject;
import necesse.level.maps.Level;
import necesse.level.maps.light.GameLight;

public class CampfireObject extends GameObject {
   public GameTexture texture;

   public CampfireObject() {
      super(new Rectangle(4, 6, 24, 20));
      this.mapColor = new Color(233, 134, 39);
      this.displayMapTooltip = true;
      this.objectHealth = 50;
      this.toolType = ToolType.ALL;
      this.isLightTransparent = true;
      this.lightHue = 50.0F;
      this.lightSat = 0.5F;
      this.setItemCategory("objects", "misc");
      this.setCraftingCategory("objects", "misc");
   }

   @Override
   public void loadTextures() {
      super.loadTextures();
      this.texture = GameTexture.fromFile("objects/campfire");
   }

   @Override
   public int getLightLevel(Level level, int layerID, int tileX, int tileY) {
      ObjectEntity objectEntity = level.entityManager.getObjectEntity(tileX, tileY);
      return objectEntity instanceof FueledInventoryObjectEntity && ((FueledInventoryObjectEntity)objectEntity).isFueled() ? 100 : 0;
   }

   @Override
   public void tickEffect(Level level, int layerID, int tileX, int tileY) {
      ObjectEntity objectEntity = level.entityManager.getObjectEntity(tileX, tileY);
      if (objectEntity instanceof FueledInventoryObjectEntity && ((FueledInventoryObjectEntity)objectEntity).isFueled()) {
         float buffer = 0.5F;

         while (buffer >= 1.0F || GameRandom.globalRandom.getChance(buffer)) {
            buffer--;
            ParticleOption particleOption = level.entityManager
               .addParticle(
                  tileX * 32 + GameRandom.globalRandom.getIntBetween(11, 21),
                  tileY * 32 + GameRandom.globalRandom.getIntBetween(10, 16),
                  GameRandom.globalRandom.getChance(0.75F) ? Particle.GType.CRITICAL : Particle.GType.COSMETIC
               )
               .movesConstant(GameRandom.globalRandom.getFloatBetween(-1.0F, 1.0F), GameRandom.globalRandom.getFloatBetween(-1.0F, 1.0F))
               .heightMoves(0.0F, 10.0F)
               .flameColor()
               .sizeFades(10, 14)
               .lifeTime(2000);
            if (GameRandom.globalRandom.nextBoolean()) {
               particleOption.onProgress(
                  0.5F,
                  p -> {
                     for (int i = 0; i < GameRandom.globalRandom.getIntBetween(1, 2); i++) {
                        level.entityManager
                           .addParticle(p.x + (int)(GameRandom.globalRandom.nextGaussian() * 2.0), p.y, Particle.GType.COSMETIC)
                           .smokeColor()
                           .sizeFades(8, 12)
                           .heightMoves(6.0F, 20.0F);
                     }
                  }
               );
            }
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
      int drawY = camera.getTileDrawY(tileY);
      TextureDrawOptions options = this.texture
         .initDraw()
         .sprite(0, 0, 32, 32)
         .addObjectDamageOverlay(this, level, tileX, tileY)
         .light(light)
         .pos(drawX, drawY - (this.texture.getHeight() - 32));
      boolean isFueled = false;
      ObjectEntity objectEntity = level.entityManager.getObjectEntity(tileX, tileY);
      if (objectEntity instanceof CampfireObjectEntity) {
         CampfireObjectEntity coe = (CampfireObjectEntity)objectEntity;
         isFueled = coe.isFueled();
      }

      TextureDrawOptions flame;
      if (isFueled) {
         int spriteX = (int)(level.getWorldEntity().getWorldTime() % 6000L / 2000L);
         flame = this.texture.initDraw().sprite(spriteX + 1, 0, 32).light(light).pos(drawX, drawY);
      } else {
         flame = null;
      }

      tileList.add(tm -> {
         options.draw();
         if (flame != null) {
            flame.draw();
         }
      });
   }

   @Override
   public void drawPreview(Level level, int tileX, int tileY, int rotation, float alpha, PlayerMob player, GameCamera camera) {
      int drawX = camera.getTileDrawX(tileX);
      int drawY = camera.getTileDrawY(tileY);
      this.texture.initDraw().sprite(0, 0, 32, 32).alpha(alpha).draw(drawX, drawY - (this.texture.getHeight() - 32));
   }

   @Override
   public boolean canInteract(Level level, int x, int y, PlayerMob player) {
      return true;
   }

   @Override
   public String getInteractTip(Level level, int x, int y, PlayerMob perspective, boolean debug) {
      return Localization.translate("controls", "opentip");
   }

   @Override
   public void interact(Level level, int x, int y, PlayerMob player) {
      super.interact(level, x, y, player);
      if (level.isServer()) {
         OEInventoryContainer.openAndSendContainer(ContainerRegistry.FUELED_OE_INVENTORY_CONTAINER, player.getServerClient(), level, x, y);
      }
   }

   @Override
   public ObjectEntity getNewObjectEntity(Level level, int x, int y) {
      return new CampfireObjectEntity(level, "campfire", x, y, true, true);
   }

   @Override
   public ListGameTooltips getItemTooltips(InventoryItem item, PlayerMob perspective) {
      ListGameTooltips tooltips = super.getItemTooltips(item, perspective);
      tooltips.add(Localization.translate("itemtooltip", "campfiretip"));
      return tooltips;
   }

   @Override
   protected boolean shouldPlayInteractSound(Level level, int tileX, int tileY) {
      return true;
   }

   @Override
   protected SoundSettings getInteractSoundOpen() {
      return SoundSettingsRegistry.defaultOpen;
   }
}
