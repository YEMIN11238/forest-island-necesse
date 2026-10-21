package necesse.entity.projectile;

import java.awt.Color;
import java.util.List;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.journal.ImpaleIceJavelinsJournalChallenge;
import necesse.engine.journal.JournalChallenge;
import necesse.engine.network.server.ServerClient;
import necesse.engine.registries.JournalChallengeRegistry;
import necesse.engine.sound.SoundSettings;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.particle.Particle;
import necesse.entity.particle.ProjectileHitStuckParticle;
import necesse.entity.trails.Trail;
import necesse.gfx.GameResources;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.texture.TextureDrawOptions;
import necesse.gfx.drawables.EntityDrawable;
import necesse.gfx.drawables.LevelSortedDrawable;
import necesse.gfx.drawables.OrderableDrawables;
import necesse.level.maps.Level;
import necesse.level.maps.LevelObjectHit;
import necesse.level.maps.light.GameLight;

public class IceJavelinProjectile extends Projectile implements RicochetableProjectile {
   @Override
   public void init() {
      super.init();
      this.height = 18.0F;
      this.setWidth(8.0F);
      this.trailOffset = -50.0F;
      this.heightBasedOnDistance = true;
   }

   @Override
   public Trail getTrail() {
      return new Trail(this, this.getLevel(), new Color(150, 150, 150), 10.0F, 250, 18.0F);
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
      if (!this.removed()) {
         GameLight light = level.getLightLevel(this);
         int drawX = camera.getDrawX(this.x) - 2;
         int drawY = camera.getDrawY(this.y) - 2;
         final TextureDrawOptions options = this.texture
            .initDraw()
            .light(light)
            .rotate(this.getAngle() + 45.0F, 2, 2)
            .pos(drawX, drawY - (int)this.getHeight());
         list.add(new EntityDrawable(this) {
            @Override
            public void draw(TickManager tickManager) {
               options.draw();
            }
         });
         this.addShadowDrawables(tileList, drawX, drawY, light, this.getAngle() + 45.0F, 2, 2);
      }
   }

   @Override
   public void doHitLogic(Mob mob, LevelObjectHit object, float x, float y) {
      super.doHitLogic(mob, object, x, y);
      if (this.isServer() && mob != null) {
         Mob owner = this.getOwner();
         if (owner.isPlayer && ((PlayerMob)owner).isServerClient()) {
            ServerClient serverClient = ((PlayerMob)owner).getServerClient();
            JournalChallenge challenge = JournalChallengeRegistry.getChallenge(JournalChallengeRegistry.IMPALE_FIVE_ICE_JAVELINS_ID);
            ((ImpaleIceJavelinsJournalChallenge)challenge).submitIceJavelinImpale(serverClient, mob);
         }
      }

      if (this.isClient() && this.traveledDistance < this.distance) {
         final float height = this.getHeight();
         this.getLevel()
            .entityManager
            .addParticle(
               new ProjectileHitStuckParticle(mob, this, x, y, mob == null ? 10.0F : 40.0F, 5000L) {
                  @Override
                  public void addDrawables(
                     Mob target,
                     float xx,
                     float yx,
                     float angle,
                     List<LevelSortedDrawable> list,
                     OrderableDrawables tileList,
                     OrderableDrawables topList,
                     Level level,
                     TickManager tickManager,
                     GameCamera camera,
                     PlayerMob perspective
                  ) {
                     GameLight light = level.getLightLevel(this);
                     int drawX = camera.getDrawX(xx) - 2;
                     int drawY = camera.getDrawY(yx - height) - 2;
                     float alpha = 1.0F;
                     long lifeCycleTime = this.getLifeCycleTime();
                     int fadeTime = 1000;
                     if (lifeCycleTime >= this.lifeTime - fadeTime) {
                        alpha = Math.abs((float)(lifeCycleTime - (this.lifeTime - fadeTime)) / fadeTime - 1.0F);
                     }

                     int cut = target == null ? 8 : 0;
                     final TextureDrawOptions options = IceJavelinProjectile.this.texture
                        .initDraw()
                        .section(cut, IceJavelinProjectile.this.texture.getWidth(), cut, IceJavelinProjectile.this.texture.getHeight())
                        .light(light)
                        .rotate(IceJavelinProjectile.this.getAngle() + 45.0F, 2, 2)
                        .alpha(alpha)
                        .pos(drawX, drawY);
                     EntityDrawable drawable = new EntityDrawable(this) {
                        @Override
                        public void draw(TickManager tickManager) {
                           options.draw();
                        }
                     };
                     if (target != null) {
                        topList.add(drawable);
                     } else {
                        list.add(drawable);
                     }
                  }
               },
               Particle.GType.IMPORTANT_COSMETIC
            );
      }
   }

   @Override
   protected SoundSettings getHitSound() {
      return new SoundSettings(GameResources.jinglehit).basePitch(0.3F).pitchVariance(0.0F).fallOffDistance(1000);
   }

   @Override
   protected SoundSettings getSpawnSound() {
      return new SoundSettings(GameResources.iceJavelin).volume(0.4F);
   }
}
