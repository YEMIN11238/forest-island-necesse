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
import necesse.entity.objectEntity.FueledIncineratorObjectEntity;
import necesse.entity.objectEntity.ObjectEntity;
import necesse.entity.objectEntity.interfaces.OEUsers;
import necesse.entity.particle.Particle;
import necesse.entity.particle.ParticleOption;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.texture.SharedTextureDrawOptions;
import necesse.gfx.drawables.LevelSortedDrawable;
import necesse.gfx.drawables.OrderableDrawables;
import necesse.gfx.gameTexture.GameTexture;
import necesse.gfx.gameTooltips.ListGameTooltips;
import necesse.inventory.InventoryItem;
import necesse.inventory.container.object.OEInventoryContainer;
import necesse.inventory.item.Item;
import necesse.inventory.item.toolItem.ToolType;
import necesse.level.gameObject.GameObject;
import necesse.level.maps.Level;
import necesse.level.maps.light.GameLight;

public class IncineratorInventoryObject extends GameObject {
   public GameTexture texture;
   public GameTexture openTexture;
   public GameTexture activeTexture;
   public GameTexture activeOpenTexture;

   public IncineratorInventoryObject() {
      super(new Rectangle(2, 6, 28, 20));
      this.setItemCategory("objects", "craftingstations");
      this.setCraftingCategory("craftingstations");
      this.mapColor = new Color(115, 115, 127);
      this.displayMapTooltip = true;
      this.toolType = ToolType.ALL;
      this.rarity = Item.Rarity.COMMON;
      this.objectHealth = 50;
      this.isLightTransparent = true;
      this.lightHue = 50.0F;
      this.lightSat = 0.2F;
      this.hoverHitbox = new Rectangle(0, -16, 32, 48);
      this.replaceCategories.add("workstation");
      this.canReplaceCategories.add("workstation");
      this.canReplaceCategories.add("wall");
      this.canReplaceCategories.add("furniture");
   }

   @Override
   public void loadTextures() {
      super.loadTextures();
      this.texture = GameTexture.fromFile("objects/incinerator");
      this.openTexture = GameTexture.fromFile("objects/incinerator_open");
      this.activeTexture = GameTexture.fromFile("objects/incinerator_active");
      this.activeOpenTexture = GameTexture.fromFile("objects/incinerator_active_open");
   }

   @Override
   public ListGameTooltips getItemTooltips(InventoryItem item, PlayerMob perspective) {
      ListGameTooltips tooltips = super.getItemTooltips(item, perspective);
      tooltips.add(Localization.translate("itemtooltip", "incineratortip"), 400);
      return tooltips;
   }

   @Override
   public int getLightLevel(Level level, int layerID, int tileX, int tileY) {
      FueledIncineratorObjectEntity incineratorObjectEntity = this.getIncineratorObjectEntity(level, tileX, tileY);
      return incineratorObjectEntity != null && incineratorObjectEntity.isFuelRunning() ? 100 : 0;
   }

   @Override
   public void tickEffect(Level level, int layerID, int tileX, int tileY) {
      super.tickEffect(level, layerID, tileX, tileY);
      FueledIncineratorObjectEntity incineratorObjectEntity = this.getIncineratorObjectEntity(level, tileX, tileY);
      if (incineratorObjectEntity != null && incineratorObjectEntity.isFuelRunning()) {
         if (incineratorObjectEntity.isInUse()) {
            float buffer = 0.5F;

            while (buffer >= 1.0F || GameRandom.globalRandom.getChance(buffer)) {
               buffer--;
               int startHeight = 30;
               int yOffset = 8;
               ParticleOption particleOption = level.entityManager
                  .addParticle(
                     tileX * 32 + GameRandom.globalRandom.getIntBetween(11, 21),
                     tileY * 32 + GameRandom.globalRandom.getIntBetween(10, 16) + yOffset,
                     GameRandom.globalRandom.getChance(0.75F) ? Particle.GType.IMPORTANT_COSMETIC : Particle.GType.COSMETIC
                  )
                  .movesConstant(GameRandom.globalRandom.getFloatBetween(-1.0F, 1.0F), GameRandom.globalRandom.getFloatBetween(-1.0F, 1.0F))
                  .heightMoves(startHeight - yOffset, startHeight + 10 - yOffset)
                  .colorRandom(30.0F, 1.0F, 0.9F, 12.0F, 0.1F, 0.1F)
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
                              .heightMoves(startHeight + 6, startHeight + 20);
                        }
                     }
                  );
               }
            }
         } else if (GameRandom.globalRandom.nextInt(10) == 0) {
            int startHeight = 24 + GameRandom.globalRandom.nextInt(8);
            level.entityManager
               .addParticle(tileX * 32 + GameRandom.globalRandom.getIntBetween(8, 24), tileY * 32 + 32, Particle.GType.COSMETIC)
               .smokeColor()
               .heightMoves(startHeight, startHeight + 20)
               .lifeTime(1000);
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
      FueledIncineratorObjectEntity incineratorObjectEntity = this.getIncineratorObjectEntity(level, tileX, tileY);
      GameTexture texture = incineratorObjectEntity != null && incineratorObjectEntity.isFuelRunning() ? this.activeTexture : this.texture;
      if (this.openTexture != null) {
         ObjectEntity ent = level.entityManager.getObjectEntity(tileX, tileY);
         if (ent != null && ent.implementsOEUsers() && ((OEUsers)ent).isInUse()) {
            texture = incineratorObjectEntity != null && incineratorObjectEntity.isFuelRunning() ? this.activeOpenTexture : this.openTexture;
         }
      }

      int rotation = level.getObjectRotation(tileX, tileY) % (texture.getWidth() / 32);
      final SharedTextureDrawOptions draws = new SharedTextureDrawOptions(texture).addObjectDamageOverlay(this, level, tileX, tileY);
      draws.addSprite(rotation, 0, 32, texture.getHeight())
         .startGlowOptions(level, this.getID())
         .light(light)
         .applyTreasure(perspective)
         .pos(drawX, drawY - texture.getHeight() + 32);
      list.add(new LevelSortedDrawable(this, tileX, tileY) {
         @Override
         public int getSortY() {
            return 16;
         }

         @Override
         public void draw(TickManager tickManager) {
            draws.draw();
         }
      });
   }

   @Override
   public void drawPreview(Level level, int tileX, int tileY, int rotation, float alpha, PlayerMob player, GameCamera camera) {
      int drawX = camera.getTileDrawX(tileX);
      int drawY = camera.getTileDrawY(tileY);
      rotation %= this.texture.getWidth() / 32;
      this.texture.initDraw().sprite(rotation, 0, 32, this.texture.getHeight()).alpha(alpha).draw(drawX, drawY - this.texture.getHeight() + 32);
   }

   @Override
   public ObjectEntity getNewObjectEntity(Level level, int x, int y) {
      return new FueledIncineratorObjectEntity(level, x, y, 2, 10);
   }

   @Override
   public String getInteractTip(Level level, int x, int y, PlayerMob perspective, boolean debug) {
      return Localization.translate("controls", "opentip");
   }

   @Override
   public boolean canInteract(Level level, int x, int y, PlayerMob player) {
      return true;
   }

   @Override
   public void interact(Level level, int x, int y, PlayerMob player) {
      super.interact(level, x, y, player);
      if (level.isServer()) {
         OEInventoryContainer.openAndSendContainer(ContainerRegistry.INCINERATOR_INVENTORY_CONTAINER, player.getServerClient(), level, x, y);
      }
   }

   public FueledIncineratorObjectEntity getIncineratorObjectEntity(Level level, int tileX, int tileY) {
      ObjectEntity objectEntity = level.entityManager.getObjectEntity(tileX, tileY);
      return objectEntity instanceof FueledIncineratorObjectEntity ? (FueledIncineratorObjectEntity)objectEntity : null;
   }

   @Override
   protected boolean shouldPlayInteractSound(Level level, int tileX, int tileY) {
      return true;
   }

   @Override
   protected SoundSettings getInteractSoundOpen() {
      return SoundSettingsRegistry.defaultOpen;
   }

   @Override
   protected SoundSettings getInteractSoundClose() {
      return null;
   }

   @Override
   protected boolean interactSoundIsGlobal() {
      return true;
   }

   @Override
   protected boolean interactSoundIsFirstAndLastOnly() {
      return true;
   }
}
