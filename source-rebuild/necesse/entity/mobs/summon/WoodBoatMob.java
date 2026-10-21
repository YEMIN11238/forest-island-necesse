package necesse.entity.mobs.summon;

import java.awt.Color;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Point2D.Float;
import java.util.List;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.localization.Localization;
import necesse.engine.network.packet.PacketMobMount;
import necesse.engine.registries.MobRegistry;
import necesse.engine.sound.SoundManager;
import necesse.engine.sound.SoundSettings;
import necesse.engine.util.GameMath;
import necesse.engine.util.GameRandom;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.MobDrawable;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.particle.Particle;
import necesse.gfx.GameResources;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.DrawOptions;
import necesse.gfx.drawOptions.texture.TextureDrawOptions;
import necesse.gfx.drawables.OrderableDrawables;
import necesse.gfx.gameTexture.GameTexture;
import necesse.inventory.lootTable.LootTable;
import necesse.inventory.lootTable.lootItem.LootItem;
import necesse.level.gameTile.GameTile;
import necesse.level.maps.CollisionFilter;
import necesse.level.maps.Level;
import necesse.level.maps.light.GameLight;

public class WoodBoatMob extends SummonedMob {
   public static LootTable lootTable = new LootTable(new LootItem("woodboat"));
   protected double deltaCounter;
   private double waterSoundTimer;

   public WoodBoatMob() {
      super(1);
      this.isSummoned = true;
      this.setSpeed(5.0F);
      this.setFriction(0.2F);
      this.setSwimSpeed(10.0F);
      this.accelerationMod = 2.0F;
      this.setKnockbackModifier(0.1F);
      this.collision = new Rectangle(-10, -10, 20, 14);
      this.hitBox = new Rectangle(-14, -15, 28, 24);
      this.selectBox = new Rectangle(-16, -26, 32, 36);
      this.overrideMountedWaterWalking = true;
   }

   @Override
   public void init() {
      super.init();
      this.shouldPlayAmbience = false;
      if (this.isClient() && !this.isMounted()) {
         this.playAmbientSound();
      }
   }

   @Override
   protected SoundSettings getAmbientSound() {
      return this.inLiquidFloat() > 0.0F ? new SoundSettings(GameResources.woodBoat).volume(0.3F) : new SoundSettings(GameResources.blunthit).volume(0.3F);
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
            addParticleEffects(this);
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
   public LootTable getLootTable() {
      return lootTable;
   }

   @Override
   public void spawnDamageText(int damage, int size, boolean isCrit) {
   }

   @Override
   public void playHurtSound() {
   }

   @Override
   public CollisionFilter getLevelCollisionFilter() {
      return super.getLevelCollisionFilter().allLandExShoreTiles();
   }

   @Override
   public void interact(PlayerMob player) {
      if (this.isServer()) {
         if (player.getUniqueID() == this.rider) {
            player.dismount();
            this.getLevel().getServer().network.sendToClientsWithEntity(new PacketMobMount(player.getUniqueID(), -1, false, player.x, player.y), player);
         } else if (player.mount(this, false)) {
            this.getLevel()
               .getServer()
               .network
               .sendToClientsWithEntity(new PacketMobMount(player.getUniqueID(), this.getUniqueID(), false, player.x, player.y), player);
         }
      }
   }

   @Override
   public boolean canInteract(Mob mob) {
      return !this.isMounted() || mob.getUniqueID() == this.rider;
   }

   @Override
   protected String getInteractTip(PlayerMob perspective, boolean debug) {
      return this.isMounted() ? null : Localization.translate("controls", "usetip");
   }

   @Override
   public void playHitSound() {
   }

   @Override
   public void playHitDeathSound() {
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
      final DrawOptions behind = MobRegistry.Textures.woodBoat
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

   public static void drawPlacePreview(Level level, int levelX, int levelY, int dir, GameCamera camera) {
      int drawX = camera.getDrawX(levelX) - 32;
      int drawY = camera.getDrawY(levelY) - 47;
      drawY += level.getLevelTile(getTileCoordinate(levelX), getTileCoordinate(levelY)).getLiquidBobbing();
      MobRegistry.Textures.woodBoat.initDraw().sprite(0, dir, 64).alpha(0.5F).draw(drawX, drawY);
   }

   public static void addParticleEffects(Mob mob) {
      addParticleEffects(mob, 0.0F, 0.0F, 13.0F);
   }

   public static void addParticleEffects(Mob mob, float xOffset, float yOffset, float dirOffset) {
      Level level = mob.getLevel();
      Float dir = GameMath.normalize(mob.dx, mob.dy);
      Float particleDir = GameMath.getPerpendicularDir(dir.x, dir.y);
      dirOffset += mob.getCurrentSpeed() / 8.0F;
      if (mob.dy > 0.0F) {
         dirOffset -= dir.y * 3.0F;
      }

      Float particleOffset = new Float(dir.x * dirOffset, dir.y * dirOffset);
      int tileX = mob.getTileX();
      int tileY = mob.getTileY();
      GameTile tile = level.getTile(tileX, tileY);
      Color tileColor = tile.getMapColor(level, tileX, tileY);
      float height = -10 + tile.getLiquidBobbing(level, tileX, tileY);
      float lifeMod = GameMath.limit(mob.getCurrentSpeed() / 40.0F, 0.2F, 1.0F);

      for (int i = 0; i < 4; i++) {
         int dirMod = i % 2 == 0 ? 1 : -1;
         Float moddedParticleDir = new Float(particleDir.x * dirMod, particleDir.y * dirMod);
         float force = GameRandom.globalRandom.nextFloat() * GameMath.limit(mob.getCurrentSpeed() / 40.0F, 0.2F, 1.0F);
         float randomHeight = 3.0F + force * 3.0F;
         float randomSpeed = 15.0F + force * 10.0F;
         level.entityManager
            .addParticle(
               mob.x + xOffset + particleOffset.x + moddedParticleDir.x * 3.0F + moddedParticleDir.x * GameRandom.globalRandom.floatGaussian() * 1.5F,
               mob.y + yOffset + particleOffset.y - 10.0F + moddedParticleDir.y * 3.0F + moddedParticleDir.y * GameRandom.globalRandom.floatGaussian() * 1.5F,
               Particle.GType.COSMETIC
            )
            .movesFriction(moddedParticleDir.x * randomSpeed, moddedParticleDir.y * randomSpeed, 0.5F)
            .color(tileColor.brighter())
            .sizeFadesInAndOut(3, 5, 0.1F)
            .height((delta, lifeTime, timeAlive, lifePercent) -> {
               double sin = Math.sin(lifePercent * Math.PI);
               return height + (float)sin * randomHeight;
            })
            .lifeTime((int)(GameRandom.globalRandom.getIntBetween(200, 400) * lifeMod));
      }
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
      return getShadowDrawOptions(this, x, y, -6, light, camera);
   }

   public static TextureDrawOptions getShadowDrawOptions(Mob mob, int x, int y, int yOffset, GameLight light, GameCamera camera) {
      GameTexture shadowTexture = MobRegistry.Textures.boat_shadow;
      int res = shadowTexture.getHeight();
      int drawX = camera.getDrawX(x) - res / 2;
      int drawY = camera.getDrawY(y) - res / 2 + yOffset;
      drawY += mob.getBobbing(x, y);
      return shadowTexture.initDraw().sprite(mob.getDir(), 0, res).light(light).pos(drawX, drawY);
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
