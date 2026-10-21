package necesse.entity.mobs.hostile;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import necesse.engine.GameTileRange;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.registries.MobRegistry;
import necesse.engine.sound.SoundEffect;
import necesse.engine.sound.SoundManager;
import necesse.engine.sound.SoundSettings;
import necesse.engine.util.GameRandom;
import necesse.entity.levelEvent.mobAbilityLevelEvent.ScrapBallEvent;
import necesse.entity.mobs.DeathMessageTable;
import necesse.entity.mobs.GameDamage;
import necesse.entity.mobs.MaskShaderOptions;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.MobDrawable;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.ai.behaviourTree.BehaviourTreeAI;
import necesse.entity.mobs.ai.behaviourTree.trees.ConfusedPlayerChaserWandererAI;
import necesse.entity.particle.FleshParticle;
import necesse.entity.particle.Particle;
import necesse.entity.projectile.RCToySoldierSpawnProjectile;
import necesse.gfx.GameResources;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.DrawOptions;
import necesse.gfx.drawOptions.human.HumanDrawOptions;
import necesse.gfx.drawOptions.itemAttack.ItemAttackDrawOptions;
import necesse.gfx.drawables.OrderableDrawables;
import necesse.level.maps.Level;
import necesse.level.maps.light.GameLight;

public class MagechanicMob extends HostileMob {
   public static GameDamage toyExplosionDamage = new GameDamage(300.0F);
   public static GameDamage mageProjectileDamage = new GameDamage(90.0F);
   static GameTileRange range = new GameTileRange(5);
   private final int scrapBallCooldown = 12000;
   private final int rcToySoldierCooldown = 12000;
   private int timeSinceLastScrapBall = 12000;
   private int timeSinceLastToySoldier = 12000;

   public MagechanicMob() {
      super(500);
      this.attackAnimTime = 200;
      this.setSpeed(50.0F);
      this.setFriction(3.0F);
      this.setKnockbackModifier(0.4F);
      this.setArmor(25);
      this.collision = new Rectangle(-10, -7, 20, 14);
      this.hitBox = new Rectangle(-14, -12, 28, 24);
      this.selectBox = new Rectangle(-14, -41, 28, 48);
      this.swimMaskMove = 16;
      this.swimMaskOffset = -2;
      this.swimSinkOffset = -4;
   }

   @Override
   public void init() {
      super.init();
      ConfusedPlayerChaserWandererAI<MagechanicMob> chaserAI = new ConfusedPlayerChaserWandererAI<MagechanicMob>(null, 640, 320, 40000, false, false) {
         public boolean attackTarget(MagechanicMob mob, Mob target) {
            if (mob.canAttack()) {
               mob.attack(target.getX(), target.getY(), false);
               mob.getLevel().entityManager.events.add(new ScrapBallEvent(mob, target, GameRandom.globalRandom, MagechanicMob.mageProjectileDamage, 2000L));
               this.wanderAfterAttack = GameRandom.globalRandom.getChance(0.75F);
               MagechanicMob.this.timeSinceLastScrapBall = 0;
               return true;
            } else if (MagechanicMob.this.timeSinceLastToySoldier > 12000) {
               mob.spawnToySoldier();
               MagechanicMob.this.timeSinceLastToySoldier = 0;
               this.getBlackboard().mover.stopMoving(mob);
               return true;
            } else {
               return false;
            }
         }
      };
      this.ai = new BehaviourTreeAI<>(this, chaserAI);
   }

   @Override
   public void clientTick() {
      super.clientTick();
      if (this.isAttacking) {
         this.getAttackAnimProgress();
      }
   }

   @Override
   public void serverTick() {
      super.serverTick();
      this.timeSinceLastScrapBall += 50;
      this.timeSinceLastToySoldier += 50;
      if (this.isAttacking) {
         this.getAttackAnimProgress();
      }
   }

   @Override
   public boolean canAttack() {
      return this.timeSinceLastScrapBall <= 12000 ? false : super.canAttack();
   }

   public void spawnToySoldier() {
      ArrayList<Point> validSpawnTiles = new ArrayList<>();

      for (Point tile : range.getValidTiles(this.getTileX(), this.getTileY())) {
         if (!this.getLevel().isSolidTile(tile.x, tile.y)) {
            validSpawnTiles.add(tile);
         }
      }

      if (!validSpawnTiles.isEmpty()) {
         Point spawnTile = GameRandom.globalRandom.getOneOf(validSpawnTiles);
         int targetX = spawnTile.x * 32 + 16;
         int targetY = spawnTile.y * 32 + 16;
         int distance = (int)this.getDistance(targetX, targetY);
         this.getLevel()
            .entityManager
            .projectiles
            .add(new RCToySoldierSpawnProjectile(this.getLevel(), this, targetX, targetY, 30.0F, distance, new GameDamage(0.0F), 50));
      }
   }

   @Override
   public DeathMessageTable getDeathMessages() {
      return this.getDeathMessages("scrapyard", 5);
   }

   @Override
   public void playHurtSound() {
      float pitch = GameRandom.globalRandom.getOneOf(0.95F, 1.0F, 1.05F);
      SoundManager.playSound(GameResources.crack, SoundEffect.effect(this).volume(1.6F).pitch(pitch));
   }

   @Override
   protected SoundSettings getHitDeathSound() {
      return new SoundSettings(GameResources.crackdeath).volume(0.8F);
   }

   @Override
   public void spawnDeathParticles(float knockbackX, float knockbackY) {
      for (int i = 0; i < 4; i++) {
         this.getLevel()
            .entityManager
            .addParticle(
               new FleshParticle(
                  this.getLevel(),
                  MobRegistry.Textures.ancientSkeletonMage.body,
                  GameRandom.globalRandom.nextInt(5),
                  8,
                  32,
                  this.x,
                  this.y,
                  20.0F,
                  knockbackX,
                  knockbackY
               ),
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
      int drawX = camera.getDrawX(x) - 22 - 10;
      int drawY = camera.getDrawY(y) - 44 - 7;
      int dir = this.getDir();
      Point sprite = this.getAnimSprite(x, y, dir);
      drawY += this.getBobbing(x, y);
      drawY += level.getTile(getTileCoordinate(x), getTileCoordinate(y)).getMobSinkingAmount(this);
      MaskShaderOptions swimMask = this.getSwimMaskShaderOptions(this.inLiquidFloat(x, y));
      HumanDrawOptions humanDrawOptions = new HumanDrawOptions(level, MobRegistry.Textures.magechanic)
         .sprite(sprite)
         .applyEnemyTracker(this, perspective)
         .light(light)
         .dir(dir)
         .mask(swimMask);
      float animProgress = this.getAttackAnimProgress();
      if (this.isAttacking) {
         ItemAttackDrawOptions attackOptions = ItemAttackDrawOptions.start(dir)
            .itemSprite(MobRegistry.Textures.magechanic.body, 1, 9, 32)
            .itemRotatePoint(4, 4)
            .itemEnd()
            .armSprite(MobRegistry.Textures.magechanic.body, 0, 8, 32)
            .swingRotation(animProgress);
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

   @Override
   public int getRockSpeed() {
      return 20;
   }

   @Override
   public void showAttack(int x, int y, int seed, boolean showAllDirections) {
      super.showAttack(x, y, seed, showAllDirections);
      if (this.isClient()) {
         SoundManager.playSound(GameResources.swing2, SoundEffect.effect(this).volume(0.7F).pitch(1.2F));
      }
   }

   @Override
   public boolean isSlimeImmune() {
      return true;
   }
}
