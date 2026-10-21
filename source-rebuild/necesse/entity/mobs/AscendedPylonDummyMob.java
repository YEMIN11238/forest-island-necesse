package necesse.entity.mobs;

import java.awt.Color;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Point2D.Float;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import necesse.engine.eventStatusBars.EventStatusBarManager;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.gameLoop.tickManager.TicksPerSecond;
import necesse.engine.modifiers.ModifierValue;
import necesse.engine.registries.MobRegistry;
import necesse.engine.sound.SoundEffect;
import necesse.engine.sound.SoundManager;
import necesse.engine.sound.SoundSettings;
import necesse.engine.util.GameBlackboard;
import necesse.engine.util.GameMath;
import necesse.engine.util.GameRandom;
import necesse.engine.util.GameUtils;
import necesse.entity.Entity;
import necesse.entity.ParticleTypeSwitcher;
import necesse.entity.levelEvent.AscendedBatJailLevelEvent;
import necesse.entity.levelEvent.WaitForSecondsEvent;
import necesse.entity.levelEvent.mobAbilityLevelEvent.AscendedLightningLevelEvent;
import necesse.entity.levelEvent.mobAbilityLevelEvent.AscendedPylonChargeUpAttackLevelEvent;
import necesse.entity.levelEvent.mobAbilityLevelEvent.AscendedSlimeQuakeEvent;
import necesse.entity.levelEvent.mobAbilityLevelEvent.AscendedSlimeQuakeWarningEvent;
import necesse.entity.manager.EntityManager;
import necesse.entity.mobs.ability.EmptyMobAbility;
import necesse.entity.mobs.ability.IntMobAbility;
import necesse.entity.mobs.buffs.BuffModifiers;
import necesse.entity.mobs.hostile.HostileMob;
import necesse.entity.mobs.hostile.PossessedSettlerMob;
import necesse.entity.mobs.hostile.bosses.ascendedWizard.AscendedBeamMob;
import necesse.entity.mobs.hostile.bosses.ascendedWizard.AscendedGauntletMob;
import necesse.entity.mobs.hostile.bosses.ascendedWizard.ai.AscendedSunlightChampionStage;
import necesse.entity.mobs.mobMovement.MobMovementSpiral;
import necesse.entity.mobs.mobMovement.MobMovementSpiralLevelPos;
import necesse.entity.objectEntity.AscendedPylonObjectEntity;
import necesse.entity.particle.Particle;
import necesse.entity.projectile.AscendedBoltProjectile;
import necesse.entity.projectile.AscendedShardBombProjectile;
import necesse.entity.projectile.AscendedSlashProjectile;
import necesse.entity.projectile.EmpressSlashWarningProjectile;
import necesse.entity.projectile.Projectile;
import necesse.gfx.GameResources;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.DrawOptions;
import necesse.gfx.drawables.OrderableDrawables;
import necesse.gfx.gameTexture.GameTexture;
import necesse.inventory.InventoryItem;
import necesse.level.gameObject.AscendedPylonObject;
import necesse.level.gameObject.GameObject;
import necesse.level.maps.Level;
import necesse.level.maps.biomes.MobChance;
import necesse.level.maps.biomes.MobSpawnTable;
import necesse.level.maps.biomes.incursions.CrystalHollowBiome;
import necesse.level.maps.biomes.incursions.GraveyardBiome;
import necesse.level.maps.biomes.incursions.SlimeCaveBiome;
import necesse.level.maps.biomes.incursions.SpiderCastleBiome;
import necesse.level.maps.biomes.plains.PlainsBiome;
import necesse.level.maps.light.GameLight;

public class AscendedPylonDummyMob extends HostileMob {
   public static int CHARGE_PARTICLE_HEIGHT = 46;
   public static MaxHealthGetter MAX_HEALTH = new MaxHealthGetter(10000, 16000, 20000, 25000, 32000);
   public static GameDamage ASCENDED_BEAM_DAMAGE = new GameDamage(120.0F);
   public static GameDamage MAGIC_VOLLEY_DAMAGE = new GameDamage(120.0F);
   public static GameDamage SLIME_QUAKE_DAMAGE = new GameDamage(120.0F);
   public static GameDamage LIGHTNING_DAMAGE = new GameDamage(120.0F);
   public static GameDamage SHARD_BOMB_DAMAGE = new GameDamage(110.0F);
   public static GameDamage SLASH_DAMAGE = new GameDamage(100.0F);
   public static MobSpawnTable defaultMobSpawnTable = new MobSpawnTable().add(100, "possessedsettler");
   public static MobSpawnTable duskSettlersSpawnTable = new MobSpawnTable().add(100, (level, client, spawnTile) -> {
      PossessedSettlerMob mob = (PossessedSettlerMob)MobRegistry.getMob("possessedsettler", level);
      mob.setArmor("duskhelmet", "duskchestplate", "duskboots", true);
      InventoryItem weapon = GameRandom.globalRandom.getOneOf(() -> {
         InventoryItem item = new InventoryItem("slimestaff");
         item.getGndData().setFloat("damageMod", 0.75F);
         return item;
      }, () -> {
         InventoryItem item = new InventoryItem("phantompopper");
         item.getGndData().setFloat("damageMod", 0.6F);
         return item;
      });
      mob.setWeapon(weapon, true);
      return mob;
   });
   public static MobSpawnTable dawnSettlersSpawnTable = new MobSpawnTable().add(100, (level, client, spawnTile) -> {
      PossessedSettlerMob mob = (PossessedSettlerMob)MobRegistry.getMob("possessedsettler", level);
      mob.setArmor("dawnhelmet", "dawnchestplate", "dawnboots", true);
      InventoryItem weapon = GameRandom.globalRandom.getOneOf(() -> {
         InventoryItem item = new InventoryItem("slimeglaive");
         item.getGndData().setFloat("damageMod", 0.5F);
         return item;
      }, () -> {
         InventoryItem item = new InventoryItem("causticexecutioner");
         item.getGndData().setFloat("damageMod", 1.0F);
         return item;
      });
      mob.setWeapon(weapon, true);
      return mob;
   });
   private int aliveTimer;
   private int tileX;
   private int tileY;
   private AscendedPylonObjectEntity objectEntity;
   public TicksPerSecond spawnTicker = TicksPerSecond.ticksPerSecond(2);
   public int lifeTimeHealthSpawns = 150;
   public int maxHealthSpawnsPerSecond = 20;
   public float nearbyPlayerSpawnsPerSecond = 0.1F;
   public int lastGameTickHealth;
   public int lastSpawnTickHealth;
   public float nextSpawnBuffer;
   public float healthDamageToTriggerInvincibility = 0.35F;
   public int invincibilityRandomMinCooldown = 90000;
   public int invincibilityRandomMaxCooldown = 120000;
   public int invincibilityDuration = 8000;
   protected float nextInvincibilityBuffer;
   public final IntMobAbility triggerInvincibility;
   protected int invincibilityTimeLeft;
   public int nextAttackMinCooldown = 2000;
   public int nextAttackMaxCooldown = 7000;
   protected float nextAttackBuffer;
   protected AscendedPylonDummyMob.AscendedPylonAttack currentAttack;
   protected MobSpawnTable currentSpawnTable = defaultMobSpawnTable;
   protected long currentEffectStartTime;
   protected long currentEffectEndTime;
   protected int currentEffectSpriteY = -1;
   public final EmptyMobAbility triggerElectricExplosionAbility;
   public final EmptyMobAbility playMagicBoltSoundAbility;
   public final EmptyMobAbility playLightningBoltSoundAbility;
   public final IntMobAbility playHoverEffectAbility;

   public AscendedPylonDummyMob() {
      super(20000);
      this.difficultyChanges.setMaxHealth(MAX_HEALTH);
      this.setArmor(40);
      this.setSpeed(0.0F);
      this.setFriction(1000.0F);
      this.setKnockbackModifier(0.0F);
      this.collision = new Rectangle(-10, -7, 20, 14);
      this.hitBox = new Rectangle(-22, -18, 44, 36);
      this.selectBox = new Rectangle(-22, -66, 44, 80);
      this.shouldSave = false;
      this.aliveTimer = 20;
      this.isStatic = true;
      this.setTeam(-2);
      this.triggerInvincibility = this.registerAbility(new IntMobAbility() {
         @Override
         protected void run(int value) {
            AscendedPylonDummyMob.this.invincibilityTimeLeft = AscendedPylonDummyMob.this.invincibilityDuration;
         }
      });
      this.triggerElectricExplosionAbility = this.registerAbility(
         new EmptyMobAbility() {
            @Override
            protected void run() {
               if (!AscendedPylonDummyMob.this.isServer()) {
                  int levelX = AscendedPylonDummyMob.this.getX();
                  int levelY = AscendedPylonDummyMob.this.getY();

                  for (int i = 0; i < 50; i++) {
                     int lifeTime = GameRandom.globalRandom.getIntBetween(500, 5000);
                     float lifePerc = lifeTime / 5000.0F;
                     float endHeight = AscendedPylonDummyMob.CHARGE_PARTICLE_HEIGHT + GameRandom.globalRandom.getIntBetween(50, 150) * lifePerc;
                     AscendedPylonDummyMob.this.getLevel()
                        .entityManager
                        .addParticle(
                           levelX + GameRandom.globalRandom.getFloatBetween(-10.0F, 10.0F),
                           levelY + GameRandom.globalRandom.getFloatBetween(-10.0F, 10.0F),
                           Particle.GType.IMPORTANT_COSMETIC
                        )
                        .sizeFades(20, 30)
                        .movesFriction(GameRandom.globalRandom.getFloatBetween(-100.0F, 100.0F), GameRandom.globalRandom.getFloatBetween(-100.0F, 100.0F), 1.0F)
                        .heightMoves(AscendedPylonDummyMob.CHARGE_PARTICLE_HEIGHT, endHeight)
                        .colorRandom(300.0F, 0.8F, 0.5F, 5.0F, 0.1F, 0.1F)
                        .lifeTime(lifeTime);
                  }

                  SoundManager.playSound(GameResources.electricExplosion, SoundEffect.effect(AscendedPylonDummyMob.this).volume(1.5F).falloffDistance(2000));
               }
            }
         }
      );
      this.playMagicBoltSoundAbility = this.registerAbility(new EmptyMobAbility() {
         @Override
         protected void run() {
            SoundManager.playSound(GameResources.laserBlast1, SoundEffect.effect(AscendedPylonDummyMob.this).volume(0.3F).falloffDistance(2000));
         }
      });
      this.playLightningBoltSoundAbility = this.registerAbility(new EmptyMobAbility() {
         @Override
         protected void run() {
            SoundManager.playSound(GameResources.zap1, SoundEffect.effect(AscendedPylonDummyMob.this).volume(0.3F).falloffDistance(2000));
         }
      });
      this.playHoverEffectAbility = this.registerAbility(new IntMobAbility() {
         @Override
         protected void run(int value) {
            if (value == -1) {
               AscendedPylonDummyMob.this.currentEffectEndTime = AscendedPylonDummyMob.this.getLocalTime();
            } else {
               AscendedPylonDummyMob.this.currentEffectStartTime = AscendedPylonDummyMob.this.getLocalTime();
               AscendedPylonDummyMob.this.currentEffectSpriteY = value;
               AscendedPylonDummyMob.this.currentEffectEndTime = 0L;
            }
         }
      });
   }

   @Override
   public void init() {
      super.init();
      this.nextInvincibilityBuffer = GameRandom.globalRandom.getIntBetween(this.invincibilityRandomMinCooldown, this.invincibilityRandomMaxCooldown);
   }

   @Override
   public void tickMovement(float delta) {
      super.tickMovement(delta);
      if (this.currentAttack != null) {
         this.currentAttack.movementTick(delta);
      }
   }

   @Override
   public void clientTick() {
      super.clientTick();
      this.tickAlive();
      this.tickInvincibilityTimer();
      if (this.isInCombat()) {
         EventStatusBarManager.registerMobHealthStatusBar(this);
      }
   }

   @Override
   public void serverTick() {
      super.serverTick();
      this.tickAlive();
      this.tickInvincibilityTimer();
      this.spawnTicker.gameTick();
      if (this.spawnTicker.shouldTick()) {
         float ticksPerSecond = this.spawnTicker.getTicksPerSecond();
         boolean hasPlayers = this.getLevel().entityManager.players.streamAreaTileRange(this.getX(), this.getY(), 25).anyMatch(p -> !p.removed());
         if (hasPlayers) {
            this.nextSpawnBuffer = this.nextSpawnBuffer + this.nearbyPlayerSpawnsPerSecond / ticksPerSecond;
         }

         int nextHealth = this.getHealth();
         int lostHealth = this.lastSpawnTickHealth - nextHealth;
         this.lastSpawnTickHealth = nextHealth;
         if (lostHealth > 0) {
            float modifier = (float)this.getMaxHealth() / this.getMaxHealthFlat();
            float lifePerProjectile = (float)this.getMaxHealthFlat() / this.lifeTimeHealthSpawns * modifier;
            float increase = lostHealth / lifePerProjectile;
            this.nextSpawnBuffer = this.nextSpawnBuffer + Math.min(this.maxHealthSpawnsPerSecond / ticksPerSecond, increase);
         }

         while (this.nextSpawnBuffer > 1.0F) {
            this.nextSpawnBuffer--;
            if (this.currentSpawnTable != null && !this.spawnMob(this.currentSpawnTable, 1)) {
               this.nextSpawnBuffer += 0.5F;
            }
         }
      }

      int nextHealth = this.getHealth();
      int lostHealth = this.lastGameTickHealth - nextHealth;
      this.lastGameTickHealth = nextHealth;
      if (this.invincibilityTimeLeft <= 0) {
         if (lostHealth > 0) {
            float modifier = (float)this.getMaxHealth() / this.getMaxHealthFlat();
            float lifePerFullInvincibilityCooldown = this.getMaxHealthFlat() * this.healthDamageToTriggerInvincibility * modifier;
            float cooldownPercentIncrease = lostHealth / lifePerFullInvincibilityCooldown;
            float cooldownTimeDecrease = this.invincibilityRandomMaxCooldown * cooldownPercentIncrease;
            this.nextInvincibilityBuffer -= cooldownTimeDecrease;
         }

         this.nextInvincibilityBuffer -= 50.0F;
         if (this.nextInvincibilityBuffer <= 0.0F) {
            this.triggerInvincibility.runAndSend(this.invincibilityDuration);
            this.nextInvincibilityBuffer = GameRandom.globalRandom.getIntBetween(this.invincibilityRandomMinCooldown, this.invincibilityRandomMaxCooldown);
         }
      }

      if (this.currentAttack != null) {
         this.currentAttack.serverTick();
         if (this.currentAttack.isDone()) {
            this.currentAttack.onDone();
            this.currentAttack = null;
            this.playHoverEffectAbility.runAndSend(-1);
         }
      } else if (this.objectEntity != null) {
         float nextAttackCooldown = GameMath.lerp(this.getHealthPercent(), this.nextAttackMinCooldown, this.nextAttackMaxCooldown);
         float increasePerTick = 1.0F / (nextAttackCooldown / 50.0F);
         this.nextAttackBuffer += increasePerTick;
         if (this.nextAttackBuffer >= 1.0F) {
            this.nextAttackBuffer--;
            int playerRange = 800;
            boolean hasPlayer = this.isInCombat()
               || this.getLevel()
                  .entityManager
                  .players
                  .streamArea(this.getX(), this.getY(), playerRange)
                  .filter(p -> GameMath.diagonalMoveDistance(p.getX(), p.getY(), this.getX(), this.getY()) < playerRange)
                  .findFirst()
                  .isPresent();
            if (hasPlayer) {
               AscendedPylonDummyMob.AscendedPylonAttack nextAttack = this.objectEntity.getNextAttack(this);
               if (nextAttack != null) {
                  this.currentAttack = nextAttack;
                  this.currentAttack.onStarted();
                  MobSpawnTable mobSpawnTable = this.currentAttack.getMobSpawnTable();
                  if (mobSpawnTable != null) {
                     this.currentSpawnTable = mobSpawnTable;
                     int spawnedMobsCount = this.currentAttack.getSpawnedMobsCount();

                     for (int i = 0; i < spawnedMobsCount; i++) {
                        this.spawnMob(mobSpawnTable, 20);
                     }
                  } else {
                     this.currentSpawnTable = defaultMobSpawnTable;
                  }

                  this.playHoverEffectAbility.runAndSend(this.currentAttack.effectSpriteY);
               } else {
                  this.nextAttackBuffer = 0.5F;
               }
            }
         }
      }
   }

   public PlayerMob getClosestPlayer(int range) {
      return this.getLevel()
         .entityManager
         .players
         .streamArea(this.getX(), this.getY(), range)
         .filter(p -> GameMath.diagonalMoveDistance(p.getX(), p.getY(), this.getX(), this.getY()) < range)
         .findFirst()
         .orElse(null);
   }

   public boolean spawnMob(MobSpawnTable spawnTable, int attempts) {
      int players = this.getLevel().entityManager.countPlayers(this.getX(), this.getY(), Mob.MOB_SPAWN_AREA);
      float maxNearbyMobs = EntityManager.getSpawnCap(players, 20.0F, -10.0F)
         * this.getWorldSettings().difficulty.enemySpawnCapModifier
         * this.getLevel().entityManager.getSpawnCapMod(this.tileX, this.tileY)
         * 3.0F;
      return this.spawnMob(spawnTable, attempts, maxNearbyMobs);
   }

   public boolean spawnMob(MobSpawnTable spawnTable, int attempts, float maxNearbyMobs) {
      if (maxNearbyMobs > 0.0F) {
         int totalMobs = this.getLevel().entityManager.countMobs(this.getX(), this.getY(), Mob.MOB_SPAWN_AREA, m -> m.isHostile);
         if (totalMobs >= maxNearbyMobs) {
            return false;
         }
      }

      for (int i = 0; i < attempts; i++) {
         Point spawnTile = EntityManager.getMobSpawnTile(this.getLevel(), this.getX(), this.getY(), Mob.MOB_SPAWN_AREA, null);
         if (spawnTile != null) {
            MobChance randomMob = spawnTable.getRandomMob(this.getLevel(), null, spawnTile, GameRandom.globalRandom, "ascendedpylon");
            if (randomMob != null) {
               Collection<Mob> spawnedMobs = randomMob.spawnMob(this.getLevel(), null, spawnTile, m -> {
                  m.spawnLightThreshold = new ModifierValue<>(BuffModifiers.MOB_SPAWN_LIGHT_THRESHOLD, 0).min(150, Integer.MAX_VALUE);
                  return true;
               }, null, "ascendedpylon");
               if (spawnedMobs != null && !spawnedMobs.isEmpty()) {
                  return true;
               }
            }
         }
      }

      return false;
   }

   @Override
   public boolean canHitThroughCollision() {
      return true;
   }

   @Override
   public boolean canBeTargetedFromAdjacentTiles() {
      return true;
   }

   protected void tickInvincibilityTimer() {
      if (this.invincibilityTimeLeft > 0) {
         this.invincibilityTimeLeft -= 50;
         if (this.invincibilityTimeLeft <= 0) {
            this.invincibilityTimeLeft = 0;
            if (!this.isServer()) {
               int particleCount = 25;
               ParticleTypeSwitcher typeSwitcher = new ParticleTypeSwitcher(Particle.GType.CRITICAL, Particle.GType.IMPORTANT_COSMETIC, Particle.GType.COSMETIC);
               float anglePerParticle = 360.0F / particleCount;

               for (int i = 0; i < particleCount; i++) {
                  int angle = (int)(i * anglePerParticle + GameRandom.globalRandom.nextFloat() * anglePerParticle);
                  float dx = (float)Math.sin(Math.toRadians(angle)) * 50.0F;
                  float dy = (float)Math.cos(Math.toRadians(angle)) * 50.0F;
                  this.getLevel()
                     .entityManager
                     .addParticle(this.x, this.y, typeSwitcher.next())
                     .sprite(GameResources.magicSparkParticles.sprite(GameRandom.globalRandom.nextInt(4), 0, 22))
                     .sizeFades(22, 44)
                     .movesFriction(dx * 2.0F, dy * 2.0F, 0.8F)
                     .color(new Color(255, 34, 226))
                     .givesLight(300.0F, 0.3F)
                     .heightMoves(0.0F, 30.0F)
                     .lifeTime(1500);
               }

               SoundManager.playSound(GameResources.shatter1, SoundEffect.effect(this).volume(0.1F).falloffDistance(2000));
               SoundManager.playSound(GameResources.fadedeath3, SoundEffect.effect(this).volume(1.2F).falloffDistance(2000));
            }
         }
      }
   }

   private void tickAlive() {
      this.aliveTimer--;
      if (this.aliveTimer <= 0) {
         this.remove();
      }
   }

   public void keepAlive(AscendedPylonObjectEntity entity) {
      this.tileX = entity.tileX;
      this.tileY = entity.tileY;
      this.aliveTimer = 20;
      this.objectEntity = entity;
      this.setPos(this.tileX * 32 + 16, this.tileY * 32 + 16, true);
   }

   @Override
   public boolean canBePushed(Mob other) {
      return false;
   }

   @Override
   public boolean canPushMob(Mob other) {
      return false;
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
   }

   @Override
   public boolean isHealthBarVisible() {
      return false;
   }

   public DrawOptions getEffectDrawOptions(GameTexture effectsTexture, int tileX, int tileY, GameLight light, GameCamera camera) {
      if (this.currentEffectSpriteY == -1) {
         return null;
      }

      long timeSinceStart = this.getLocalTime() - this.currentEffectStartTime;
      long timeSinceEnd = this.getLocalTime() - this.currentEffectEndTime;
      float alpha = 1.0F;
      if (timeSinceStart < 1000L) {
         alpha *= Math.max(0.0F, (float)timeSinceStart / 1000.0F);
      }

      if (this.currentEffectEndTime > 0L) {
         if (timeSinceEnd >= 1000L) {
            this.currentEffectSpriteY = -1;
            return null;
         }

         alpha *= Math.max(0.0F, 1.0F - (float)timeSinceEnd / 1000.0F);
      }

      float animFloat = GameUtils.getAnimFloatContinuous(this.getLocalTime(), 4000);
      alpha *= GameMath.lerp(animFloat, 0.2F, 0.4F);
      int drawX = camera.getTileDrawX(tileX);
      int drawY = camera.getTileDrawY(tileY);
      return effectsTexture.initDraw().sprite(0, this.currentEffectSpriteY, 96, 64).light(light).alpha(alpha).pos(drawX + 16 - 48, drawY - 120);
   }

   @Override
   public boolean canTakeDamage() {
      return this.invincibilityTimeLeft <= 0;
   }

   @Override
   public boolean countDamageDealt() {
      return true;
   }

   @Override
   public boolean onMouseHover(GameCamera camera, PlayerMob perspective, GameBlackboard blackboard, boolean debug) {
      return !debug ? false : super.onMouseHover(camera, perspective, blackboard, debug);
   }

   @Override
   protected void onDeath(Attacker attacker, HashSet<Attacker> attackers) {
      super.onDeath(attacker, attackers);
      if (this.isServer()) {
         GameObject object = this.getLevel().getObject(this.tileX, this.tileY);
         if (object instanceof AscendedPylonObject) {
            this.getLevel().entityManager.destroyObjectOverride(0, this.tileX, this.tileY);
         }
      }
   }

   @Override
   public void spawnDeathParticles(float knockbackX, float knockbackY) {
      super.spawnDeathParticles(knockbackX, knockbackY);
      if (this.isClient()) {
         this.getClient().addShockwaveEffect(this.getX(), this.getY() - CHARGE_PARTICLE_HEIGHT, 2000.0F, 50.0F, 500.0F, 100.0F, 150);
      }
   }

   @Override
   protected SoundSettings getDeathSound() {
      return new SoundSettings(GameResources.ascendedPylonDeath).volume(1.8F).fallOffDistance(5000);
   }

   public abstract static class AscendedPylonAttack {
      public final AscendedPylonDummyMob mob;
      public final int effectSpriteY;

      public AscendedPylonAttack(AscendedPylonDummyMob mob, int effectSpriteY) {
         this.mob = mob;
         this.effectSpriteY = effectSpriteY;
      }

      public abstract void onStarted();

      public void movementTick(float delta) {
      }

      public abstract void serverTick();

      public abstract boolean isDone();

      public void onDone() {
      }

      public abstract MobSpawnTable getMobSpawnTable();

      public int getSpawnedMobsCount() {
         return GameMath.lerp(this.mob.getHealthPercent(), 5, 2);
      }
   }

   public static class AscendedShardBombsPylonAttack extends AscendedPylonDummyMob.ChargeEventAscendedPylonAttack {
      public float minBombsPerSec = 4.0F;
      public float maxBombsPerSec = 6.0F;
      public int minBombs = 20;
      public int maxBombs = 30;
      public float nextFireBuffer;
      public int totalFired;
      public boolean firing;

      public AscendedShardBombsPylonAttack(AscendedPylonDummyMob mob) {
         super(mob, 0, 0);
      }

      @Override
      public void onChargedEventReady() {
         this.firing = true;
      }

      @Override
      public void movementTick(float delta) {
         super.movementTick(delta);
         if (this.firing) {
            GameRandom random = GameRandom.globalRandom;
            float projectilesPerSec = GameMath.lerp(this.mob.getHealthPercent(), this.maxBombsPerSec, this.minBombsPerSec);
            float projectilesPerTick = projectilesPerSec * delta / 1000.0F;
            this.nextFireBuffer += projectilesPerTick;

            while (this.nextFireBuffer >= 1.0F) {
               this.nextFireBuffer--;
               this.totalFired++;
               float randomAngle = random.getFloatBetween(0.0F, 360.0F);
               Float dir = GameMath.getAngleDir(randomAngle);
               float randomDistance = random.getFloatBetween(300.0F, 1200.0F);
               int targetX = (int)(this.mob.getX() + dir.x * randomDistance);
               int targetY = (int)(this.mob.getY() + dir.y * randomDistance);
               AscendedShardBombProjectile projectile = new AscendedShardBombProjectile(
                  this.mob.x, this.mob.y, targetX, targetY, 100, (int)randomDistance, AscendedPylonDummyMob.SHARD_BOMB_DAMAGE, this.mob
               );
               this.mob.getLevel().entityManager.projectiles.add(projectile);
               this.mob.playLightningBoltSoundAbility.runAndSend();
            }
         }
      }

      @Override
      public boolean isDone() {
         float totalProjectiles = GameMath.lerp(this.mob.getHealthPercent(), this.maxBombs, this.minBombs);
         return this.totalFired >= totalProjectiles;
      }

      @Override
      public MobSpawnTable getMobSpawnTable() {
         return CrystalHollowBiome.mobs;
      }
   }

   public static class AscendedSlashesPylonAttack extends AscendedPylonDummyMob.ChargeEventAscendedPylonAttack {
      public float minSlashesPerSec = 8.0F;
      public float maxSlashesPerSec = 12.0F;
      public int minSlashes = 40;
      public int maxSlashes = 60;
      public float nextFireBuffer;
      public int totalFired;
      public boolean firing;

      public AscendedSlashesPylonAttack(AscendedPylonDummyMob mob) {
         super(mob, 0, 5);
      }

      @Override
      public void onChargedEventReady() {
         this.firing = true;
      }

      @Override
      public void movementTick(float delta) {
         super.movementTick(delta);
         if (this.firing) {
            GameRandom random = GameRandom.globalRandom;
            float projectilesPerSec = GameMath.lerp(this.mob.getHealthPercent(), this.maxSlashesPerSec, this.minSlashesPerSec);
            float projectilesPerTick = projectilesPerSec * delta / 1000.0F;
            this.nextFireBuffer += projectilesPerTick;

            while (this.nextFireBuffer >= 1.0F) {
               this.nextFireBuffer--;
               this.totalFired++;
               final int startX = this.mob.getX() + random.getIntBetween(-800, 800);
               final int startY = this.mob.getY() + random.getIntBetween(-800, 800);
               final int randomX = random.getIntBetween(-200, 200);
               final int randomY = random.getIntBetween(-200, 200);
               final float angle = random.getIntBetween(-180, 180);
               EmpressSlashWarningProjectile slashProjectile = new EmpressSlashWarningProjectile(
                  startX + randomX, startY + randomY, angle, AscendedPylonDummyMob.SLASH_DAMAGE, this.mob
               );
               this.mob.getLevel().entityManager.projectiles.add(slashProjectile);
               EmpressSlashWarningProjectile slashProjectileReverse = new EmpressSlashWarningProjectile(
                  startX + randomX, startY + randomY, angle - 180.0F, AscendedPylonDummyMob.SLASH_DAMAGE, this.mob
               );
               this.mob.getLevel().entityManager.projectiles.add(slashProjectileReverse);
               this.mob
                  .getLevel()
                  .entityManager
                  .events
                  .addHidden(
                     new WaitForSecondsEvent(0.5F) {
                        @Override
                        public void onWaitOver() {
                           AscendedSlashProjectile slashProjectilex = new AscendedSlashProjectile(
                              startX + randomX, startY + randomY, angle, AscendedPylonDummyMob.SLASH_DAMAGE, AscendedSlashesPylonAttack.this.mob
                           );
                           AscendedSlashesPylonAttack.this.mob.getLevel().entityManager.projectiles.add(slashProjectilex);
                           AscendedSlashProjectile slashProjectileReversex = new AscendedSlashProjectile(
                              startX + randomX, startY + randomY, angle - 180.0F, AscendedPylonDummyMob.SLASH_DAMAGE, AscendedSlashesPylonAttack.this.mob
                           );
                           AscendedSlashesPylonAttack.this.mob.getLevel().entityManager.projectiles.add(slashProjectileReversex);
                        }
                     }
                  );
               if (this.totalFired % 2 == 1) {
                  this.mob.playLightningBoltSoundAbility.runAndSend();
               }
            }
         }
      }

      @Override
      public boolean isDone() {
         float totalProjectiles = GameMath.lerp(this.mob.getHealthPercent(), this.maxSlashes, this.minSlashes);
         return this.totalFired >= totalProjectiles;
      }

      @Override
      public MobSpawnTable getMobSpawnTable() {
         return SpiderCastleBiome.mobs;
      }
   }

   public static class BatJailPylonAttack extends AscendedPylonDummyMob.ChargeEventAscendedPylonAttack {
      public AscendedBatJailLevelEvent event;

      public BatJailPylonAttack(AscendedPylonDummyMob mob) {
         super(mob, 0, 2);
      }

      @Override
      public void onChargedEventReady() {
         this.event = new AscendedBatJailLevelEvent(this.mob, this.mob.x, this.mob.y);
         this.mob.getLevel().entityManager.events.addHidden(this.event);
      }

      @Override
      public boolean isDone() {
         return this.event != null && this.event.isOver();
      }

      @Override
      public MobSpawnTable getMobSpawnTable() {
         return GraveyardBiome.mobs;
      }
   }

   public static class BeamTornadoPylonAttack extends AscendedPylonDummyMob.ChargeEventAscendedPylonAttack {
      public float minBeamsPerSec = 3.0F;
      public float maxBeamsPerSec = 5.0F;
      public int minBeams = 15;
      public int maxBeams = 25;
      public float nextFireBuffer;
      public int totalFired;
      public boolean firing;

      public BeamTornadoPylonAttack(AscendedPylonDummyMob mob) {
         super(mob, 0, -1);
      }

      @Override
      public void onChargedEventReady() {
         this.firing = true;
      }

      @Override
      public void movementTick(float delta) {
         super.movementTick(delta);
         if (this.firing) {
            GameRandom random = GameRandom.globalRandom;
            float projectilesPerSec = GameMath.lerp(this.mob.getHealthPercent(), this.maxBeamsPerSec, this.minBeamsPerSec);
            float projectilesPerTick = projectilesPerSec * delta / 1000.0F;
            this.nextFireBuffer += projectilesPerTick;

            while (this.nextFireBuffer >= 1.0F) {
               this.nextFireBuffer--;
               this.totalFired++;
               Mob beam = MobRegistry.getMob("ascendedbeam", this.mob.getLevel());
               ((AscendedBeamMob)beam).damage = AscendedPylonDummyMob.ASCENDED_BEAM_DAMAGE;
               ((AscendedBeamMob)beam).master.uniqueID = this.mob.getUniqueID();
               beam.setPos(this.mob.x, this.mob.y, true);
               int distanceAtTheEnd = 4000;
               int radiusDecrease = random.getIntBetween(100, 300);
               int semiCircles = MobMovementSpiral.getSemiCircles(distanceAtTheEnd, radiusDecrease, 10.0F);
               int startAngle = random.nextInt(360);
               boolean clockwise = random.nextBoolean();
               beam.setMovement(
                  new MobMovementSpiralLevelPos(beam, this.mob.x, this.mob.y, 10.0F, semiCircles, -radiusDecrease, beam.getSpeed(), startAngle, clockwise)
               );
               this.mob.getLevel().entityManager.mobs.add(beam);
            }
         }
      }

      @Override
      public boolean isDone() {
         float totalProjectiles = GameMath.lerp(this.mob.getHealthPercent(), this.maxBeams, this.minBeams);
         return this.totalFired >= totalProjectiles;
      }

      @Override
      public MobSpawnTable getMobSpawnTable() {
         return PlainsBiome.deepCaveMobs;
      }
   }

   public abstract static class ChargeEventAscendedPylonAttack extends AscendedPylonDummyMob.AscendedPylonAttack {
      public final int triggerAtTimeLeft;
      protected AscendedPylonChargeUpAttackLevelEvent chargeEvent;
      protected boolean spawnedAttackEvent;
      public float healthDamageToTriggerChargeEvent = 0.1F;
      public int chargeEventMinChargeTime = 2000;
      public int chargeEventMaxChargeTime = 6000;
      public int lastGameTickHealth;

      public ChargeEventAscendedPylonAttack(AscendedPylonDummyMob mob, int triggerAtTimeLeft, int effectSpriteY) {
         super(mob, effectSpriteY);
         this.triggerAtTimeLeft = triggerAtTimeLeft;
      }

      @Override
      public void onStarted() {
         this.spawnedAttackEvent = false;
         int changeTime = GameMath.lerp(this.mob.getHealthPercent(), this.chargeEventMaxChargeTime, this.chargeEventMinChargeTime);
         this.mob
            .getLevel()
            .entityManager
            .events
            .add(this.chargeEvent = new AscendedPylonChargeUpAttackLevelEvent(this.mob, GameRandom.globalRandom.nextInt(), changeTime));
         this.lastGameTickHealth = this.mob.getHealth();
      }

      @Override
      public void serverTick() {
         if (this.chargeEvent == null) {
            if (!this.spawnedAttackEvent) {
               this.spawnedAttackEvent = true;
               this.onChargedEventReady();
            }
         } else {
            int nextHealth = this.mob.getHealth();
            int lostHealth = this.lastGameTickHealth - nextHealth;
            this.lastGameTickHealth = nextHealth;
            float timeLeft = this.chargeEvent.getEventTimeLeft();
            if (lostHealth > 0 && timeLeft > this.triggerAtTimeLeft) {
               float modifier = (float)this.mob.getMaxHealth() / this.mob.getMaxHealthFlat();
               float lifePerFullChargeEvent = this.mob.getMaxHealthFlat() * this.healthDamageToTriggerChargeEvent * modifier;
               float chargeEventPercentIncrease = lostHealth / lifePerFullChargeEvent;
               float chargeEventTimeIncrease = this.chargeEvent.chargeTime * chargeEventPercentIncrease;
               this.chargeEvent
                  .setCurrentTimeAction
                  .runAndSend(Math.min(this.chargeEvent.currentTime + chargeEventTimeIncrease, this.chargeEvent.chargeTime - this.triggerAtTimeLeft));
            }

            if (!this.spawnedAttackEvent && timeLeft <= this.triggerAtTimeLeft) {
               this.spawnedAttackEvent = true;
               this.onChargedEventReady();
            }
         }
      }

      public abstract void onChargedEventReady();

      @Override
      public void onDone() {
         super.onDone();
         if (this.chargeEvent != null) {
            this.chargeEvent.over();
            this.chargeEvent = null;
            this.mob.triggerElectricExplosionAbility.runAndSend();
         }
      }
   }

   public static class LightningStrikesPylonAttack extends AscendedPylonDummyMob.ChargeEventAscendedPylonAttack {
      public float minStrikesPerSec = 3.0F;
      public float maxStrikesPerSec = 5.0F;
      public int minStrikes = 15;
      public int maxStrikes = 25;
      public float nextFireBuffer;
      public int totalFired;
      public boolean firing;

      public LightningStrikesPylonAttack(AscendedPylonDummyMob mob) {
         super(mob, 0, -1);
      }

      @Override
      public void onChargedEventReady() {
         this.firing = true;
      }

      @Override
      public void movementTick(float delta) {
         super.movementTick(delta);
         if (this.firing) {
            GameRandom random = GameRandom.globalRandom;
            float projectilesPerSec = GameMath.lerp(this.mob.getHealthPercent(), this.maxStrikesPerSec, this.minStrikesPerSec);
            float projectilesPerTick = projectilesPerSec * delta / 1000.0F;
            this.nextFireBuffer += projectilesPerTick;

            while (this.nextFireBuffer >= 1.0F) {
               this.nextFireBuffer--;
               this.totalFired++;
               float randomAngle = random.getFloatBetween(0.0F, 360.0F);
               Float dir = GameMath.getAngleDir(randomAngle);
               float randomDistance = random.getFloatBetween(100.0F, 600.0F);
               int targetX = (int)(this.mob.getX() + dir.x * randomDistance);
               int targetY = (int)(this.mob.getY() + dir.y * randomDistance);
               AscendedLightningLevelEvent event = new AscendedLightningLevelEvent(this.mob, 300, AscendedPylonDummyMob.LIGHTNING_DAMAGE, targetX, targetY);
               this.mob.getLevel().entityManager.events.add(event);
               this.mob.playLightningBoltSoundAbility.runAndSend();
            }
         }
      }

      @Override
      public boolean isDone() {
         float totalProjectiles = GameMath.lerp(this.mob.getHealthPercent(), this.maxStrikes, this.minStrikes);
         return this.totalFired >= totalProjectiles;
      }

      @Override
      public MobSpawnTable getMobSpawnTable() {
         return null;
      }
   }

   public static class MagicVolleyAscendedPylonAttack extends AscendedPylonDummyMob.ChargeEventAscendedPylonAttack {
      public float minProjectilesPerSec = 8.0F;
      public float maxProjectilesPerSec = 12.0F;
      public int minProjectiles = 20;
      public int maxProjectiles = 30;
      public float nextProjectileBuffer;
      public int projectilesFired;
      public boolean firingProjectiles;

      public MagicVolleyAscendedPylonAttack(AscendedPylonDummyMob mob) {
         super(mob, 0, 4);
      }

      @Override
      public void onChargedEventReady() {
         this.firingProjectiles = true;
      }

      @Override
      public void movementTick(float delta) {
         super.movementTick(delta);
         if (this.firingProjectiles) {
            GameRandom random = GameRandom.globalRandom;
            float projectilesPerSec = GameMath.lerp(this.mob.getHealthPercent(), this.maxProjectilesPerSec, this.minProjectilesPerSec);
            float projectilesPerTick = projectilesPerSec * delta / 1000.0F;
            this.nextProjectileBuffer += projectilesPerTick;

            while (this.nextProjectileBuffer >= 1.0F) {
               this.nextProjectileBuffer--;
               this.projectilesFired++;
               PlayerMob player = this.mob.getClosestPlayer(1280);
               float targetX;
               float targetY;
               if (player != null) {
                  targetX = player.getX();
                  targetY = player.getY();
               } else {
                  float angle = random.getFloatBetween(0.0F, 360.0F);
                  Float dir = GameMath.getAngleDir(angle);
                  targetX = this.mob.getX() + dir.x * 150.0F;
                  targetY = this.mob.getY() + dir.y * 150.0F;
               }

               Projectile projectile = new AscendedBoltProjectile(
                  this.mob.getLevel(),
                  this.mob.x,
                  this.mob.y,
                  targetX + random.getIntBetween(-50, 50),
                  targetY + random.getIntBetween(-50, 50),
                  200.0F,
                  1500,
                  AscendedPylonDummyMob.MAGIC_VOLLEY_DAMAGE,
                  this.mob
               );
               this.mob.getLevel().entityManager.projectiles.add(projectile);
               this.mob.playMagicBoltSoundAbility.runAndSend();
            }
         }
      }

      @Override
      public boolean isDone() {
         float totalProjectiles = GameMath.lerp(this.mob.getHealthPercent(), this.maxProjectiles, this.minProjectiles);
         return this.projectilesFired >= totalProjectiles;
      }

      @Override
      public MobSpawnTable getMobSpawnTable() {
         return AscendedPylonDummyMob.duskSettlersSpawnTable;
      }
   }

   public static class SlimeQuakeAscendedPylonAttack extends AscendedPylonDummyMob.ChargeEventAscendedPylonAttack {
      public int minVelocity = 500;
      public int maxVelocity = 800;
      public int minWarningTime = 1000;
      public int maxWarningTime = 2000;

      public SlimeQuakeAscendedPylonAttack(AscendedPylonDummyMob mob) {
         super(mob, 250, 1);
      }

      @Override
      public void onChargedEventReady() {
         final int range = 1000;
         final int velocity = GameMath.lerp(this.mob.getHealthPercent(), this.maxVelocity, this.minVelocity);
         final int warningTime = GameMath.lerp(this.mob.getHealthPercent(), this.minWarningTime, this.maxWarningTime);
         int totalEvents = GameRandom.globalRandom.getIntBetween(8, 12);
         if (totalEvents % 2 == 1) {
            totalEvents++;
         }

         float totalSeconds = GameRandom.globalRandom.getFloatBetween(0.5F, 1.0F);
         float secondsPerEvent = totalSeconds / totalEvents;
         float baseAngleOffset = GameRandom.globalRandom.getFloatBetween(0.0F, 360.0F);
         final float angleExtent = 360.0F / totalEvents;
         ArrayList<Integer> eventOrder = new ArrayList<>(totalEvents);

         for (int i = 0; i < totalEvents; i++) {
            eventOrder.add(i);
         }

         Collections.shuffle(eventOrder, GameRandom.globalRandom);
         int offset = GameRandom.globalRandom.nextInt(135);

         for (int index = 0; index < totalEvents; index++) {
            int currentIndex = eventOrder.get(index);
            final int thisOffset = currentIndex % 2 == 0 ? offset : offset + 200;
            final float thisAngle = currentIndex * angleExtent + baseAngleOffset;
            this.mob
               .getLevel()
               .entityManager
               .events
               .addHidden(
                  new WaitForSecondsEvent(index * secondsPerEvent) {
                     @Override
                     public void onWaitOver() {
                        if (!SlimeQuakeAscendedPylonAttack.this.mob.removed()) {
                           SlimeQuakeAscendedPylonAttack.this.mob
                              .getLevel()
                              .entityManager
                              .events
                              .add(
                                 new AscendedSlimeQuakeWarningEvent(
                                    SlimeQuakeAscendedPylonAttack.this.mob,
                                    SlimeQuakeAscendedPylonAttack.this.mob.getX(),
                                    SlimeQuakeAscendedPylonAttack.this.mob.getY(),
                                    new GameRandom(),
                                    thisAngle,
                                    angleExtent,
                                    velocity,
                                    range,
                                    warningTime,
                                    thisOffset
                                 )
                              );
                           SlimeQuakeAscendedPylonAttack.this.mob
                              .getLevel()
                              .entityManager
                              .events
                              .addHidden(
                                 new WaitForSecondsEvent(warningTime / 1000.0F) {
                                    @Override
                                    public void onWaitOver() {
                                       if (!SlimeQuakeAscendedPylonAttack.this.mob.removed()) {
                                          SlimeQuakeAscendedPylonAttack.this.mob
                                             .getLevel()
                                             .entityManager
                                             .events
                                             .add(
                                                new AscendedSlimeQuakeEvent(
                                                   SlimeQuakeAscendedPylonAttack.this.mob,
                                                   SlimeQuakeAscendedPylonAttack.this.mob.getX(),
                                                   SlimeQuakeAscendedPylonAttack.this.mob.getY(),
                                                   new GameRandom(),
                                                   thisAngle,
                                                   angleExtent,
                                                   AscendedPylonDummyMob.SLIME_QUAKE_DAMAGE,
                                                   velocity,
                                                   50.0F,
                                                   range,
                                                   thisOffset
                                                )
                                             );
                                       }
                                    }
                                 }
                              );
                        }
                     }
                  }
               );
         }
      }

      @Override
      public boolean isDone() {
         return this.chargeEvent == null || this.chargeEvent.getEventPercentProgress() >= 1.0F;
      }

      @Override
      public MobSpawnTable getMobSpawnTable() {
         return SlimeCaveBiome.mobs;
      }
   }

   public static class SpawnGauntletsPylonAttack extends AscendedPylonDummyMob.ChargeEventAscendedPylonAttack {
      public ArrayList<AscendedGauntletMob> gauntletMobs;

      public SpawnGauntletsPylonAttack(AscendedPylonDummyMob mob) {
         super(mob, 0, 3);
      }

      @Override
      public void onChargedEventReady() {
         PlayerMob player = this.mob.getClosestPlayer(1280);
         this.gauntletMobs = AscendedSunlightChampionStage.spawnGauntletPair(this.mob, player);
      }

      @Override
      public boolean isDone() {
         if (this.gauntletMobs != null) {
            this.gauntletMobs.removeIf(Entity::removed);
         }

         return this.gauntletMobs != null && this.gauntletMobs.isEmpty();
      }

      @Override
      public MobSpawnTable getMobSpawnTable() {
         return AscendedPylonDummyMob.dawnSettlersSpawnTable;
      }
   }
}
