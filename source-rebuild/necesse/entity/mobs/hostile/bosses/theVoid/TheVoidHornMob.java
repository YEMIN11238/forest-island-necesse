package necesse.entity.mobs.hostile.bosses.theVoid;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.List;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.localization.message.GameMessage;
import necesse.engine.registries.MobRegistry;
import necesse.engine.util.GameMath;
import necesse.engine.util.GameRandom;
import necesse.engine.util.GameUtils;
import necesse.entity.ParticleTypeSwitcher;
import necesse.entity.mobs.Attacker;
import necesse.entity.mobs.DeathMessageTable;
import necesse.entity.mobs.LevelMob;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.MobDrawable;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.ability.BooleanMobAbility;
import necesse.entity.mobs.hostile.bosses.FlyingBossMob;
import necesse.entity.mobs.mobMovement.MobMovementRelative;
import necesse.entity.particle.FleshParticle;
import necesse.entity.particle.Particle;
import necesse.gfx.GameResources;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.DrawOptions;
import necesse.gfx.drawables.Drawable;
import necesse.gfx.drawables.OrderableDrawables;
import necesse.level.maps.Level;

public class TheVoidHornMob extends FlyingBossMob {
   protected ParticleTypeSwitcher particleTypeSwitcher = new ParticleTypeSwitcher(
      Particle.GType.IMPORTANT_COSMETIC, Particle.GType.COSMETIC, Particle.GType.CRITICAL
   );
   public int removeTicker;
   public final LevelMob<TheVoidMob> master = new LevelMob<>();
   private int damageTaken;
   private int destructionThreshold = 50000;
   public boolean isBroken;
   public boolean isLeftHorn;
   public BooleanMobAbility setBrokenAbility;

   public TheVoidHornMob() {
      super(10);
      this.isSummoned = true;
      this.dropsLoot = false;
      this.collision = new Rectangle(-40, -130, 80, 140);
      this.hitBox = new Rectangle(-50, -140, 100, 160);
      this.selectBox = new Rectangle(-70, -160, 140, 200);
      this.setKnockbackModifier(0.0F);
      this.setRegen(0.0F);
      this.setSpeed(0.0F);
      this.setArmor(100);
      this.setBrokenAbility = this.registerAbility(new BooleanMobAbility() {
         @Override
         protected void run(boolean value) {
            TheVoidHornMob.this.isBroken = value;
            TheVoidMob master = TheVoidHornMob.this.master.get(TheVoidHornMob.this.getLevel());
            if (TheVoidHornMob.this.isClient() && TheVoidHornMob.this.isBroken) {
               TheVoidHornMob.this.playDeathSound();
               TheVoidHornMob.this.spawnDeathParticles(0.0F, 0.0F);
               TheVoidHornMob.this.playHurtSound();
               if (master != null) {
                  master.playHurtSound(true);
               }
            }

            if (master != null) {
               master.regenHornsAtPercentHealth = master.getHealthPercent() - 0.25F;
            }

            TheVoidHornMob.this.damageTaken = 0;
         }
      });
   }

   public Point getHornOffset() {
      TheVoidMob master = this.master.get(this.getLevel());
      return master != null ? master.getHornOffset(this.isLeftHorn) : new Point(0, 0);
   }

   @Override
   public boolean shouldSendSpawnPacket() {
      return false;
   }

   @Override
   public Mob getSpawnPacketMaster() {
      return this.master.get(this.getLevel());
   }

   @Override
   public void init() {
      super.init();
      this.setMovement(new MobMovementRelative(this.master.get(this.getLevel()), 0.0F, 0.0F));
      this.countStats = false;
   }

   @Override
   public void clientTick() {
      super.clientTick();
      this.tickMaster();
      if (this.isBroken) {
         for (int i = 0; i < 2; i++) {
            int xOffset = GameRandom.globalRandom.getIntBetween(-15, 15);
            this.getLevel()
               .entityManager
               .addParticle(this.x + xOffset, this.y + GameRandom.globalRandom.getIntBetween(-15, 15), this.particleTypeSwitcher.next())
               .sprite(GameResources.voidPuffParticles.sprite(GameRandom.globalRandom.nextInt(4), 0, 12))
               .sizeFades(24, 48)
               .movesFriction(GameRandom.globalRandom.getIntBetween(-5, 5), GameRandom.globalRandom.getIntBetween(-30, -60) + Math.abs(xOffset), 0.8F)
               .lifeTime(1000);
         }
      } else {
         int xOffset = GameRandom.globalRandom.getIntBetween(-30, 30);
         float var10001 = this.x + xOffset + (this.isLeftHorn ? -20 : 20);
         float var10002 = this.y + (this.isLeftHorn ? -xOffset : xOffset);
         this.getLevel()
            .entityManager
            .addParticle(var10001, var10002, Particle.GType.COSMETIC)
            .sprite(GameResources.voidPuffParticles.sprite(GameRandom.globalRandom.nextInt(4), 0, 12))
            .sizeFades(24, 48)
            .movesFriction(GameRandom.globalRandom.getIntBetween(-5, 5), GameRandom.globalRandom.getIntBetween(-60, -90) + Math.abs(xOffset), 0.8F)
            .lifeTime(1000);
      }
   }

   @Override
   public void serverTick() {
      super.serverTick();
      if (!this.isBroken) {
         TheVoidMob master = this.master.get(this.getLevel());
         if (master != null) {
            this.destructionThreshold = master.getMaxHealth() / 10;
         }
      }

      this.movementUpdateTime = this.getTime();
      this.healthUpdateTime = this.getTime();
      if (this.damageTaken > this.destructionThreshold) {
         this.setBrokenAbility.runAndSend(true);
      }

      this.tickMaster();
   }

   @Override
   public void spawnDeathParticles(float knockbackX, float knockbackY) {
      int height = this.getFlyingHeight() + 10;

      for (int i = 0; i < 6; i++) {
         float xOffset = this.isLeftHorn ? -200.0F : 200.0F;
         xOffset += GameRandom.globalRandom.getFloatBetween(-20.0F, 20.0F);
         float yOffset = GameRandom.globalRandom.getFloatBetween(-20.0F, 20.0F);
         FleshParticle particle = new FleshParticle(
            this.getLevel(), MobRegistry.Textures.theVoidDebris, i, 0, 96, this.x + xOffset, this.y + yOffset, knockbackX, knockbackY
         );
         particle.lifeTime = 10000L;
         particle.dx = GameRandom.globalRandom.getFloatBetween(-80.0F, 80.0F);
         particle.dy = GameRandom.globalRandom.getFloatBetween(-80.0F, 80.0F);
         particle.friction = 0.2F;
         particle.height = GameRandom.globalRandom.getFloatBetween(height, height + 20);
         this.getLevel().entityManager.addParticle(particle, Particle.GType.CRITICAL);
      }
   }

   @Override
   public int getHealth() {
      if (this.master != null) {
         TheVoidMob head = this.master.get(this.getLevel());
         if (head != null) {
            return head.getHealth();
         }
      }

      return super.getHealth();
   }

   @Override
   public int getMaxHealth() {
      if (this.master != null) {
         TheVoidMob head = this.master.get(this.getLevel());
         if (head != null) {
            return head.getMaxHealth();
         }
      }

      return super.getMaxHealth();
   }

   @Override
   public void setHealthHidden(int health, float knockbackX, float knockbackY, Attacker attacker, boolean fromNetworkUpdate) {
      int lastHealth = this.getHealth();
      if (this.master != null) {
         TheVoidMob master = this.master.get(this.getLevel());
         if (master != null) {
            master.setHealthHidden(health, knockbackX, knockbackY, attacker, fromNetworkUpdate);
         }
      }

      super.setHealthHidden(health, knockbackX, knockbackY, attacker, fromNetworkUpdate);
      int currentHealth = this.getHealth();
      if (currentHealth < lastHealth && !this.isBroken) {
         this.damageTaken += lastHealth - currentHealth;
      }
   }

   @Override
   public boolean isHealthBarVisible() {
      return false;
   }

   @Override
   public float getIncomingDamageModifier() {
      return 5.0F;
   }

   @Override
   public boolean canBeHit(Attacker attacker) {
      return !this.isBroken;
   }

   @Override
   public int getFlyingHeight() {
      TheVoidMob master = this.master.get(this.getLevel());
      return master == null ? 0 : master.getFlyingHeight();
   }

   @Override
   public boolean canBePushed(Mob other) {
      return false;
   }

   @Override
   public void requestServerUpdate() {
   }

   public void tickMaster() {
      if (!this.removed()) {
         this.master.computeIfPresent(this.getLevel(), m -> {
            this.setMaxHealth(m.getMaxHealth());
            this.setHealthHidden(m.getHealth(), 0.0F, 0.0F, null);
            this.setArmor(m.getArmorFlat() * 2);
         });
         this.removeTicker++;
         if (this.removeTicker > 20) {
            this.remove();
         }
      }
   }

   @Override
   public Rectangle getHitBox(int x, int y) {
      return super.getHitBox(x + (this.isLeftHorn ? -160 : 160), y);
   }

   @Override
   public Rectangle getCollision(int x, int y) {
      return super.getCollision(x + (this.isLeftHorn ? -160 : 160), y);
   }

   @Override
   public Rectangle getSelectBox(int x, int y) {
      return super.getSelectBox(x + (this.isLeftHorn ? -160 : 160), y);
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
      int halfSpriteWidth = 224;
      int drawX = camera.getDrawX(x) - halfSpriteWidth;
      int drawY = camera.getDrawY(y) - 176 + 25;
      drawY -= this.getFlyingHeight();
      TheVoidMob master = this.master.get(this.getLevel());
      float masterDX = master != null ? master.dx : 0.0F;
      float rotate = GameMath.limit(masterDX / 10.0F, -10.0F, 10.0F);
      float alpha = master != null ? master.getFadeAlpha() : 1.0F;
      final float whiteness = master != null ? master.getFadeWhiteness() : 0.0F;
      alpha = Math.max(alpha, whiteness);
      float headAlpha;
      float overlayAlpha;
      if (alpha >= 0.5F) {
         headAlpha = 1.0F;
         overlayAlpha = GameMath.lerp(GameMath.clamp(alpha, 0.5F, 1.0F), 1.0F, 0.0F);
      } else {
         headAlpha = 0.0F;
         overlayAlpha = GameMath.lerp(GameMath.clamp(alpha, 0.0F, 0.5F), 0.0F, 1.0F);
      }

      int spriteX = GameUtils.getAnim(this.getTime(), 6, 600) * 2;
      if (!this.isLeftHorn) {
         spriteX++;
         drawX += halfSpriteWidth;
      }

      final DrawOptions hornOptions = MobRegistry.Textures.theVoidHead
         .initDraw()
         .sprite(spriteX, this.isBroken ? 6 : 5, halfSpriteWidth, 352)
         .startGlowOptions(level, this.getID())
         .applyEnemyTracker(this, perspective)
         .rotate(rotate, this.isLeftHorn ? halfSpriteWidth : 0, 176)
         .alpha(headAlpha)
         .pos(drawX, drawY);
      final DrawOptions hornOverlayOptions = MobRegistry.Textures.theVoidHead
         .initDraw()
         .sprite(spriteX, this.isBroken ? 6 : 5, halfSpriteWidth, 352)
         .startGlowOptions(level, this.getID())
         .applyEnemyTracker(this, perspective)
         .rotate(rotate, this.isLeftHorn ? halfSpriteWidth : 0, 176)
         .color(0.0F, 0.0F, 0.0F, overlayAlpha)
         .alpha(overlayAlpha)
         .pos(drawX, drawY);
      final DrawOptions orbOptions = MobRegistry.Textures.theVoidHead
         .initDraw()
         .sprite(spriteX, 7, halfSpriteWidth, 352)
         .startGlowOptions(level, this.getID())
         .applyEnemyTracker(this, perspective)
         .rotate(rotate, this.isLeftHorn ? halfSpriteWidth : 0, 176)
         .alpha(headAlpha)
         .pos(drawX, drawY);
      final DrawOptions orbOverlayOptions = MobRegistry.Textures.theVoidHead
         .initDraw()
         .sprite(spriteX, 7, halfSpriteWidth, 352)
         .startGlowOptions(level, this.getID())
         .applyEnemyTracker(this, perspective)
         .rotate(rotate, this.isLeftHorn ? halfSpriteWidth : 0, 176)
         .alpha(overlayAlpha)
         .pos(drawX, drawY);
      topList.add(new MobDrawable() {
         @Override
         public void draw(TickManager tickManager) {
            try {
               GameResources.sceneShader.use();
               GameResources.sceneShader.pass1f("whiteness", whiteness);
               hornOptions.draw();
               hornOverlayOptions.draw();
            } finally {
               GameResources.sceneShader.pass1f("whiteness", 0.0F);
               GameResources.sceneShader.stop();
            }
         }
      });
      topList.add(100, new Drawable() {
         @Override
         public void draw(TickManager tickManager) {
            if (!TheVoidHornMob.this.isBroken) {
               try {
                  GameResources.sceneShader.use();
                  GameResources.sceneShader.pass1f("whiteness", whiteness);
                  orbOptions.draw();
                  orbOverlayOptions.draw();
               } finally {
                  GameResources.sceneShader.pass1f("whiteness", 0.0F);
                  GameResources.sceneShader.stop();
               }
            }
         }
      });
   }

   @Override
   public Mob getAttackOwner() {
      Mob master = this.master.get(this.getLevel());
      return master != null ? master : super.getAttackOwner();
   }

   @Override
   public GameMessage getAttackerName() {
      Mob master = this.master.get(this.getLevel());
      return master != null ? master.getAttackerName() : super.getAttackerName();
   }

   @Override
   public DeathMessageTable getDeathMessages() {
      Mob master = this.master.get(this.getLevel());
      return master != null ? master.getDeathMessages() : super.getDeathMessages();
   }
}
