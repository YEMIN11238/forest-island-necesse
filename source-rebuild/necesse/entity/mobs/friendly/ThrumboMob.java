package necesse.entity.mobs.friendly;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.HashSet;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Stream;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.modifiers.ModifierValue;
import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.engine.registries.MobRegistry;
import necesse.engine.sound.SoundSettings;
import necesse.engine.util.GameRandom;
import necesse.engine.util.gameAreaSearch.GameAreaStream;
import necesse.entity.mobs.Attacker;
import necesse.entity.mobs.DeathMessageTable;
import necesse.entity.mobs.GameDamage;
import necesse.entity.mobs.MaskShaderOptions;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.MobDrawable;
import necesse.entity.mobs.MobWasHitEvent;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.ability.BooleanMobAbility;
import necesse.entity.mobs.ai.behaviourTree.BehaviourTreeAI;
import necesse.entity.mobs.ai.behaviourTree.trees.CollisionChaserWandererAI;
import necesse.entity.mobs.ai.behaviourTree.util.TargetFinderDistance;
import necesse.entity.mobs.buffs.BuffModifiers;
import necesse.entity.particle.FleshParticle;
import necesse.entity.particle.Particle;
import necesse.gfx.GameResources;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.DrawOptions;
import necesse.gfx.drawables.OrderableDrawables;
import necesse.inventory.lootTable.LootTable;
import necesse.inventory.lootTable.lootItem.LootItem;
import necesse.level.gameObject.GameObject;
import necesse.level.maps.Level;
import necesse.level.maps.LevelObject;
import necesse.level.maps.light.GameLight;

public class ThrumboMob extends FriendlyMob {
   public static LootTable lootTable = new LootTable(new LootItem("thrumbohorn"));
   public static GameDamage damage = new GameDamage(150.0F);
   private final HashSet<Mob> targets = new HashSet<>();
   public final BooleanMobAbility setHostileAbility;

   public ThrumboMob() {
      super(1200);
      this.setArmor(60);
      this.updateStats();
      this.setFriction(3.0F);
      this.setKnockbackModifier(0.1F);
      this.prioritizeVerticalDir = false;
      this.collision = new Rectangle(-25, -15, 50, 30);
      this.hitBox = new Rectangle(-30, -20, 60, 40);
      this.selectBox = new Rectangle(-40, -90, 80, 110);
      this.swimMaskMove = 20;
      this.swimMaskOffset = -55;
      this.swimSinkOffset = -5;
      this.setHostileAbility = this.registerAbility(new BooleanMobAbility() {
         @Override
         protected void run(boolean value) {
            ThrumboMob.this.isHostile = value;
            ThrumboMob.this.updateStats();
         }
      });
   }

   @Override
   public Point getPathMoveOffset() {
      return new Point(32, 32);
   }

   @Override
   public void applySpawnPacket(PacketReader reader) {
      super.applySpawnPacket(reader);
      this.isHostile = reader.getNextBoolean();
      this.updateStats();
   }

   @Override
   public void setupSpawnPacket(PacketWriter writer) {
      super.setupSpawnPacket(writer);
      writer.putNextBoolean(this.isHostile);
   }

   public void updateStats() {
      this.setSpeed(this.isHostile ? 50.0F : 15.0F);
      this.setCombatRegen(this.isHostile ? 0.0F : this.getMaxHealth() / 100.0F);
   }

   @Override
   public void init() {
      super.init();
      CollisionChaserWandererAI<ThrumboMob> collisionChaserWandererAI = new CollisionChaserWandererAI<ThrumboMob>(null, 640, damage, 100, 40000) {
         public GameAreaStream<Mob> streamPossibleTargets(ThrumboMob mob, Point base, TargetFinderDistance<ThrumboMob> distance) {
            return distance.streamMobsAndPlayersInRange(base, mob).filter(ThrumboMob.this.targets::contains);
         }
      };
      collisionChaserWandererAI.collisionChaserAI.targetFinderAINode.loseTargetMinCooldown = 1000;
      collisionChaserWandererAI.collisionChaserAI.targetFinderAINode.loseTargetMaxCooldown = 2000;
      collisionChaserWandererAI.collisionChaserAI.targetFinderAINode.noTargetFoundMinCooldown = 250;
      collisionChaserWandererAI.collisionChaserAI.targetFinderAINode.noTargetFoundMaxCooldown = 500;
      collisionChaserWandererAI.collisionChaserAI.collisionChaserAINode.minPathFailedWaitTime = 500;
      collisionChaserWandererAI.collisionChaserAI.collisionChaserAINode.maxPathFailedWaitTime = 1000;
      this.ai = new BehaviourTreeAI<>(this, collisionChaserWandererAI);
   }

   @Override
   public Stream<ModifierValue<?>> getDefaultModifiers() {
      return this.isHostile
         ? Stream.concat(super.getDefaultModifiers(), Stream.of(new ModifierValue<>(BuffModifiers.CAN_BREAK_OBJECTS, true)))
         : super.getDefaultModifiers();
   }

   @Override
   public double getPathBreakDownCost(Level level, int tileX, int tileY, GameObject object) {
      return 5.0;
   }

   @Override
   public int getPathBreakDownDamage(LevelObject lo) {
      return super.getPathBreakDownDamage(lo) * 6;
   }

   @Override
   public void serverTick() {
      super.serverTick();
      this.targets.removeIf(m -> m.removed() || !m.isSamePlace(this) || m.getDistance(this) > 640.0F);
      this.setHostile(!this.targets.isEmpty());
   }

   @Override
   public MobWasHitEvent isServerHit(GameDamage damage, float x, float y, float knockback, Attacker attacker) {
      MobWasHitEvent out = super.isServerHit(damage, x, y, knockback, attacker);
      if (out != null && !out.wasPrevented && attacker != null) {
         Mob attackOwner = attacker.getAttackOwner();
         if (attackOwner != null) {
            this.targets.add(attackOwner);
         }
      }

      return out;
   }

   @Override
   public LootTable getLootTable() {
      return lootTable;
   }

   @Override
   public DeathMessageTable getDeathMessages() {
      return this.getDeathMessages("thrumbo", 3);
   }

   @Override
   public void spawnDeathParticles(float knockbackX, float knockbackY) {
      for (int i = 0; i < 4; i++) {
         this.getLevel()
            .entityManager
            .addParticle(
               new FleshParticle(this.getLevel(), MobRegistry.Textures.thrumbo, i, 16, 32, this.x, this.y, 20.0F, knockbackX, knockbackY),
               Particle.GType.IMPORTANT_COSMETIC
            );
      }
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
      int spriteSize = 128;
      int drawX = camera.getDrawX(x) - spriteSize / 2;
      int drawY = camera.getDrawY(y) - spriteSize + 28;
      int dir = this.getDir();
      Point sprite = this.getAnimSprite(x, y, dir);
      drawY += this.getBobbing(x, y);
      drawY += level.getTile(getTileCoordinate(x), getTileCoordinate(y)).getMobSinkingAmount(this);
      final MaskShaderOptions swimMask = this.getSwimMaskShaderOptions(this.inLiquidFloat(x, y));
      final DrawOptions options = MobRegistry.Textures.thrumbo
         .initDraw()
         .sprite(sprite.x, sprite.y, spriteSize)
         .addMaskShader(swimMask)
         .startGlowOptions(level, this.getID())
         .light(light)
         .applyEnemyTracker(this, perspective)
         .pos(drawX, drawY);
      DrawOptions shadowOptions = MobRegistry.Textures.thrumbo_shadow.initDraw().sprite(0, sprite.y, spriteSize).light(light).pos(drawX, drawY);
      list.add(new MobDrawable() {
         @Override
         public void draw(TickManager tickManager) {
            swimMask.use();
            options.draw();
            swimMask.stop();
         }
      });
      tileList.add(tm -> shadowOptions.draw());
   }

   @Override
   public int getRockSpeed() {
      return 10;
   }

   public void setHostile(boolean hostile) {
      if (this.getLevel() != null && this.getLevel().getServer() != null) {
         if (this.isHostile != hostile) {
            this.setHostileAbility.runAndSend(hostile);
         }
      }
   }

   @Override
   protected SoundSettings getHurtSound() {
      return new SoundSettings(GameRandom.globalRandom.getOneOf(GameResources.thrumboHurt1, GameResources.thrumboHurt2))
         .fallOffDistance(2000)
         .volume(0.6F)
         .basePitch(1.2F)
         .pitchVariance(0.1F);
   }

   @Override
   protected SoundSettings getDeathSound() {
      return new SoundSettings(GameResources.thrumboDeath).fallOffDistance(2000).volume(1.1F).pitchVariance(0.1F);
   }
}
