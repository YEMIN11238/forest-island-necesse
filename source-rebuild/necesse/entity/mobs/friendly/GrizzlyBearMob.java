package necesse.entity.mobs.friendly;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.List;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.engine.registries.MobRegistry;
import necesse.engine.save.LoadData;
import necesse.engine.save.SaveData;
import necesse.engine.sound.SoundManager;
import necesse.engine.sound.SoundPlayer;
import necesse.engine.sound.SoundSettings;
import necesse.engine.util.GameMath;
import necesse.entity.ParticleTypeSwitcher;
import necesse.entity.mobs.Attacker;
import necesse.entity.mobs.DeathMessageTable;
import necesse.entity.mobs.GameDamage;
import necesse.entity.mobs.MaskShaderOptions;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.MobDrawable;
import necesse.entity.mobs.MobWasHitEvent;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.ability.BooleanMobAbility;
import necesse.entity.mobs.ai.behaviourTree.AINode;
import necesse.entity.mobs.ai.behaviourTree.AINodeResult;
import necesse.entity.mobs.ai.behaviourTree.BehaviourTreeAI;
import necesse.entity.mobs.ai.behaviourTree.Blackboard;
import necesse.entity.mobs.ai.behaviourTree.composites.SelectorAINode;
import necesse.entity.mobs.ai.behaviourTree.leaves.WandererAINode;
import necesse.entity.mobs.ai.behaviourTree.trees.CollisionPlayerChaserAI;
import necesse.entity.mobs.ai.behaviourTree.util.TargetFinderDistance;
import necesse.entity.mobs.hostile.HostileMob;
import necesse.entity.particle.FleshParticle;
import necesse.entity.particle.Particle;
import necesse.gfx.GameResources;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.DrawOptions;
import necesse.gfx.drawOptions.texture.TextureDrawOptions;
import necesse.gfx.drawables.OrderableDrawables;
import necesse.gfx.gameTexture.GameTexture;
import necesse.inventory.lootTable.LootTable;
import necesse.inventory.lootTable.lootItem.LootItem;
import necesse.level.maps.Level;
import necesse.level.maps.light.GameLight;

public class GrizzlyBearMob extends HostileMob {
   public static LootTable lootTable = new LootTable(LootItem.between("furfish", 15, 25), LootItem.between("honey", 10, 15));
   public int particleBuffer;
   public boolean isSleeping;
   public Point baseTile;
   public BooleanMobAbility setIsSleeping;
   protected SoundPlayer sleepSoundPlayer;
   public ParticleTypeSwitcher sleepTypeSwitcher = new ParticleTypeSwitcher(Particle.GType.CRITICAL, Particle.GType.IMPORTANT_COSMETIC, Particle.GType.COSMETIC);

   public GrizzlyBearMob() {
      super(750);
      this.setArmor(20);
      this.setSpeed(40.0F);
      this.setCombatRegen(5.0F);
      this.setFriction(3.0F);
      this.setKnockbackModifier(0.2F);
      this.prioritizeVerticalDir = false;
      this.isSleeping = true;
      this.collision = new Rectangle(-10, -7, 20, 14);
      this.hitBox = new Rectangle(-20, -16, 40, 32);
      this.selectBox = new Rectangle(-20, -50, 40, 55);
      this.swimMaskMove = 32;
      this.swimMaskOffset = -55;
      this.swimSinkOffset = -8;
      this.setIsSleeping = this.registerAbility(new BooleanMobAbility() {
         @Override
         protected void run(boolean value) {
            GrizzlyBearMob.this.isSleeping = value;
         }
      });
   }

   @Override
   public void addSaveData(SaveData save) {
      super.addSaveData(save);
      save.addBoolean("isSleeping", this.isSleeping);
      save.addPoint("baseTile", this.baseTile);
   }

   @Override
   public void applyLoadData(LoadData save) {
      super.applyLoadData(save);
      this.isSleeping = save.getBoolean("isSleeping", true, false);
      this.baseTile = save.getPoint("baseTile", new Point(this.getTileX(), this.getTileY()), false);
   }

   @Override
   public void setupSpawnPacket(PacketWriter writer) {
      super.setupSpawnPacket(writer);
      writer.putNextBoolean(this.isSleeping);
   }

   @Override
   public void applySpawnPacket(PacketReader reader) {
      super.applySpawnPacket(reader);
      this.isSleeping = reader.getNextBoolean();
   }

   @Override
   public void init() {
      super.init();
      if (this.baseTile == null) {
         this.baseTile = new Point(this.getTileX(), this.getTileY());
      }

      this.ai = new BehaviourTreeAI<>(this, new GrizzlyBearMob.GrizzlyAI<>(this.isSleeping));
   }

   @Override
   public void dispose() {
      if (this.sleepSoundPlayer != null) {
         this.sleepSoundPlayer.stop();
      }

      super.dispose();
   }

   @Override
   public void clientTick() {
      super.clientTick();
      if (this.isSleeping) {
         this.particleBuffer++;
         if (this.particleBuffer >= 10) {
            int dir = this.getDir();
            int xOffset = dir == 1 ? 30 : (dir == 3 ? -30 : 0);
            int yOffset = dir == 0 ? -30 : 0;
            float var10001 = this.x + xOffset;
            this.getLevel()
               .entityManager
               .addTopParticle(var10001, this.y + yOffset, this.sleepTypeSwitcher.next())
               .movesConstant(-5.0F, -5.0F)
               .sprite(GameResources.sleepParticles.sprite(0, 0, 20))
               .dontRotate()
               .lifeTime(2000)
               .sizeFades(10, 20);
            this.particleBuffer -= 10;
         }

         if (this.sleepSoundPlayer == null || this.sleepSoundPlayer.isDone()) {
            this.sleepSoundPlayer = SoundManager.playSound(new SoundSettings(GameResources.grizzlySleep).volume(0.1F), this);
         }

         if (this.sleepSoundPlayer != null) {
            this.sleepSoundPlayer.refreshLooping(0.2F);
         }
      }
   }

   @Override
   public MobWasHitEvent isServerHit(GameDamage damage, float x, float y, float knockback, Attacker attacker) {
      MobWasHitEvent out = super.isServerHit(damage, x, y, knockback, attacker);
      if (out != null && !out.wasPrevented) {
         this.setIsSleeping.runAndSend(false);
      }

      return out;
   }

   @Override
   public LootTable getLootTable() {
      return lootTable;
   }

   @Override
   public void playAmbientSound() {
      if (!this.isSleeping) {
         super.playAmbientSound();
      }
   }

   @Override
   protected SoundSettings getAmbientSound() {
      return new SoundSettings(GameResources.grizzlyAmbient).volume(0.2F);
   }

   @Override
   protected SoundSettings getHurtSound() {
      return new SoundSettings(GameResources.grizzlyHurt).volume(1.2F);
   }

   @Override
   protected SoundSettings getDeathSound() {
      return new SoundSettings(GameResources.grizzlyDeath).volume(1.3F);
   }

   @Override
   public DeathMessageTable getDeathMessages() {
      return this.getDeathMessages("grizzly", 3);
   }

   @Override
   public void spawnDeathParticles(float knockbackX, float knockbackY) {
      for (int i = 0; i < 4; i++) {
         this.getLevel()
            .entityManager
            .addParticle(
               new FleshParticle(this.getLevel(), MobRegistry.Textures.grizzlyBear, i, 16, 32, this.x, this.y, 20.0F, knockbackX, knockbackY),
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
      int drawX = camera.getDrawX(x) - 64;
      int drawY = camera.getDrawY(y) - 128 + 36;
      int dir = this.getDir();
      Point sprite = this.getAnimSprite(x, y, dir);
      drawY += this.getBobbing(x, y);
      drawY += level.getTile(getTileCoordinate(x), getTileCoordinate(y)).getMobSinkingAmount(this);
      final MaskShaderOptions swimMask = this.getSwimMaskShaderOptions(this.inLiquidFloat(x, y));
      final DrawOptions options = MobRegistry.Textures.grizzlyBear
         .initDraw()
         .sprite(this.isSleeping ? 6 : sprite.x, sprite.y, 128)
         .addMaskShader(swimMask)
         .startGlowOptions(level, this.getID())
         .light(light)
         .applyEnemyTracker(this, perspective)
         .pos(drawX, drawY);
      list.add(new MobDrawable() {
         @Override
         public void draw(TickManager tickManager) {
            swimMask.use();
            options.draw();
            swimMask.stop();
         }
      });
      this.addShadowDrawables(tileList, level, x, y, light, camera);
   }

   @Override
   protected TextureDrawOptions getShadowDrawOptions(Level level, int x, int y, GameLight light, GameCamera camera) {
      GameTexture shadowTexture = MobRegistry.Textures.grizzlyBear_shadow;
      int drawX = camera.getDrawX(x) - 64;
      int drawY = camera.getDrawY(y) - 128 + 36;
      drawY += this.getBobbing(x, y);
      return shadowTexture.initDraw().sprite(0, this.getDir(), 128).light(light).pos(drawX, drawY);
   }

   @Override
   public int getRockSpeed() {
      return 10;
   }

   public static class GrizzlyAI<T extends GrizzlyBearMob> extends SelectorAINode<T> {
      public final CollisionPlayerChaserAI<T> chaserAINode;
      private final int sleepingSearch = 96;
      private final int awakeSearch = 384;

      public GrizzlyAI(boolean initSleeping) {
         this.addChild(new AINode<T>() {
            protected void onRootSet(AINode<T> root, T mob, Blackboard<T> blackboard) {
            }

            public void init(T mob, Blackboard<T> blackboard) {
            }

            public AINodeResult tick(T mob, Blackboard<T> blackboard) {
               Mob currentTarget = blackboard.getObject(Mob.class, "currentTarget");
               if (mob.isSleeping) {
                  if (currentTarget != null) {
                     mob.setIsSleeping.runAndSend(false);
                     GrizzlyAI.this.chaserAINode.targetFinderAINode.distance = new TargetFinderDistance<>(384);
                  }
               } else if (currentTarget == null) {
                  Point baseTile = mob.baseTile;
                  if (GameMath.diamondDistance(mob.getTileX(), mob.getTileY(), baseTile.x, baseTile.y) < 5.0F) {
                     blackboard.mover.stopMoving(mob);
                     mob.setIsSleeping.runAndSend(true);
                     GrizzlyAI.this.chaserAINode.targetFinderAINode.distance = new TargetFinderDistance<>(96);
                  }
               }

               return AINodeResult.FAILURE;
            }
         });
         this.addChild(this.chaserAINode = new CollisionPlayerChaserAI<>(initSleeping ? 96 : 384, new GameDamage(30.0F), 100));
         this.addChild(new AINode<T>() {
            protected void onRootSet(AINode<T> root, T mob, Blackboard<T> blackboard) {
            }

            public void init(T mob, Blackboard<T> blackboard) {
            }

            public AINodeResult tick(T mob, Blackboard<T> blackboard) {
               return mob.isSleeping ? AINodeResult.SUCCESS : AINodeResult.FAILURE;
            }
         });
         this.addChild(new WandererAINode<>(5000));
      }
   }
}
