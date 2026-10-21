package necesse.entity.pickup;

import java.awt.Color;
import java.util.List;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.network.NetworkClient;
import necesse.engine.network.Packet;
import necesse.engine.network.packet.PacketPickupEntityPickup;
import necesse.engine.network.server.ServerClient;
import necesse.engine.registries.BuffRegistry;
import necesse.engine.sound.SoundEffect;
import necesse.engine.sound.SoundManager;
import necesse.engine.util.GameRandom;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.buffs.ActiveBuff;
import necesse.entity.particle.Particle;
import necesse.gfx.GameResources;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.DrawOptions;
import necesse.gfx.drawables.LevelSortedDrawable;
import necesse.gfx.drawables.OrderableDrawables;
import necesse.level.maps.Level;

public class StarBarrierPickupEntity extends PickupEntity {
   protected int tickCounter;

   public StarBarrierPickupEntity() {
   }

   public StarBarrierPickupEntity(Level level, float x, float y, float dx, float dy) {
      super(level, x, y, dx, dy);
      this.bouncy = 0.75F;
   }

   @Override
   public void init() {
      super.init();
      if (this.isClient()) {
         GameRandom random = GameRandom.globalRandom;

         for (int i = 0; i < 10; i++) {
            this.getLevel()
               .entityManager
               .addParticle(this.x + random.floatGaussian() * 16.0F, this.y + random.floatGaussian() * 12.0F, Particle.GType.IMPORTANT_COSMETIC)
               .sizeFades(22, 11)
               .sprite(GameResources.magicSparkParticles.sprite(random.nextInt(4), 0, 22))
               .color(new Color(184, 174, 255))
               .movesFrictionAngle(random.getIntBetween(0, 360), 50.0F, 0.5F)
               .lifeTime(5000)
               .givesLight(75.0F, 0.5F);
         }
      }
   }

   @Override
   public void clientTick() {
      this.tickCounter++;
      if (this.tickCounter > 200) {
         this.remove();
      } else {
         super.clientTick();
         if (this.getLevel() != null) {
            this.getLevel()
               .entityManager
               .addParticle(
                  this.x + (float)(GameRandom.globalRandom.nextGaussian() * 10.0),
                  this.y - 20.0F + (float)(GameRandom.globalRandom.nextGaussian() * 8.0),
                  Particle.GType.IMPORTANT_COSMETIC
               )
               .sizeFades(5, 10)
               .lifeTime(1500)
               .movesConstantAngle(GameRandom.globalRandom.getIntBetween(0, 360), 3.0F)
               .height(-20.0F)
               .givesLight(247.0F, 1.0F);
         }
      }
   }

   @Override
   public void serverTick() {
      this.tickCounter++;
      if (this.tickCounter > 200) {
         this.remove();
      } else {
         super.serverTick();
      }
   }

   @Override
   public void tickMovement(float delta) {
      NetworkClient target = this.getTarget();
      if (target == null) {
         super.tickMovement(delta);
      } else if (target.playerMob.buffManager.getStacks(BuffRegistry.STAR_BARRIER_BUFF) >= BuffRegistry.STAR_BARRIER_BUFF.getStackSize(null)) {
         return;
      }

      super.tickMovement(delta);
   }

   @Override
   public void onPickup(ServerClient client) {
      if (client.playerMob.buffManager.getStacks(BuffRegistry.STAR_BARRIER_BUFF) < BuffRegistry.STAR_BARRIER_BUFF.getStackSize(null)) {
         this.getLevel().getServer().network.sendToClientsWithEntity(new PacketPickupEntityPickup(this, new Packet()), this);
         if (!this.isServer()) {
            SoundManager.playSound(GameResources.shatter2, SoundEffect.effect(this).volume(3.0F).pitch(2.0F));
         }

         client.playerMob.buffManager.addBuff(new ActiveBuff(BuffRegistry.STAR_BARRIER_BUFF, client.playerMob, 20000, null), true);
         this.remove();
      }
   }

   @Override
   public void addDrawables(
      List<LevelSortedDrawable> list,
      OrderableDrawables tileList,
      OrderableDrawables topList,
      OrderableDrawables overlayList,
      Level level,
      TickManager tickManager,
      GameCamera camera,
      PlayerMob perspective
   ) {
      int drawX = camera.getDrawX(this.x);
      int drawY = camera.getDrawY(this.y);
      int timePerFrame = 100;
      int spriteIndex = (int)(this.getWorldEntity().getTime() / timePerFrame) % 2;
      final DrawOptions pickupOptions = GameResources.starBarrierPickup.initDraw().sprite(spriteIndex, 0, 28, 40).size(28, 40).pos(drawX - 14, drawY - 20);
      topList.add(new LevelSortedDrawable(this) {
         @Override
         public int getSortY() {
            return 0;
         }

         @Override
         public void draw(TickManager tickManager) {
            pickupOptions.draw();
         }
      });
   }
}
