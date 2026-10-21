package necesse.entity.mobs.hostile;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.List;
import java.util.function.Supplier;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.registries.MobRegistry;
import necesse.engine.registries.TileRegistry;
import necesse.engine.sound.SoundManager;
import necesse.engine.sound.SoundSettings;
import necesse.engine.util.GameRandom;
import necesse.engine.util.GameUtils;
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
import necesse.entity.particle.SmokePuffParticle;
import necesse.entity.projectile.VampireProjectile;
import necesse.gfx.GameResources;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.DrawOptions;
import necesse.gfx.drawOptions.human.HumanDrawOptions;
import necesse.gfx.drawOptions.itemAttack.ItemAttackDrawOptions;
import necesse.gfx.drawables.OrderableDrawables;
import necesse.inventory.lootTable.LootTable;
import necesse.inventory.lootTable.lootItem.LootItem;
import necesse.level.maps.Level;
import necesse.level.maps.TilePosition;
import necesse.level.maps.biomes.Biome;
import necesse.level.maps.light.GameLight;

public class VampireMob extends HostileMob {
   public static LootTable lootTable = new LootTable(LootItem.between("batwing", 1, 3), HostileMob.randomMapDrop);
   private boolean isBat;

   public VampireMob() {
      super(75);
      this.setSpeed(60.0F);
      this.setFriction(1.0F);
      this.moveAccuracy = 20;
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
      this.ai = new BehaviourTreeAI<>(this, new ConfusedPlayerChaserWandererAI<VampireMob>(null, 384, 320, 40000, true, false) {
         public boolean attackTarget(VampireMob mob, Mob target) {
            if (mob.canAttack() && !mob.isBat) {
               GameDamage damage = new GameDamage(9.0F);
               mob.attack(target.getX(), target.getY(), false);
               mob.getLevel().entityManager.projectiles.add(new VampireProjectile(mob.x, mob.y, target.x, target.y, damage, mob));
               this.wanderAfterAttack = GameRandom.globalRandom.getChance(0.75F);
               return true;
            } else {
               return false;
            }
         }
      });
   }

   private void tickIsBat() {
      boolean nextIsBat = (this.isAccelerating() || this.hasCurrentMovement()) && this.getSpeedModifier() > 0.0F;
      if (this.isBat != nextIsBat) {
         this.isBat = nextIsBat;
         if (this.isClient()) {
            this.getLevel().entityManager.addParticle(new SmokePuffParticle(this.getLevel(), this.x, this.y), Particle.GType.IMPORTANT_COSMETIC);
            SoundManager.playSound(new SoundSettings(GameResources.swing1).basePitch(1.1F).volume(0.2F).fallOffDistance(1200), this);
         }
      }
   }

   @Override
   public void clientTick() {
      super.clientTick();
      this.tickIsBat();
   }

   @Override
   public void serverTick() {
      super.serverTick();
      this.tickIsBat();
   }

   @Override
   public int getFlyingHeight() {
      return this.isBat ? 20 : super.getFlyingHeight();
   }

   @Override
   public LootTable getLootTable() {
      return lootTable;
   }

   @Override
   public void spawnDeathParticles(float knockbackX, float knockbackY) {
      for (int i = 0; i < 5; i++) {
         this.getLevel()
            .entityManager
            .addParticle(
               new FleshParticle(this.getLevel(), MobRegistry.Textures.vampire.body, i, 8, 32, this.x, this.y, 20.0F, knockbackX, knockbackY),
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
      HumanDrawOptions humanDrawOptions = new HumanDrawOptions(level, MobRegistry.Textures.vampire)
         .sprite(sprite)
         .dir(dir)
         .mask(swimMask)
         .light(light)
         .applyEnemyTracker(this, perspective);
      float animProgress = this.getAttackAnimProgress();
      if (this.isAttacking) {
         ItemAttackDrawOptions attackOptions = ItemAttackDrawOptions.start(dir)
            .armSprite(MobRegistry.Textures.vampire.body, 0, 8, 32)
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
   public Point getAnimSprite(int x, int y, int dir) {
      Point p = new Point(0, dir);
      if (!this.isBat) {
         if (!this.inLiquid(x, y)) {
            p.x = 0;
         } else {
            p.x = 5;
         }
      } else {
         p.x = GameUtils.getAnim(this.getWorldEntity().getTime(), 4, 400) + 1;
      }

      return p;
   }

   @Override
   public int getTileWanderPriority(TilePosition pos, Biome baseBiome) {
      return pos.tileID() == TileRegistry.cryptAshID ? 1000 : super.getTileWanderPriority(pos, baseBiome);
   }

   @Override
   public int getRockSpeed() {
      return 25;
   }

   @Override
   public DeathMessageTable getDeathMessages() {
      return this.getDeathMessages("vamp", 3);
   }
}
