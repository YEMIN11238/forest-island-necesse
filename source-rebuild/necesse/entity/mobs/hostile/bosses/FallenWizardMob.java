package necesse.entity.mobs.hostile.bosses;

import java.awt.Color;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Point2D.Float;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Objects;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import necesse.engine.achievements.AchievementManager;
import necesse.engine.eventStatusBars.EventStatusBarManager;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.localization.message.LocalMessage;
import necesse.engine.modifiers.ModifierValue;
import necesse.engine.network.Packet;
import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.engine.network.client.Client;
import necesse.engine.network.packet.PacketChatMessage;
import necesse.engine.network.packet.PacketMobAttack;
import necesse.engine.network.server.ServerClient;
import necesse.engine.postProcessing.PostProcessingEffects;
import necesse.engine.registries.BiomeRegistry;
import necesse.engine.registries.ItemRegistry;
import necesse.engine.registries.MobRegistry;
import necesse.engine.registries.MusicRegistry;
import necesse.engine.sound.SoundEffect;
import necesse.engine.sound.SoundManager;
import necesse.engine.sound.SoundPlayer;
import necesse.engine.sound.SoundSettings;
import necesse.engine.sound.SoundSettingsRegistry;
import necesse.engine.util.GameMath;
import necesse.engine.util.GameRandom;
import necesse.engine.util.PriorityMap;
import necesse.engine.util.gameAreaSearch.GameAreaStream;
import necesse.entity.Entity;
import necesse.entity.levelEvent.FallenWizardRespawnEvent;
import necesse.entity.levelEvent.LevelEvent;
import necesse.entity.levelEvent.mobAbilityLevelEvent.FallenWizardBeamLevelEvent;
import necesse.entity.mobs.Attacker;
import necesse.entity.mobs.GameDamage;
import necesse.entity.mobs.MaskShaderOptions;
import necesse.entity.mobs.MaxHealthGetter;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.MobDrawable;
import necesse.entity.mobs.MobHealthScaling;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.ability.BooleanMobAbility;
import necesse.entity.mobs.ability.CoordinateMobAbility;
import necesse.entity.mobs.ability.EmptyMobAbility;
import necesse.entity.mobs.ability.IntMobAbility;
import necesse.entity.mobs.ability.MobAbility;
import necesse.entity.mobs.ai.behaviourTree.AINode;
import necesse.entity.mobs.ai.behaviourTree.AINodeResult;
import necesse.entity.mobs.ai.behaviourTree.BehaviourTreeAI;
import necesse.entity.mobs.ai.behaviourTree.Blackboard;
import necesse.entity.mobs.ai.behaviourTree.composites.SequenceAINode;
import necesse.entity.mobs.ai.behaviourTree.decorators.IsolateRunningAINode;
import necesse.entity.mobs.ai.behaviourTree.decorators.MoveTaskAINode;
import necesse.entity.mobs.ai.behaviourTree.leaves.TargetFinderAINode;
import necesse.entity.mobs.ai.behaviourTree.util.TargetFinderDistance;
import necesse.entity.mobs.buffs.BuffModifiers;
import necesse.entity.mobs.buffs.staticBuffs.BossNearbyBuff;
import necesse.entity.mobs.hostile.bosses.bossAIUtils.AttackStageInterface;
import necesse.entity.mobs.hostile.bosses.bossAIUtils.AttackStageManagerNode;
import necesse.entity.mobs.hostile.bosses.bossAIUtils.IdleTimeAttackStage;
import necesse.entity.mobs.hostile.bosses.bossAIUtils.RotationAttackStageNode;
import necesse.entity.particle.Particle;
import necesse.entity.particle.SmokePuffParticle;
import necesse.entity.projectile.FallenWizardBallProjectile;
import necesse.entity.projectile.FallenWizardWaveProjectile;
import necesse.entity.projectile.Projectile;
import necesse.entity.projectile.followingProjectile.FallenWizardScepterProjectile;
import necesse.gfx.GameResources;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.DrawOptions;
import necesse.gfx.drawOptions.human.HumanDrawOptions;
import necesse.gfx.drawOptions.itemAttack.ItemAttackDrawOptions;
import necesse.gfx.drawOptions.texture.TextureDrawOptions;
import necesse.gfx.drawables.EntityDrawable;
import necesse.gfx.drawables.LevelSortedDrawable;
import necesse.gfx.drawables.OrderableDrawables;
import necesse.gfx.gameTexture.GameTexture;
import necesse.gfx.gameTooltips.GameTooltips;
import necesse.gfx.gameTooltips.StringTooltips;
import necesse.inventory.InventoryItem;
import necesse.inventory.item.miscItem.GatewayTabletItem;
import necesse.inventory.lootTable.LootItemInterface;
import necesse.inventory.lootTable.LootList;
import necesse.inventory.lootTable.LootTable;
import necesse.inventory.lootTable.lootItem.ChanceLootItem;
import necesse.inventory.lootTable.lootItem.ConditionLootItem;
import necesse.inventory.lootTable.lootItem.LootItem;
import necesse.inventory.lootTable.lootItem.RotationLootItem;
import necesse.level.maps.CollisionFilter;
import necesse.level.maps.Level;
import necesse.level.maps.biomes.temple.TempleArenaLevel;
import necesse.level.maps.light.GameLight;

public class FallenWizardMob extends BossMob {
   public static LootTable lootTable = new LootTable(new LootItemInterface() {
      @Override
      public void addPossibleLoot(LootList list, Object... extra) {
         list.add("gatewaytablet");
      }

      @Override
      public void addItems(List<InventoryItem> list, GameRandom random, float lootMultiplier, Object... extra) {
         InventoryItem gatewayTablet = new InventoryItem("gatewaytablet");
         GatewayTabletItem.initializeGatewayTablet(gatewayTablet, random, 1, null);
         list.add(gatewayTablet);
      }
   }, new LootItem("altardust", 60), new ChanceLootItem(0.2F, "wizardsrematchvinyl"));
   public static RotationLootItem uniqueDrops = RotationLootItem.privateLootRotation();
   public static LootTable privateLootTable = new LootTable(
      new ConditionLootItem(
         "wizardsocket",
         (r, o) -> {
            ServerClient client = LootTable.expectExtra(ServerClient.class, o, 1);
            return client != null
               && client.playerMob.getInv().equipment.getTrinketSlotsSize() < 7
               && client.playerMob.getInv().getAmount(ItemRegistry.getItem("wizardsocket"), false, false, true, true, "have") == 0;
         }
      ),
      new ConditionLootItem(
         "abyssalcloak",
         (r, o) -> {
            ServerClient client = LootTable.expectExtra(ServerClient.class, o, 1);
            return client != null
               && client.playerMob.getInv().equipment.getTotalSets() < 4
               && client.playerMob.getInv().getAmount(ItemRegistry.getItem("abyssalcloak"), false, false, true, true, "have") == 0;
         }
      ),
      uniqueDrops
   );
   public static MaxHealthGetter MAX_HEALTH = new MaxHealthGetter(20000, 30000, 35000, 40000, 50000);
   protected MobHealthScaling scaling = new MobHealthScaling(this);
   protected LinkedList<Projectile> projectiles = new LinkedList<>();
   public boolean isVisible;
   public long visibilityFadeTime;
   public boolean spawnParticles;
   public final FallenWizardMob.FallenTeleportAbility teleportAbility;
   public final FallenWizardMob.FallenSetVisibilityAbility visibilityAbility;
   public final BooleanMobAbility changeHostile;
   public final EmptyMobAbility laserBlastSound;
   public final EmptyMobAbility magicBoltSound;
   public final CoordinateMobAbility spawnDragonEffects;
   public final IntMobAbility humanSoundAbility;
   protected SoundPlayer tiptoeSoundPlayer;
   public static GameDamage laserDamage = new GameDamage(130.0F);
   public static GameDamage scepterDamage = new GameDamage(100.0F);
   public static GameDamage waveDamage = new GameDamage(90.0F);
   public static GameDamage ballDamage = new GameDamage(90.0F);
   public static GameDamage dragonHeadDamage = new GameDamage(130.0F);
   public static GameDamage dragonBodyDamage = new GameDamage(100.0F);

   public FallenWizardMob() {
      super(100);
      this.difficultyChanges.setMaxHealth(MAX_HEALTH);
      this.setSpeed(60.0F);
      this.setFriction(3.0F);
      this.setArmor(30);
      this.setKnockbackModifier(0.0F);
      this.collision = new Rectangle(-10, -7, 20, 14);
      this.hitBox = new Rectangle(-26, -24, 52, 48);
      this.selectBox = new Rectangle(-19, -52, 38, 64);
      this.swimMaskMove = 20;
      this.swimMaskOffset = -20;
      this.swimSinkOffset = 0;
      this.setDir(2);
      this.shouldSave = true;
      this.isHostile = false;
      this.isVisible = true;
      this.teleportAbility = this.registerAbility(new FallenWizardMob.FallenTeleportAbility());
      this.visibilityAbility = this.registerAbility(new FallenWizardMob.FallenSetVisibilityAbility());
      this.changeHostile = this.registerAbility(new BooleanMobAbility() {
         @Override
         protected void run(boolean value) {
            if (!FallenWizardMob.this.isHostile && value) {
               if (FallenWizardMob.this.isClient()) {
                  SoundManager.playSound(GameResources.fallenwizardbegin, SoundEffect.effect(FallenWizardMob.this).volume(0.7F).falloffDistance(4000));
               }
            } else if (FallenWizardMob.this.isHostile && !value) {
               FallenWizardMob.this.setDir(2);
               FallenWizardMob.this.isVisible = true;
               FallenWizardMob.this.stopMoving();
               FallenWizardMob.this.resetAI();
            }

            FallenWizardMob.this.isHostile = value;
         }
      });
      this.laserBlastSound = this.registerAbility(
         new EmptyMobAbility() {
            @Override
            protected void run() {
               if (FallenWizardMob.this.isClient()) {
                  SoundManager.playSound(
                     GameResources.laserBlast2,
                     SoundEffect.effect(FallenWizardMob.this).falloffDistance(1250).volume(0.7F).pitch(GameRandom.globalRandom.getOneOf(0.95F, 1.0F, 1.05F))
                  );
               }
            }
         }
      );
      this.magicBoltSound = this.registerAbility(
         new EmptyMobAbility() {
            @Override
            protected void run() {
               if (FallenWizardMob.this.isClient()) {
                  SoundManager.playSound(
                     GameResources.magicbolt2,
                     SoundEffect.effect(FallenWizardMob.this).falloffDistance(1250).volume(0.8F).pitch(GameRandom.globalRandom.getOneOf(0.95F, 1.0F, 1.05F))
                  );
               }
            }
         }
      );
      this.spawnDragonEffects = this.registerAbility(
         new CoordinateMobAbility() {
            @Override
            protected void run(int x, int y) {
               if (FallenWizardMob.this.isClient()) {
                  FallenWizardMob.this.getLevel()
                     .entityManager
                     .addParticle(new FallenWizardMob.PortalParticle(FallenWizardMob.this.getLevel(), x, y, 1000L), true, Particle.GType.CRITICAL);
                  SoundManager.playSound(GameResources.magicbolt4, SoundEffect.effect(FallenWizardMob.this).falloffDistance(1400).volume(0.6F));
                  SoundManager.playSound(GameResources.roar, SoundEffect.effect(x, y).falloffDistance(1400).volume(0.7F));
               }
            }
         }
      );
      this.humanSoundAbility = this.registerAbility(
         new IntMobAbility() {
            @Override
            protected void run(int value) {
               if (FallenWizardMob.this.isClient()) {
                  switch (value) {
                     case 0:
                        if (FallenWizardMob.this.tiptoeSoundPlayer == null || FallenWizardMob.this.tiptoeSoundPlayer.isDone()) {
                           FallenWizardMob.this.tiptoeSoundPlayer = SoundManager.playSound(SoundSettingsRegistry.humanFootsteps, FallenWizardMob.this);
                        }

                        return;
                     case 1:
                        SoundManager.playSound(
                           new SoundSettings(GameResources.warcryumf).volume(0.3F).pitchVariance(0.0F).fallOffDistance(1000), FallenWizardMob.this
                        );
                        return;
                     case 2:
                        SoundManager.playSound(
                           new SoundSettings(GameResources.swoosh2).volume(0.2F).basePitch(0.7F).pitchVariance(0.0F).fallOffDistance(1000),
                           FallenWizardMob.this
                        );
                  }
               }
            }
         }
      );
   }

   @Override
   public void setupSpawnPacket(PacketWriter writer) {
      super.setupSpawnPacket(writer);
      writer.putNextBoolean(this.spawnParticles);
   }

   @Override
   public void applySpawnPacket(PacketReader reader) {
      super.applySpawnPacket(reader);
      this.spawnParticles = reader.getNextBoolean();
   }

   @Override
   public void setupMovementPacket(PacketWriter writer) {
      super.setupMovementPacket(writer);
      writer.putNextBoolean(this.isHostile);
   }

   @Override
   public void applyMovementPacket(PacketReader reader, boolean isDirect) {
      super.applyMovementPacket(reader, isDirect);
      this.isHostile = reader.getNextBoolean();
   }

   @Override
   public void setupHealthPacket(PacketWriter writer, boolean isFull) {
      this.scaling.setupHealthPacket(writer, isFull);
      super.setupHealthPacket(writer, isFull);
   }

   @Override
   public void applyHealthPacket(PacketReader reader, boolean isFull) {
      this.scaling.applyHealthPacket(reader, isFull);
      super.applyHealthPacket(reader, isFull);
   }

   @Override
   public void setMaxHealth(int maxHealth) {
      super.setMaxHealth(maxHealth);
      if (this.scaling != null) {
         this.scaling.updatedMaxHealth();
      }
   }

   @Override
   public void init() {
      super.init();
      this.resetAI();
      this.setDir(2);
      if (this.spawnParticles && !this.isServer()) {
         this.getLevel()
            .entityManager
            .addParticle(new SmokePuffParticle(this.getLevel(), this.getX(), this.getY() + 2, 96, new Color(67, 6, 127)), Particle.GType.CRITICAL);
      }

      if (this.isServer() && this.isInArena()) {
         Float pos = TempleArenaLevel.getBossPosition();
         this.setPos(pos.x, pos.y, true);
         this.setHealth(this.getMaxHealth());
      }
   }

   protected void resetAI() {
      this.ai = new BehaviourTreeAI<>(this, new FallenWizardMob.FallenWizardAI<>());
   }

   public boolean isInArena() {
      return this.getLevel().getBiome(this.getTileX(), this.getTileY()) == BiomeRegistry.TEMPLE && this.getLevel() instanceof TempleArenaLevel;
   }

   @Override
   public LootTable getLootTable() {
      return lootTable;
   }

   @Override
   public LootTable getPrivateLootTable() {
      return privateLootTable;
   }

   public void addProjectile(Projectile p) {
      this.projectiles.add(p);
   }

   public void clearProjectiles() {
      this.projectiles.forEach(Projectile::remove);
      this.projectiles.clear();
   }

   @Override
   public int getMaxHealth() {
      return super.getMaxHealth() + (int)((this.scaling == null ? 0 : this.scaling.getHealthIncrease()) * this.getMaxHealthModifier());
   }

   @Override
   public void clientTick() {
      super.clientTick();
      float healthPercInv = Math.abs((float)this.getHealth() / this.getMaxHealth() - 1.0F);
      this.setSpeed(60.0F + 30.0F * healthPercInv);
      if (this.isHostile) {
         if (this.isClientPlayerNearby()) {
            SoundManager.setMusic(MusicRegistry.WizardsRematch, SoundManager.MusicPriority.EVENT, 1.5F);
            EventStatusBarManager.registerMobHealthStatusBar(this);
            Color shade = VoidWizard.getWizardShade(this);
            PostProcessingEffects.setSceneShade(shade);
         }

         BossNearbyBuff.applyAround(this);
      }

      this.getLevel().lightManager.refreshParticleLightFloat(this.x, this.y, 260.0F, 0.5F);
      this.spawnParticles = false;
   }

   @Override
   public void serverTick() {
      super.serverTick();
      float healthPercInv = Math.abs((float)this.getHealth() / this.getMaxHealth() - 1.0F);
      this.setSpeed(60.0F + 30.0F * healthPercInv);
      this.scaling.serverTick();
      if (this.isHostile) {
         BossNearbyBuff.applyAround(this);
      }

      this.spawnParticles = false;
   }

   @Override
   public CollisionFilter getLevelCollisionFilter() {
      return super.getLevelCollisionFilter().addFilter(tp -> tp.object().object.isWall || tp.object().object.isRock);
   }

   @Override
   public boolean canBePushed(Mob other) {
      return false;
   }

   @Override
   public boolean isVisible() {
      return this.isVisible;
   }

   @Override
   public boolean canTakeDamage() {
      return super.canTakeDamage() && this.isVisible();
   }

   @Override
   public void spawnDeathParticles(float knockbackX, float knockbackY) {
      for (int i = 0; i < 50; i++) {
         int lifeTime = GameRandom.globalRandom.getIntBetween(500, 5000);
         float lifePerc = lifeTime / 5000.0F;
         float startHeight = 10.0F;
         float height = startHeight + GameRandom.globalRandom.getIntBetween(70, 150) * lifePerc;
         this.getLevel()
            .entityManager
            .addParticle(
               this.x + GameRandom.globalRandom.getFloatBetween(-20.0F, 20.0F),
               this.y + GameRandom.globalRandom.getFloatBetween(-10.0F, 10.0F),
               Particle.GType.IMPORTANT_COSMETIC
            )
            .sizeFades(20, 30)
            .movesFriction(GameRandom.globalRandom.getFloatBetween(-40.0F, 40.0F), GameRandom.globalRandom.getFloatBetween(-20.0F, 20.0F), 0.5F)
            .heightMoves(startHeight, height)
            .colorRandom(270.0F, 0.8F, 0.5F, 10.0F, 0.1F, 0.1F)
            .lifeTime(lifeTime);
      }
   }

   @Override
   public void playHurtSound() {
      if (this.isHostile) {
         float pitch = GameRandom.globalRandom.getOneOf(0.95F, 1.0F, 1.05F);
         SoundManager.playSound(GameResources.fallenwizardhurt, SoundEffect.effect(this).volume(0.4F).pitch(pitch).falloffDistance(1500));
      }
   }

   @Override
   public void playDeathSound() {
      SoundManager.playSound(GameResources.fallenwizarddeath, SoundEffect.effect(this).falloffExponent(3000.0F));
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
      long timeSinceFade = level.getWorldEntity().getLocalTime() - this.visibilityFadeTime;
      if (this.isVisible() || timeSinceFade <= 1000L) {
         GameLight light = level.getLightLevel(getTileCoordinate(x), getTileCoordinate(y));
         boolean hasGlowEffect = perspective != null && perspective.buffManager.getModifier(BuffModifiers.ENEMY_TRACKER);
         int drawX = camera.getDrawX(x) - 48;
         int drawY = camera.getDrawY(y) - 75;
         int dir = this.getDir();
         Point sprite = this.getAnimSprite(x, y, dir);
         drawY += this.getBobbing(x, y);
         drawY += level.getTile(getTileCoordinate(x), getTileCoordinate(y)).getMobSinkingAmount(this);
         MaskShaderOptions swimMask = this.getSwimMaskShaderOptions(this.inLiquidFloat(x, y));
         HumanDrawOptions humanDrawOptions = new HumanDrawOptions(level, MobRegistry.Textures.fallenWizard)
            .sprite(sprite, 96)
            .size(96, 96)
            .mask(swimMask)
            .dir(dir)
            .light(light)
            .hasGlowEffect(hasGlowEffect, this.getID())
            .attackOffsets(48, 34, 16, 25, 18, 5, 20);
         if (timeSinceFade < 1000L) {
            float alpha = Math.max(0.0F, (float)timeSinceFade / 1000.0F);
            if (!this.isVisible()) {
               alpha = Math.abs(alpha - 1.0F);
            }

            humanDrawOptions.allAlpha(alpha);
         }

         float animProgress = this.getAttackAnimProgress();
         if (this.isAttacking) {
            ItemAttackDrawOptions attackOptions = ItemAttackDrawOptions.start(dir)
               .itemSprite(MobRegistry.Textures.fallenWizard.body, 0, 9, 48)
               .itemRotatePoint(14, 4)
               .itemEnd()
               .armSprite(MobRegistry.Textures.fallenWizard.body, 0, 8, 48);
            attackOptions.pointRotation(this.attackDir.x, this.attackDir.y).forEachItemSprite(i -> i.itemRotateOffset(45.0F));
            humanDrawOptions.attackAnim(attackOptions, animProgress);
         }

         final DrawOptions drawOptions = humanDrawOptions.pos(drawX, drawY);
         list.add(new MobDrawable() {
            @Override
            public void draw(TickManager tickManager) {
               drawOptions.draw();
            }
         });
         this.addShadowDrawables(tileList, level, x, y, light, camera);
      }
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
   public boolean shouldDrawOnMap() {
      return this.isVisible();
   }

   @Override
   public Rectangle drawOnMapBox(double tileScale, boolean isMinimap) {
      return new Rectangle(-12, -28, 24, 34);
   }

   @Override
   public GameTooltips getMapTooltips() {
      return new StringTooltips(this.getDisplayName() + " " + this.getHealth() + "/" + this.getMaxHealth());
   }

   @Override
   public void drawOnMap(TickManager tickManager, Client client, int x, int y, double tileScale, Rectangle drawBounds, boolean isMinimap) {
      super.drawOnMap(tickManager, client, x, y, tileScale, drawBounds, isMinimap);
      int drawX = x - 24;
      int drawY = y - 34;
      int dir = this.getDir();
      Point sprite = this.getAnimSprite(this.getDrawX(), this.getDrawY(), dir);
      new HumanDrawOptions(this.getLevel(), MobRegistry.Textures.fallenWizard).sprite(sprite, 96).dir(dir).size(48, 48).draw(drawX, drawY);
   }

   @Override
   public Stream<ModifierValue<?>> getDefaultModifiers() {
      return Stream.of(new ModifierValue<>(BuffModifiers.SLOW, 0.0F).max(0.2F), new ModifierValue<>(BuffModifiers.ATTACK_MOVEMENT_MOD, 0.0F));
   }

   @Override
   protected void onDeath(Attacker attacker, HashSet<Attacker> attackers) {
      super.onDeath(attacker, attackers);
      boolean dodgeThisValid = this.getLevel().entityManager.mobs.stream().filter(m -> m instanceof FallenDragonHead).count() >= 5L;
      attackers.stream()
         .map(Attacker::getFirstPlayerOwner)
         .filter(Objects::nonNull)
         .filter(PlayerMob::isServerClient)
         .map(PlayerMob::getServerClient)
         .distinct()
         .forEach(c -> {
            c.sendChatMessage(new LocalMessage("misc", "bossdefeat", "name", this.getLocalization()));
            if (c.achievementsLoaded()) {
               c.achievements().REMATCH.markCompleted(c);
               if (dodgeThisValid) {
                  c.achievements().DODGE_THIS.markCompleted(c);
               }
            }
         });
      if (!this.isDamagedByPlayers) {
         AchievementManager.checkMeAndThisArmyKill(this.getLevel(), attackers);
      }

      if (this.isInArena() && this.getLevel().entityManager.events.stream().noneMatch(e -> e instanceof FallenWizardRespawnEvent)) {
         Float pos = TempleArenaLevel.getBossPosition();
         long spawnInTime = 600000L;
         this.getLevel().entityManager.events.add(new FallenWizardRespawnEvent(pos.x, pos.y, this.getLevel().getWorldEntity().getWorldTime() + spawnInTime));
      }
   }

   public class FallenSetVisibilityAbility extends MobAbility {
      public void runAndSend(boolean visible, boolean fade) {
         Packet content = new Packet();
         PacketWriter writer = new PacketWriter(content);
         writer.putNextBoolean(visible);
         writer.putNextBoolean(fade);
         this.runAndSendAbility(content);
      }

      @Override
      public void executePacket(PacketReader reader) {
         FallenWizardMob.this.isVisible = reader.getNextBoolean();
         if (reader.getNextBoolean()) {
            FallenWizardMob.this.visibilityFadeTime = FallenWizardMob.this.getWorldEntity().getLocalTime();
         }
      }
   }

   public class FallenTeleportAbility extends MobAbility {
      public void runAndSend(int x, int y, int dir, boolean smokeParticle, boolean spawnParticlesBefore, boolean spawnParticlesAfter) {
         Packet content = new Packet();
         PacketWriter writer = new PacketWriter(content);
         writer.putNextInt(x);
         writer.putNextInt(y);
         writer.putNextInt(dir);
         writer.putNextBoolean(smokeParticle);
         writer.putNextBoolean(spawnParticlesBefore);
         writer.putNextBoolean(spawnParticlesAfter);
         this.runAndSendAbility(content);
      }

      @Override
      public void executePacket(PacketReader reader) {
         int x = reader.getNextInt();
         int y = reader.getNextInt();
         int dir = reader.getNextInt();
         boolean smokeParticle = reader.getNextBoolean();
         boolean spawnParticlesBefore = reader.getNextBoolean();
         boolean spawnParticlesAfter = reader.getNextBoolean();
         if (spawnParticlesBefore && FallenWizardMob.this.isClient()) {
            if (smokeParticle) {
               FallenWizardMob.this.getLevel()
                  .entityManager
                  .addParticle(
                     new SmokePuffParticle(FallenWizardMob.this.getLevel(), FallenWizardMob.this.getX(), FallenWizardMob.this.getY(), 96, new Color(67, 6, 127)),
                     Particle.GType.CRITICAL
                  );
            } else {
               FallenWizardMob.this.getLevel()
                  .entityManager
                  .addParticle(
                     new FallenWizardMob.TeleportParticle(
                        FallenWizardMob.this.getLevel(), FallenWizardMob.this.getX(), FallenWizardMob.this.getY(), FallenWizardMob.this
                     ),
                     Particle.GType.CRITICAL
                  );
               FallenWizardMob.this.getLevel().lightManager.refreshParticleLightFloat(x, y, 270.0F, 0.5F);
            }
         }

         FallenWizardMob.this.setDir(dir);
         FallenWizardMob.this.setPos(x, y, true);
         if (spawnParticlesAfter && FallenWizardMob.this.isClient()) {
            if (smokeParticle) {
               FallenWizardMob.this.getLevel()
                  .entityManager
                  .addParticle(
                     new SmokePuffParticle(
                        FallenWizardMob.this.getLevel(), FallenWizardMob.this.getX(), FallenWizardMob.this.getY() + 2, 96, new Color(67, 6, 127)
                     ),
                     Particle.GType.CRITICAL
                  );
            } else {
               FallenWizardMob.this.getLevel()
                  .entityManager
                  .addParticle(
                     new FallenWizardMob.TeleportParticle(
                        FallenWizardMob.this.getLevel(), FallenWizardMob.this.getX(), FallenWizardMob.this.getY() + 2, FallenWizardMob.this
                     ),
                     Particle.GType.CRITICAL
                  );
            }
         }
      }
   }

   public static class FallenWizardAI<T extends FallenWizardMob> extends SequenceAINode<T> {
      private int inActiveTimer;

      public FallenWizardAI() {
         this.addChild(new AINode<T>() {
            protected void onRootSet(AINode<T> root, T mob, Blackboard<T> blackboard) {
            }

            public void init(T mob, Blackboard<T> blackboard) {
            }

            public AINodeResult tick(T mob, Blackboard<T> blackboard) {
               if (!mob.isHostile) {
                  blackboard.mover.stopMoving(mob);
                  return AINodeResult.FAILURE;
               } else {
                  return AINodeResult.SUCCESS;
               }
            }
         });
         this.addChild(new AINode<T>() {
            protected void onRootSet(AINode<T> root, T mob, Blackboard<T> blackboard) {
            }

            public void init(T mob, Blackboard<T> blackboard) {
            }

            public AINodeResult tick(T mob, Blackboard<T> blackboard) {
               Mob currentTarget = blackboard.getObject(Mob.class, "currentTarget");
               if (currentTarget == null) {
                  FallenWizardAI.this.inActiveTimer++;
                  if (FallenWizardAI.this.inActiveTimer > 100) {
                     if (mob.isInArena()) {
                        Float tpPos = TempleArenaLevel.getBossPosition();
                        mob.teleportAbility.runAndSend((int)tpPos.x, (int)tpPos.y, 2, false, true, false);
                        mob.humanSoundAbility.runAndSend(2);
                     }

                     mob.changeHostile.runAndSend(false);
                     FallenWizardAI.this.inActiveTimer = 0;
                     mob.clearProjectiles();
                     mob.setHealth(mob.getMaxHealth());
                     return AINodeResult.FAILURE;
                  }
               } else {
                  FallenWizardAI.this.inActiveTimer = 0;
               }

               return AINodeResult.SUCCESS;
            }
         });
         this.addChild(new TargetFinderAINode<T>(3200) {
            public GameAreaStream<? extends Mob> streamPossibleTargets(T mob, Point base, TargetFinderDistance<T> distance) {
               return TargetFinderAINode.streamPlayers(mob, base, distance);
            }
         });
         AttackStageManagerNode<T> attackStages = new AttackStageManagerNode<>();
         attackStages.allowSkippingBack = true;
         this.addChild(new IsolateRunningAINode<>(attackStages));
         attackStages.addChild(new FallenWizardMob.FallenWizardAI.FindNewPosStage(true));
         attackStages.addChild(new IdleTimeAttackStage<>(0, 200));
         attackStages.addChild(
            new RotationAttackStageNode<>(
               new FallenWizardMob.FallenWizardAI.DirectionWaveProjectilesStage(0, 250, 200, 400, 60.0F, 5),
               new FallenWizardMob.FallenWizardAI.RandomWaveProjectilesStage(0, 250, 400, 600, 4)
            )
         );
         attackStages.addChild(new FallenWizardMob.FallenWizardAI.GhostFindNewPosStage());
         attackStages.addChild(new IdleTimeAttackStage<>(100, 500));
         attackStages.addChild(new FallenWizardMob.FallenWizardAI.BeamSweepStage(1500, 3000, 110.0F, 270.0F));
         attackStages.addChild(new FallenWizardMob.FallenWizardAI.FindNewPosStage(true));
         attackStages.addChild(new IdleTimeAttackStage<>(0, 100));
         attackStages.addChild(new FallenWizardMob.FallenWizardAI.BulletHellStage(250, -35.0F, 100.0F, 4, 1000, 2000, 7.5F));
         attackStages.addChild(new FallenWizardMob.FallenWizardAI.FindNewPosStage(true));
         attackStages.addChild(new IdleTimeAttackStage<>(0, 100));
         attackStages.addChild(new FallenWizardMob.FallenWizardAI.DragonSpawnStage(0, 250, 500, 1000, 2));
         attackStages.addChild(new FallenWizardMob.FallenWizardAI.FindNewPosStage(true));
         attackStages.addChild(new IdleTimeAttackStage<>(0, 100));
         attackStages.addChild(new FallenWizardMob.FallenWizardAI.ScepterProjectilesStage(25, 2250, 2750));
      }

      public PriorityMap<Point> findNewPositions(Mob mob, Blackboard<T> blackboard) {
         Mob currentTarget = blackboard.getObject(Mob.class, "currentTarget");
         Float base = new Float(mob.x, mob.y);
         if (currentTarget != null) {
            base = new Float(currentTarget.x, currentTarget.y);
         }

         int searchRadius = 8;
         int minRadius = 3;
         int mobTileX = mob.getTileX();
         int mobTileY = mob.getTileY();
         int xOffset = (int)Math.signum(Entity.getTileCoordinate(base.x) - mobTileX) * searchRadius / 2;
         int yOffset = (int)Math.signum(Entity.getTileCoordinate(base.y) - mobTileY) * searchRadius / 2;
         PriorityMap<Point> priorityMap = new PriorityMap<>();

         for (int x = -searchRadius; x <= searchRadius; x++) {
            if (x <= -minRadius || x >= minRadius) {
               for (int y = -searchRadius; y <= searchRadius; y++) {
                  if (y <= -minRadius || y >= minRadius) {
                     Point p = new Point(mobTileX + xOffset + x, mobTileY + yOffset + y);
                     if (!mob.getLevel().isSolidTile(p.x, p.y)) {
                        int height = mob.getLevel().liquidManager.getHeight(p.x, p.y);
                        if (height >= 8) {
                           priorityMap.add(10, p);
                        } else {
                           priorityMap.add(height / 2, p);
                        }
                     }
                  }
               }
            }
         }

         return priorityMap;
      }

      public Point findNewPosition(Mob mob, Blackboard<T> blackboard) {
         return this.findNewPositions(mob, blackboard).getRandomBestObject(GameRandom.globalRandom, 20);
      }

      public void onRootSet(AINode<T> root, T mob, Blackboard<T> blackboard) {
         super.onRootSet(root, mob, blackboard);
         blackboard.onWasHit(e -> this.makeHostile(mob));
      }

      public void makeHostile(T mob) {
         if (!mob.isHostile) {
            if (mob.isServer()) {
               mob.getLevel()
                  .getServer()
                  .network
                  .sendToClientsWithEntity(new PacketChatMessage(new LocalMessage("misc", "bossawoke", "name", mob.getLocalization())), mob);
            }

            mob.changeHostile.runAndSend(true);
         }
      }

      public int getHealthPercValue(T mob, int noHealthValue, int fullHealthValue) {
         int delta = fullHealthValue - noHealthValue;
         float healthPerc = (float)mob.getHealth() / mob.getMaxHealth();
         return noHealthValue + (int)(delta * healthPerc);
      }

      protected class BeamSweepStage extends AINode<T> implements AttackStageInterface<T> {
         private boolean clockwise = GameRandom.globalRandom.nextBoolean();
         private LevelEvent event;
         public int noHealthSweepTime;
         public int fullHealthSweepTime;
         public float startAngleOffset;
         public float totalAngleCover;

         public BeamSweepStage(int noHealthSweepTime, int fullHealthSweepTime, float startAngleOffset, float totalAngleCover) {
            this.noHealthSweepTime = noHealthSweepTime;
            this.fullHealthSweepTime = fullHealthSweepTime;
            this.startAngleOffset = startAngleOffset;
            this.totalAngleCover = totalAngleCover;
         }

         protected void onRootSet(AINode<T> root, T mob, Blackboard<T> blackboard) {
         }

         public void init(T mob, Blackboard<T> blackboard) {
         }

         public AINodeResult tick(T mob, Blackboard<T> blackboard) {
            return this.event.isOver() ? AINodeResult.SUCCESS : AINodeResult.RUNNING;
         }

         public void onStarted(T mob, Blackboard<T> blackboard) {
            Mob currentTarget = blackboard.getObject(Mob.class, "currentTarget");
            long startTime = mob.getWorldEntity().getTime();
            float startAngle;
            if (currentTarget != null) {
               startAngle = GameMath.getAngle(new Float(currentTarget.x - mob.x, currentTarget.y - mob.y));
            } else {
               startAngle = GameRandom.globalRandom.nextInt(360);
            }

            float endAngle;
            if (this.clockwise) {
               startAngle += this.startAngleOffset;
               endAngle = startAngle - this.totalAngleCover;
            } else {
               startAngle -= this.startAngleOffset;
               endAngle = startAngle + this.totalAngleCover;
            }

            int sweepTime = FallenWizardAI.this.getHealthPercValue(mob, this.noHealthSweepTime, this.fullHealthSweepTime);
            mob.getLevel()
               .entityManager
               .events
               .add(
                  this.event = new FallenWizardBeamLevelEvent(
                     mob, startAngle, endAngle, startTime, sweepTime, GameRandom.globalRandom.nextInt(), 1400.0F, FallenWizardMob.laserDamage, 50, 500, 0
                  )
               );
            this.clockwise = !this.clockwise;
            mob.humanSoundAbility.runAndSend(1);
         }

         public void onEnded(T mob, Blackboard<T> blackboard) {
         }
      }

      protected class BulletHellStage extends AINode<T> implements AttackStageInterface<T> {
         private long nextShowAttackTime;
         private float currentAngle;
         private float remainingAngle;
         private float angleBuffer;
         private int attackDirection;
         private int direction;
         private long startTime;
         private int currentShootTime;
         public float targetStartAngleOffset;
         public float totalAngle;
         public float anglePerProjectile;
         public int arms;
         public int chargeUpTime;
         public int noHealthShootTime;
         public int fullHealthShootTime;

         public BulletHellStage(
            int chargeUpTime,
            float targetStartAngleOffset,
            float totalAngle,
            int arms,
            int noHealthShootTime,
            int fullHealthShootTime,
            float anglePerProjectile
         ) {
            this.targetStartAngleOffset = targetStartAngleOffset;
            this.totalAngle = totalAngle;
            this.arms = arms;
            this.chargeUpTime = chargeUpTime;
            this.noHealthShootTime = noHealthShootTime;
            this.fullHealthShootTime = fullHealthShootTime;
            this.anglePerProjectile = anglePerProjectile;
            this.direction = GameRandom.globalRandom.getOneOf(-1, 1);
         }

         protected void onRootSet(AINode<T> root, T mob, Blackboard<T> blackboard) {
         }

         public void init(T mob, Blackboard<T> blackboard) {
         }

         public AINodeResult tick(T mob, Blackboard<T> blackboard) {
            long currentTime = mob.getWorldEntity().getTime();
            long timePassed = currentTime - this.startTime;
            if (this.nextShowAttackTime <= currentTime) {
               mob.showAttack((int)(mob.x + this.attackDirection * 30), (int)(mob.y - 100.0F), false);
               this.nextShowAttackTime = currentTime + 200L;
               if (timePassed >= this.chargeUpTime) {
                  mob.magicBoltSound.runAndSend();
               }

               if (mob.isServer()) {
                  mob.getLevel()
                     .getServer()
                     .network
                     .sendToClientsWithEntity(new PacketMobAttack(mob, (int)(mob.x + this.attackDirection * 30), (int)(mob.y - 100.0F), false), mob);
               }
            }

            if (timePassed < this.chargeUpTime) {
               return AINodeResult.RUNNING;
            }

            this.angleBuffer = this.angleBuffer + this.totalAngle * 50.0F / this.currentShootTime;

            while (this.angleBuffer >= this.anglePerProjectile) {
               this.currentAngle = this.currentAngle + this.anglePerProjectile * this.direction;

               for (int i = 0; i < this.arms; i++) {
                  float thisAngle = this.currentAngle + 360.0F / this.arms * i;
                  FallenWizardBallProjectile projectile = new FallenWizardBallProjectile(
                     mob.getLevel(), mob, mob.x + this.attackDirection * 15, mob.y - 35.0F, thisAngle, 90.0F, 1200, FallenWizardMob.ballDamage, 50
                  );
                  mob.getLevel().entityManager.projectiles.add(projectile);
               }

               this.angleBuffer = this.angleBuffer - this.anglePerProjectile;
               this.remainingAngle = this.remainingAngle - this.anglePerProjectile;
               if (this.remainingAngle <= 0.0F) {
                  break;
               }
            }

            return this.angleBuffer >= this.remainingAngle ? AINodeResult.SUCCESS : AINodeResult.RUNNING;
         }

         public void onStarted(T mob, Blackboard<T> blackboard) {
            this.direction *= -1;
            Mob target = blackboard.getObject(Mob.class, "currentTarget");
            if (target != null) {
               this.currentAngle = GameMath.getAngle(new Float(target.x - mob.x, target.y - mob.y)) + this.targetStartAngleOffset * this.direction + 90.0F;
            } else {
               this.currentAngle = GameRandom.globalRandom.nextInt(360);
            }

            this.attackDirection = (int)Math.signum(GameMath.getAngleDir(this.currentAngle).x);
            if (this.attackDirection == 0) {
               this.attackDirection = GameRandom.globalRandom.getOneOf(-1, 1);
            }

            this.remainingAngle = this.totalAngle;
            this.startTime = mob.getLevel().getWorldEntity().getTime();
            this.currentShootTime = FallenWizardAI.this.getHealthPercValue(mob, this.noHealthShootTime, this.fullHealthShootTime);
            this.nextShowAttackTime = 0L;
            mob.humanSoundAbility.runAndSend(1);
         }

         public void onEnded(T mob, Blackboard<T> blackboard) {
         }
      }

      protected class DirectionWaveProjectilesStage extends AINode<T> implements AttackStageInterface<T> {
         private Float direction;
         private int projectilesFired;
         private long lastProjectileTime;
         private long nextShowAttackTime;
         private boolean reversed;
         private int currentTimeGap;
         public float gapSize;
         public int noHealthStartDelay;
         public int fullHealthStartDelay;
         public int noHealthTimeGap;
         public int fullHealthTimeGap;
         public int waves;

         public DirectionWaveProjectilesStage(
            int noHealthStartDelay, int fullHealthStartDelay, int noHealthTimeGap, int fullHealthTimeGap, float gapSize, int waves
         ) {
            this.noHealthStartDelay = noHealthStartDelay;
            this.fullHealthStartDelay = fullHealthStartDelay;
            this.noHealthTimeGap = noHealthTimeGap;
            this.fullHealthTimeGap = fullHealthTimeGap;
            this.gapSize = gapSize;
            this.waves = waves;
         }

         protected void onRootSet(AINode<T> root, T mob, Blackboard<T> blackboard) {
         }

         public void init(T mob, Blackboard<T> blackboard) {
         }

         public AINodeResult tick(T mob, Blackboard<T> blackboard) {
            long currentTime = mob.getWorldEntity().getTime();
            if (this.nextShowAttackTime <= currentTime) {
               mob.showAttack((int)(mob.x + this.direction.x * 100.0F), (int)(mob.y + this.direction.y * 100.0F), false);
               this.nextShowAttackTime = currentTime + 100L;
               if (mob.isServer()) {
                  mob.getLevel()
                     .getServer()
                     .network
                     .sendToClientsWithEntity(
                        new PacketMobAttack(mob, (int)(mob.x + this.direction.x * 100.0F), (int)(mob.y + this.direction.y * 100.0F), false), mob
                     );
               }
            }

            long timeSinceLastProjectile = currentTime - this.lastProjectileTime;

            while (this.projectilesFired < this.waves && timeSinceLastProjectile >= this.currentTimeGap) {
               this.lastProjectileTime = this.lastProjectileTime + this.currentTimeGap;
               timeSinceLastProjectile = currentTime - this.lastProjectileTime;
               Float perpDir = GameMath.getPerpendicularDir(this.direction.x, this.direction.y);
               if (this.reversed) {
                  perpDir = new Float(-perpDir.x, -perpDir.y);
               }

               float projectileWidth = 200.0F;
               float totalDistance = (projectileWidth + this.gapSize) * this.waves - this.gapSize;
               float currentDistance = (projectileWidth + this.gapSize) * this.projectilesFired + projectileWidth / 2.0F;
               float startBehindDistance = 500.0F;
               float startX = mob.x + perpDir.x * totalDistance / 2.0F - perpDir.x * currentDistance - this.direction.x * startBehindDistance;
               float startY = mob.y + perpDir.y * totalDistance / 2.0F - perpDir.y * currentDistance - this.direction.y * startBehindDistance;
               int range = 2000;
               mob.getLevel()
                  .entityManager
                  .projectiles
                  .add(
                     new FallenWizardWaveProjectile(
                        startX,
                        startY,
                        startX + this.direction.x * 100.0F,
                        startY + this.direction.y * 100.0F,
                        120.0F,
                        range,
                        FallenWizardMob.waveDamage,
                        50,
                        mob
                     )
                  );
               this.projectilesFired++;
               mob.magicBoltSound.runAndSend();
            }

            return this.projectilesFired < this.waves ? AINodeResult.RUNNING : AINodeResult.SUCCESS;
         }

         public void onStarted(T mob, Blackboard<T> blackboard) {
            Mob target = blackboard.getObject(Mob.class, "currentTarget");
            Float targetPos;
            if (target != null) {
               targetPos = new Float(target.x, target.y);
            } else if (mob.isInArena()) {
               targetPos = TempleArenaLevel.getBossPosition();
            } else {
               targetPos = new Float(mob.x, mob.y + 100.0F);
            }

            this.direction = GameMath.normalize(targetPos.x - mob.x, targetPos.y - mob.y);
            if (this.direction.x == 0.0F && this.direction.y == 0.0F) {
               this.direction = new Float(0.0F, 1.0F);
            }

            this.reversed = !this.reversed;
            this.nextShowAttackTime = 0L;
            this.projectilesFired = 0;
            this.currentTimeGap = FallenWizardAI.this.getHealthPercValue(mob, this.noHealthTimeGap, this.fullHealthTimeGap);
            int startDelay = FallenWizardAI.this.getHealthPercValue(mob, this.noHealthStartDelay, this.fullHealthStartDelay);
            this.lastProjectileTime = mob.getWorldEntity().getTime() - (this.currentTimeGap - startDelay);
            mob.humanSoundAbility.runAndSend(1);
         }

         public void onEnded(T mob, Blackboard<T> blackboard) {
         }
      }

      protected class DragonSpawnStage extends AINode<T> implements AttackStageInterface<T> {
         private Float direction;
         private int dragonsSpawned;
         private long lastSpawnTime;
         private long nextShowAttackTime;
         private int currentTimeGap;
         public int noHealthStartDelay;
         public int fullHealthStartDelay;
         public int noHealthTimeGap;
         public int fullHealthTimeGap;
         public int dragons;
         private float circlingOffset;

         public DragonSpawnStage(int noHealthStartDelay, int fullHealthStartDelay, int noHealthTimeGap, int fullHealthTimeGap, int dragons) {
            this.noHealthStartDelay = noHealthStartDelay;
            this.fullHealthStartDelay = fullHealthStartDelay;
            this.noHealthTimeGap = noHealthTimeGap;
            this.fullHealthTimeGap = fullHealthTimeGap;
            this.dragons = dragons;
         }

         protected void onRootSet(AINode<T> root, T mob, Blackboard<T> blackboard) {
         }

         public void init(T mob, Blackboard<T> blackboard) {
         }

         public AINodeResult tick(T mob, Blackboard<T> blackboard) {
            long currentTime = mob.getWorldEntity().getTime();
            if (this.nextShowAttackTime <= currentTime) {
               mob.showAttack((int)(mob.x + this.direction.x * 100.0F), (int)(mob.y + this.direction.y * 100.0F), false);
               this.nextShowAttackTime = currentTime + 100L;
               if (mob.isServer()) {
                  mob.getLevel()
                     .getServer()
                     .network
                     .sendToClientsWithEntity(
                        new PacketMobAttack(mob, (int)(mob.x + this.direction.x * 100.0F), (int)(mob.y + this.direction.y * 100.0F), false), mob
                     );
               }
            }

            for (long timeSinceLastProjectile = currentTime - this.lastSpawnTime;
               this.dragonsSpawned < this.dragons && timeSinceLastProjectile >= this.currentTimeGap;
               this.circlingOffset = this.circlingOffset + 360.0F / this.dragons
            ) {
               this.lastSpawnTime = this.lastSpawnTime + this.currentTimeGap;
               timeSinceLastProjectile = currentTime - this.lastSpawnTime;
               Float perpDir = GameMath.getPerpendicularDir(this.direction.x, this.direction.y);
               int perpDistance = GameRandom.globalRandom.getIntBetween(-150, 150);
               int behindDistance = GameRandom.globalRandom.getIntBetween(150, 300);
               float startX = mob.x + perpDir.x * perpDistance - this.direction.x * behindDistance;
               float startY = mob.y + perpDir.y * perpDistance - this.direction.y * behindDistance;
               this.dragonsSpawned++;
               mob.spawnDragonEffects.runAndSend((int)startX, (int)startY);
               FallenDragonHead dragon = (FallenDragonHead)MobRegistry.getMob("fallendragon", mob.getLevel());
               dragon.master = mob;
               dragon.circlingAngleOffset = this.circlingOffset;
               if (mob.isInArena()) {
                  dragon.centerPosition = TempleArenaLevel.getBossPosition();
               }

               mob.getLevel().entityManager.addMob(dragon, startX, startY);
            }

            if (this.dragonsSpawned < this.dragons) {
               return AINodeResult.RUNNING;
            }

            this.circlingOffset = this.circlingOffset + (360.0F / this.dragons / 2.0F + 18.0F);
            return AINodeResult.SUCCESS;
         }

         public void onStarted(T mob, Blackboard<T> blackboard) {
            Mob target = blackboard.getObject(Mob.class, "currentTarget");
            Float targetPos;
            if (target != null) {
               targetPos = new Float(target.x, target.y);
            } else if (mob.isInArena()) {
               targetPos = TempleArenaLevel.getBossPosition();
            } else {
               targetPos = new Float(mob.x, mob.y + 100.0F);
            }

            this.direction = GameMath.normalize(targetPos.x - mob.x, targetPos.y - mob.y);
            if (this.direction.x == 0.0F && this.direction.y == 0.0F) {
               this.direction = new Float(0.0F, 1.0F);
            }

            this.nextShowAttackTime = 0L;
            this.dragonsSpawned = 0;
            this.currentTimeGap = FallenWizardAI.this.getHealthPercValue(mob, this.noHealthTimeGap, this.fullHealthTimeGap);
            int startDelay = FallenWizardAI.this.getHealthPercValue(mob, this.noHealthStartDelay, this.fullHealthStartDelay);
            this.lastSpawnTime = mob.getWorldEntity().getTime() - (this.currentTimeGap - startDelay);
            mob.humanSoundAbility.runAndSend(1);
         }

         public void onEnded(T mob, Blackboard<T> blackboard) {
         }
      }

      protected class FindNewPosStage extends MoveTaskAINode<T> implements AttackStageInterface<T> {
         public boolean waitForArrive;
         private boolean hasStartedMoving;

         public FindNewPosStage(boolean waitForArrive) {
            this.waitForArrive = waitForArrive;
         }

         protected void onRootSet(AINode<T> root, T mob, Blackboard<T> blackboard) {
         }

         public void init(T mob, Blackboard<T> blackboard) {
         }

         public AINodeResult tickNode(T mob, Blackboard<T> blackboard) {
            if (!this.hasStartedMoving) {
               this.hasStartedMoving = true;
               Point pos = FallenWizardAI.this.findNewPosition(mob, blackboard);
               if (pos != null) {
                  return this.moveToTileTask(pos.x, pos.y, null, path -> {
                     path.move(null);
                     return null;
                  });
               }
            }

            if (this.waitForArrive && blackboard.mover.isCurrentlyMovingFor(this) && blackboard.mover.isMoving()) {
               mob.humanSoundAbility.runAndSend(0);
               return AINodeResult.RUNNING;
            } else {
               return AINodeResult.SUCCESS;
            }
         }

         public void onStarted(T mob, Blackboard<T> blackboard) {
            this.hasStartedMoving = false;
            mob.humanSoundAbility.runAndSend(2);
         }

         public void onEnded(T mob, Blackboard<T> blackboard) {
         }
      }

      protected class GhostFindNewPosStage extends AINode<T> implements AttackStageInterface<T> {
         private final LinkedList<Mob> ghostMobs = new LinkedList<>();

         public GhostFindNewPosStage() {
         }

         protected void onRootSet(AINode<T> root, T mob, Blackboard<T> blackboard) {
         }

         public void init(T mob, Blackboard<T> blackboard) {
         }

         public AINodeResult tick(T mob, Blackboard<T> blackboard) {
            ListIterator<Mob> li = this.ghostMobs.listIterator();
            Mob last = null;

            while (li.hasNext()) {
               Mob next = li.next();
               if (next.removed()) {
                  last = next;
                  li.remove();
               }
            }

            if (!this.ghostMobs.isEmpty()) {
               return AINodeResult.RUNNING;
            }

            if (last != null) {
               mob.teleportAbility.runAndSend(last.getX(), last.getY(), last.getDir(), true, false, true);
               mob.visibilityAbility.runAndSend(true, false);
               mob.humanSoundAbility.runAndSend(1);
            }

            return AINodeResult.SUCCESS;
         }

         public void onStarted(T mob, Blackboard<T> blackboard) {
            this.ghostMobs.clear();
            PriorityMap<Point> positions = FallenWizardAI.this.findNewPositions(mob, blackboard);
            ArrayList<Point> best = positions.getBestObjects(20);
            LinkedList<Point> lastPoints = new LinkedList<>();

            for (int i = 0; i < 4 && !best.isEmpty(); i++) {
               Point pos;
               if (lastPoints.isEmpty()) {
                  pos = best.stream().min(Comparator.comparingDouble(p -> p.distance(mob.x, mob.y))).get();
               } else {
                  int bestIndex = IntStream.range(0, best.size())
                     .boxed()
                     .max(Comparator.comparingDouble(n -> lastPoints.stream().mapToDouble(p -> best.get(n).distance(p)).sum()))
                     .orElse(-1);
                  pos = best.remove(bestIndex);
               }

               FallenWizardGhostMob newGhost = (FallenWizardGhostMob)MobRegistry.getMob("fallenwizardghost", mob.getLevel());
               mob.getLevel().entityManager.addMob(newGhost, mob.x, mob.y);
               newGhost.moveToPos(pos.x, pos.y);
               this.ghostMobs.add(newGhost);
               lastPoints.add(pos);
               mob.humanSoundAbility.runAndSend(2);
            }

            if (!this.ghostMobs.isEmpty()) {
               mob.visibilityAbility.runAndSend(false, false);
            }
         }

         public void onEnded(T mob, Blackboard<T> blackboard) {
         }
      }

      protected class RandomWaveProjectilesStage extends AINode<T> implements AttackStageInterface<T> {
         private Float lastDirection;
         private int projectilesFired;
         private long lastProjectileTime;
         private long nextShowAttackTime;
         private int currentTimeGap;
         public int noHealthStartDelay;
         public int fullHealthStartDelay;
         public int noHealthTimeGap;
         public int fullHealthTimeGap;
         public int waves;

         public RandomWaveProjectilesStage(int noHealthStartDelay, int fullHealthStartDelay, int noHealthTimeGap, int fullHealthTimeGap, int waves) {
            this.noHealthStartDelay = noHealthStartDelay;
            this.fullHealthStartDelay = fullHealthStartDelay;
            this.noHealthTimeGap = noHealthTimeGap;
            this.fullHealthTimeGap = fullHealthTimeGap;
            this.waves = waves;
         }

         protected void onRootSet(AINode<T> root, T mob, Blackboard<T> blackboard) {
         }

         public void init(T mob, Blackboard<T> blackboard) {
         }

         public AINodeResult tick(T mob, Blackboard<T> blackboard) {
            long currentTime = mob.getWorldEntity().getTime();
            if (this.lastDirection != null && this.nextShowAttackTime <= currentTime) {
               mob.showAttack((int)(mob.x + this.lastDirection.x * 100.0F), (int)(mob.y + this.lastDirection.y * 100.0F), false);
               this.nextShowAttackTime = currentTime + 100L;
               if (mob.isServer()) {
                  mob.getLevel()
                     .getServer()
                     .network
                     .sendToClientsWithEntity(
                        new PacketMobAttack(mob, (int)(mob.x + this.lastDirection.x * 100.0F), (int)(mob.y + this.lastDirection.y * 100.0F), false), mob
                     );
               }
            }

            long timeSinceLastProjectile = currentTime - this.lastProjectileTime;

            while (this.projectilesFired < this.waves && timeSinceLastProjectile >= this.currentTimeGap) {
               Mob target = blackboard.getObject(Mob.class, "currentTarget");
               this.lastProjectileTime = this.lastProjectileTime + this.currentTimeGap;
               timeSinceLastProjectile = currentTime - this.lastProjectileTime;
               if (target != null) {
                  this.lastDirection = GameMath.getAngleDir(GameRandom.globalRandom.nextInt(360));
                  Float perpDir = GameMath.getPerpendicularDir(this.lastDirection.x, this.lastDirection.y);
                  float perpDistance = GameRandom.globalRandom.getFloatBetween(-200.0F, 200.0F);
                  float startBehindDistance = 800.0F;
                  float startX = target.x + perpDir.x * perpDistance - this.lastDirection.x * startBehindDistance;
                  float startY = target.y + perpDir.y * perpDistance - this.lastDirection.y * startBehindDistance;
                  int range = 2300;
                  mob.getLevel()
                     .entityManager
                     .projectiles
                     .add(
                        new FallenWizardWaveProjectile(
                           startX,
                           startY,
                           startX + this.lastDirection.x * 100.0F,
                           startY + this.lastDirection.y * 100.0F,
                           120.0F,
                           range,
                           FallenWizardMob.waveDamage,
                           50,
                           mob
                        )
                     );
               }

               this.projectilesFired++;
               mob.magicBoltSound.runAndSend();
            }

            return this.projectilesFired < this.waves ? AINodeResult.RUNNING : AINodeResult.SUCCESS;
         }

         public void onStarted(T mob, Blackboard<T> blackboard) {
            this.nextShowAttackTime = 0L;
            this.projectilesFired = 0;
            this.currentTimeGap = FallenWizardAI.this.getHealthPercValue(mob, this.noHealthTimeGap, this.fullHealthTimeGap);
            int startDelay = FallenWizardAI.this.getHealthPercValue(mob, this.noHealthStartDelay, this.fullHealthStartDelay);
            this.lastProjectileTime = mob.getWorldEntity().getTime() - (this.currentTimeGap - startDelay);
            mob.humanSoundAbility.runAndSend(1);
         }

         public void onEnded(T mob, Blackboard<T> blackboard) {
         }
      }

      protected class ScepterProjectilesStage extends AINode<T> implements AttackStageInterface<T> {
         private Mob target;
         private long startTime;
         private int currentTimeSpan;
         private float projectileBuffer;
         private long nextShowAttackTime;
         public int projectiles;
         public int noHealthTimeSpan;
         public int fullHealthTimeSpan;

         public ScepterProjectilesStage(int projectiles, int noHealthTimeSpan, int fullHealthTimeSpan) {
            this.projectiles = projectiles;
            this.noHealthTimeSpan = noHealthTimeSpan;
            this.fullHealthTimeSpan = fullHealthTimeSpan;
         }

         protected void onRootSet(AINode<T> root, T mob, Blackboard<T> blackboard) {
         }

         public void init(T mob, Blackboard<T> blackboard) {
         }

         public AINodeResult tick(T mob, Blackboard<T> blackboard) {
            long currentTime = mob.getWorldEntity().getTime();
            if (currentTime >= this.startTime + this.currentTimeSpan) {
               return AINodeResult.SUCCESS;
            }

            if (this.target == null || this.target.removed()) {
               this.target = blackboard.getObject(Mob.class, "currentTarget");
            }

            if (this.target != null) {
               if (this.nextShowAttackTime <= currentTime) {
                  mob.showAttack(this.target.getX(), this.target.getY(), false);
                  this.nextShowAttackTime = currentTime + 100L;
                  if (mob.isServer()) {
                     mob.getLevel().getServer().network.sendToClientsWithEntity(new PacketMobAttack(mob, this.target.getX(), this.target.getY(), false), mob);
                  }
               }

               this.projectileBuffer = this.projectileBuffer + TickManager.getTickDelta(this.currentTimeSpan / 1000.0F / this.projectiles);
               if (this.projectileBuffer >= 1.0F) {
                  mob.laserBlastSound.runAndSend();

                  while (this.projectileBuffer >= 1.0F) {
                     this.projectileBuffer--;
                     int targetX = this.target.getX() + GameRandom.globalRandom.getIntBetween(-40, 40);
                     int targetY = this.target.getY() + GameRandom.globalRandom.getIntBetween(-40, 40);
                     Float dir = GameMath.normalize(targetX - mob.x, targetY - mob.y);
                     Float perpDir = GameMath.getPerpendicularDir(dir.x, dir.y);
                     int perpRange = GameRandom.globalRandom.getIntBetween(-20, 20);
                     int range = 1500;
                     FallenWizardScepterProjectile proj = new FallenWizardScepterProjectile(
                        mob.x + perpDir.x * perpRange,
                        mob.y + perpDir.y * perpRange,
                        mob.x - dir.x * 100.0F,
                        mob.y - dir.y * 100.0F,
                        250,
                        range,
                        FallenWizardMob.scepterDamage,
                        50,
                        mob
                     );
                     proj.setAngle(proj.getAngle() + GameRandom.globalRandom.getIntBetween(-140, 140));
                     proj.targetPos = new Point(targetX, targetY);
                     proj.turnSpeed = GameRandom.globalRandom.getFloatBetween(1.0F, 2.0F);
                     proj.angleLeftToTurn = 240.0F;
                     proj.resetUniqueID(GameRandom.globalRandom);
                     mob.getLevel().entityManager.projectiles.add(proj);
                  }
               }
            }

            return AINodeResult.RUNNING;
         }

         public void onStarted(T mob, Blackboard<T> blackboard) {
            this.target = null;
            this.startTime = mob.getWorldEntity().getTime();
            this.projectileBuffer = 0.0F;
            this.nextShowAttackTime = 0L;
            this.currentTimeSpan = FallenWizardAI.this.getHealthPercValue(mob, this.noHealthTimeSpan, this.fullHealthTimeSpan);
            mob.humanSoundAbility.runAndSend(1);
         }

         public void onEnded(T mob, Blackboard<T> blackboard) {
         }
      }
   }

   public static class PortalParticle extends Particle {
      public PortalParticle(Level level, int x, int y, long lifeTime) {
         super(level, x, y, 0.0F, 0.0F, lifeTime);
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
            int drawX = camera.getDrawX(this.x);
            int drawY = camera.getDrawY(this.y);
            long lifeCycleTime = this.getLifeCycleTime();
            float alpha = 1.0F;
            if (lifeCycleTime >= this.lifeTime - 500L) {
               float fadeOutPerc = GameMath.limit((float)(lifeCycleTime - (this.lifeTime - 500L)) / 500.0F, 0.0F, 1.0F);
               alpha = Math.abs(fadeOutPerc - 1.0F);
            }

            float angle = (float)lifeCycleTime / 2.0F;
            final TextureDrawOptions drawOptions = GameResources.fallenWizardPortalParticle
               .initDraw()
               .light(light)
               .alpha(alpha)
               .rotate(-angle)
               .posMiddle(drawX, drawY - 15);
            list.add(new EntityDrawable(this) {
               @Override
               public void draw(TickManager tickManager) {
                  drawOptions.draw();
               }
            });
         }
      }
   }

   public static class TeleportParticle extends Particle {
      private final Point animSprite;
      private int dir;

      public TeleportParticle(Level level, int x, int y, FallenWizardMob owner) {
         super(level, x, y, 0.0F, 0.0F, 1000L);
         this.animSprite = owner.getAnimSprite(x, y, this.dir);
         this.dir = owner.getDir();
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
         float life = this.getLifeCyclePercent();
         if (!this.removed()) {
            GameLight light = level.getLightLevel(this);
            int drawX = camera.getDrawX(this.x) - 48;
            int drawY = camera.getDrawY(this.y) - 70;
            float alpha = Math.abs(life - 1.0F);
            final DrawOptions drawOptions = new HumanDrawOptions(level, MobRegistry.Textures.fallenWizard)
               .sprite(this.animSprite, 96)
               .size(96, 96)
               .dir(this.dir)
               .light(light)
               .alpha(alpha)
               .pos(drawX, drawY);
            list.add(new EntityDrawable(this) {
               @Override
               public void draw(TickManager tickManager) {
                  drawOptions.draw();
               }
            });
         }
      }
   }
}
