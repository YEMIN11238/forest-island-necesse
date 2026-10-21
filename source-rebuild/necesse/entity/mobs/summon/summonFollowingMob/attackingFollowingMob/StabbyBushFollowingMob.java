package necesse.entity.mobs.summon.summonFollowingMob.attackingFollowingMob;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.HashSet;
import java.util.List;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.network.server.ServerClient;
import necesse.engine.registries.BuffRegistry;
import necesse.engine.registries.MobRegistry;
import necesse.engine.sound.SoundEffect;
import necesse.engine.sound.SoundManager;
import necesse.engine.util.GameRandom;
import necesse.entity.levelEvent.explosionEvent.StabbyBushExplosionLevelEvent;
import necesse.entity.mobs.Attacker;
import necesse.entity.mobs.DeathMessageTable;
import necesse.entity.mobs.GameDamage;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.MobDrawable;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.ai.behaviourTree.BehaviourTreeAI;
import necesse.entity.mobs.ai.behaviourTree.trees.PlayerFollowerChaserOnlyAI;
import necesse.entity.mobs.buffs.ActiveBuff;
import necesse.entity.mobs.buffs.BuffModifiers;
import necesse.entity.mobs.buffs.staticBuffs.Buff;
import necesse.entity.particle.FleshParticle;
import necesse.entity.particle.Particle;
import necesse.gfx.GameResources;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.DrawOptions;
import necesse.gfx.drawOptions.human.HumanDrawOptions;
import necesse.gfx.drawOptions.itemAttack.ItemAttackDrawOptions;
import necesse.gfx.drawOptions.texture.TextureDrawOptions;
import necesse.gfx.drawables.OrderableDrawables;
import necesse.level.maps.Level;
import necesse.level.maps.light.GameLight;

public class StabbyBushFollowingMob extends AttackingFollowingMob {
   private int lifeTime = 40000;
   private final int startAttackCooldown = 1000;

   public StabbyBushFollowingMob() {
      super(10);
      this.setSpeed(0.0F);
      this.setFriction(2.0F);
      this.collision = new Rectangle(-10, -7, 20, 14);
      this.hitBox = new Rectangle(-16, -22, 32, 32);
      this.selectBox = new Rectangle(-26, -32, 52, 42);
      this.attackCooldown = 1000;
   }

   @Override
   public GameDamage getCollisionDamage(Mob target, boolean fromPacket, ServerClient packetSubmitter) {
      return this.summonDamage;
   }

   @Override
   public int getCollisionKnockback(Mob target) {
      return 15;
   }

   @Override
   public void handleCollisionHit(Mob target, GameDamage damage, int knockback) {
   }

   @Override
   public void init() {
      super.init();
      this.ai = new BehaviourTreeAI<>(this, new PlayerFollowerChaserOnlyAI<StabbyBushFollowingMob>(576, 64, false, false, 640, 64) {
         public boolean attackTarget(StabbyBushFollowingMob mob, Mob target) {
            if (mob.canAttack()) {
               mob.attack(target.getX(), target.getY(), false);
               target.isServerHit(StabbyBushFollowingMob.this.summonDamage, mob.dx, mob.dy, 15.0F, mob);
               mob.buffManager.addBuff(new ActiveBuff(BuffRegistry.STABBY_BUSH_FRENZY_BUFF, mob, 20.0F, null), true);
               return true;
            } else {
               return false;
            }
         }
      });
   }

   @Override
   public void serverTick() {
      super.serverTick();
      this.updateAttackSpeed();
      this.lifeTime -= 50;
      if (this.lifeTime <= 0) {
         this.remove(0.0F, 0.0F, null, true);
      } else {
         Buff stabbyBushFrenzyBuff = BuffRegistry.STABBY_BUSH_FRENZY_BUFF;
         ActiveBuff buff = this.buffManager.getBuff(stabbyBushFrenzyBuff);
         if (buff != null && buff.getStacks() >= buff.getMaxStacks()) {
            GameDamage bombDamage = this.summonDamage.modDamage(2.0F);
            StabbyBushExplosionLevelEvent explosionLevelEvent = new StabbyBushExplosionLevelEvent(
               this.x, this.y, 100, bombDamage, false, 0.0F, this.getAttackOwner()
            );
            this.getLevel().entityManager.events.add(explosionLevelEvent);
            this.remove(0.0F, 0.0F, null, true);
         }
      }
   }

   @Override
   public void clientTick() {
      super.clientTick();
      this.updateAttackSpeed();
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
               new FleshParticle(this.getLevel(), MobRegistry.Textures.stabbyBush.body, i, 8, 32, this.x, this.y, 20.0F, knockbackX, knockbackY),
               Particle.GType.IMPORTANT_COSMETIC
            );
      }
   }

   @Override
   public void playDeathSound() {
      float pitch = GameRandom.globalRandom.getOneOf(0.95F, 1.0F, 1.05F);
      SoundManager.playSound(GameResources.npcdeath, SoundEffect.effect(this).volume(0.1F).pitch(pitch));
   }

   @Override
   protected void onDeath(Attacker attacker, HashSet<Attacker> attackers) {
      super.onDeath(attacker, attackers);
   }

   @Override
   public DeathMessageTable getDeathMessages() {
      return this.getDeathMessages("stabbybush", 3);
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
      int drawX = camera.getDrawX(x) - 32;
      int drawY = camera.getDrawY(y) - 44 - 7;
      int dir = this.getDir();
      Point sprite = this.getAnimSprite(x, y, dir);
      drawY += this.getBobbing(x, y);
      drawY += level.getTile(getTileCoordinate(x), getTileCoordinate(y)).getMobSinkingAmount(this);
      float animProgress = this.getAttackAnimProgress();
      HumanDrawOptions humanDrawOptions = new HumanDrawOptions(level, MobRegistry.Textures.stabbyBush)
         .sprite(sprite)
         .dir(dir)
         .light(light)
         .applyEnemyTracker(this, perspective)
         .attackOffsets(dir == 3 ? 20 : 34, 0, 8, 16, 12, 4, 12);
      if (this.isAttacking) {
         ItemAttackDrawOptions attackOptions = ItemAttackDrawOptions.start(dir)
            .itemSprite(MobRegistry.Textures.stabbyBush.body, 5, 8, 32)
            .itemRotatePoint(12, 18)
            .itemRotateOffset(40.0F)
            .itemEnd()
            .armSprite(MobRegistry.Textures.stabbyBush.body, 0, 8, 32)
            .swingRotation(this.getAttackAnimProgress())
            .itemAfterHand();
         humanDrawOptions.attackAnim(attackOptions, animProgress);
      }

      final DrawOptions drawOptions = humanDrawOptions.pos(drawX, drawY);
      list.add(new MobDrawable() {
         @Override
         public void draw(TickManager tickManager) {
            drawOptions.draw();
         }
      });
      TextureDrawOptions shadow = MobRegistry.Textures.stabbyBush_shadow.initDraw().sprite(sprite.x, sprite.y, 64, 64).light(light).pos(drawX, drawY);
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
