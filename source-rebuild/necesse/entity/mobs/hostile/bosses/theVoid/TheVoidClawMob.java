package necesse.entity.mobs.hostile.bosses.theVoid;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Point2D.Float;
import java.util.List;
import necesse.engine.CameraShake;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.localization.message.GameMessage;
import necesse.engine.network.NetworkClient;
import necesse.engine.network.Packet;
import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.engine.network.server.ServerClient;
import necesse.engine.registries.MobRegistry;
import necesse.engine.sound.SoundEffect;
import necesse.engine.sound.SoundManager;
import necesse.engine.util.GameMath;
import necesse.engine.util.GameRandom;
import necesse.engine.util.GameUtils;
import necesse.engine.util.tween.Easings;
import necesse.entity.ParticleTypeSwitcher;
import necesse.entity.levelEvent.TheVoidClawGroundShatterGroundEvent;
import necesse.entity.levelEvent.mobAbilityLevelEvent.TheVoidClawBeamLevelEvent;
import necesse.entity.mobs.Attacker;
import necesse.entity.mobs.DeathMessageTable;
import necesse.entity.mobs.GameDamage;
import necesse.entity.mobs.LevelMob;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.MobDrawable;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.ability.MobAbility;
import necesse.entity.mobs.hostile.bosses.FlyingBossMob;
import necesse.entity.mobs.mobMovement.MobMovementCircle;
import necesse.entity.mobs.mobMovement.MobMovementCircleRelative;
import necesse.entity.mobs.mobMovement.MobMovementLevelPos;
import necesse.entity.mobs.mobMovement.MobMovementRelative;
import necesse.entity.particle.Particle;
import necesse.gfx.GameResources;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.DrawOptions;
import necesse.gfx.drawOptions.texture.TextureDrawOptions;
import necesse.gfx.drawables.OrderableDrawables;
import necesse.gfx.gameTexture.GameTexture;
import necesse.level.maps.Level;
import necesse.level.maps.light.GameLight;

public class TheVoidClawMob extends FlyingBossMob {
   public final LevelMob<TheVoidMob> master = new LevelMob<>();
   protected ParticleTypeSwitcher particleTypeSwitcher = new ParticleTypeSwitcher(
      Particle.GType.IMPORTANT_COSMETIC, Particle.GType.COSMETIC, Particle.GType.CRITICAL
   );
   public int clawIndex;
   public boolean leftHanded;
   protected Runnable onArrived;
   protected int maxArrivedTime;
   protected long nextIdleMovementChangeTime;
   protected long slamAnimationStartTime;
   protected int slamAnimationDuration = 500;
   protected int slamAnimationWaitTime;
   protected int slamRiseTime = 2000;
   protected boolean isSlamClenched;
   protected boolean slamEventTriggered;
   public final TheVoidClawMob.StartSlamAbility startSlamAbility;
   protected long beamStartTime;
   protected int beamDuration;
   public final TheVoidClawMob.StartBeamAbility startBeamAbility;

   public TheVoidClawMob() {
      super(10);
      this.isSummoned = true;
      this.dropsLoot = false;
      this.moveAccuracy = 40;
      this.collision = new Rectangle(-45, -60, 90, 90);
      this.hitBox = new Rectangle(-50, -65, 100, 100);
      this.selectBox = new Rectangle(-60, -85, 120, 120);
      this.setKnockbackModifier(0.0F);
      this.setRegen(0.0F);
      this.setSpeed(250.0F);
      this.setFriction(2.0F);
      this.startSlamAbility = this.registerAbility(new TheVoidClawMob.StartSlamAbility());
      this.startBeamAbility = this.registerAbility(new TheVoidClawMob.StartBeamAbility());
   }

   @Override
   public void setupSpawnPacket(PacketWriter writer) {
      super.setupSpawnPacket(writer);
      writer.putNextInt(this.master.uniqueID);
      writer.putNextBoolean(this.leftHanded);
      writer.putNextInt(this.clawIndex);
   }

   @Override
   public void applySpawnPacket(PacketReader reader) {
      super.applySpawnPacket(reader);
      this.master.uniqueID = reader.getNextInt();
      this.leftHanded = reader.getNextBoolean();
      this.clawIndex = reader.getNextInt();
   }

   @Override
   public void init() {
      super.init();
      this.countStats = false;
   }

   public Point getClawBasePositionOffset() {
      int xOffset = 220 + this.clawIndex * 75;
      return new Point(this.leftHanded ? xOffset : -xOffset, 250 - this.clawIndex * 75);
   }

   @Override
   public void clientTick() {
      super.clientTick();
      this.tickMaster();
      TheVoidMob master = this.master.get(this.getLevel());
      float alpha = master == null ? 1.0F : master.getFadeAlpha();
      alpha = Math.min(master == null ? 1.0F : 1.0F - master.getFadeWhiteness(), alpha);

      for (int i = 0; i < 5; i++) {
         int xOffset = GameRandom.globalRandom.getIntBetween(-50, 50);
         this.getLevel()
            .entityManager
            .addParticle(this.x + xOffset, this.y + GameRandom.globalRandom.getIntBetween(-30, 30), this.particleTypeSwitcher.next())
            .height(this.getCurrentHeight() + 75.0F)
            .sprite(GameResources.voidPuffParticles.sprite(GameRandom.globalRandom.nextInt(4), 0, 12))
            .sizeFades(24, 48)
            .color(1.0F, 1.0F, 1.0F, alpha)
            .movesFriction(GameRandom.globalRandom.getIntBetween(-5, 5), GameRandom.globalRandom.getIntBetween(-30, -60) + Math.abs(xOffset), 0.8F)
            .lifeTime(1000);
      }
   }

   @Override
   public void serverTick() {
      super.serverTick();
      this.movementUpdateTime = this.getTime();
      this.healthUpdateTime = this.getTime();
      this.tickMaster();
      boolean checkMaxArrivedTime = false;
      if (this.maxArrivedTime > 0) {
         checkMaxArrivedTime = true;
         this.maxArrivedTime -= 50;
      }

      if (this.onArrived != null
         && this.slamAnimationStartTime == 0L
         && this.beamStartTime == 0L
         && (this.hasArrivedAtTarget() || checkMaxArrivedTime && this.maxArrivedTime <= 0)) {
         this.onArrived.run();
         this.onArrived = null;
      }

      if (this.isIdle() && this.nextIdleMovementChangeTime <= this.getTime()) {
         TheVoidMob master = this.master.get(this.getLevel());
         if (master != null && !master.isInDeathAnimation()) {
            Point baseOffset = this.getClawBasePositionOffset();
            if (GameRandom.globalRandom.nextBoolean()) {
               Point randomOffset = new Point(GameRandom.globalRandom.getIntBetween(-35, 35), GameRandom.globalRandom.getIntBetween(-35, 35));
               this.setMovement(new MobMovementRelative(master, baseOffset.x + randomOffset.x, baseOffset.y + randomOffset.y));
               this.nextIdleMovementChangeTime = this.getTime() + 1000L;
            } else {
               int radius = GameRandom.globalRandom.getIntBetween(30, 50);
               float speed = MobMovementCircle.convertToRotSpeed(radius, this.getSpeed());
               this.setMovement(
                  new MobMovementCircleRelative(this, master, baseOffset.x, baseOffset.y, radius, speed / 4.0F, GameRandom.globalRandom.nextBoolean())
               );
               this.nextIdleMovementChangeTime = this.getTime() + 2000L;
            }
         }
      }
   }

   public void flyToPositionAndSlam(float levelX, float levelY, int duration, int waitTime, int riseTime, boolean isSlamClenched) {
      this.setMovement(new MobMovementLevelPos(levelX, levelY));
      this.maxArrivedTime = 0;
      this.onArrived = () -> {
         this.stopMoving();
         this.startSlamAbility.runAndSend(duration, waitTime, riseTime, isSlamClenched);
      };
   }

   public void flyToTargetAndSlam(
      Mob target, float relativeX, float relativeY, int maxFlyTime, int duration, int waitTime, int riseTime, boolean isSlamClenched
   ) {
      this.setMovement(new MobMovementRelative(target, relativeX, relativeY));
      this.maxArrivedTime = maxFlyTime;
      this.onArrived = () -> {
         this.stopMoving();
         this.startSlamAbility.runAndSend(duration, waitTime, riseTime, isSlamClenched);
      };
   }

   public void flyToTargetAndSlam(int posX, int posY, int maxFlyTime, int duration, int waitTime, int riseTime, boolean isSlamClenched) {
      this.setMovement(new MobMovementLevelPos(posX, posY));
      this.maxArrivedTime = maxFlyTime;
      this.onArrived = () -> {
         this.stopMoving();
         this.startSlamAbility.runAndSend(duration, waitTime, riseTime, isSlamClenched);
      };
   }

   public void flyToTargetAndSlam(Mob target, int maxFlyTime, int duration, int waitTime, int riseTime, boolean isSlamClenched) {
      this.flyToTargetAndSlam(target, 0.0F, 0.0F, maxFlyTime, duration, waitTime, riseTime, isSlamClenched);
   }

   @Override
   public void tickMovement(float delta) {
      TheVoidMob master = this.master.get(this.getLevel());
      if (master != null && master.isInDeathAnimation()) {
         this.dx = 0.0F;
         this.dy = 0.0F;
         this.stopMoving();
      }

      if (this.slamAnimationStartTime != 0L) {
         long timeSinceStart = this.getTime() - this.slamAnimationStartTime;
         if (master != null && master.isInDeathAnimation()) {
            long timeSinceDeathStart = this.getTime() - master.deathAnimationStartTime;
            timeSinceStart -= timeSinceDeathStart;
         }

         if (!this.slamEventTriggered && timeSinceStart > this.slamAnimationDuration) {
            if (!this.isClient()) {
               Mob owner = this;
               if (master != null) {
                  owner = master;
               }

               if (this.isSlamClenched) {
                  TheVoidClawGroundShatterGroundEvent e = new TheVoidClawGroundShatterGroundEvent(
                     owner, this.getX(), this.getY(), TheVoidMob.clawShatterDamage, 200, 500, GameRandom.globalRandom
                  );
                  this.getLevel().entityManager.events.add(e);
               }
            } else {
               if (this.isClient()) {
                  CameraShake cameraShake = this.getClient().startCameraShake(this.x, this.y, 200, 40, 2.0F, 2.0F, true);
                  cameraShake.minDistance = 200;
                  cameraShake.listenDistance = 2000;
               }

               SoundManager.playSound(GameResources.explosionHeavy, SoundEffect.effect(this.x, this.y).volume(0.8F).pitch(0.8F).falloffDistance(1400));
               SoundManager.playSound(GameResources.heavyHammer, SoundEffect.effect(this.x, this.y).volume(1.2F).pitch(0.4F).falloffDistance(1400));

               for (int i = 0; i < 20; i++) {
                  int lifeTime = GameRandom.globalRandom.getIntBetween(500, 3000);
                  float lifePerc = lifeTime / 2000.0F;
                  float startHeight = 0.0F;
                  float height = startHeight + GameRandom.globalRandom.getIntBetween(30, 60) * lifePerc;
                  int angle = GameRandom.globalRandom.nextInt(360);
                  Float dir = GameMath.getAngleDir(angle);
                  this.getLevel()
                     .entityManager
                     .addParticle(
                        this.x + GameRandom.globalRandom.getFloatBetween(-20.0F, 20.0F),
                        this.y + GameRandom.globalRandom.getFloatBetween(-15.0F, 15.0F),
                        Particle.GType.IMPORTANT_COSMETIC
                     )
                     .sizeFades(20, 30)
                     .movesFriction(
                        dir.x * GameRandom.globalRandom.getFloatBetween(30.0F, 50.0F), dir.y * GameRandom.globalRandom.getFloatBetween(30.0F, 50.0F), 0.5F
                     )
                     .heightMoves(startHeight, height)
                     .colorRandom(310.0F, 0.95F, 0.95F, 10.0F, 0.05F, 0.05F)
                     .lifeTime(lifeTime);
               }
            }

            this.slamEventTriggered = true;
         }

         if (timeSinceStart <= this.slamAnimationDuration + this.slamAnimationWaitTime) {
            this.dx = 0.0F;
            this.dy = 0.0F;
            this.colDx = 0.0F;
            this.colDy = 0.0F;
         }

         if (timeSinceStart > this.slamAnimationDuration + this.slamAnimationWaitTime + this.slamRiseTime) {
            this.slamAnimationStartTime = 0L;
         }
      }

      if (this.beamStartTime != 0L) {
         long timeSinceStart = this.getTime() - this.beamStartTime;
         if (timeSinceStart > this.beamDuration) {
            this.beamStartTime = 0L;
         }
      }

      super.tickMovement(delta);
   }

   public boolean isIdle() {
      if (this.slamAnimationStartTime != 0L) {
         long timeSinceStart = this.getTime() - this.slamAnimationStartTime;
         if (timeSinceStart <= this.slamAnimationDuration + this.slamAnimationWaitTime) {
            return false;
         }
      }

      return this.beamStartTime == 0L && this.onArrived == null;
   }

   public float getCurrentHeight() {
      long startTime = this.getTime();
      TheVoidMob master = this.master.get(this.getLevel());
      if (master != null && master.isInDeathAnimation()) {
         startTime = master.deathAnimationStartTime;
      }

      float bobbing = GameUtils.getAnimFloatContinuous(startTime, 2000) * 5.0F;
      float height = 100.0F + bobbing;
      float addedHeight = 0.0F;
      if (master != null && master.isInDeathAnimation()) {
         int timeSinceDeathAnimationStart = (int)(this.getTime() - master.deathAnimationStartTime);
         float totalHeightIncreaseOnDeath = 150.0F;
         float deathProgress = (float)timeSinceDeathAnimationStart / master.totalDeathAnimationDuration;
         addedHeight = GameMath.lerp(deathProgress, 0.0F, totalHeightIncreaseOnDeath);
      }

      if (this.slamAnimationStartTime != 0L) {
         long timeSinceSlamStart = startTime - this.slamAnimationStartTime;
         if (timeSinceSlamStart < this.slamAnimationDuration) {
            float progress = (float)timeSinceSlamStart / this.slamAnimationDuration;
            float ease = Easings.BackIn.ease(progress);
            if (ease < 0.0F) {
               ease *= 2.0F;
            }

            return GameMath.lerp(ease, height, 0.0F) + addedHeight;
         }

         timeSinceSlamStart -= this.slamAnimationDuration;
         if (timeSinceSlamStart < this.slamAnimationWaitTime) {
            return addedHeight;
         }

         timeSinceSlamStart -= this.slamAnimationWaitTime;
         if (timeSinceSlamStart < this.slamRiseTime) {
            float progress = (float)timeSinceSlamStart / this.slamRiseTime;
            return GameMath.lerp(Easings.CubicIn.ease(progress), 0.0F, height) + addedHeight;
         }
      }

      return height + addedHeight;
   }

   @Override
   public int getHealth() {
      if (this.master != null) {
         TheVoidMob head = this.master.get(this.getLevel());
         if (head != null) {
            return head.getHealth();
         }
      }

      return super.getHealth();
   }

   @Override
   public int getMaxHealth() {
      if (this.master != null) {
         TheVoidMob head = this.master.get(this.getLevel());
         if (head != null) {
            return head.getMaxHealth();
         }
      }

      return super.getMaxHealth();
   }

   public void tickMaster() {
      if (!this.removed()) {
         TheVoidMob master = this.master.get(this.getLevel());
         if (master == null) {
            this.remove();
         } else {
            this.setMaxHealth(master.getMaxHealth());
            this.setHealthHidden(master.getHealth(), 0.0F, 0.0F, null);
            this.setArmor(master.getArmorFlat() * 2);
         }

         this.setSpeed(GameMath.lerp(this.getHealthPercent(), 325, 250));
      }
   }

   @Override
   public boolean canBeTargeted(Mob attacker, NetworkClient attackerClient) {
      return this.getFlyingHeight() < 50 && super.canBeTargeted(attacker, attackerClient);
   }

   @Override
   public boolean canBeHit(Attacker attacker) {
      return this.getFlyingHeight() < 50 && super.canBeHit(attacker);
   }

   public void teleportToMasterIfIdle() {
      if (this.isIdle()) {
         TheVoidMob master = this.master.get(this.getLevel());
         if (master != null) {
            Point offset = this.getClawBasePositionOffset();
            this.setPos(master.x + offset.x, master.y + offset.y, true);
            this.sendMovementPacket(true);
            this.dx = 0.0F;
            this.dy = 0.0F;
         }
      }
   }

   @Override
   public boolean canPushMob(Mob other) {
      return false;
   }

   @Override
   public boolean canBePushed(Mob other) {
      return false;
   }

   @Override
   public void setHealthHidden(int health, float knockbackX, float knockbackY, Attacker attacker, boolean fromNetworkUpdate) {
      if (this.master != null) {
         this.master.computeIfPresent(this.getLevel(), m -> m.setHealthHidden(health, knockbackX, knockbackY, attacker, fromNetworkUpdate));
      }

      super.setHealthHidden(health, knockbackX, knockbackY, attacker, fromNetworkUpdate);
   }

   @Override
   public boolean isHealthBarVisible() {
      return false;
   }

   @Override
   public float getIncomingDamageModifier() {
      TheVoidMob master = this.master.get(this.getLevel());
      return master == null ? super.getIncomingDamageModifier() : master.getIncomingDamageModifier();
   }

   @Override
   public GameDamage getCollisionDamage(Mob target, boolean fromPacket, ServerClient packetSubmitter) {
      if (fromPacket) {
         return TheVoidMob.clawCollisionDamage;
      }

      if (this.slamAnimationStartTime != 0L) {
         long timeSinceStart = this.getTime() - this.slamAnimationStartTime;
         if (timeSinceStart >= this.slamAnimationDuration && timeSinceStart <= this.slamAnimationDuration + this.slamAnimationWaitTime) {
            return TheVoidMob.clawCollisionDamage;
         }
      }

      return super.getCollisionDamage(target, fromPacket, packetSubmitter);
   }

   @Override
   public int getFlyingHeight() {
      return (int)this.getCurrentHeight();
   }

   @Override
   public int getCollisionKnockback(Mob target) {
      return 150;
   }

   @Override
   public Rectangle getSelectBox(int x, int y) {
      Rectangle selectBox = super.getSelectBox(x, y);
      selectBox.y = selectBox.y - this.getFlyingHeight();
      return selectBox;
   }

   @Override
   protected void addDrawables(
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
      int drawX = camera.getDrawX(x) - 144 + (this.leftHanded ? -10 : 10);
      int drawY = camera.getDrawY(y) - 144 - 35;
      drawY -= this.getFlyingHeight();
      float rotate = GameMath.limit(this.dx / 10.0F, -10.0F, 10.0F);
      int spriteY = 0;
      if (this.slamAnimationStartTime != 0L) {
         spriteY = 2;
      } else if (this.beamStartTime != 0L) {
         spriteY = 3;
      }

      TheVoidMob master = this.master.get(this.getLevel());
      float alpha = master != null ? master.getFadeAlpha() : 1.0F;
      final float whiteness = master != null ? master.getFadeWhiteness() : 0.0F;
      alpha = Math.max(alpha, whiteness);
      final DrawOptions clawOptions = MobRegistry.Textures.theVoidClaw
         .initDraw()
         .sprite(0, spriteY, 288, 288)
         .startGlowOptions(level, this.getID())
         .applyEnemyTracker(this, perspective)
         .mirror(this.leftHanded, false)
         .rotate(rotate)
         .alpha(alpha)
         .pos(drawX, drawY);
      list.add(new MobDrawable() {
         @Override
         public void draw(TickManager tickManager) {
            try {
               GameResources.sceneShader.use();
               GameResources.sceneShader.pass1f("whiteness", whiteness);
               clawOptions.draw();
            } finally {
               GameResources.sceneShader.pass1f("whiteness", 0.0F);
               GameResources.sceneShader.stop();
            }
         }
      });
      this.addShadowDrawables(tileList, level, x, y, light, camera);
   }

   @Override
   protected TextureDrawOptions getShadowDrawOptions(Level level, int x, int y, GameLight light, GameCamera camera) {
      GameTexture shadowTexture = MobRegistry.Textures.human_big_shadow;
      int res = shadowTexture.getHeight();
      int drawX = camera.getDrawX(x) - res / 2;
      int drawY = camera.getDrawY(y) - res / 2;
      drawY += this.getBobbing(x, y);
      return shadowTexture.initDraw().sprite(this.getDir(), 0, res).light(light).pos(drawX, drawY);
   }

   @Override
   public Mob getAttackOwner() {
      Mob master = this.master.get(this.getLevel());
      return master != null ? master : super.getAttackOwner();
   }

   @Override
   public GameMessage getAttackerName() {
      Mob master = this.master.get(this.getLevel());
      return master != null ? master.getAttackerName() : super.getAttackerName();
   }

   @Override
   public DeathMessageTable getDeathMessages() {
      Mob master = this.master.get(this.getLevel());
      return master != null ? master.getDeathMessages() : super.getDeathMessages();
   }

   public class StartBeamAbility extends MobAbility {
      public void runAndSend(int attackSeed, float startAngleOffset, float endAngleOffset, int duration, boolean reversed) {
         Packet content = new Packet();
         PacketWriter writer = new PacketWriter(content);
         writer.putNextInt(attackSeed);
         writer.putNextFloat(startAngleOffset);
         writer.putNextFloat(endAngleOffset);
         writer.putNextInt(duration);
         writer.putNextBoolean(reversed);
         this.runAndSendAbility(content);
      }

      @Override
      public void executePacket(PacketReader reader) {
         TheVoidClawMob.this.beamStartTime = TheVoidClawMob.this.getTime();
         int attackSeed = reader.getNextInt();
         float startAngleOffset = reader.getNextFloat();
         float endAngleOffset = reader.getNextFloat();
         TheVoidClawMob.this.beamDuration = reader.getNextInt();
         boolean reversed = reader.getNextBoolean();
         float startAngle = 90.0F - (reversed ? -startAngleOffset : startAngleOffset);
         float endAngle = 90.0F + (reversed ? -endAngleOffset : endAngleOffset);
         TheVoidClawBeamLevelEvent event = new TheVoidClawBeamLevelEvent(
            TheVoidClawMob.this,
            startAngle,
            endAngle,
            TheVoidClawMob.this.beamStartTime,
            TheVoidClawMob.this.beamDuration,
            attackSeed,
            1500.0F,
            TheVoidMob.clawBeamDamage,
            100,
            1000,
            0
         );
         TheVoidClawMob.this.getLevel().entityManager.events.addHidden(event);
         if (!TheVoidClawMob.this.isClient()) {
            TheVoidMob master = TheVoidClawMob.this.master.get(TheVoidClawMob.this.getLevel());
            if (master != null) {
               master.spawnedEvents.add(event);
            }
         }

         TheVoidClawMob.this.stopMoving();
      }
   }

   public class StartSlamAbility extends MobAbility {
      public void runAndSend(int duration, int waitTime, int riseTime, boolean isSlamClenched) {
         Packet content = new Packet();
         PacketWriter writer = new PacketWriter(content);
         writer.putNextInt(duration);
         writer.putNextInt(waitTime);
         writer.putNextInt(riseTime);
         writer.putNextBoolean(isSlamClenched);
         this.runAndSendAbility(content);
      }

      @Override
      public void executePacket(PacketReader reader) {
         TheVoidClawMob.this.collisionHitCooldowns.resetCooldowns();
         TheVoidClawMob.this.slamAnimationStartTime = TheVoidClawMob.this.getTime();
         TheVoidClawMob.this.slamAnimationDuration = reader.getNextInt();
         TheVoidClawMob.this.slamAnimationWaitTime = reader.getNextInt();
         TheVoidClawMob.this.slamRiseTime = reader.getNextInt();
         TheVoidClawMob.this.isSlamClenched = reader.getNextBoolean();
         TheVoidClawMob.this.slamEventTriggered = false;
      }
   }
}
