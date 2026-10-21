package necesse.entity.mobs.buffs.staticBuffs;

import java.awt.Point;
import java.util.concurrent.atomic.AtomicReference;
import necesse.engine.network.gameNetworkData.GNDItemMap;
import necesse.engine.sound.SoundEffect;
import necesse.engine.sound.SoundManager;
import necesse.engine.util.GameMath;
import necesse.engine.util.GameRandom;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.buffs.ActiveBuff;
import necesse.entity.mobs.buffs.BuffEventSubscriber;
import necesse.entity.mobs.buffs.BuffModifiers;
import necesse.entity.mobs.buffs.HumanDrawBuff;
import necesse.entity.mobs.buffs.MovementTickBuff;
import necesse.entity.particle.Particle;
import necesse.entity.particle.ParticleOption;
import necesse.gfx.GameResources;
import necesse.gfx.drawOptions.human.HumanDrawOptions;
import necesse.inventory.InventoryItem;

public class RingOfAscensionActiveBuff extends Buff implements MovementTickBuff, HumanDrawBuff {
   public RingOfAscensionActiveBuff() {
      this.shouldSave = false;
      this.isVisible = false;
   }

   @Override
   public void init(ActiveBuff buff, BuffEventSubscriber eventSubscriber) {
      buff.setMinModifier(BuffModifiers.SPEED_FLAT, 150.0F);
      buff.setMinModifier(BuffModifiers.FRICTION, 5.0F, Integer.MAX_VALUE);
      buff.setMinModifier(BuffModifiers.ACCELERATION, 1.0F, Integer.MAX_VALUE);
      buff.setMinModifier(BuffModifiers.DECELERATION, 1.0F, Integer.MAX_VALUE);
      buff.setMaxModifier(BuffModifiers.SPEED, 1.0F, Integer.MAX_VALUE);
      buff.setModifier(BuffModifiers.WATER_WALKING, true);
   }

   @Override
   public void clientTick(ActiveBuff buff) {
      super.clientTick(buff);
      Mob owner = buff.owner;
      int minHeight = 14;
      int maxHeight = 16;
      final boolean reversed = GameRandom.globalRandom.nextBoolean();
      final float height = GameMath.lerp(GameRandom.globalRandom.nextFloat(), minHeight, maxHeight);
      final AtomicReference<Float> currentAngle = new AtomicReference<>(GameRandom.globalRandom.nextFloat() * 360.0F);
      float distance = 20.0F;
      owner.getLevel()
         .entityManager
         .addParticle(
            owner.x + GameMath.sin(currentAngle.get()) * distance,
            owner.y + GameMath.cos(currentAngle.get()) * distance * 0.75F,
            Particle.GType.IMPORTANT_COSMETIC
         )
         .colorRandom(310.0F, 0.95F, 0.95F, 10.0F, 0.05F, 0.05F)
         .minDrawLight(150)
         .moves((pos, delta, lifeTime, timeAlive, lifePercent) -> {
            float angle = currentAngle.accumulateAndGet(delta * 150.0F / 250.0F, Float::sum);
            pos.x = owner.x + GameMath.sin(angle) * 70.0F;
            pos.y = owner.y + (reversed ? -GameMath.cos(angle) : GameMath.cos(angle)) * 70.0F * 0.9F;
         })
         .height(new ParticleOption.HeightGetter() {
            @Override
            public float tick(float delta, int lifeTime, int timeAlive, float lifePercent) {
               Float angle = currentAngle.get();
               float offset = GameMath.sin(angle) * 35.0F;
               return height + (reversed ? -offset : offset);
            }
         })
         .sizeFades(16, 20)
         .lifeTime(2000);
      minHeight = -10;
      maxHeight = 0;
      float heightx = GameMath.lerp(GameRandom.globalRandom.nextFloat(), minHeight, maxHeight);
      owner.getLevel()
         .entityManager
         .addParticle(
            owner.x + GameRandom.globalRandom.getFloatBetween(-10.0F, 10.0F),
            owner.y + GameRandom.globalRandom.getFloatBetween(-10.0F, 10.0F),
            Particle.GType.IMPORTANT_COSMETIC
         )
         .colorRandom(310.0F, 0.95F, 0.95F, 10.0F, 0.05F, 0.05F)
         .minDrawLight(150)
         .movesFriction(
            owner.dx + GameRandom.globalRandom.getFloatBetween(-20.0F, 20.0F), owner.dy + GameRandom.globalRandom.getFloatBetween(-20.0F, 20.0F), 0.8F
         )
         .heightMoves(heightx, heightx - 30.0F)
         .sizeFades(10, 16)
         .lifeTime(1000);
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

      if (!this.isBuffStillValid(buff)) {
         buff.remove();
      }
   }

   public boolean isBuffStillValid(ActiveBuff ab) {
      Mob owner = ab.owner;
      if (owner.isRiding()) {
         return false;
      }

      if (!owner.isPlayer) {
         return false;
      }

      PlayerMob player = (PlayerMob)owner;
      InventoryItem mountItem = player.getInv().equipment.getSelectedEquipmentSlot(0).getItem();
      return mountItem == null ? false : mountItem.item.getStringID().equals("ringofascension");
   }

   @Override
   public void addHumanDraw(ActiveBuff buff, HumanDrawOptions drawOptions) {
      int dir = buff.owner.getDir();
      drawOptions.sprite(new Point(0, dir % 4));
      drawOptions.addDrawOffset(0, (int)(-buff.getGndData().getFloat("hoverHeight")));
      float rotate = buff.owner.dx;
      drawOptions.rotate(Math.min(rotate / 40.0F, 5.0F), 32, 55);
   }
}
