package necesse.entity.mobs.summon.summonFollowingMob.mountFollowingMob;

import java.awt.Color;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Point2D.Float;
import java.util.List;
import java.util.stream.Stream;
import necesse.engine.CameraShake;
import necesse.engine.Settings;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.input.Control;
import necesse.engine.localization.Localization;
import necesse.engine.modifiers.ModifierValue;
import necesse.engine.network.Packet;
import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.engine.network.server.ServerClient;
import necesse.engine.registries.BuffRegistry;
import necesse.engine.registries.MobRegistry;
import necesse.engine.sound.SoundEffect;
import necesse.engine.sound.SoundManager;
import necesse.engine.sound.SoundSettings;
import necesse.engine.util.GameMath;
import necesse.engine.util.GameRandom;
import necesse.entity.mobs.ActiveMountAbility;
import necesse.entity.mobs.MaskShaderOptions;
import necesse.entity.mobs.MobDrawable;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.buffs.ActiveBuff;
import necesse.entity.mobs.buffs.BuffModifiers;
import necesse.entity.mobs.buffs.staticBuffs.StaminaBuff;
import necesse.entity.particle.Particle;
import necesse.gfx.GameResources;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.DrawOptions;
import necesse.gfx.drawOptions.texture.TextureDrawOptions;
import necesse.gfx.drawables.OrderableDrawables;
import necesse.gfx.gameTexture.GameTexture;
import necesse.level.maps.Level;
import necesse.level.maps.light.GameLight;

public class BumperCarMob extends MountFollowingMob implements ActiveMountAbility {
   public static int totalVariants = 6;
   public final float speedTicketAchievementSpeed = 350.0F;
   public int variant = 0;
   public CameraShake driftShake;

   public BumperCarMob() {
      super(100);
      this.setSpeed(200.0F);
      this.setFriction(0.5F);
      this.setSwimSpeed(0.1F);
      this.accelerationMod = 0.25F;
      this.setKnockbackModifier(0.5F);
      this.collision = new Rectangle(-10, -7, 20, 14);
      this.hitBox = new Rectangle(-14, -14, 28, 28);
      this.selectBox = new Rectangle(-15, -15, 30, 30);
      this.overrideMountedWaterWalking = true;
      this.shouldPlayAmbience = true;
      this.swimMaskMove = 10;
      this.swimMaskOffset = 0;
      this.swimSinkOffset = 0;
   }

   @Override
   public void setupSpawnPacket(PacketWriter writer) {
      super.setupSpawnPacket(writer);
      writer.putNextShortUnsigned(this.variant);
   }

   @Override
   public void applySpawnPacket(PacketReader reader) {
      super.applySpawnPacket(reader);
      this.variant = reader.getNextShortUnsigned();
   }

   @Override
   public boolean tickActiveMountAbility(PlayerMob player, boolean isRunningClient) {
      if (this.inLiquid()) {
         this.buffManager.removeBuff(BuffRegistry.NITRO_ACTIVE, false);
      } else {
         ActiveBuff speedBuff = this.buffManager.getBuff(BuffRegistry.NITRO_ACTIVE);
         if (speedBuff != null) {
            speedBuff.setDurationLeftSeconds(1.0F);
         } else {
            this.buffManager.addBuff(new ActiveBuff(BuffRegistry.NITRO_ACTIVE, this, 1.0F, player), false);
         }

         if ((this.moveX != 0.0F || this.moveY != 0.0F) && (this.dx != 0.0F || this.dy != 0.0F)) {
            long msToDeplete = 2500L;
            float usage = 50.0F / (float)msToDeplete;
            if (!StaminaBuff.useStaminaAndGetValid(player, usage)) {
               return false;
            }
         }

         if (this.isServer() && player.isServerClient()) {
            ServerClient client = player.getServerClient();
            if (client.achievementsLoaded() && this.currentSpeed >= 350.0F) {
               client.achievements().SPEEDING_TICKET.markCompleted(client);
            }
         }
      }

      return !isRunningClient || Control.TRINKET_ABILITY.isDown();
   }

   @Override
   public void onActiveMountAbilityUpdate(PlayerMob player, Packet content) {
   }

   @Override
   public boolean canRunMountAbility(PlayerMob player, Packet content) {
      if (this.inLiquid()) {
         return false;
      } else {
         return player.isServer() && !Settings.strictServerAuthority ? true : StaminaBuff.canStartStaminaUsage(player);
      }
   }

   @Override
   public void onActiveMountAbilityStarted(PlayerMob player, Packet content) {
      if (this.isClient()) {
         SoundManager.playSound(GameResources.bumperCarNitro, SoundEffect.effect(this));
      }
   }

   @Override
   public void onActiveMountAbilityStopped(PlayerMob player) {
      this.buffManager.removeBuff(BuffRegistry.NITRO_ACTIVE, false);
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

      if (this.isDrifting()) {
         for (int i = 0; i < 5; i++) {
            this.getLevel()
               .entityManager
               .addParticle(
                  this.x + GameRandom.globalRandom.getFloatBetween(-10.0F, 10.0F),
                  this.y + GameRandom.globalRandom.getFloatBetween(-10.0F, 10.0F),
                  Particle.GType.COSMETIC
               )
               .sprite(GameResources.smokePuff.sprite(0, 0, 32))
               .color(new Color(205, 210, 218))
               .sizeFades(16, 32)
               .movesFriction(0.0F, -10.0F, 0.7F)
               .heightMoves(0.0F, -10.0F)
               .fadesAlphaTime(100, 200)
               .lifeTime(600 + GameRandom.globalRandom.getIntBetween(0, 200));
         }

         SoundManager.playSound(GameResources.shake, SoundEffect.effect(this).pitch(3.0F).volume(0.3F));
      }

      float idleToDriveLerp = GameMath.limit(this.currentSpeed / 100.0F, 0.0F, 1.0F);
      SoundManager.playSound(
         GameResources.bumperCarIdle,
         SoundEffect.effect(this).volume((1.0F - idleToDriveLerp) / 4.0F).pitch(GameRandom.globalRandom.getFloatBetween(1.4F, 1.6F))
      );
      SoundManager.playSound(GameResources.bumperCarDrive, SoundEffect.effect(this).volume(idleToDriveLerp / 5.0F).pitch(1.0F + idleToDriveLerp / 1.1F));
   }

   private boolean isDrifting() {
      if (this.currentSpeed < 30.0F) {
         return false;
      }

      Float inputVector = new Float(this.moveX, this.moveY);
      Float velocityVector = GameMath.normalize(this.dx, this.dy);
      double dot = inputVector.x * velocityVector.x + inputVector.y * velocityVector.y;
      double angleBetween = getTurnAngle(inputVector, velocityVector, dot);
      return Math.toDegrees(angleBetween) > 30.0;
   }

   private static double getTurnAngle(Float inputVector, Float velocityVector, double dot) {
      double magA = Math.sqrt(inputVector.x * inputVector.x + inputVector.y * inputVector.y);
      double magB = Math.sqrt(velocityVector.x * velocityVector.x + velocityVector.y * velocityVector.y);
      double angleBetween = 0.0;
      if (magA > 0.0 && magB > 0.0) {
         double cosTheta = dot / (magA * magB);
         cosTheta = Math.max(-1.0, Math.min(1.0, cosTheta));
         angleBetween = Math.acos(cosTheta);
      }

      return angleBetween;
   }

   @Override
   protected String getInteractTip(PlayerMob perspective, boolean debug) {
      return this.isMounted() ? null : Localization.translate("controls", "usetip");
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
      int drawY = camera.getDrawY(y) - 38;
      Point sprite = this.getAnimSprite(x, y, this.getDir());
      drawY += this.getBobbing(x, y);
      drawY += level.getTile(getTileCoordinate(x), getTileCoordinate(y)).getMobSinkingAmount(this);
      Float shake = new Float(0.0F, 0.0F);
      if (this.isDrifting()) {
         if (this.driftShake == null || this.driftShake.isOver(this.getLocalTime())) {
            this.driftShake = new CameraShake(this.getLocalTime(), 250, 75, 1.0F, 1.0F, true);
         }

         shake = this.driftShake.getCurrentShake(this.getWorldEntity().getLocalTime());
      } else if (this.currentSpeed < 30.0F) {
         if (this.driftShake == null || this.driftShake.isOver(this.getLocalTime())) {
            this.driftShake = new CameraShake(this.getLocalTime(), 250, 20, 0.0F, 0.5F, true);
         }

         shake = this.driftShake.getCurrentShake(this.getWorldEntity().getLocalTime());
      }

      drawX = (int)(drawX + shake.x);
      drawY = (int)(drawY + shake.y);
      final MaskShaderOptions swimMask = this.getSwimMaskShaderOptions(this.inLiquidFloat(x, y));
      final DrawOptions behind = MobRegistry.Textures.bumperCar
         .initDraw()
         .sprite(Math.floorMod(this.variant, totalVariants), sprite.y, 64)
         .addMaskShader(swimMask)
         .light(light)
         .pos(drawX, drawY);
      final DrawOptions front = MobRegistry.Textures.bumperCarOverlay
         .initDraw()
         .sprite(Math.floorMod(this.variant, totalVariants), sprite.y, 64)
         .addMaskShader(swimMask)
         .light(light)
         .pos(drawX, drawY);
      list.add(new MobDrawable() {
         @Override
         public void draw(TickManager tickManager) {
            swimMask.use();
            front.draw();
            swimMask.stop();
         }

         @Override
         public void drawBehindRider(TickManager tickManager) {
            swimMask.use();
            behind.draw();
            swimMask.stop();
         }
      });
      this.addShadowDrawables(tileList, level, x, y, light, camera);
   }

   @Override
   public Point getAnimSprite(int x, int y, int dir) {
      return new Point((int)(this.getWorldEntity().getTime() / this.getRockSpeed()) % 4, dir % 4);
   }

   @Override
   protected TextureDrawOptions getShadowDrawOptions(Level level, int x, int y, GameLight light, GameCamera camera) {
      GameTexture shadowTexture = MobRegistry.Textures.bumperCar_shadow;
      int drawX = camera.getDrawX(x) - 32;
      int drawY = camera.getDrawY(y) - 38;
      drawY += this.getBobbing(x, y);
      return shadowTexture.initDraw().sprite(0, this.getDir(), 64).light(light).pos(drawX, drawY);
   }

   @Override
   public GameTexture getRiderMask() {
      return MobRegistry.Textures.mountmask;
   }

   @Override
   public int getRockSpeed() {
      return 500;
   }

   @Override
   public int getWaterRockSpeed() {
      return 500;
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
      return this.getSwimMaskShaderOptions(this.inLiquidFloat(this.getDrawX(), this.getDrawY())).drawYOffset + 5;
   }

   @Override
   public int getRiderArmSpriteX() {
      return 0;
   }

   @Override
   public int getRiderSpriteX() {
      return 0;
   }

   @Override
   public Stream<ModifierValue<?>> getDefaultModifiers() {
      return Stream.of(new ModifierValue<>(BuffModifiers.BOUNCY, true));
   }

   @Override
   protected SoundSettings getAmbientSound() {
      return new SoundSettings(GameResources.shake).basePitch(1.0F + 6.0F / (200.0F - this.currentSpeed));
   }
}
