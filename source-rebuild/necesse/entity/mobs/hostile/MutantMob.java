package necesse.entity.mobs.hostile;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.function.Supplier;
import necesse.engine.GameTileRange;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.registries.MobRegistry;
import necesse.engine.util.GameRandom;
import necesse.entity.mobs.Attacker;
import necesse.entity.mobs.DeathMessageTable;
import necesse.entity.mobs.GameDamage;
import necesse.entity.mobs.HumanTexture;
import necesse.entity.mobs.MaskShaderOptions;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.MobDrawable;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.ai.behaviourTree.BehaviourTreeAI;
import necesse.entity.mobs.ai.behaviourTree.leaves.CooldownAttackTargetAINode;
import necesse.entity.mobs.ai.behaviourTree.trees.CollisionShooterPlayerChaserWandererAI;
import necesse.entity.particle.FleshParticle;
import necesse.entity.particle.Particle;
import necesse.entity.projectile.MutantGroundAcidProjectile;
import necesse.entity.projectile.MutantSpitProjectile;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.DrawOptions;
import necesse.gfx.drawOptions.human.HumanDrawOptions;
import necesse.gfx.drawOptions.itemAttack.ItemAttackDrawOptions;
import necesse.gfx.drawables.OrderableDrawables;
import necesse.inventory.item.toolItem.projectileToolItem.ProjectileToolItem;
import necesse.level.maps.Level;
import necesse.level.maps.light.GameLight;

public class MutantMob extends HostileMob {
   private HumanTexture texture;
   public static GameDamage collisionDamage = new GameDamage(115.0F);
   public static GameDamage spitDamage = new GameDamage(115.0F);
   static GameTileRange range = new GameTileRange(3);

   public MutantMob() {
      super(600);
      this.setSpeed(40.0F);
      this.setArmor(40);
      this.setFriction(3.0F);
      this.collision = new Rectangle(-10, -7, 20, 14);
      this.hitBox = new Rectangle(-14, -12, 28, 24);
      this.selectBox = new Rectangle(-14, -41, 28, 48);
      this.swimMaskMove = 20;
      this.swimMaskOffset = -4;
      this.swimSinkOffset = -4;
   }

   @Override
   public void init() {
      super.init();
      if (this.texture == null && this.getLevel() != null) {
         this.texture = new GameRandom(this.getUniqueID())
            .nextSeeded(23)
            .getOneOf(MobRegistry.Textures.mutant1, MobRegistry.Textures.mutant2, MobRegistry.Textures.mutant3);
      }

      this.ai = new BehaviourTreeAI<>(
         this,
         new CollisionShooterPlayerChaserWandererAI<MutantMob>(
            null, 256, collisionDamage, 100, CooldownAttackTargetAINode.CooldownTimer.HAS_TARGET, 3500, 480, 40000
         ) {
            public boolean shootAtTarget(MutantMob mob, Mob target) {
               if (mob.canAttack()) {
                  int targetDistance = (int)mob.getDistance(target);
                  mob.attack(target.getX(), target.getY(), false);
                  Point point = ProjectileToolItem.controlledRangePosition(
                     GameRandom.globalRandom, mob.getX(), mob.getY(), target.getX(), target.getY(), Math.max(320, targetDistance + 32), 32, 16
                  );
                  mob.getLevel().entityManager.projectiles.add(new MutantSpitProjectile(mob.x, mob.y, point.x, point.y, MutantMob.spitDamage, mob, 1000));
                  return true;
               } else {
                  return false;
               }
            }
         }
      );
   }

   @Override
   protected void onDeath(Attacker attacker, HashSet<Attacker> attackers) {
      super.onDeath(attacker, attackers);
      ArrayList<Point> validSpawnTiles = new ArrayList<>();

      for (Point tile : range.getValidTiles(this.getTileX(), this.getTileY())) {
         if (!this.getLevel().isSolidTile(tile.x, tile.y)) {
            validSpawnTiles.add(tile);
         }
      }

      for (int i = 0; i < 3 && !validSpawnTiles.isEmpty(); i++) {
         Point spawnTile = GameRandom.globalRandom.getOneOf(validSpawnTiles);
         validSpawnTiles.remove(spawnTile);
         int targetX = spawnTile.x * 32 + 16;
         int targetY = spawnTile.y * 32 + 16;
         int distance = (int)this.getDistance(targetX, targetY);
         MutantGroundAcidProjectile p = new MutantGroundAcidProjectile(this.getLevel(), this, targetX, targetY, 30.0F, distance, spitDamage, 0);
         this.getLevel().entityManager.projectiles.add(p);
      }
   }

   @Override
   public DeathMessageTable getDeathMessages() {
      return this.getDeathMessages("scrapyard", 5);
   }

   @Override
   public void spawnDeathParticles(float knockbackX, float knockbackY) {
      for (int i = 0; i < 4; i++) {
         this.getLevel()
            .entityManager
            .addParticle(
               new FleshParticle(this.getLevel(), this.texture.body, GameRandom.globalRandom.nextInt(5), 8, 32, this.x, this.y, 20.0F, knockbackX, knockbackY),
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
      HumanDrawOptions humanDrawOptions = new HumanDrawOptions(level, this.texture)
         .sprite(sprite)
         .applyEnemyTracker(this, perspective)
         .light(light)
         .dir(dir)
         .mask(swimMask);
      float animProgress = this.getAttackAnimProgress();
      if (this.isAttacking) {
         ItemAttackDrawOptions attackOptions = ItemAttackDrawOptions.start(dir)
            .itemSprite(this.texture.body, 0, 9, 32)
            .itemRotatePoint(4, 4)
            .itemEnd()
            .armSprite(this.texture.body, 0, 8, 32)
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
   public boolean isSlimeImmune() {
      return true;
   }
}
