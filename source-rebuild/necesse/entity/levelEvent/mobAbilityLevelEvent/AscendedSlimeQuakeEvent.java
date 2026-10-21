package necesse.entity.levelEvent.mobAbilityLevelEvent;

import java.awt.Polygon;
import java.awt.geom.Point2D.Float;
import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.engine.util.GameRandom;
import necesse.engine.util.GroundPillar;
import necesse.engine.util.GroundPillarList;
import necesse.entity.manager.GroundPillarHandler;
import necesse.entity.mobs.GameDamage;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.PlayerMob;
import necesse.gfx.GameResources;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.DrawOptions;
import necesse.gfx.gameTexture.GameTexture;
import necesse.gfx.gameTexture.GameTextureSection;
import necesse.level.maps.Level;
import necesse.level.maps.light.GameLight;

public class AscendedSlimeQuakeEvent extends WeaponShockWaveLevelEvent {
   protected final GroundPillarList<GroundPillar> pillars = new GroundPillarList<>();

   public AscendedSlimeQuakeEvent() {
      super(360.0F, 135.0F, 50.0F);
   }

   public AscendedSlimeQuakeEvent(
      Mob owner,
      int x,
      int y,
      GameRandom uniqueIDRandom,
      float targetAngle,
      float angleExtent,
      GameDamage damage,
      float velocity,
      float knockback,
      float range,
      float offset
   ) {
      super(owner, x, y, uniqueIDRandom, targetAngle, angleExtent, 135.0F, 50.0F, damage, 0.0F, velocity, knockback, range);
      this.hitDistanceOffset = offset;
   }

   @Override
   public void setupSpawnPacket(PacketWriter writer) {
      super.setupSpawnPacket(writer);
      writer.putNextFloat(this.hitDistanceOffset);
      writer.putNextFloat(this.angleExtent);
   }

   @Override
   public void applySpawnPacket(PacketReader reader) {
      super.applySpawnPacket(reader);
      this.hitDistanceOffset = reader.getNextFloat();
      this.angleExtent = reader.getNextFloat();
   }

   @Override
   public void init() {
      super.init();
      this.allowConsecutiveHits = true;
      if (this.isClient()) {
         this.level.entityManager.addPillarHandler(new GroundPillarHandler<GroundPillar>(this.pillars) {
            @Override
            protected boolean canRemove() {
               return AscendedSlimeQuakeEvent.this.isOver();
            }

            @Override
            public double getCurrentDistanceMoved() {
               return 0.0;
            }
         });
      }
   }

   @Override
   protected void spawnHitboxParticles(Polygon hitbox) {
   }

   @Override
   protected void spawnHitboxParticles(float radius, float startAngle, float endAngle) {
      if (this.isClient()) {
         synchronized (this.pillars) {
            for (Float pos : this.getPositionsAlongHit(radius, startAngle, endAngle, 20.0F, false)) {
               this.pillars
                  .add(
                     new AscendedSlimeQuakeEvent.AscendedSlimePillar(
                        (int)(pos.x + GameRandom.globalRandom.getFloatBetween(-10.0F, 10.0F)),
                        (int)(pos.y + GameRandom.globalRandom.getFloatBetween(-10.0F, 10.0F)),
                        radius,
                        this.level.getWorldEntity().getLocalTime()
                     )
                  );
            }

            for (Float pos : this.getPositionsAlongHit(radius + 20.0F, startAngle, endAngle, 20.0F, false)) {
               this.pillars
                  .add(
                     new AscendedSlimeQuakeEvent.AscendedSlimePillar(
                        (int)(pos.x + GameRandom.globalRandom.getFloatBetween(-10.0F, 10.0F)),
                        (int)(pos.y + GameRandom.globalRandom.getFloatBetween(-10.0F, 10.0F)),
                        radius,
                        this.level.getWorldEntity().getLocalTime()
                     )
                  );
            }

            for (Float pos : this.getPositionsAlongHit(radius - 20.0F, startAngle, endAngle, 20.0F, false)) {
               this.pillars
                  .add(
                     new AscendedSlimeQuakeEvent.AscendedSlimePillar(
                        (int)(pos.x + GameRandom.globalRandom.getFloatBetween(-10.0F, 10.0F)),
                        (int)(pos.y + GameRandom.globalRandom.getFloatBetween(-10.0F, 10.0F)),
                        radius,
                        this.level.getWorldEntity().getLocalTime()
                     )
                  );
            }
         }
      }
   }

   public static class AscendedSlimePillar extends GroundPillar {
      public GameTextureSection texture;
      public boolean mirror = GameRandom.globalRandom.nextBoolean();

      public AscendedSlimePillar(int x, int y, double spawnDistance, long spawnTime) {
         super(x, y, spawnDistance, spawnTime);
         this.texture = null;
         GameTexture pillarSprites = GameResources.ascendedSlimeSpike;
         if (pillarSprites != null) {
            int res = pillarSprites.getHeight();
            int sprite = GameRandom.globalRandom.nextInt(pillarSprites.getWidth() / res);
            this.texture = new GameTextureSection(GameResources.ascendedSlimeSpike).sprite(sprite, 0, res);
         }

         this.behaviour = new GroundPillar.TimedBehaviour(500, 100, 200);
      }

      @Override
      public DrawOptions getDrawOptions(Level level, long currentTime, double distanceMoved, GameCamera camera, PlayerMob perspective) {
         GameLight light = level.getLightLevel(this.x / 32, this.y / 32);
         int drawX = camera.getDrawX(this.x);
         int drawY = camera.getDrawY(this.y);
         double height = this.getHeight(currentTime, distanceMoved);
         int endY = (int)(height * this.texture.getHeight());
         return this.texture
            .section(0, this.texture.getWidth(), 0, endY)
            .initDraw()
            .mirror(this.mirror, false)
            .light(light.minLevelCopy(150.0F))
            .pos(drawX - this.texture.getWidth() / 2, drawY - endY);
      }
   }
}
