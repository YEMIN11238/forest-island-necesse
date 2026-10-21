package necesse.entity.projectile.boomerangProjectile;

import java.awt.Color;
import java.util.List;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.sound.SoundEffect;
import necesse.engine.sound.SoundManager;
import necesse.engine.sound.SoundPlayer;
import necesse.engine.sound.SoundSettings;
import necesse.engine.util.GameMath;
import necesse.engine.util.GameRandom;
import necesse.entity.chains.Chain;
import necesse.entity.mobs.GameDamage;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.PlayerMob;
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

public class AnchorBoomerangProjectile extends BoomerangProjectile {
   private Chain chain;

   public AnchorBoomerangProjectile() {
   }

   public AnchorBoomerangProjectile(
      Level level, float x, float y, float targetX, float targetY, float speed, int distance, GameDamage damage, int knockback, Mob owner
   ) {
      this.setLevel(level);
      this.x = x;
      this.y = y;
      this.setTarget(targetX, targetY);
      this.speed = speed;
      this.setDistance(distance);
      this.setDamage(damage);
      this.knockback = knockback;
      this.setOwner(owner);
   }

   @Override
   public void init() {
      super.init();
      this.setWidth(30.0F, true);
      this.height = 10.0F;
      this.bouncing = 0;
      this.piercing = 0;
      Mob owner = this.getOwner();
      if (owner != null) {
         this.chain = new Chain(owner, this) {
            @Override
            public int getDrawY() {
               return super.getDrawY() - 30;
            }
         };
         this.chain.sprite = new GameSprite(GameResources.chains, 9, 0, 32);
         this.chain.height = this.getHeight();
         this.getLevel().entityManager.addChain(this.chain);
      }
   }

   @Override
   public void clientTick() {
      super.clientTick();
      if (this.getOwner() == null) {
         this.remove();
      }

      if (this.moveSoundPlayer != null) {
         this.moveSoundPlayer.refreshLooping();
      }
   }

   @Override
   public void serverTick() {
      super.serverTick();
      if (this.getOwner() == null) {
         this.remove();
      }
   }

   @Override
   public void applyDamage(Mob mob, float x, float y, float knockbackDirX, float knockbackDirY) {
      mob.isServerHit(this.getDistanceBasedDamage(mob), knockbackDirX, knockbackDirY, this.getDistanceBasedKnockback(mob), this);
   }

   @Override
   public Trail getTrail() {
      return new Trail(this, this.getLevel(), new Color(150, 150, 150), 25.0F, 250, 18.0F);
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
         int drawX = camera.getDrawX(this.x) - this.texture.getWidth() / 2;
         int drawY = camera.getDrawY(this.y) - this.texture.getHeight() / 2;
         final TextureDrawOptions options = this.texture
            .initDraw()
            .light(light)
            .rotate(this.getAngle() + 45.0F + (this.returningToOwner ? 180.0F : 0.0F), this.texture.getWidth() / 2, this.texture.getHeight() / 2)
            .pos(drawX, drawY - (int)this.getHeight());
         list.add(new EntityDrawable(this) {
            @Override
            public void draw(TickManager tickManager) {
               options.draw();
            }
         });
         this.addShadowDrawables(tileList, drawX, drawY, light, this.getAngle() + 45.0F + (this.returningToOwner ? 180.0F : 0.0F), this.texture.getWidth() / 2);
      }
   }

   @Override
   public float getAngle() {
      return this.angle % 360.0F;
   }

   private GameDamage getDistanceBasedDamage(Mob mob) {
      Mob attackOwner = this.getAttackOwner();
      float dmgMod = 2.0F - GameMath.diamondDistance(attackOwner.x, attackOwner.y, mob.x, mob.y) / this.distance;
      return new GameDamage(this.getDamage().damage * dmgMod);
   }

   private int getDistanceBasedKnockback(Mob mob) {
      Mob attackOwner = this.getAttackOwner();
      return (int)((this.returningToOwner ? 1 : -1) * GameMath.diamondDistance(attackOwner.x, attackOwner.y, mob.x, mob.y));
   }

   @Override
   public SoundPlayer playMoveSound() {
      return SoundManager.playSound(this.getMoveSound(), this);
   }

   @Override
   protected SoundSettings getMoveSound() {
      return new SoundSettings(GameResources.jingle).volume(0.5F).basePitch(0.86F).pitchVariance(0.0F);
   }

   @Override
   protected SoundSettings getSpawnSound() {
      return null;
   }

   @Override
   public void playHitSound(float x, float y) {
      float variation = GameRandom.globalRandom.getFloatBetween(-0.1F, 0.1F);
      SoundManager.playSound(GameResources.cling, SoundEffect.effect(x, y).pitch(0.5F + variation));
      SoundManager.playSound(GameResources.blunthit, SoundEffect.effect(x, y).pitch(0.6F + variation).volume(1.4F));
   }

   @Override
   public void remove() {
      if (this.chain != null) {
         this.chain.remove();
      }

      super.remove();
   }
}
