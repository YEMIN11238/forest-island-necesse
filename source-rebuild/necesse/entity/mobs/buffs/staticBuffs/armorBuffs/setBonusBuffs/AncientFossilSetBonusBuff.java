package necesse.entity.mobs.buffs.staticBuffs.armorBuffs.setBonusBuffs;

import java.awt.Color;
import java.util.concurrent.atomic.AtomicReference;
import necesse.engine.localization.Localization;
import necesse.engine.network.Packet;
import necesse.engine.registries.BuffRegistry;
import necesse.engine.util.GameBlackboard;
import necesse.engine.util.GameMath;
import necesse.engine.util.GameRandom;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.buffs.ActiveBuff;
import necesse.entity.mobs.buffs.BuffAbility;
import necesse.entity.mobs.buffs.BuffEventSubscriber;
import necesse.entity.particle.Particle;
import necesse.gfx.gameTooltips.ListGameTooltips;

public class AncientFossilSetBonusBuff extends SetBonusBuff implements BuffAbility {
   @Override
   public void init(ActiveBuff buff, BuffEventSubscriber eventSubscriber) {
   }

   @Override
   public void tickEffect(ActiveBuff buff, Mob owner) {
   }

   @Override
   public void runAbility(PlayerMob player, ActiveBuff buff, Packet content) {
      Mob owner = buff.owner;
      float active = 5.0F;
      float cooldown = 60.0F;
      owner.buffManager.addBuff(new ActiveBuff(BuffRegistry.Debuffs.ANCIENT_FOSSIL_SET_COOLDOWN, owner, cooldown, null), false);
      owner.buffManager.addBuff(new ActiveBuff(BuffRegistry.ANCIENT_FOSSIL_ACTIVE, owner, active, null), false);
      int minHeight = 0;
      int maxHeight = 40;
      int particles = 40;

      for (int i = 0; i < particles; i++) {
         float height = minHeight + (float)(maxHeight - minHeight) * i / particles;
         AtomicReference<Float> currentAngle = new AtomicReference<>(GameRandom.globalRandom.nextFloat() * 360.0F);
         float distance = 20.0F;
         player.getLevel()
            .entityManager
            .addParticle(
               player.x + GameMath.sin(currentAngle.get()) * distance, player.y + GameMath.cos(currentAngle.get()) * distance * 0.75F, Particle.GType.CRITICAL
            )
            .color(new Color(95, 82, 44))
            .height(height)
            .moves((pos, delta, lifeTime, timeAlive, lifePercent) -> {
               float angle = currentAngle.accumulateAndGet(delta * 150.0F / 250.0F, Float::sum);
               float distY = distance * 0.75F;
               pos.x = player.x + GameMath.sin(angle) * distance;
               pos.y = player.y + GameMath.cos(angle) * distY * 0.75F;
            })
            .lifeTime((int)(active * 1000.0F))
            .sizeFades(16, 24);
      }
   }

   @Override
   public boolean canRunAbility(PlayerMob player, ActiveBuff buff, Packet content) {
      return !buff.owner.buffManager.hasBuff(BuffRegistry.Debuffs.ANCIENT_FOSSIL_SET_COOLDOWN.getID());
   }

   @Override
   public ListGameTooltips getTooltip(ActiveBuff ab, GameBlackboard blackboard) {
      ListGameTooltips tooltips = super.getTooltip(ab, blackboard);
      tooltips.add(Localization.translate("itemtooltip", "ancientfossilset"));
      return tooltips;
   }
}
