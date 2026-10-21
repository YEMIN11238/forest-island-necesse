package necesse.entity.mobs.hostile;

import java.awt.Color;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Point2D.Float;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Stream;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.gameLoop.tickManager.TicksPerSecond;
import necesse.engine.modifiers.ModifierValue;
import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.engine.network.server.Server;
import necesse.engine.network.server.ServerClient;
import necesse.engine.registries.MobRegistry;
import necesse.engine.registries.TileRegistry;
import necesse.engine.sound.SoundEffect;
import necesse.engine.sound.SoundManager;
import necesse.engine.sound.SoundPlayer;
import necesse.engine.sound.SoundSettings;
import necesse.engine.util.GameMath;
import necesse.engine.util.GameRandom;
import necesse.engine.util.GameUtils;
import necesse.engine.util.gameAreaSearch.GameAreaStream;
import necesse.engine.util.tween.Easings;
import necesse.entity.ParticleTypeSwitcher;
import necesse.entity.mobs.Attacker;
import necesse.entity.mobs.GameDamage;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.MobDrawable;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.ai.behaviourTree.AINodeResult;
import necesse.entity.mobs.ai.behaviourTree.BehaviourTreeAI;
import necesse.entity.mobs.ai.behaviourTree.Blackboard;
import necesse.entity.mobs.ai.behaviourTree.composites.SelectorAINode;
import necesse.entity.mobs.ai.behaviourTree.composites.SequenceAINode;
import necesse.entity.mobs.ai.behaviourTree.leaves.ChargingCirclingChaserAINode;
import necesse.entity.mobs.ai.behaviourTree.leaves.TargetFinderAINode;
import necesse.entity.mobs.ai.behaviourTree.leaves.WandererAINode;
import necesse.entity.mobs.ai.behaviourTree.util.FlyingAIMover;
import necesse.entity.mobs.ai.behaviourTree.util.TargetFinderDistance;
import necesse.entity.mobs.ai.behaviourTree.util.WandererBaseOptions;
import necesse.entity.mobs.buffs.BuffModifiers;
import necesse.entity.particle.Particle;
import necesse.entity.projectile.PhoenixFeatherProjectile;
import necesse.entity.projectile.Projectile;
import necesse.entity.trails.Trail;
import necesse.entity.trails.TrailVector;
import necesse.gfx.GameResources;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.texture.TextureDrawOptions;
import necesse.gfx.drawables.OrderableDrawables;
import necesse.inventory.lootTable.LootTable;
import necesse.level.maps.Level;
import necesse.level.maps.TilePosition;
import necesse.level.maps.biomes.Biome;
import necesse.level.maps.light.GameLight;

public class PhoenixMob extends FlyingHostileMob {
   public ParticleTypeSwitcher particleTypes = new ParticleTypeSwitcher(Particle.GType.COSMETIC, Particle.GType.IMPORTANT_COSMETIC, Particle.GType.COSMETIC);
   public static GameDamage damage = new GameDamage(115.0F);
   public final float featherCooldown = 500.0F;
   public float featherBuffer;
   public Trail trail;
   public SoundPlayer windSound;
   public TicksPerSecond particleTicks = TicksPerSecond.ticksPerSecond(20);

   public PhoenixMob() {
      super(1500);
      this.moveAccuracy = 80;
      this.spawnLightThreshold = new ModifierValue<>(BuffModifiers.MOB_SPAWN_LIGHT_THRESHOLD, 0).min(150, Integer.MAX_VALUE);
      this.setSpeed(150.0F);
      this.setArmor(60);
      this.accelerationMod = 0.5F;
      this.decelerationMod = 1.0F;
      this.setKnockbackModifier(0.0F);
      this.collision = new Rectangle(-25, -25, 50, 50);
      this.hitBox = new Rectangle(-30, -30, 60, 60);
      this.selectBox = new Rectangle(-40, -40, 80, 80);
   }

   @Override
   public boolean isValidSpawnLocation(Server server, ServerClient client, int targetX, int targetY) {
      return super.isValidSpawnLocation(server, client, targetX, targetY);
   }

   @Override
   public void setupMovementPacket(PacketWriter writer) {
      super.setupMovementPacket(writer);
      writer.putNextInt(this.moveAccuracy);
   }

   @Override
   public void applyMovementPacket(PacketReader reader, boolean isDirect) {
      super.applyMovementPacket(reader, isDirect);
      this.moveAccuracy = reader.getNextInt();
   }

   @Override
   public void init() {
      super.init();
      this.ai = new BehaviourTreeAI<>(this, new PhoenixMob.PhoenixAI<>(), new FlyingAIMover());
      if (this.isClient()) {
         this.trail = new Trail(this, this.getLevel(), new Color(255, 175, 50), 24.0F, 1000, 20.0F);
         this.trail.drawOnTop = true;
         this.trail.removeOnFadeOut = false;
         this.getLevel().entityManager.addTrail(this.trail);
      }
   }

   @Override
   public void tickMovement(float delta) {
      super.tickMovement(delta);
      this.featherBuffer += delta;
      if (this.isServer()) {
         if (this.featherBuffer > 500.0F) {
            Point projectileStartPos = this.getRandomPointOnWingspan();
            Projectile p = new PhoenixFeatherProjectile(
               this.getLevel(),
               this,
               projectileStartPos.x,
               projectileStartPos.y,
               projectileStartPos.x - this.dx * 5.0F,
               projectileStartPos.y - this.dy * 5.0F,
               10.0F,
               64,
               damage
            );
            this.getLevel().entityManager.projectiles.add(p);
            this.featherBuffer -= 500.0F;
         }
      } else if (this.trail != null) {
         this.trail.addPoint(new TrailVector(this.x, this.y, this.dx, this.dy, this.trail.thickness, 20.0F));
      }
   }

   @Override
   public GameDamage getCollisionDamage(Mob target, boolean fromPacket, ServerClient packetSubmitter) {
      return damage;
   }

   @Override
   public int stoppingDistance(float friction, float currentSpeed) {
      return super.stoppingDistance(friction, currentSpeed) / 3;
   }

   @Override
   public void clientTick() {
      super.clientTick();
      float difficulty = Easings.CubicOut.ease(1.0F - this.getHealthPercent());
      this.setSpeed(GameMath.lerp(difficulty, 150, 400));
      this.accelerationMod = GameMath.lerp(this.getHealthPercent(), 0.5F, 1.0F);
      this.particleTicks.gameTick();

      while (this.particleTicks.shouldTick()) {
         this.getLevel()
            .entityManager
            .addParticle(this.getRandomPointOnWingspan().x, this.getRandomPointOnWingspan().y, this.particleTypes.next())
            .movesConstant(this.dx / 5.0F + GameRandom.globalRandom.floatGaussian() * 4.0F, this.dy / 5.0F + GameRandom.globalRandom.floatGaussian() * 4.0F)
            .color(new Color(255, 175, 50))
            .ignoreLight(true)
            .givesLight(35.0F, 0.8F)
            .height(20.0F)
            .lifeTime(1000);
      }

      if (this.windSound == null || this.windSound.isDone()) {
         this.windSound = SoundManager.playSound(GameResources.wind1, SoundEffect.effect(this).falloffDistance(1000).volume(0.4F));
      }

      if (this.windSound != null) {
         this.windSound.refreshLooping(1.0F);
      }
   }

   @Override
   public void serverTick() {
      super.serverTick();
      float difficulty = Easings.CubicOut.ease(1.0F - this.getHealthPercent());
      this.setSpeed(GameMath.lerp(difficulty, 150, 400));
      this.accelerationMod = GameMath.lerp(this.getHealthPercent(), 0.5F, 1.0F);
      if (this.getLevel().getTileID(this.getTileX(), this.getTileY()) == TileRegistry.ashID) {
         this.spawnTilePosition = new Point(this.getTileX(), this.getTileY());
      }
   }

   private Point getRandomPointOnWingspan() {
      int wingspanWidth = 100;
      float angle = GameMath.getAngle(new Float(this.dx, this.dy)) + 90.0F;
      float offset = GameRandom.globalRandom.getFloatBetween(-0.5F, 0.5F) * wingspanWidth;
      int offsetX = (int)(GameMath.cos(angle) * offset);
      int offsetY = (int)(GameMath.sin(angle) * offset);
      return new Point((int)this.x + offsetX, (int)this.y + offsetY);
   }

   @Override
   public int getTileWanderPriority(TilePosition pos, Biome baseBiome) {
      return pos.tileID() == TileRegistry.ashID ? 1000 : super.getTileWanderPriority(pos, baseBiome);
   }

   @Override
   protected void onDeath(Attacker attacker, HashSet<Attacker> attackers) {
      super.onDeath(attacker, attackers);
      if (this.isServer()) {
         PhoenixEggMob mob = new PhoenixEggMob();
         if (this.spawnTilePosition != null) {
            mob.setSpawnTilePosition(this.spawnTilePosition.x, this.spawnTilePosition.y);
         }

         this.getLevel().entityManager.addMob(mob, this.getX(), this.getY());
      }
   }

   @Override
   public void spawnDeathParticles(float knockbackX, float knockbackY) {
      GameRandom random = GameRandom.globalRandom;
      float anglePerParticle = 36.0F;

      for (int i = 0; i < 30; i++) {
         int angle = (int)(i * anglePerParticle + random.nextFloat() * anglePerParticle);
         float dx = (float)Math.sin(Math.toRadians(angle)) * GameRandom.globalRandom.getFloatBetween(20.0F, 40.0F);
         float dy = (float)Math.cos(Math.toRadians(angle)) * GameRandom.globalRandom.getFloatBetween(20.0F, 40.0F);
         this.getLevel()
            .entityManager
            .addParticle(this, Particle.GType.IMPORTANT_COSMETIC)
            .sprite(GameResources.puffParticles.sprite(random.nextInt(5), 0, 12))
            .sizeFades(12, 24)
            .movesFriction(dx * 2.0F, dy * 2.0F, 0.8F)
            .color(new Color(255, 175, 50))
            .heightMoves(15.0F, 45.0F)
            .lifeTime(1500);
      }
   }

   @Override
   protected SoundSettings getHitDeathSound() {
      return new SoundSettings(GameResources.fireShot).volume(1.0F).fallOffDistance(1800).basePitch(1.0F);
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
      GameLight light = level.getLightLevel(getTileCoordinate(x), getTileCoordinate(y)).minLevelCopy(150.0F);
      int drawX = camera.getDrawX(x) - 64;
      int drawY = camera.getDrawY(y) - 48 - 16;
      float angle = GameMath.fixAngle(GameMath.getAngle(new Float(this.dx, this.dy)) - 90.0F);
      float bobbing = GameUtils.getBobbing(this.getTime(), 1000) * 5.0F;
      drawY = (int)(drawY + bobbing);
      drawY += level.getTile(getTileCoordinate(x), getTileCoordinate(y)).getMobSinkingAmount(this);
      long animDistanceRan = (long)this.getDistanceRan() * 2L;
      int anim = GameUtils.getAnim(animDistanceRan, 4, 400);
      TextureDrawOptions shadow = MobRegistry.Textures.phoenix_shadow.initDraw().sprite(anim, 0, 128, 96).rotate(angle).light(light).pos(drawX, drawY + 16);
      topList.add(tm -> shadow.draw());
      final TextureDrawOptions drawOptions = MobRegistry.Textures.phoenix
         .initDraw()
         .sprite(anim, 0, 128, 96)
         .rotate(angle)
         .startGlowOptions(level, this.getID())
         .light(light)
         .applyEnemyTracker(this, perspective)
         .pos(drawX, drawY);
      topList.add(new MobDrawable() {
         @Override
         public void draw(TickManager tickManager) {
            drawOptions.draw();
         }
      });
   }

   @Override
   public void dispose() {
      super.dispose();
      if (this.trail != null) {
         this.trail.removeOnFadeOut = true;
      }
   }

   @Override
   public Stream<ModifierValue<?>> getDefaultModifiers() {
      return Stream.of(new ModifierValue<>(BuffModifiers.FIRE_DAMAGE, 0.0F).max(0.0F));
   }

   @Override
   public LootTable showAdditionalLootTableInJournal() {
      return PhoenixEggMob.lootTable;
   }

   public static class PhoenixAI<T extends PhoenixMob> extends SelectorAINode<T> {
      public final TargetFinderAINode<T> targetFinder;
      public final ChargingCirclingChaserAINode<T> chaser;

      public PhoenixAI() {
         SequenceAINode<T> chaserSequence = new SequenceAINode<>();
         chaserSequence.addChild(this.targetFinder = new TargetFinderAINode<T>(768) {
            public GameAreaStream<? extends Mob> streamPossibleTargets(T mob, Point base, TargetFinderDistance<T> distance) {
               return TargetFinderAINode.streamPlayersAndHumans(mob, base, distance);
            }

            public Point getBase(T mob, Blackboard<T> blackboard) {
               return mob.spawnTilePosition == null ? null : new Point(mob.spawnTilePosition.x * 32 + 16, mob.spawnTilePosition.y * 32 + 16);
            }
         });
         this.targetFinder.distance.targetLostAddedDistance = 320;
         chaserSequence.addChild(this.chaser = new ChargingCirclingChaserAINode<>(500, 20));
         this.chaser.offsetChasingBy = 100;
         this.addChild(chaserSequence);
         WandererAINode<T> wanderer = new WandererAINode<T>(0) {
            public AINodeResult tick(T mob, Blackboard<T> blackboard) {
               if (mob.hasArrivedAtTarget()) {
                  this.findNewPosition(mob, this.getBaseOptions());
               }

               return super.tick(mob, blackboard);
            }

            @Override
            public WandererBaseOptions<T> getBaseOptions() {
               return new WandererBaseOptions<T>() {
                  public Point getBaseTile(T mob) {
                     return mob.spawnTilePosition;
                  }

                  public boolean forceFindAroundBase(T mob) {
                     return true;
                  }
               };
            }
         };
         wanderer.searchRadius = 10;
         this.addChild(wanderer);
      }
   }
}
