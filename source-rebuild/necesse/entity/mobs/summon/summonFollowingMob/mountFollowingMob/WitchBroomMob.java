package necesse.entity.mobs.summon.summonFollowingMob.mountFollowingMob;

import java.awt.Color;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.List;
import java.util.stream.Stream;
import necesse.engine.Settings;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.gameLoop.tickManager.TicksPerSecond;
import necesse.engine.input.Control;
import necesse.engine.localization.Localization;
import necesse.engine.localization.message.GameMessage;
import necesse.engine.localization.message.LocalMessage;
import necesse.engine.modifiers.ModifierValue;
import necesse.engine.network.Packet;
import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.engine.network.packet.PacketMobMount;
import necesse.engine.registries.MobRegistry;
import necesse.engine.sound.SoundSettings;
import necesse.engine.util.GameMath;
import necesse.engine.util.GameRandom;
import necesse.entity.mobs.ActiveMountAbility;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.MobDrawable;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.buffs.BuffModifiers;
import necesse.entity.mobs.buffs.staticBuffs.StaminaBuff;
import necesse.entity.particle.Particle;
import necesse.gfx.GameResources;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.DrawOptions;
import necesse.gfx.drawOptions.texture.TextureDrawOptions;
import necesse.gfx.drawables.OrderableDrawables;
import necesse.gfx.gameTexture.GameTexture;
import necesse.inventory.InventoryItem;
import necesse.level.maps.CollisionFilter;
import necesse.level.maps.Level;
import necesse.level.maps.light.GameLight;

public class WitchBroomMob extends MountFollowingMob implements ActiveMountAbility {
   protected TicksPerSecond particleTicks = TicksPerSecond.ticksPerSecond(30);
   protected float broomHeightTimer = 0.0F;
   protected float timeBeforeMaxBroomHeight = 3000.0F;
   protected int flyHighBuffer = 0;

   public WitchBroomMob() {
      super(100);
      this.setSpeed(100.0F);
      this.setFriction(0.3F);
      this.setSwimSpeed(1.0F);
      this.accelerationMod = 0.6F;
      this.setKnockbackModifier(0.5F);
      this.collision = new Rectangle(-10, -7, 20, 14);
      this.hitBox = new Rectangle(-14, -14, 28, 28);
      this.selectBox = new Rectangle(-15, -15, 30, 30);
      this.overrideMountedWaterWalking = true;
   }

   @Override
   public void setupSpawnPacket(PacketWriter writer) {
      super.setupSpawnPacket(writer);
      writer.putNextFloat(this.broomHeightTimer);
   }

   @Override
   public void applySpawnPacket(PacketReader reader) {
      super.applySpawnPacket(reader);
      this.broomHeightTimer = reader.getNextFloat();
   }

   @Override
   public void onActiveMountAbilityStarted(PlayerMob player, Packet content) {
      if (!this.getLevel().isCave) {
         this.flyHighBuffer = 20;
      }
   }

   @Override
   public boolean tickActiveMountAbility(PlayerMob player, boolean isRunningClient) {
      if (!this.getLevel().isCave) {
         this.flyHighBuffer = 20;
      }

      if (this.flyHighBuffer > 0) {
         long msToDeplete = 3000L;
         float usage = 50.0F / (float)msToDeplete;
         if (!StaminaBuff.useStaminaAndGetValid(player, usage)) {
            return false;
         }
      }

      return !isRunningClient || Control.TRINKET_ABILITY.isDown();
   }

   @Override
   public void onActiveMountAbilityUpdate(PlayerMob player, Packet content) {
   }

   @Override
   public void onActiveMountAbilityStopped(PlayerMob player) {
      this.flyHighBuffer = 0;
   }

   @Override
   public boolean canRunMountAbility(PlayerMob player, Packet content) {
      return player.isServer() && !Settings.strictServerAuthority ? true : StaminaBuff.canStartStaminaUsage(player);
   }

   protected boolean canFlyHigh() {
      return this.flyHighBuffer > 0;
   }

   @Override
   public CollisionFilter getLevelCollisionFilter() {
      return this.getFlyingHeight() > 32 ? null : super.getLevelCollisionFilter();
   }

   protected void setHeightTimer(float delta, boolean positive) {
      if (positive && !this.canFlyHigh()) {
         positive = false;
      }

      int lastHeight = this.getFlyingHeight();
      float lastTimer = this.broomHeightTimer;
      this.broomHeightTimer += positive ? delta : -delta * 2.0F;
      this.broomHeightTimer = GameMath.limit(this.broomHeightTimer, 0.0F, this.timeBeforeMaxBroomHeight);
      if (lastHeight > this.getFlyingHeight() && this.collidesWith(this.getLevel())) {
         this.broomHeightTimer = lastTimer;
      }
   }

   @Override
   public void tickMovement(float delta) {
      super.tickMovement(delta);
      boolean isMoving = this.moveX != 0.0F || this.moveY != 0.0F;
      this.setHeightTimer(delta, this.canFlyHigh());
      if (this.isClient() && isMoving) {
         this.particleTicks.tick(delta);

         while (this.particleTicks.shouldTick()) {
            int dir = this.getDir();
            float dirX = 0.0F;
            float dirY = 0.0F;
            if (dir == 0) {
               dirY = -1.0F;
            } else if (dir == 1) {
               dirX = 1.0F;
            } else if (dir == 2) {
               dirY = 1.0F;
            } else if (dir == 3) {
               dirX = -1.0F;
            }

            this.getLevel()
               .entityManager
               .addParticle(
                  this.x - dirX * 20.0F + GameRandom.globalRandom.floatGaussian() * 3.0F,
                  this.y - dirY * 12.0F + GameRandom.globalRandom.floatGaussian() * 3.0F,
                  Particle.GType.COSMETIC
               )
               .movesConstant(this.dx / 5.0F, this.dy / 5.0F)
               .height(this.getFlyingHeight())
               .color(this.canFlyHigh() ? new Color(77, 26, 77) : new Color(136, 119, 136))
               .sizeFades(6, 12)
               .lifeTime(1000);
         }
      }
   }

   @Override
   public void serverTick() {
      super.serverTick();
      if (this.flyHighBuffer > 0) {
         this.flyHighBuffer--;
      }

      if (!this.isMounted()) {
         this.moveX = 0.0F;
         this.moveY = 0.0F;
      }
   }

   @Override
   public void clientTick() {
      super.clientTick();
      if (this.flyHighBuffer > 0) {
         this.flyHighBuffer--;
      }

      if (!this.isMounted()) {
         this.moveX = 0.0F;
         this.moveY = 0.0F;
      }
   }

   @Override
   public void interact(PlayerMob player) {
      if (this.isServer()) {
         if (player.getUniqueID() == this.rider && this.getFlyingHeight() <= 32) {
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
   public GameMessage getMountDismountError(Mob rider, InventoryItem item) {
      return this.getFlyingHeight() > 32 ? new LocalMessage("misc", "witchbroomdismount") : super.getMountDismountError(rider, item);
   }

   @Override
   public int getFlyingHeight() {
      float speedPerc = this.broomHeightTimer / this.timeBeforeMaxBroomHeight;
      float expoIncrease = GameMath.lerpExp(speedPerc, 2.0F, 0.0F, 1.0F);
      return 2 + (int)(expoIncrease * 32.0F * 3.0F);
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
      int drawY = camera.getDrawY(y) - 46;
      int dir = this.getDir();
      Point sprite = this.getAnimSprite(x, y, dir);
      drawY += this.getBobbing(x, y);
      final DrawOptions options = MobRegistry.Textures.witchBroom
         .body
         .initDraw()
         .sprite(sprite.x, sprite.y, 64)
         .startGlowOptions(level, this.getID())
         .light(light)
         .applyEnemyTracker(this, perspective)
         .pos(drawX, drawY - this.getFlyingHeight());
      list.add(new MobDrawable() {
         @Override
         public void draw(TickManager tickManager) {
         }

         @Override
         public void drawBehindRider(TickManager tickManager) {
            options.draw();
         }
      });
      TextureDrawOptions shadowDrawOptions = this.getShadowDrawOptions(level, x, y, light, camera);
      tileList.add(tm -> shadowDrawOptions.draw());
   }

   @Override
   public Point getAnimSprite(int x, int y, int dir) {
      return new Point((int)(this.getWorldEntity().getTime() / this.getRockSpeed()) % 4, dir % 4);
   }

   @Override
   protected TextureDrawOptions getShadowDrawOptions(Level level, int x, int y, GameLight light, GameCamera camera) {
      GameTexture shadowTexture = MobRegistry.Textures.witchBroom.shadow;
      int drawX = camera.getDrawX(x) - 32;
      int drawY = camera.getDrawY(y) - 32;
      drawY += this.getBobbing(x, y);
      return shadowTexture.initDraw().sprite(0, this.getDir(), 64).light(light).pos(drawX, drawY);
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
      if (spriteX == 0 || spriteX == 2) {
         p.y = 2;
      }

      p.x = p.x + this.getRiderDrawXOffset();
      p.y = p.y + this.getRiderDrawYOffset();
      return p;
   }

   @Override
   public int getRiderDrawYOffset() {
      return 52 - this.getFlyingHeight() - 64;
   }

   @Override
   public int getRiderArmSpriteX() {
      return 0;
   }

   @Override
   public Stream<ModifierValue<?>> getDefaultModifiers() {
      return Stream.of(new ModifierValue<>(BuffModifiers.BOUNCY, true));
   }

   @Override
   public Stream<ModifierValue<?>> getDefaultRiderModifiers() {
      return this.getFlyingHeight() <= 32 && !this.canFlyHigh()
         ? Stream.of(new ModifierValue<>(BuffModifiers.WATER_WALKING, true), new ModifierValue<>(BuffModifiers.STAMINA_CAPACITY, 2.0F))
         : Stream.of(
            new ModifierValue<>(BuffModifiers.WATER_WALKING, true),
            new ModifierValue<>(BuffModifiers.STAMINA_CAPACITY, 2.0F),
            new ModifierValue<>(BuffModifiers.INTIMIDATED, true)
         );
   }

   @Override
   protected SoundSettings getAmbientSound() {
      return new SoundSettings(GameResources.witchBroom).volume(0.2F);
   }
}
