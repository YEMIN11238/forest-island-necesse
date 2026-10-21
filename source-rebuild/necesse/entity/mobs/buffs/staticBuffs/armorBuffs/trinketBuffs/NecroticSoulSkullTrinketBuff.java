package necesse.entity.mobs.buffs.staticBuffs.armorBuffs.trinketBuffs;

import java.util.concurrent.atomic.AtomicReference;
import necesse.engine.localization.Localization;
import necesse.engine.network.Packet;
import necesse.engine.registries.BuffRegistry;
import necesse.engine.util.GameMath;
import necesse.engine.util.GameRandom;
import necesse.entity.levelEvent.mobAbilityLevelEvent.NecroticSoulSkullPushEvent;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.buffs.ActiveBuff;
import necesse.entity.mobs.buffs.BuffAbility;
import necesse.entity.mobs.buffs.BuffEventSubscriber;
import necesse.entity.mobs.buffs.BuffModifiers;
import necesse.entity.mobs.buffs.NecroticPoisonBuff;
import necesse.entity.particle.Particle;
import necesse.gfx.gameTooltips.ListGameTooltips;
import necesse.inventory.InventoryItem;
import necesse.inventory.item.trinketItem.TrinketItem;

public class NecroticSoulSkullTrinketBuff extends TrinketBuff implements BuffAbility {
   protected float cooldown = 6.0F;

   @Override
   public void init(ActiveBuff buff, BuffEventSubscriber eventSubscriber) {
      buff.setModifier(BuffModifiers.SUMMON_DAMAGE, 0.1F);
   }

   @Override
   public void runAbility(PlayerMob player, ActiveBuff buff, Packet content) {
      player.buffManager.addBuff(new ActiveBuff(BuffRegistry.Debuffs.NECROTIC_SOUL_SKULL_COOLDOWN, player, this.cooldown, null), false);
      player.getLevel().entityManager.addLevelEvent(new NecroticSoulSkullPushEvent(player));
      player.buffManager.forceUpdateBuffs();
      float maxDist = 128.0F;
      int lifeTime = 1100;
      int minHeight = 0;
      int maxHeight = 30;
      int particles = 77;

      for (int i = 0; i < particles; i++) {
         float height = minHeight + (float)(maxHeight - minHeight) * i / particles;
         AtomicReference<Float> currentAngle = new AtomicReference<>(GameRandom.globalRandom.nextFloat() * 360.0F);
         float outDistance = GameRandom.globalRandom.getFloatBetween(60.0F, maxDist + 32.0F);
         boolean counterclockwise = GameRandom.globalRandom.nextBoolean();
         player.getLevel()
            .entityManager
            .addParticle(
               player.x + GameRandom.globalRandom.getFloatBetween(0.0F, GameMath.sin(currentAngle.get()) * maxDist),
               player.y + GameRandom.globalRandom.getFloatBetween(0.0F, GameMath.cos(currentAngle.get()) * maxDist * 0.75F),
               Particle.GType.CRITICAL
            )
            .color(NecroticPoisonBuff.getNecroticParticleColor())
            .height(height)
            .moves((pos, delta, cLifeTime, timeAlive, lifePercent) -> {
               float angle = currentAngle.accumulateAndGet(delta * 150.0F / 250.0F, Float::sum);
               if (counterclockwise) {
                  angle = -angle;
               }

               float linearDown = GameMath.lerpExp(lifePercent, 0.525F, 0.0F, 1.0F);
               pos.x = player.x + outDistance * GameMath.sin(angle) * linearDown;
               pos.y = player.y + outDistance * GameMath.cos(angle) * linearDown * 0.75F;
            })
            .lifeTime(lifeTime)
            .sizeFades(14, 18);
      }
   }

   @Override
   public boolean canRunAbility(PlayerMob player, ActiveBuff buff, Packet content) {
      return !buff.owner.buffManager.hasBuff(BuffRegistry.Debuffs.NECROTIC_SOUL_SKULL_COOLDOWN);
   }

   @Override
   public ListGameTooltips getTrinketTooltip(TrinketItem trinketItem, InventoryItem item, PlayerMob perspective) {
      ListGameTooltips tooltips = new ListGameTooltips();
      tooltips.add(Localization.translate("itemtooltip", "necroticsoulskulltip1"));
      tooltips.add(Localization.translate("itemtooltip", "necroticsoulskulltip2"));
      tooltips.add(Localization.translate("itemtooltip", "necroticsoulskulltip3"));
      return tooltips;
   }
}
