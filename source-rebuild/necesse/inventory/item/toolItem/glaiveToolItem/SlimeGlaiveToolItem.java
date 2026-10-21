package necesse.inventory.item.toolItem.glaiveToolItem;

import java.awt.Color;
import java.awt.geom.Point2D.Float;
import necesse.engine.network.gameNetworkData.GNDItemMap;
import necesse.engine.sound.SoundSettings;
import necesse.engine.util.GameRandom;
import necesse.entity.levelEvent.GlaiveShowAttackEvent;
import necesse.entity.mobs.itemAttacker.ItemAttackerMob;
import necesse.entity.particle.Particle;
import necesse.gfx.GameResources;
import necesse.inventory.InventoryItem;
import necesse.inventory.item.Item;
import necesse.inventory.lootTable.presets.IncursionGlaiveWeaponsLootTable;
import necesse.level.maps.Level;
import necesse.level.maps.incursion.IncursionData;

public class SlimeGlaiveToolItem extends GlaiveToolItem {
   public SlimeGlaiveToolItem() {
      super(1900, IncursionGlaiveWeaponsLootTable.incursionGlaiveWeapons);
      this.rarity = Item.Rarity.EPIC;
      this.attackAnimTime.setBaseValue(400);
      this.attackDamage.setBaseValue(60.0F).setUpgradedValue(1.0F, 75.83335F);
      this.attackRange.setBaseValue(200);
      this.knockback.setBaseValue(150);
      this.width = 20.0F;
      this.attackXOffset = 74;
      this.attackYOffset = 74;
      this.canBeUsedForRaids = true;
      this.useForRaidsOnlyIfObtained = true;
      this.minRaidTier = 1;
      this.maxRaidTier = IncursionData.ITEM_TIER_UPGRADE_CAP;
      this.raidTicketsModifier = 0.25F;
   }

   @Override
   public void showAttack(
      Level level, int x, int y, ItemAttackerMob attackerMob, int attackHeight, InventoryItem item, int animAttack, int seed, GNDItemMap mapContent
   ) {
      super.showAttack(level, x, y, attackerMob, attackHeight, item, animAttack, seed, mapContent);
      if (level.isClient()) {
         level.entityManager
            .events
            .addHidden(
               new GlaiveShowAttackEvent(attackerMob, x, y, seed, 10.0F) {
                  @Override
                  public void tick(float angle) {
                     GameRandom gameRandom = new GameRandom();
                     float colorModifier = gameRandom.getFloatBetween(0.0F, 1.0F);
                     Color randomColor = SlimeGlaiveToolItem.this.getParticleColor(colorModifier);
                     Float angleDir = this.getAngleDir(angle);
                     this.level
                        .entityManager
                        .addParticle(
                           this.attackMob.x + angleDir.x * 85.0F + this.attackMob.getCurrentAttackDrawXOffset(),
                           this.attackMob.y + angleDir.y * 85.0F + this.attackMob.getCurrentAttackDrawYOffset(),
                           Particle.GType.COSMETIC
                        )
                        .sprite(GameResources.bubbleParticle.sprite(0, 0, 12))
                        .color(randomColor)
                        .movesConstant(angleDir.x * 40.0F, angleDir.y * 40.0F)
                        .lifeTime(400);
                     this.level
                        .entityManager
                        .addParticle(
                           this.attackMob.x - angleDir.x * 85.0F + this.attackMob.getCurrentAttackDrawXOffset(),
                           this.attackMob.y - angleDir.y * 85.0F + this.attackMob.getCurrentAttackDrawYOffset(),
                           Particle.GType.COSMETIC
                        )
                        .sprite(GameResources.bubbleParticle.sprite(0, 0, 12))
                        .color(randomColor)
                        .movesConstant(angleDir.x * -40.0F, angleDir.y * -40.0F)
                        .lifeTime(400);
                  }
               }
            );
      }
   }

   private Color getParticleColor(float modifier) {
      return new Color((int)(70.0F * (1.0F + 1.8F * modifier)), (int)(178.0F * (1.0F + 0.3F * modifier)), (int)(170.0F * (1.0F + 0.2F * modifier)));
   }

   @Override
   protected SoundSettings getAttackSound() {
      return new SoundSettings(GameResources.slimeGlaive).volume(0.26F);
   }
}
