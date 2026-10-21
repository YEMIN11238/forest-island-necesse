package necesse.entity.mobs.hostile;

import java.awt.Rectangle;
import java.util.List;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.registries.MobRegistry;
import necesse.engine.sound.SoundEffect;
import necesse.engine.sound.SoundManager;
import necesse.entity.mobs.GameDamage;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.MobDrawable;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.ai.behaviourTree.BehaviourTreeAI;
import necesse.entity.mobs.ai.behaviourTree.trees.StationaryPlayerShooterAI;
import necesse.entity.particle.FleshParticle;
import necesse.entity.particle.Particle;
import necesse.entity.projectile.SwampBoltProjectile;
import necesse.gfx.GameResources;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.DrawOptions;
import necesse.gfx.drawOptions.texture.TextureDrawOptions;
import necesse.gfx.drawables.OrderableDrawables;
import necesse.gfx.gameTexture.GameTexture;
import necesse.inventory.lootTable.LootTable;
import necesse.inventory.lootTable.lootItem.ChanceLootItem;
import necesse.level.maps.Level;
import necesse.level.maps.light.GameLight;

public class SwampShooterMob extends HostileMob {
   public static LootTable lootTable = new LootTable(randomMapDrop, ChanceLootItem.between(0.5F, "swampsludge", 1, 1));
   public static GameDamage damage = new GameDamage(40.0F);

   public SwampShooterMob() {
      super(200);
      this.setSpeed(0.0F);
      this.setFriction(3.0F);
      this.setKnockbackModifier(0.0F);
      this.setArmor(10);
      this.collision = new Rectangle(-10, -7, 20, 14);
      this.hitBox = new Rectangle(-14, -12, 28, 24);
      this.selectBox = new Rectangle(-14, -41, 28, 48);
   }

   @Override
   public void init() {
      super.init();
      this.ai = new BehaviourTreeAI<>(
         this,
         new StationaryPlayerShooterAI<SwampShooterMob>(448) {
            public void shootTarget(SwampShooterMob mob, Mob target) {
               SwampBoltProjectile projectile = new SwampBoltProjectile(
                  SwampShooterMob.this.getLevel(), mob, mob.x, mob.y, target.x, target.y, 100.0F, 640, SwampShooterMob.damage, 50
               );
               projectile.setTargetPrediction(target);
               SwampShooterMob.this.attack((int)(mob.x + projectile.dx * 100.0F), (int)(mob.y + projectile.dy * 100.0F), false);
               projectile.x = projectile.x + Math.signum(SwampShooterMob.this.attackDir.x) * 10.0F;
               projectile.y = projectile.y + SwampShooterMob.this.attackDir.y * 6.0F;
               SwampShooterMob.this.getLevel().entityManager.projectiles.add(projectile);
            }
         }
      );
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
      if (this.isAttacking) {
         this.getAttackAnimProgress();
      }
   }

   @Override
   public boolean canBePushed(Mob other) {
      return false;
   }

   @Override
   public LootTable getLootTable() {
      return lootTable;
   }

   @Override
   public void spawnDeathParticles(float knockbackX, float knockbackY) {
      for (int i = 0; i < 4; i++) {
         this.getLevel()
            .entityManager
            .addParticle(
               new FleshParticle(this.getLevel(), MobRegistry.Textures.swampShooter, 10, i, 32, this.x, this.y, 20.0F, knockbackX, knockbackY),
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
      int drawX = camera.getDrawX(x) - 32;
      int drawY = camera.getDrawY(y) - 56;
      drawY += this.getBobbing(x, y);
      drawY += level.getTile(getTileCoordinate(x), getTileCoordinate(y)).getMobSinkingAmount(this);
      boolean mirror = false;
      int spriteY;
      if (this.attackDir != null) {
         float threshold = 0.4F;
         if (Math.abs(this.attackDir.x) - Math.abs(this.attackDir.y) <= threshold) {
            spriteY = this.attackDir.y < 0.0F ? 0 : 2;
            if (this.attackDir.x < 0.0F) {
               mirror = true;
            }
         } else {
            spriteY = this.attackDir.x < 0.0F ? 3 : 1;
         }
      } else {
         int dir = this.getDir();
         if (dir != 0 && dir != 1) {
            spriteY = 3;
         } else {
            spriteY = 1;
         }
      }

      float animProgress = this.getAttackAnimProgress();
      int spriteX;
      if (this.isAttacking) {
         spriteX = 1 + Math.min((int)(animProgress * 4.0F), 3);
      } else {
         spriteX = 0;
      }

      final DrawOptions body = MobRegistry.Textures.swampShooter
         .initDraw()
         .sprite(spriteX, spriteY, 64)
         .mirror(mirror, false)
         .startGlowOptions(this, this.getID())
         .light(light)
         .applyEnemyTracker(this, perspective)
         .pos(drawX, drawY);
      list.add(new MobDrawable() {
         @Override
         public void draw(TickManager tickManager) {
            body.draw();
         }
      });
      this.addShadowDrawables(tileList, level, x, y, light, camera);
   }

   @Override
   protected TextureDrawOptions getShadowDrawOptions(Level level, int x, int y, GameLight light, GameCamera camera) {
      GameTexture shadowTexture = MobRegistry.Textures.human_shadow;
      int res = shadowTexture.getHeight();
      int drawX = camera.getDrawX(x) - res / 2;
      int drawY = camera.getDrawY(y) - res / 2;
      drawY += this.getBobbing(x, y);
      drawY += level.getTile(getTileCoordinate(x), getTileCoordinate(y)).getMobSinkingAmount(this);
      return shadowTexture.initDraw().sprite(0, 0, res).light(light).pos(drawX, drawY);
   }

   @Override
   public void showAttack(int x, int y, int seed, boolean showAllDirections) {
      super.showAttack(x, y, seed, showAllDirections);
      if (this.isClient()) {
         SoundManager.playSound(GameResources.flick, SoundEffect.effect(this).pitch(1.2F));
      }
   }
}
