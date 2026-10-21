package necesse.entity.mobs.buffs.staticBuffs.armorBuffs.trinketBuffs;

import java.awt.Color;
import java.awt.geom.Point2D.Float;
import java.util.LinkedList;
import necesse.engine.localization.Localization;
import necesse.engine.modifiers.ModifierTooltip;
import necesse.engine.network.gameNetworkData.GNDItemMap;
import necesse.engine.util.GameBlackboard;
import necesse.engine.util.GameMath;
import necesse.engine.util.GameRandom;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.buffs.ActiveBuff;
import necesse.entity.mobs.buffs.BuffEventSubscriber;
import necesse.entity.mobs.buffs.BuffModifiers;
import necesse.entity.mobs.buffs.staticBuffs.Buff;
import necesse.entity.particle.Particle;
import necesse.entity.particle.ParticleOption;
import necesse.gfx.GameResources;
import necesse.gfx.gameTooltips.ListGameTooltips;
import necesse.level.maps.Level;

public class LovedBuff extends Buff {
   @Override
   public void init(ActiveBuff buff, BuffEventSubscriber eventSubscriber) {
      buff.setModifier(BuffModifiers.MAX_HEALTH_FLAT, 50);
      buff.setModifier(BuffModifiers.HEALTH_REGEN_FLAT, 2.0F);
      buff.setModifier(BuffModifiers.SPEED, 0.1F);
   }

   @Override
   public ListGameTooltips getTooltip(ActiveBuff ab, GameBlackboard blackboard) {
      ListGameTooltips tooltips = super.getTooltip(ab, blackboard);
      LinkedList<ModifierTooltip> modifierTooltips = ab.getModifierTooltips();
      if (modifierTooltips.isEmpty()) {
         tooltips.add(Localization.translate("bufftooltip", "nomodifiers"));
      } else {
         ab.getModifierTooltips().stream().map(mft -> mft.toTooltip(true)).forEach(tooltips::add);
      }

      return tooltips;
   }

   @Override
   public void clientTick(ActiveBuff buff) {
      super.clientTick(buff);
      Level level = buff.owner.getLevel();
      long nextHeartSpawnTick = buff.getGndData().getLong("nextHeartSpawnTick");
      if (level.getTime() >= nextHeartSpawnTick) {
         buff.setGndData(new GNDItemMap().setLong("nextHeartSpawnTick", level.getTime() + GameRandom.globalRandom.getIntBetween(3000, 9000)));
         Mob mob = buff.owner;
         float posX = mob.getX() + GameRandom.globalRandom.getIntBetween(-5, 5);
         float posY = mob.getY() + GameRandom.globalRandom.getIntBetween(-5, 5);
         int startHeight = 8 + GameRandom.globalRandom.nextInt(24);
         final int swing = GameRandom.globalRandom.getIntBetween(5, 10);
         final float swingOffset = GameRandom.globalRandom.nextFloat();
         float moveX = GameRandom.globalRandom.floatGaussian() * 2.0F;
         float moveY = GameRandom.globalRandom.floatGaussian() * 1.5F;
         final ParticleOption.FrictionMover frictionMover = new ParticleOption.FrictionMover(moveX, moveY, 0.0F);
         mob.getLevel()
            .entityManager
            .addParticle(posX, posY, Particle.GType.COSMETIC)
            .sprite(GameResources.heartParticle)
            .dontRotate()
            .color(new Color(255, 255, 255))
            .heightMoves(startHeight, startHeight + 40)
            .moves(new ParticleOption.Mover() {
               @Override
               public void tick(Float pos, float delta, int lifeTime, int timeAlive, float lifePercent) {
                  frictionMover.tick(pos, delta, lifeTime, timeAlive, lifePercent);
                  float angle = (lifePercent + swingOffset) * 500.0F;
                  pos.x = pos.x + GameMath.sin(angle) * swing * delta / 250.0F;
               }
            })
            .lifeTime(1000);
      }

      level.lightManager.refreshParticleLightFloat(buff.owner.x, buff.owner.y, 240.0F, 0.35F, 135);
   }
}
