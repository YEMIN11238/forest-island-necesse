package necesse.entity.mobs.buffs.staticBuffs;

import necesse.engine.registries.BuffRegistry;
import necesse.engine.util.GameMath;
import necesse.entity.levelEvent.ReverseDamageGlyphTrapEvent;
import necesse.entity.mobs.buffs.ActiveBuff;
import necesse.entity.mobs.buffs.BuffEventSubscriber;
import necesse.entity.mobs.buffs.BuffModifiers;
import necesse.entity.particle.Particle;
import necesse.entity.particle.ParticleOption;
import necesse.gfx.GameResources;
import necesse.level.maps.Level;

public class ReverseDamageGlyphBuff extends Buff {
   public ReverseDamageGlyphBuff() {
      this.isImportant = true;
      this.canCancel = false;
   }

   @Override
   public void init(ActiveBuff buff, BuffEventSubscriber eventSubscriber) {
      buff.addModifier(BuffModifiers.INCOMING_DAMAGE_MOD, 1.25F);
      buff.addModifier(BuffModifiers.ALL_DAMAGE, 0.25F);
      if (buff.owner.isClient()) {
         Level level = buff.owner.getLevel();
         int count = 8;

         for (int i = 0; i < count; i++) {
            float offset = i * 360.0F / count;
            float rotationSpeed = 0.15F;
            ParticleOption particle = level.entityManager
               .addParticle(buff.owner, Particle.GType.IMPORTANT_COSMETIC)
               .lifeTime(buff.getDurationLeft())
               .sprite(GameResources.reverseDamageParticles)
               .color(ReverseDamageGlyphTrapEvent.particleColor)
               .givesLight(ReverseDamageGlyphTrapEvent.particleHue, 0.6F)
               .moves((pos, delta, lifeTime, timeAlive, lifePercent) -> {
                  pos.x = GameMath.cos((float)buff.owner.getTime() * rotationSpeed + offset) * 12.0F + 1.0F;
                  pos.y = GameMath.sin((float)buff.owner.getTime() * rotationSpeed + offset) * 10.0F - 5.0F;
               })
               .size((options, lifeTime, timeAlive, lifePercent) -> {
                  ActiveBuff currentBuff = buff.owner.buffManager.getBuff(BuffRegistry.REVERSE_DAMAGE_GLYPH);
                  if (currentBuff != null) {
                     int size = (int)(6.0F * ((float)currentBuff.getDurationLeft() / currentBuff.getDuration())) + 10;
                     size += size % 2;
                     options.size(size, size);
                  }
               })
               .rotation((lifeTime, timeAlive, lifePercent) -> {
                  int direction = offset % 2.0F == 0.0F ? 0 : -1;
                  return 180 * direction;
               })
               .onDied(
                  pos -> level.entityManager
                     .addParticle(buff.owner.x + pos.x, buff.owner.y + pos.y, Particle.GType.IMPORTANT_COSMETIC)
                     .color(ReverseDamageGlyphTrapEvent.particleColor)
                     .givesLight(ReverseDamageGlyphTrapEvent.particleHue, 0.8F)
                     .sizeFades(8, 8)
                     .lifeTime(500)
                     .movesFriction(pos.x, -10.0F, 1.0F)
               );
            particle.removeIf(() -> {
               ActiveBuff currentBuff = buff.owner.buffManager.getBuff(BuffRegistry.REVERSE_DAMAGE_GLYPH);
               return currentBuff != buff;
            });
         }
      }
   }
}
