package necesse.entity.mobs.summon.summonFollowingMob.mountFollowingMob;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.List;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.localization.Localization;
import necesse.engine.registries.MobRegistry;
import necesse.engine.sound.SoundManager;
import necesse.engine.sound.SoundSettings;
import necesse.engine.util.GameMath;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.MobDrawable;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.summon.WoodBoatMob;
import necesse.gfx.GameResources;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.DrawOptions;
import necesse.gfx.drawOptions.texture.TextureDrawOptions;
import necesse.gfx.drawables.OrderableDrawables;
import necesse.gfx.gameTexture.GameTexture;
import necesse.level.maps.CollisionFilter;
import necesse.level.maps.Level;
import necesse.level.maps.light.GameLight;

public class SteelBoatMob extends MountFollowingMob {
   protected double deltaCounter;
   private double waterSoundTimer;

   public SteelBoatMob() {
      super(100);
      this.setSpeed(5.0F);
      this.setFriction(0.2F);
      this.setSwimSpeed(14.0F);
      this.accelerationMod = 2.0F;
      this.setKnockbackModifier(0.1F);
      this.collision = new Rectangle(-10, -7, 20, 14);
      this.hitBox = new Rectangle(-14, -15, 28, 24);
      this.selectBox = new Rectangle(-14, -20, 28, 30);
      this.overrideMountedWaterWalking = true;
   }

   @Override
   protected SoundSettings getAmbientSound() {
      return this.inLiquidFloat() > 0.0F ? new SoundSettings(GameResources.steelBoat).volume(0.3F) : new SoundSettings(GameResources.blunthit).volume(0.3F);
   }

   @Override
   public void serverTick() {
      super.serverTick();
      if (!this.isMounted()) {
         this.moveX = 0.0F;
         this.moveY = 0.0F;
      }
   }

   @Override
   public void clientTick() {
      super.clientTick();
      if (!this.isMounted()) {
         this.moveX = 0.0F;
         this.moveY = 0.0F;
      }
   }

   @Override
   public void tickMovement(float delta) {
      super.tickMovement(delta);
      if (!this.isServer() && this.inLiquid()) {
         this.deltaCounter = this.deltaCounter + delta * Math.max(0.2F, this.getCurrentSpeed() / 30.0F);
         if (this.deltaCounter >= 50.0) {
            this.deltaCounter -= 50.0;
            WoodBoatMob.addParticleEffects(this);
         }

         this.waterSoundTimer = this.waterSoundTimer + delta * Math.max(0.2F, this.getCurrentSpeed() / 30.0F);
         if (this.waterSoundTimer >= 400.0 && this.getCurrentSpeed() >= 5.0F) {
            this.waterSoundTimer = 0.0;
            if (this.inLiquid()) {
               SoundManager.playSound(new SoundSettings(GameResources.waterblob).volume(0.1F), this);
            }
         }
      }
   }

   @Override
   public CollisionFilter getLevelCollisionFilter() {
      return super.getLevelCollisionFilter().allLandExShoreTiles();
   }

   @Override
   protected String getInteractTip(PlayerMob perspective, boolean debug) {
      return this.isMounted() ? null : Localization.translate("controls", "usetip");
   }

   @Override
   public void onFollowingAnotherLevel(Mob followingMob) {
      if (this.getRider() == followingMob) {
         super.onFollowingAnotherLevel(followingMob);
      } else {
         this.remove();
      }
   }

   @Override
   public void playDeathSound() {
   }

   @Override
   public void addDrawables(
      List<MobDrawable> list,
      OrderableDrawables tileList,
      OrderableDrawables topList,
      Level level,
      int x,
      int y,
      TickManager tickManager,
      GameCamera camera,
      PlayerMob perspective
   ) {
      super.addDrawables(list, tileList, topList, level, x, y, tickManager, camera, perspective);
      GameLight light = level.getLightLevel(getTileCoordinate(x), getTileCoordinate(y));
      int drawX = camera.getDrawX(x) - 32;
      int drawY = camera.getDrawY(y) - 47 + 5;
      int dir = this.getDir();
      Point sprite = this.getAnimSprite(x, y, dir);
      drawY += this.getBobbing(x, y);
      drawY += level.getTile(getTileCoordinate(x), getTileCoordinate(y)).getMobSinkingAmount(this);
      final DrawOptions behind = MobRegistry.Textures.steelBoat
         .initDraw()
         .sprite(sprite.x, sprite.y, 64)
         .startGlowOptions(level, this.getID())
         .light(light)
         .applyEnemyTracker(this, perspective)
         .pos(drawX, drawY);
      list.add(new MobDrawable() {
         @Override
         public void draw(TickManager tickManager) {
         }

         @Override
         public void drawBehindRider(TickManager tickManager) {
            behind.draw();
         }
      });
   }

   @Override
   public float getFullInLiquidAtPercent(int x, int y) {
      return 0.0F;
   }

   @Override
   public Point getAnimSprite(int x, int y, int dir) {
      return new Point(0, dir);
   }

   @Override
   protected TextureDrawOptions getShadowDrawOptions(Level level, int x, int y, GameLight light, GameCamera camera) {
      GameTexture shadowTexture = MobRegistry.Textures.boat_shadow;
      int res = shadowTexture.getHeight();
      int drawX = camera.getDrawX(x) - res / 2;
      int drawY = camera.getDrawY(y) - res / 2 - 6;
      drawY += this.getBobbing(x, y);
      return shadowTexture.initDraw().sprite(this.getDir(), 0, res).light(light).pos(drawX, drawY);
   }

   @Override
   public int getRockSpeed() {
      return 10000;
   }

   @Override
   public int getWaterRockSpeed() {
      return 100;
   }

   @Override
   public Point getSpriteOffset(int spriteX, int spriteY) {
      Point p = new Point(0, 0);
      p.x = p.x + this.getRiderDrawXOffset();
      p.y = p.y + this.getRiderDrawYOffset();
      return p;
   }

   @Override
   public int getRiderDrawYOffset() {
      return 2;
   }

   @Override
   public int getRiderArmSpriteX() {
      return 2;
   }

   @Override
   public GameTexture getRiderMask() {
      return MobRegistry.Textures.boat_mask[GameMath.limit(this.getDir(), 0, MobRegistry.Textures.boat_mask.length - 1)];
   }

   @Override
   public int getRiderMaskYOffset() {
      return -7;
   }
}
