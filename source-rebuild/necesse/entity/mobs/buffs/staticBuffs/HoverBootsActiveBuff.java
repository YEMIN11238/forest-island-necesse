package necesse.entity.mobs.buffs.staticBuffs;

import java.awt.Color;
import java.awt.Point;
import necesse.engine.network.gameNetworkData.GNDItemMap;
import necesse.engine.sound.SoundEffect;
import necesse.engine.sound.SoundManager;
import necesse.engine.util.GameMath;
import necesse.engine.util.GameRandom;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.buffs.ActiveBuff;
import necesse.entity.mobs.buffs.BuffEventSubscriber;
import necesse.entity.mobs.buffs.BuffModifiers;
import necesse.entity.mobs.buffs.HumanDrawBuff;
import necesse.entity.mobs.buffs.MovementTickBuff;
import necesse.entity.particle.Particle;
import necesse.gfx.GameResources;
import necesse.gfx.drawOptions.human.HumanDrawOptions;

public class HoverBootsActiveBuff extends Buff implements MovementTickBuff, HumanDrawBuff {
   public HoverBootsActiveBuff() {
      this.shouldSave = false;
      this.isVisible = false;
   }

   @Override
   public void init(ActiveBuff buff, BuffEventSubscriber eventSubscriber) {
      buff.setMinModifier(BuffModifiers.SPEED_FLAT, 200.0F);
      buff.setMaxModifier(BuffModifiers.FRICTION, 0.2F, Integer.MAX_VALUE);
      buff.setMaxModifier(BuffModifiers.ACCELERATION, 0.25F, Integer.MAX_VALUE);
      buff.setMaxModifier(BuffModifiers.SPEED, 1.0F, Integer.MAX_VALUE);
      buff.setModifier(BuffModifiers.WATER_WALKING, true);
   }

   @Override
   public void clientTick(ActiveBuff buff) {
      super.clientTick(buff);
      Mob owner = buff.owner;
      int dir = owner.getDir();
      float yOffset = GameRandom.globalRandom.floatGaussian() * 2.0F;
      float var10001 = owner.x + 4.0F;
      owner.getLevel()
         .entityManager
         .addParticle(var10001, owner.y + yOffset - 2.0F + (dir == 1 ? -4 : 0), Particle.GType.IMPORTANT_COSMETIC)
         .sprite(GameResources.puffParticles.sprite(GameRandom.globalRandom.nextInt(5), 0, 12))
         .color(
            (options, lifeTime, timeAlive, lifePercent) -> options.color(
               new Color(255 - GameMath.lerp(lifePercent, 0, 245), 255 - GameMath.lerp(lifePercent, 0, 50), 255 - GameMath.lerp(lifePercent, 0, 33))
            )
         )
         .sizeFadesInAndOut(10, 16, 50, 200)
         .movesConstant(GameRandom.globalRandom.floatGaussian(), 10.0F)
         .lifeTime(300)
         .height(2.0F);
      var10001 = owner.x - 4.0F;
      owner.getLevel()
         .entityManager
         .addParticle(var10001, owner.y + yOffset - 2.0F + (dir == 3 ? -4 : 0), Particle.GType.IMPORTANT_COSMETIC)
         .sprite(GameResources.puffParticles.sprite(GameRandom.globalRandom.nextInt(5), 0, 12))
         .color(
            (options, lifeTime, timeAlive, lifePercent) -> options.color(
               new Color(255 - GameMath.lerp(lifePercent, 0, 245), 255 - GameMath.lerp(lifePercent, 0, 50), 255 - GameMath.lerp(lifePercent, 0, 33))
            )
         )
         .sizeFadesInAndOut(10, 16, 50, 200)
         .movesConstant(GameRandom.globalRandom.floatGaussian(), 10.0F)
         .lifeTime(300)
         .height(2.0F);
   }

   @Override
   public void tickMovement(ActiveBuff buff, float delta) {
      float hoverHeight = buff.getGndData().getFloat("hoverHeight");
      hoverHeight += 0.04F * delta;
      hoverHeight = Math.min(hoverHeight, 10.0F);
      buff.getGndData().setFloat("hoverHeight", hoverHeight);
      Mob owner = buff.owner;
      if (owner.isClient() && (owner.dx != 0.0F || owner.dy != 0.0F)) {
         float speed = owner.getCurrentSpeed() * delta / 250.0F;
         GNDItemMap gndData = buff.getGndData();
         float soundBuffer = gndData.getFloat("soundBuffer") + Math.min(speed, 80.0F * delta / 250.0F);
         if (soundBuffer >= 55.0F) {
            soundBuffer -= 55.0F;
            SoundManager.playSound(GameResources.hoverboots, SoundEffect.effect(owner).pitch(1.0F));
         }

         gndData.setFloat("soundBuffer", soundBuffer);
      }
   }

   @Override
   public void addHumanDraw(ActiveBuff buff, HumanDrawOptions drawOptions) {
      int dir = buff.owner.getDir();
      drawOptions.sprite(new Point(0, dir % 4));
      drawOptions.addDrawOffset(0, (int)(-buff.getGndData().getFloat("hoverHeight")));
      float rotate = buff.owner.dx;
      drawOptions.rotate(Math.min(rotate / 10.0F, 10.0F), 32, 55);
   }
}
