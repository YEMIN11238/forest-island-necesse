package necesse.entity.pickup;

import java.awt.Color;
import java.util.List;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.network.Packet;
import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.engine.network.client.ClientClient;
import necesse.engine.network.packet.PacketPickupEntityPickup;
import necesse.engine.network.server.ServerClient;
import necesse.engine.sound.SoundEffect;
import necesse.engine.sound.SoundManager;
import necesse.engine.util.GameRandom;
import necesse.entity.ParticleTypeSwitcher;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.particle.Particle;
import necesse.gfx.GameResources;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.DrawOptions;
import necesse.gfx.drawables.LevelSortedDrawable;
import necesse.gfx.drawables.OrderableDrawables;
import necesse.level.maps.Level;
import necesse.level.maps.hudManager.floatText.DamageText;
import necesse.level.maps.light.GameLight;

public class VampiricHeartPickupEntity extends PickupEntity {
   protected ParticleTypeSwitcher particleTypeSwitcher = new ParticleTypeSwitcher(
      Particle.GType.IMPORTANT_COSMETIC, Particle.GType.COSMETIC, Particle.GType.CRITICAL
   );
   protected int tickCounter;
   public float healPercent = 0.05F;

   public VampiricHeartPickupEntity() {
   }

   public VampiricHeartPickupEntity(Level level, float x, float y, float dx, float dy) {
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
               .sizeFades(24, 12)
               .sprite(GameResources.bubbleParticle.sprite(0, 0, 12))
               .color(new Color(223, 12, 12))
               .movesFrictionAngle(random.getIntBetween(0, 360), 50.0F, 0.5F)
               .lifeTime(1000);
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
         this.getLevel()
            .entityManager
            .addParticle(
               this.x + (float)(GameRandom.globalRandom.nextGaussian() * 8.0),
               this.y + (float)(GameRandom.globalRandom.nextGaussian() * 6.0),
               this.particleTypeSwitcher.next()
            )
            .color(new Color(129, 33, 29))
            .movesFriction(0.0F, -30.0F, 0.8F)
            .givesLight(353.0F, 0.8F)
            .ignoreLight(true)
            .lifeTime(500);
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
   public boolean isValidTarget(ServerClient client) {
      return client.playerMob.getHealth() < client.playerMob.getMaxHealth();
   }

   @Override
   public void onPickup(ServerClient client) {
      int healthGained = Math.max(1, (int)(client.playerMob.getMaxHealth() * this.healPercent));
      client.playerMob.setHealth(client.playerMob.getHealth() + healthGained);
      Packet content = new Packet();
      PacketWriter writer = new PacketWriter(content);
      writer.putNextInt(healthGained);
      writer.putNextInt(client.playerMob.getHealth());
      this.getLevel().getServer().network.sendToClientsWithEntity(new PacketPickupEntityPickup(this, content), this);
      this.remove();
   }

   @Override
   public void onPickup(ClientClient client, Packet data) {
      super.onPickup(client, data);
      if (client.playerMob != null) {
         Level level = client.playerMob.getLevel();
         if (level != null) {
            PacketReader reader = new PacketReader(data);
            int healthGained = reader.getNextInt();
            int finalHealth = reader.getNextInt();
            client.playerMob.setHealth(finalHealth);
            if (level.isSamePlace(client.getClient().getLevel())) {
               SoundManager.playSound(GameResources.fadedeath2, SoundEffect.effect(client.playerMob).volume(0.5F).pitch(1.6F));
               level.hudManager.addElement(new DamageText(client.playerMob, healthGained, Color.GREEN, GameRandom.globalRandom.getIntBetween(30, 40)));
            }

            this.remove();
         }
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
      GameLight light = level.getLightLevel((int)Math.floor(this.x / 32.0), (int)Math.floor(this.y / 32.0));
      int drawX = camera.getDrawX(this.x);
      int drawY = camera.getDrawY(this.y) - 16;
      int timePerFrame = 100;
      int spriteIndex = (int)(this.getWorldEntity().getTime() / timePerFrame) % 4;
      final DrawOptions pickupOptions = GameResources.vampiricHeartPickup
         .initDraw()
         .sprite(spriteIndex, 0, 32)
         .light(light)
         .size(32 - spriteIndex * 2, 32 - spriteIndex * 2)
         .pos(drawX - 16 + spriteIndex, drawY - 16 + spriteIndex);
      final DrawOptions pickupShadowOptions = GameResources.item_shadow.initDraw().light(light).size(24, 16).pos(drawX - 12, drawY + 8);
      topList.add(new LevelSortedDrawable(this) {
         @Override
         public int getSortY() {
            return 0;
         }

         @Override
         public void draw(TickManager tickManager) {
            pickupShadowOptions.draw();
            pickupOptions.draw();
         }
      });
   }
}
