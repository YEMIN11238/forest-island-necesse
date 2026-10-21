package necesse.entity.levelEvent.mobAbilityLevelEvent;

import java.awt.Color;
import java.awt.Polygon;
import java.awt.geom.Point2D.Float;
import necesse.engine.registries.MobRegistry;
import necesse.engine.util.GameRandom;
import necesse.engine.util.GroundPillar;
import necesse.engine.util.GroundPillarList;
import necesse.entity.levelEvent.LevelEvent;
import necesse.entity.manager.GroundPillarHandler;
import necesse.entity.mobs.GameDamage;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.PlayerMob;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.DrawOptions;
import necesse.gfx.gameTexture.GameTexture;
import necesse.level.gameTile.GameTile;
import necesse.level.maps.Level;
import necesse.level.maps.LevelObjectHit;
import necesse.level.maps.light.GameLight;

public class MoundShockWaveLevelEvent extends ShockWaveLevelEvent {
   protected final GroundPillarList<MoundShockWaveLevelEvent.Mound> pillars = new GroundPillarList<>();

   public MoundShockWaveLevelEvent() {
      super(360.0F, 150.0F, 150.0F, 20.0F, 5.0F);
   }

   public MoundShockWaveLevelEvent(Mob owner, int x, int y, GameRandom uniqueIDRandom, float targetAngle) {
      super(owner, x, y, uniqueIDRandom, targetAngle, 360.0F, 150.0F, 150.0F, 20.0F, 5.0F);
   }

   @Override
   public void init() {
      super.init();
      this.drawDebugHitboxes = true;
      if (this.isClient()) {
         this.level.entityManager.addPillarHandler(new GroundPillarHandler<MoundShockWaveLevelEvent.Mound>(this.pillars) {
            @Override
            protected boolean canRemove() {
               return MoundShockWaveLevelEvent.this.isOver();
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
      target.isServerHit(new GameDamage(10.0F), 0.0F, 0.0F, 0.0F, this.owner);
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
                     new MoundShockWaveLevelEvent.Mound(
                        (int)(pos.x + GameRandom.globalRandom.getFloatBetween(-5.0F, 5.0F)),
                        (int)(pos.y + GameRandom.globalRandom.getFloatBetween(-5.0F, 5.0F)),
                        radius,
                        this.level.getWorldEntity().getLocalTime()
                     )
                  );
            }
         }
      }
   }

   @Override
   public void hitObject(LevelObjectHit hit) {
   }

   private static class Mound extends GroundPillar {
      public GameTexture texture = GameRandom.globalRandom.getOneOf(MobRegistry.Textures.mound1, MobRegistry.Textures.mound2, MobRegistry.Textures.mound3);

      public Mound(int x, int y, double spawnDistance, long spawnTime) {
         super(x, y, spawnDistance, spawnTime);
         this.behaviour = new GroundPillar.TimedBehaviour(200, 100, 200);
      }

      @Override
      public DrawOptions getDrawOptions(Level level, long currentTime, double distanceMoved, GameCamera camera, PlayerMob perspective) {
         GameTile tile = level.getTile(LevelEvent.getTileCoordinate(this.x), LevelEvent.getTileCoordinate(this.y));
         if (tile.isLiquid) {
            return null;
         }

         GameLight light = level.getLightLevel(LevelEvent.getTileCoordinate(this.x), LevelEvent.getTileCoordinate(this.y));
         Color color = tile.getMapColor(level, LevelEvent.getTileCoordinate(this.x), LevelEvent.getTileCoordinate(this.y));
         int drawX = camera.getDrawX(this.x);
         int drawY = camera.getDrawY(this.y);
         double height = this.getHeight(currentTime, distanceMoved);
         int endY = (int)(height * this.texture.getHeight());
         return this.texture
            .initDraw()
            .section(0, this.texture.getWidth(), 0, endY)
            .color(color)
            .light(light)
            .pos(drawX - this.texture.getWidth() / 2, drawY - endY);
      }
   }
}
