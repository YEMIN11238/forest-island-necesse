package necesse.inventory.item.toolItem.glaiveToolItem;

import java.awt.Color;
import java.awt.geom.Point2D.Float;
import necesse.engine.network.gameNetworkData.GNDItemMap;
import necesse.engine.sound.SoundSettings;
import necesse.entity.levelEvent.GlaiveShowAttackEvent;
import necesse.entity.mobs.itemAttacker.ItemAttackerMob;
import necesse.entity.particle.Particle;
import necesse.gfx.GameResources;
import necesse.inventory.InventoryItem;
import necesse.inventory.item.Item;
import necesse.inventory.lootTable.presets.GlaiveWeaponsLootTable;
import necesse.level.maps.Level;

public class CryoGlaiveToolItem extends GlaiveToolItem {
   public CryoGlaiveToolItem() {
      super(1500, GlaiveWeaponsLootTable.glaiveWeapons);
      this.rarity = Item.Rarity.EPIC;
      this.attackAnimTime.setBaseValue(400);
      this.attackDamage.setBaseValue(60.0F).setUpgradedValue(1.0F, 75.83335F);
      this.attackRange.setBaseValue(160);
      this.knockback.setBaseValue(100);
      this.width = 20.0F;
      this.attackXOffset = 58;
      this.attackYOffset = 58;
      this.canBeUsedForRaids = true;
      this.raidTicketsModifier = 0.5F;
      this.useForRaidsOnlyIfObtained = true;
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
                     Float angleDir = this.getAngleDir(angle);
                     this.level
                        .entityManager
                        .addParticle(
                           this.attackMob.x + angleDir.x * 75.0F + this.attackMob.getCurrentAttackDrawXOffset(),
                           this.attackMob.y + angleDir.y * 75.0F + this.attackMob.getCurrentAttackDrawYOffset(),
                           Particle.GType.COSMETIC
                        )
                        .color(new Color(0, 222, 218))
                        .minDrawLight(150)
                        .givesLight(179.0F, 1.0F)
                        .lifeTime(400);
                     this.level
                        .entityManager
                        .addParticle(
                           this.attackMob.x - angleDir.x * 75.0F + this.attackMob.getCurrentAttackDrawXOffset(),
                           this.attackMob.y - angleDir.y * 75.0F + this.attackMob.getCurrentAttackDrawYOffset(),
                           Particle.GType.COSMETIC
                        )
                        .color(new Color(0, 222, 218))
                        .minDrawLight(150)
                        .givesLight(179.0F, 1.0F)
                        .lifeTime(400);
                  }
               }
            );
      }
   }

   @Override
   protected SoundSettings getAttackSound() {
      return new SoundSettings(GameResources.cryoGlaive).volume(0.4F);
   }
}
