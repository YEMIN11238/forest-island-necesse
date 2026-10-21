package necesse.level.gameObject.container;

import java.awt.Rectangle;
import necesse.engine.localization.Localization;
import necesse.engine.registries.ObjectRegistry;
import necesse.engine.sound.SoundSettings;
import necesse.engine.sound.SoundSettingsRegistry;
import necesse.engine.util.GameRandom;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.objectEntity.CampfireObjectEntity;
import necesse.entity.objectEntity.FueledInventoryObjectEntity;
import necesse.entity.objectEntity.ObjectEntity;
import necesse.entity.particle.Particle;
import necesse.entity.particle.ParticleOption;
import necesse.gfx.gameTooltips.ListGameTooltips;
import necesse.inventory.InventoryItem;
import necesse.inventory.item.Item;
import necesse.inventory.item.placeableItem.objectItem.CampfireAddonObjectItem;
import necesse.inventory.item.toolItem.ToolType;
import necesse.inventory.lootTable.LootTable;
import necesse.level.maps.Level;

public class CampfireAddonObject extends FueledCraftingStationObject {
   public CampfireAddonObject() {
      super(new Rectangle(4, 6, 24, 20));
      this.objectHealth = 50;
      this.toolType = ToolType.ALL;
      this.isLightTransparent = true;
      this.lightHue = 50.0F;
      this.lightSat = 0.5F;
   }

   @Override
   public int getLightLevel(Level level, int layerID, int tileX, int tileY) {
      FueledInventoryObjectEntity fueledObjectEntity = this.getFueledObjectEntity(level, tileX, tileY);
      return fueledObjectEntity != null && fueledObjectEntity.isFueled() ? 100 : 0;
   }

   @Override
   public void tickEffect(Level level, int layerID, int tileX, int tileY) {
      FueledInventoryObjectEntity fueledObjectEntity = this.getFueledObjectEntity(level, tileX, tileY);
      if (fueledObjectEntity != null && fueledObjectEntity.isFueled()) {
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
   public ObjectEntity getNewObjectEntity(Level level, int x, int y) {
      return new CampfireObjectEntity(level, this.getStringID(), x, y, false, false);
   }

   @Override
   public LootTable getLootTable(Level level, int layerID, int tileX, int tileY) {
      return new LootTable(super.getLootTable(level, layerID, tileX, tileY), ObjectRegistry.getObject("campfire").getLootTable(level, layerID, tileX, tileY));
   }

   @Override
   public String canPlace(Level level, int layerID, int x, int y, int rotation, boolean byPlayer, boolean ignoreOtherLayers) {
      return !level.getObject(layerID, x, y).getStringID().equals("campfire") ? "notcampfire" : null;
   }

   @Override
   public ListGameTooltips getItemTooltips(InventoryItem item, PlayerMob perspective) {
      ListGameTooltips tooltips = new ListGameTooltips();
      tooltips.add(Localization.translate("itemtooltip", "campfireaddontip"));
      return tooltips;
   }

   @Override
   public Item generateNewObjectItem() {
      return new CampfireAddonObjectItem(this);
   }

   @Override
   protected SoundSettings getInteractSoundOpen() {
      return SoundSettingsRegistry.defaultOpen;
   }
}
