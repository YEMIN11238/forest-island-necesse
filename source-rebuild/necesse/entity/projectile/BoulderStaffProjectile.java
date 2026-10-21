package necesse.entity.projectile;

import java.awt.Color;
import java.util.List;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.engine.sound.SoundEffect;
import necesse.engine.sound.SoundManager;
import necesse.engine.util.GameMath;
import necesse.entity.levelEvent.explosionEvent.ExplosionEvent;
import necesse.entity.mobs.GameDamage;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.particle.Particle;
import necesse.entity.trails.Trail;
import necesse.gfx.GameResources;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.texture.TextureDrawOptions;
import necesse.gfx.drawables.EntityDrawable;
import necesse.gfx.drawables.LevelSortedDrawable;
import necesse.gfx.drawables.OrderableDrawables;
import necesse.gfx.gameTexture.GameSprite;
import necesse.level.maps.Level;
import necesse.level.maps.light.GameLight;

public class BoulderStaffProjectile extends Projectile {
   protected float deltaHeight;
   protected float boulderHeight;

   public BoulderStaffProjectile() {
   }

   public BoulderStaffProjectile(
      Level level, Mob owner, float x, float y, float targetX, float targetY, float speed, int distance, GameDamage damage, int knockback
   ) {
      this.setLevel(level);
      this.x = x;
      this.y = y;
      this.speed = speed;
      this.setTarget(targetX, targetY);
      this.setDamage(damage);
      this.knockback = knockback;
      this.setDistance(distance);
      this.setOwner(owner);
      this.boulderHeight = 18.0F;
      this.deltaHeight = 50.0F;
   }

   @Override
   public void setupPositionPacket(PacketWriter writer) {
      super.setupPositionPacket(writer);
      writer.putNextFloat(this.deltaHeight);
      writer.putNextFloat(this.boulderHeight);
   }

   @Override
   public void applyPositionPacket(PacketReader reader) {
      super.applyPositionPacket(reader);
      this.deltaHeight = reader.getNextFloat();
      this.boulderHeight = reader.getNextFloat();
   }

   @Override
   public void init() {
      super.init();
      this.spawnTime = this.getWorldEntity().getTime();
      this.isSolid = true;
      this.width = 32.0F;
      this.trailOffset = 0.0F;
      this.piercing = 2;
   }

   @Override
   public float tickMovement(float delta) {
      float out = super.tickMovement(delta);
      float heightChange = 50.0F * delta / 250.0F;
      this.deltaHeight -= heightChange;
      this.boulderHeight = this.boulderHeight + this.deltaHeight * delta / 250.0F;
      if (this.boulderHeight < 0.0F) {
         if (this.isClient()) {
            SoundManager.playSound(GameResources.punch, SoundEffect.effect(this).volume(0.5F).pitch(1.0F));
            ExplosionEvent.spawnExplosionParticles(
               this.getLevel(),
               this.x,
               this.y,
               20,
               0.0F,
               20.0F,
               (level, x, y, dirX, dirY, lifeTime, range) -> level.entityManager
                  .addParticle(x, y, Particle.GType.CRITICAL)
                  .movesConstant(dirX, dirY)
                  .color(new Color(50, 50, 50))
                  .lifeTime(lifeTime)
            );
            SoundManager.playSound(GameResources.blunthit, SoundEffect.effect(this).volume(0.45F).falloffDistance(1000));
         }

         this.deltaHeight = -this.deltaHeight * 0.95F;
         this.boulderHeight = -this.boulderHeight;
         if (Math.abs(this.deltaHeight) < heightChange * 2.0F) {
            this.boulderHeight = -1.0F;
            this.deltaHeight = 0.0F;
         }
      }

      this.height = Math.max(this.boulderHeight, 0.0F);
      return out;
   }

   @Override
   public Color getParticleColor() {
      return new Color(59, 38, 77);
   }

   @Override
   public Trail getTrail() {
      Trail trail = new Trail(this, this.getLevel(), new Color(50, 50, 50), 0.0F, 500, this.getHeight());
      trail.sprite = new GameSprite(GameResources.chains, 7, 0, 32);
      return trail;
   }

   @Override
   public float getTrailThickness() {
      return 30.0F;
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
         GameLight light = level.getLightLevel(this.getTileX(), this.getTileY());
         int drawX = camera.getDrawX(this.x) - this.texture.getWidth() / 2;
         int drawY = camera.getDrawY(this.y) - this.texture.getHeight() / 2;
         float angle = (float)(this.getWorldEntity().getTime() - this.spawnTime) / 1.5F;
         if (this.dx < 0.0F) {
            angle = -angle;
         }

         final TextureDrawOptions options = this.texture
            .initDraw()
            .light(light)
            .rotate(angle, this.texture.getWidth() / 2, this.texture.getHeight() / 2)
            .pos(drawX, drawY - (int)this.getHeight());
         list.add(new EntityDrawable(this) {
            @Override
            public void draw(TickManager tickManager) {
               options.draw();
            }
         });
         float shadowAlpha = Math.abs(GameMath.limit(this.height / 100.0F, 0.0F, 1.0F) - 1.0F);
         float sizeMod = Math.abs(GameMath.limit(this.height / 100.0F, 0.0F, 1.0F) - 1.0F);
         int shadowWidth = (int)(this.shadowTexture.getWidth() * sizeMod);
         int shadowHeight = (int)(this.shadowTexture.getHeight() * sizeMod);
         int shadowX = camera.getDrawX(this.x) - shadowWidth / 2;
         int shadowY = camera.getDrawY(this.y) - shadowHeight / 2;
         TextureDrawOptions shadowOptions = this.shadowTexture.initDraw().size(shadowWidth, shadowHeight).light(light).alpha(shadowAlpha).pos(shadowX, shadowY);
         tileList.add(tm -> shadowOptions.draw());
      }
   }
}
