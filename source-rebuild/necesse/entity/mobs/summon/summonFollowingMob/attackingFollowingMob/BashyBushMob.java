package necesse.entity.mobs.summon.summonFollowingMob.attackingFollowingMob;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Stream;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.modifiers.ModifierValue;
import necesse.engine.registries.BuffRegistry;
import necesse.engine.registries.MobRegistry;
import necesse.engine.sound.SoundSettings;
import necesse.engine.util.gameAreaSearch.GameAreaStream;
import necesse.entity.mobs.DeathMessageTable;
import necesse.entity.mobs.GameDamage;
import necesse.entity.mobs.HumanTexture;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.MobDrawable;
import necesse.entity.mobs.MobWasHitEvent;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.ai.behaviourTree.BehaviourTreeAI;
import necesse.entity.mobs.ai.behaviourTree.leaves.TargetFinderAINode;
import necesse.entity.mobs.ai.behaviourTree.trees.ConfusedPlayerChaserWandererAI;
import necesse.entity.mobs.ai.behaviourTree.util.TargetFinderDistance;
import necesse.entity.mobs.buffs.ActiveBuff;
import necesse.entity.mobs.buffs.BuffModifiers;
import necesse.entity.mobs.hostile.HostileMob;
import necesse.entity.particle.FleshParticle;
import necesse.entity.particle.Particle;
import necesse.gfx.GameResources;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.DrawOptions;
import necesse.gfx.drawOptions.human.HumanDrawOptions;
import necesse.gfx.drawOptions.itemAttack.ItemAttackDrawOptions;
import necesse.gfx.drawOptions.texture.TextureDrawOptions;
import necesse.gfx.drawables.OrderableDrawables;
import necesse.inventory.lootTable.LootTable;
import necesse.inventory.lootTable.lootItem.LootItem;
import necesse.level.maps.Level;
import necesse.level.maps.light.GameLight;

public class BashyBushMob extends HostileMob {
   private final int startAttackCooldown = 1000;
   public static LootTable lootTable = new LootTable(new LootItem("bashybush"), new LootItem("raspberry", 2), new LootItem("raspberrysapling"));

   public BashyBushMob() {
      super(100);
      this.setSpeed(0.0F);
      this.setFriction(2.0F);
      this.collision = new Rectangle(-10, -7, 20, 14);
      this.hitBox = new Rectangle(-16, -22, 32, 32);
      this.selectBox = new Rectangle(-26, -32, 52, 42);
      this.attackCooldown = 1000;
   }

   @Override
   public int getCollisionKnockback(Mob target) {
      return 15;
   }

   @Override
   public void handleCollisionHit(Mob target, GameDamage damage, int knockback) {
   }

   @Override
   public float getSpeedModifier() {
      ActiveBuff buff = this.buffManager.getBuff(BuffRegistry.BASHY_BUSH_FRENZY_BUFF);
      if (buff != null && this.isFollowing()) {
         Mob attackOwner = this.getAttackOwner();
         if (attackOwner != null) {
            return attackOwner.buffManager.getModifier(BuffModifiers.SUMMONS_SPEED) * super.getSpeedModifier();
         }
      }

      return super.getSpeedModifier();
   }

   @Override
   public void init() {
      super.init();
      this.ai = new BehaviourTreeAI<>(this, new ConfusedPlayerChaserWandererAI<BashyBushMob>(null, 576, 64, -1, false, false) {
         public boolean attackTarget(BashyBushMob mob, Mob target) {
            if (mob.canAttack()) {
               mob.attack(target.getX(), target.getY(), false);
               target.isServerHit(new GameDamage(45.0F), mob.dx, mob.dy, 15.0F, mob);
               mob.buffManager.addBuff(new ActiveBuff(BuffRegistry.BASHY_BUSH_FRENZY_BUFF, mob, 8.0F, null), true);
               return true;
            } else {
               return false;
            }
         }

         public GameAreaStream<Mob> streamPossibleTargets(BashyBushMob mob, Point base, TargetFinderDistance<BashyBushMob> distance) {
            return TargetFinderAINode.streamPlayersAndHumans(mob, base, distance).filter(m -> m.isPlayer || BashyBushMob.this.isAttacker(m));
         }
      });
   }

   @Override
   public void serverTick() {
      super.serverTick();
      this.updateAttackSpeed();
   }

   @Override
   public void clientTick() {
      super.clientTick();
      this.updateAttackSpeed();
   }

   @Override
   protected SoundSettings getAmbientSound() {
      return new SoundSettings(GameResources.bashyBushAmbient).volume(0.2F);
   }

   @Override
   protected SoundSettings getHurtSound() {
      return new SoundSettings(GameResources.bashyBushHurt).volume(0.2F).pitchVariance(0.05F);
   }

   @Override
   protected SoundSettings getDeathSound() {
      return this.getHurtSound().volume(0.25F);
   }

   @Override
   public LootTable getLootTable() {
      return lootTable;
   }

   public void updateAttackSpeed() {
      this.attackCooldown = (int)(1000.0F * (1.0F / this.buffManager.getModifier(BuffModifiers.ATTACK_SPEED)));
      this.attackAnimTime = (int)(200.0F * (1.0F / this.buffManager.getModifier(BuffModifiers.ATTACK_SPEED)));
   }

   @Override
   public void spawnDeathParticles(float knockbackX, float knockbackY) {
      for (int i = 0; i < 4; i++) {
         this.getLevel()
            .entityManager
            .addParticle(
               new FleshParticle(this.getLevel(), MobRegistry.Textures.bashyBush.body, i, 8, 32, this.x, this.y, 20.0F, knockbackX, knockbackY),
               Particle.GType.IMPORTANT_COSMETIC
            );
      }
   }

   @Override
   protected void doWasHitLogic(MobWasHitEvent event) {
      super.doWasHitLogic(event);
      this.buffManager.addBuff(new ActiveBuff(BuffRegistry.BASHY_BUSH_FRENZY_BUFF, this, 5.0F, null), true);
      this.buffManager.forceUpdateBuffs();
   }

   @Override
   public Stream<ModifierValue<?>> getDefaultModifiers() {
      return this.buffManager != null && !(this.getSpeed() <= 0.0F)
         ? super.getDefaultModifiers()
         : Stream.concat(super.getDefaultModifiers(), Stream.of(new ModifierValue<>(BuffModifiers.TARGET_RANGE, -1.0F)));
   }

   @Override
   public DeathMessageTable getDeathMessages() {
      return this.getDeathMessages("bashybush", 3);
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
      int drawX = camera.getDrawX(x) - 64;
      int drawY = camera.getDrawY(y) - 64 - 32;
      int dir = this.getDir();
      Point sprite = this.getAnimSprite(x, y, dir);
      drawY += this.getBobbing(x, y);
      drawY += level.getTile(getTileCoordinate(x), getTileCoordinate(y)).getMobSinkingAmount(this);
      float animProgress = this.getAttackAnimProgress();
      HumanTexture bashyBush = MobRegistry.Textures.bashyBush;
      HumanDrawOptions humanDrawOptions = new HumanDrawOptions(level, bashyBush)
         .sprite(sprite, 128)
         .size(128, 128)
         .dir(dir)
         .light(light)
         .applyEnemyTracker(this, perspective)
         .attackOffsets(dir == 3 ? 68 : 62, 56, 0, 16, 12, 4, 12);
      if (this.isAttacking) {
         ItemAttackDrawOptions attackOptions = ItemAttackDrawOptions.start(dir)
            .itemSprite(bashyBush.body, 3, 8, 64)
            .itemRotatePoint(48, 0)
            .itemRotateOffset(-30.0F)
            .itemEnd()
            .armSprite(bashyBush.body, 2, 8, 64)
            .addedArmPosOffset(-32, -28)
            .swingRotation(this.getAttackAnimProgress() + 15.0F);
         humanDrawOptions.attackAnim(attackOptions, animProgress);
      }

      final DrawOptions drawOptions = humanDrawOptions.pos(drawX, drawY);
      list.add(new MobDrawable() {
         @Override
         public void draw(TickManager tickManager) {
            drawOptions.draw();
         }
      });
      TextureDrawOptions shadow = MobRegistry.Textures.stabbyBush_shadow
         .initDraw()
         .sprite(sprite.x, sprite.y, 64, 64)
         .light(light)
         .pos(drawX + 32, drawY + 64 - 3);
      tileList.add(tm -> shadow.draw());
   }

   @Override
   public Point getAnimSprite(int x, int y, int dir) {
      Point p = new Point(0, dir);
      if (this.inLiquid(x, y)) {
         p.x = 5;
      } else if (Math.abs(this.dx) <= 0.1F & Math.abs(this.dy) <= 0.1F) {
         p.x = 0;
         if (this.getNextAttackCooldown() >= -100L) {
            p.x = 1;
         }
      } else {
         p.x = (int)(this.getDistanceRan() / this.getRockSpeed()) % 4 + 1;
      }

      return p;
   }
}
