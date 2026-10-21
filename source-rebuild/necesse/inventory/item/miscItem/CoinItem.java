package necesse.inventory.item.miscItem;

import java.awt.Color;
import necesse.engine.localization.Localization;
import necesse.engine.util.GameBlackboard;
import necesse.engine.util.GameRandom;
import necesse.engine.util.GameUtils;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.particle.Particle;
import necesse.entity.particle.ParticleOption;
import necesse.entity.pickup.ItemPickupEntity;
import necesse.gfx.GameResources;
import necesse.gfx.gameTexture.GameSprite;
import necesse.gfx.gameTooltips.ListGameTooltips;
import necesse.inventory.InventoryItem;
import necesse.inventory.item.placeableItem.objectItem.ObjectItem;
import necesse.level.gameObject.GameObject;

public class CoinItem extends ObjectItem {
   public CoinItem(GameObject object) {
      super(object);
      this.keyWords.add("currency");
      this.worldDrawSize = 28;
      this.attackAnimTime.setBaseValue(100);
   }

   @Override
   public ListGameTooltips getTooltips(InventoryItem item, PlayerMob perspective, GameBlackboard blackboard) {
      ListGameTooltips tooltips = super.getTooltips(item, perspective, blackboard);
      tooltips.add(Localization.translate("itemtooltip", "cointip"));
      return tooltips;
   }

   @Override
   public GameSprite getItemSprite(InventoryItem item, PlayerMob perspective) {
      return item.getGndData().getBoolean("showCoinStackIcon") ? new GameSprite(GameResources.coinStackIcon) : new GameSprite(this.itemTexture, 0, 0, 32);
   }

   @Override
   public GameSprite getWorldItemSprite(InventoryItem item, PlayerMob perspective) {
      if (perspective != null && perspective.getWorldEntity() != null) {
         long time = perspective.getWorldEntity().getTime() % 2500L;
         int sprite;
         if (time < 2000L) {
            sprite = 0;
         } else {
            sprite = GameUtils.getAnim(time - 2000L, this.itemTexture.getWidth() / 32, 500);
         }

         return new GameSprite(this.itemTexture, sprite, 0, 32);
      } else {
         return super.getWorldItemSprite(item, perspective);
      }
   }

   @Override
   public void tickPickupEntity(ItemPickupEntity entity) {
      super.tickPickupEntity(entity);
      if (entity.isClient()) {
         boolean giveLight = (Math.abs(entity.dx) > 2.0F || Math.abs(entity.dy) > 2.0F) && GameRandom.globalRandom.getEveryXthChance(2);
         if (entity.getTarget() != null || giveLight || GameRandom.globalRandom.getEveryXthChance(20)) {
            ParticleOption particle = entity.getLevel()
               .entityManager
               .addParticle(
                  entity.x + GameRandom.globalRandom.floatGaussian() * 2.0F,
                  entity.y + 2.0F + GameRandom.globalRandom.floatGaussian() * 2.0F,
                  Particle.GType.COSMETIC
               )
               .movesConstant(GameRandom.globalRandom.floatGaussian() * 4.0F, GameRandom.globalRandom.floatGaussian() * 4.0F)
               .sizeFades(6, 12)
               .color(new Color(-1123718))
               .heightMoves(0.0F, 10.0F);
            if (giveLight) {
               particle.givesLight(50.0F, 0.8F);
            }
         }
      }
   }

   @Override
   public String getTranslatedTypeName() {
      return Localization.translate("item", "wealth");
   }
}
