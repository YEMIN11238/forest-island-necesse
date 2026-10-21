package necesse.inventory.item.trinketItem;

import java.awt.Color;
import necesse.engine.localization.Localization;
import necesse.engine.util.GameBlackboard;
import necesse.engine.util.GameRandom;
import necesse.entity.ParticleTypeSwitcher;
import necesse.entity.mobs.GameDamage;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.MobWasHitEvent;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.particle.Particle;
import necesse.entity.projectile.CrystalShieldRetaliationProjectile;
import necesse.gfx.GameResources;
import necesse.gfx.gameTooltips.ListGameTooltips;
import necesse.inventory.InventoryItem;
import necesse.inventory.item.Item;
import necesse.inventory.lootTable.presets.IncursionTrinketsLootTable;

public class CrystalShieldTrinketItem extends ShieldTrinketItem {
   public CrystalShieldTrinketItem(Item.Rarity rarity, int enchantCost) {
      super(rarity, 5, 0.5F, 3000, 0.1F, 25, 360.0F, enchantCost, IncursionTrinketsLootTable.incursionTrinkets);
   }

   @Override
   public ListGameTooltips getExtraShieldTooltips(InventoryItem item, PlayerMob perspective, GameBlackboard blackboard) {
      ListGameTooltips tooltips = super.getExtraShieldTooltips(item, perspective, blackboard);
      tooltips.add(Localization.translate("itemtooltip", "crystalshieldtip"));
      return tooltips;
   }

   @Override
   public void onShieldHit(InventoryItem item, Mob mob, MobWasHitEvent hitEvent) {
      if (mob.isServer()) {
         if (hitEvent.attacker != null && hitEvent.attacker.getAttackOwner() != null) {
            Mob target = hitEvent.attacker.getAttackOwner();
            CrystalShieldRetaliationProjectile projectile = new CrystalShieldRetaliationProjectile(
               mob.getLevel(), mob.x, mob.y, target.x, target.y, 300, new GameDamage(hitEvent.damage * 4 * 2), mob
            );
            mob.getLevel().entityManager.projectiles.add(projectile);
         }
      } else {
         this.drawBlockParticles(mob);
      }

      super.onShieldHit(item, mob, hitEvent);
   }

   private void drawBlockParticles(Mob mob) {
      ParticleTypeSwitcher typeSwitcher = new ParticleTypeSwitcher(Particle.GType.CRITICAL, Particle.GType.IMPORTANT_COSMETIC, Particle.GType.COSMETIC);
      int particleCount = 10;
      float anglePerParticle = 360.0F / particleCount;

      for (int i = 0; i < particleCount; i++) {
         int angle = (int)(i * anglePerParticle + GameRandom.globalRandom.nextFloat() * anglePerParticle);
         float dx = (float)Math.sin(Math.toRadians(angle)) * GameRandom.globalRandom.getIntBetween(25, 50);
         float dy = (float)Math.cos(Math.toRadians(angle)) * GameRandom.globalRandom.getIntBetween(25, 50);
         mob.getLevel()
            .entityManager
            .addParticle(mob, typeSwitcher.next())
            .color(new Color(131, 198, 247))
            .movesFriction(dx, dy, 0.8F)
            .sprite(GameResources.magicSparkParticles.sprite(GameRandom.globalRandom.nextInt(4), 0, 22))
            .sizeFades(30, 40)
            .givesLight(180.0F, 200.0F)
            .lifeTime(500);
      }
   }
}
