package necesse.entity.levelEvent.mobAbilityLevelEvent;

import java.awt.Polygon;
import java.awt.geom.Point2D.Float;
import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.engine.util.GameRandom;
import necesse.engine.util.GroundPillar;
import necesse.engine.util.GroundPillarList;
import necesse.entity.levelEvent.LevelEvent;
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
import necesse.level.maps.LevelObjectHit;
import necesse.level.maps.light.GameLight;

public class SlimeQuakeWarningEvent extends WeaponShockWaveLevelEvent {
   protected final GroundPillarList<GroundPillar> pillars = new GroundPillarList<>();
   protected int warningTime;

   public SlimeQuakeWarningEvent() {
      super(360.0F, 135.0F, 50.0F);
   }

   public SlimeQuakeWarningEvent(
      Mob owner, int x, int y, GameRandom uniqueIDRandom, float targetAngle, float velocity, float range, int warningTime, float offset
   ) {
      super(owner, x, y, uniqueIDRandom, targetAngle, 360.0F, 135.0F, 50.0F, new GameDamage(0.0F), 0.0F, velocity, 0.0F, range);
      this.hitDistanceOffset = offset;
      this.warningTime = warningTime;
   }

   @Override
   public void setupSpawnPacket(PacketWriter writer) {
      super.setupSpawnPacket(writer);
      writer.putNextInt(this.warningTime);
      writer.putNextFloat(this.hitDistanceOffset);
   }

   @Override
   public void applySpawnPacket(PacketReader reader) {
      super.applySpawnPacket(reader);
      this.warningTime = reader.getNextInt();
      this.hitDistanceOffset = reader.getNextFloat();
   }

   @Override
   public void init() {
      super.init();
      if (this.isClient()) {
         this.level.entityManager.addPillarHandler(new GroundPillarHandler<GroundPillar>(this.pillars) {
            @Override
            protected boolean canRemove() {
               return SlimeQuakeWarningEvent.this.isOver();
            }

            @Override
            public double getCurrentDistanceMoved() {
               return 0.0;
            }
         });
      }
   }

   @Override
   public void damageTarget(Mob target) {
   }

   @Override
   public void hitObject(LevelObjectHit hit) {
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
                     new SlimeQuakeWarningEvent.SlimePillar(
                        (int)(pos.x + GameRandom.globalRandom.getFloatBetween(-10.0F, 10.0F)),
                        (int)(pos.y + GameRandom.globalRandom.getFloatBetween(-10.0F, 10.0F)),
                        radius,
                        this.level.getWorldEntity().getLocalTime(),
                        this.warningTime
                     )
                  );
            }

            for (Float pos : this.getPositionsAlongHit(radius + 20.0F, startAngle, endAngle, 20.0F, false)) {
               this.pillars
                  .add(
                     new SlimeQuakeWarningEvent.SlimePillar(
                        (int)(pos.x + GameRandom.globalRandom.getFloatBetween(-10.0F, 10.0F)),
                        (int)(pos.y + GameRandom.globalRandom.getFloatBetween(-10.0F, 10.0F)),
                        radius,
                        this.level.getWorldEntity().getLocalTime(),
                        this.warningTime
                     )
                  );
            }

            for (Float pos : this.getPositionsAlongHit(radius - 20.0F, startAngle, endAngle, 20.0F, false)) {
               this.pillars
                  .add(
                     new SlimeQuakeWarningEvent.SlimePillar(
                        (int)(pos.x + GameRandom.globalRandom.getFloatBetween(-10.0F, 10.0F)),
                        (int)(pos.y + GameRandom.globalRandom.getFloatBetween(-10.0F, 10.0F)),
                        radius,
                        this.level.getWorldEntity().getLocalTime(),
                        this.warningTime
                     )
                  );
            }
         }
      }
   }

   public static class SlimePillar extends GroundPillar {
      public GameTextureSection texture;
      public boolean mirror = GameRandom.globalRandom.nextBoolean();

      public SlimePillar(int x, int y, double spawnDistance, long spawnTime, int warningTime) {
         super(x, y, spawnDistance, spawnTime);
         this.texture = null;
         GameTexture pillarSprites = GameResources.slimeGround;
         if (pillarSprites != null) {
            int res = pillarSprites.getHeight();
            int sprite = GameRandom.globalRandom.nextInt(pillarSprites.getWidth() / res);
            this.texture = new GameTextureSection(GameResources.slimeGround).sprite(sprite, 0, res);
         }

         this.behaviour = new GroundPillar.TimedBehaviour(Math.max(warningTime - 300, 100), 500, 200);
      }

      @Override
      public DrawOptions getDrawOptions(Level level, long currentTime, double distanceMoved, GameCamera camera, PlayerMob perspective) {
         GameLight light = level.getLightLevel(LevelEvent.getTileCoordinate(this.x), LevelEvent.getTileCoordinate(this.y));
         int drawX = camera.getDrawX(this.x);
         int drawY = camera.getDrawY(this.y);
         double height = this.getHeight(currentTime, distanceMoved);
         int endY = (int)(height * this.texture.getHeight());
         return this.texture
            .section(0, this.texture.getWidth(), 0, endY)
            .initDraw()
            .mirror(this.mirror, false)
            .light(light)
            .pos(drawX - this.texture.getWidth() / 2, drawY - endY);
      }
   }
}
