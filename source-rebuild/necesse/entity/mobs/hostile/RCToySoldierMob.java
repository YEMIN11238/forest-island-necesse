package necesse.entity.mobs.hostile;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.HashSet;
import java.util.List;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.network.server.ServerClient;
import necesse.engine.registries.MobRegistry;
import necesse.engine.util.GameRandom;
import necesse.entity.levelEvent.explosionEvent.DynamiteExplosionEvent;
import necesse.entity.mobs.Attacker;
import necesse.entity.mobs.DeathMessageTable;
import necesse.entity.mobs.GameDamage;
import necesse.entity.mobs.MaskShaderOptions;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.MobDrawable;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.ai.behaviourTree.BehaviourTreeAI;
import necesse.entity.mobs.ai.behaviourTree.trees.CollisionPlayerChaserWandererAI;
import necesse.entity.particle.FleshParticle;
import necesse.entity.particle.Particle;
import necesse.entity.projectile.RCToySoldierDynamiteProjectile;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.DrawOptions;
import necesse.gfx.drawOptions.texture.TextureDrawOptions;
import necesse.gfx.drawables.OrderableDrawables;
import necesse.level.maps.Level;
import necesse.level.maps.light.GameLight;

public class RCToySoldierMob extends HostileMob {
   public int lifetime = 8000;

   public RCToySoldierMob() {
      super(200);
      this.setSpeed(50.0F);
      this.setFriction(3.0F);
      this.setKnockbackModifier(0.4F);
      this.setArmor(50);
      this.collision = new Rectangle(-8, -6, 16, 12);
      this.hitBox = new Rectangle(-12, -10, 24, 20);
      this.selectBox = new Rectangle(-12, -34, 24, 36);
      this.swimMaskMove = 12;
      this.swimMaskOffset = 6;
      this.swimSinkOffset = 0;
   }

   @Override
   public void init() {
      super.init();
      this.ai = new BehaviourTreeAI<>(this, new CollisionPlayerChaserWandererAI<>(null, 512, null, 100, 40000));
   }

   @Override
   public void handleCollisionHit(Mob target, GameDamage damage, int knockback) {
      super.handleCollisionHit(target, damage, knockback);
      this.explode();
   }

   @Override
   public void serverTick() {
      super.serverTick();
      this.lifetime -= 50;
      if (this.lifetime <= 0) {
         this.explode();
      }
   }

   @Override
   public GameDamage getCollisionDamage(Mob target, boolean fromPacket, ServerClient packetSubmitter) {
      return MagechanicMob.toyExplosionDamage;
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
               new FleshParticle(this.getLevel(), MobRegistry.Textures.rcToySoldier, i, 8, 32, this.x, this.y, 20.0F, knockbackX, knockbackY),
               Particle.GType.IMPORTANT_COSMETIC
            );
      }
   }

   @Override
   protected void onDeath(Attacker attacker, HashSet<Attacker> attackers) {
      super.onDeath(attacker, attackers);
      if (this.lifetime > 0 && !this.isClient()) {
         for (int i = 0; i < 3; i++) {
            int targetX = (int)this.x + GameRandom.globalRandom.getIntBetween(-128, 128);
            int targetY = (int)this.y + GameRandom.globalRandom.getIntBetween(-128, 128);
            int newRange = (int)this.getDistance(targetX, targetY);
            int fuseTime = this.lifetime + GameRandom.globalRandom.getIntBetween(500, 1500);
            RCToySoldierDynamiteProjectile projectile = new RCToySoldierDynamiteProjectile(
               this.x, this.y, targetX, targetY, 25, newRange, MagechanicMob.toyExplosionDamage, fuseTime, this
            );
            this.getLevel().entityManager.projectiles.add(projectile);
         }
      }
   }

   private void explode() {
      if (this.isServer()) {
         this.remove();
         this.getLevel().entityManager.events.add(new DynamiteExplosionEvent(this.x, this.y, 150, MagechanicMob.toyExplosionDamage, false, false, 0.0F, null));
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
      int drawY = camera.getDrawY(y) - 32;
      int dir = this.getDir();
      Point sprite = this.getAnimSprite(x, y, dir);
      drawY += this.getBobbing(x, y);
      drawY += level.getTile(getTileCoordinate(x), getTileCoordinate(y)).getMobSinkingAmount(this);
      final MaskShaderOptions swimMask = this.getSwimMaskShaderOptions(this.inLiquidFloat(x, y));
      final DrawOptions options = MobRegistry.Textures.rcToySoldier
         .initDraw()
         .sprite(sprite.x, sprite.y, 64)
         .addMaskShader(swimMask)
         .light(light)
         .pos(drawX, drawY - 18);
      list.add(new MobDrawable() {
         @Override
         public void draw(TickManager tickManager) {
            swimMask.use();
            options.draw();
            swimMask.stop();
         }
      });
      TextureDrawOptions shadow = MobRegistry.Textures.bloatedSpider.shadow.initDraw().sprite(sprite.x, sprite.y, 64).light(light).pos(drawX, drawY - 10);
      tileList.add(tm -> shadow.draw());
   }

   @Override
   public boolean isSlimeImmune() {
      return true;
   }
}
