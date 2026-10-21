package necesse.entity.mobs.summon.summonFollowingMob.mountFollowingMob;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Point2D.Float;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.input.Control;
import necesse.engine.modifiers.ModifierValue;
import necesse.engine.network.Packet;
import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.engine.registries.EnchantmentRegistry;
import necesse.engine.registries.MobRegistry;
import necesse.engine.sound.SoundEffect;
import necesse.engine.sound.SoundManager;
import necesse.engine.sound.SoundSettings;
import necesse.engine.util.GameMath;
import necesse.engine.util.GameRandom;
import necesse.engine.util.GameUtils;
import necesse.entity.mobs.ActiveMountAbility;
import necesse.entity.mobs.GameDamage;
import necesse.entity.mobs.MaskShaderOptions;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.MobDrawable;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.TrainingDummyMob;
import necesse.entity.mobs.ai.behaviourTree.BehaviourTreeAI;
import necesse.entity.mobs.ai.behaviourTree.trees.PlayerFollowerChaserAI;
import necesse.entity.mobs.buffs.BuffModifiers;
import necesse.entity.mobs.itemAttacker.CheckSlotType;
import necesse.entity.mobs.itemAttacker.ItemAttackerMob;
import necesse.entity.mobs.summon.summonFollowingMob.attackingFollowingMob.ToolItemSummonedMob;
import necesse.entity.particle.FleshParticle;
import necesse.entity.particle.Particle;
import necesse.entity.projectile.JunkMechMachineGunProjectile;
import necesse.entity.projectile.Projectile;
import necesse.entity.projectile.followingProjectile.MiniRocketProjectile;
import necesse.gfx.GameResources;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.DrawOptions;
import necesse.gfx.drawOptions.texture.TextureDrawOptions;
import necesse.gfx.drawables.OrderableDrawables;
import necesse.gfx.gameTexture.GameTexture;
import necesse.inventory.enchants.ToolItemEnchantment;
import necesse.inventory.enchants.ToolItemModifiers;
import necesse.inventory.item.Item;
import necesse.level.maps.Level;
import necesse.level.maps.light.GameLight;

public class JunkMechMob extends MountFollowingMob implements ToolItemSummonedMob, ActiveMountAbility {
   public final int salvoBurstCount = 10;
   public final int baseActiveSalvoCooldown = 3000;
   public final int salvoMsBetweenAttacks = 100;
   public int activeSalvoCooldown;
   public int salvosRemaining = 10;
   protected GameDamage summonDamage;
   private ToolItemEnchantment summonEnchantment = ToolItemEnchantment.noEnchant;
   private ModifierValue<?>[] summonModifiers = new ModifierValue[0];
   private boolean fireLeftMachineGun;
   private boolean fireLeftRocket;
   private Mob activeTarget;

   public JunkMechMob() {
      super(50);
      this.setSpeed(65.0F);
      this.setFriction(3.0F);
      this.setSwimSpeed(0.4F);
      this.moveAccuracy = 10;
      this.collision = new Rectangle(-10, -7, 20, 14);
      this.hitBox = new Rectangle(-16, -12, 32, 24);
      this.selectBox = new Rectangle(-18, -83, 36, 90);
      this.swimMaskMove = 32;
      this.swimMaskOffset = -32;
      this.swimSinkOffset = -2;
   }

   @Override
   public boolean canRunMountAbility(PlayerMob player, Packet content) {
      return true;
   }

   @Override
   public void onActiveMountAbilityStarted(PlayerMob player, Packet content) {
      this.fireLeftMachineGun = true;
   }

   @Override
   public boolean tickActiveMountAbility(PlayerMob player, boolean isRunningClient) {
      int checkForMobsRange = 320;
      Mob target = null;
      ItemAttackerMob followingAttacker = this.getFollowingItemAttacker();
      if (followingAttacker != null) {
         int summonFocusUniqueID = followingAttacker.getSummonFocusUniqueID();
         if (summonFocusUniqueID != -1) {
            target = GameUtils.streamTargets(player, GameUtils.rangeBounds(this.x, this.y, checkForMobsRange))
               .filter(m -> m.getDistance(this) <= checkForMobsRange)
               .filter(m -> m.getUniqueID() == summonFocusUniqueID)
               .findAny()
               .orElse(null);
         }
      }

      if (target == null) {
         target = GameUtils.streamTargets(player, GameUtils.rangeBounds(this.x, this.y, checkForMobsRange))
            .filter(m -> player.isHostile || m.isHostile || m instanceof TrainingDummyMob)
            .filter(m -> m.getDistance(this) <= checkForMobsRange)
            .min(Comparator.comparing(m -> m.getDistance(this.x, this.y)))
            .orElse(null);
      }

      if (target != null) {
         this.activeTarget = target;
         float targetX = this.activeTarget.x + GameRandom.globalRandom.getIntBetween(-16, 16);
         float targetY = this.activeTarget.y + GameRandom.globalRandom.getIntBetween(-16, 16);
         this.salvoTick();
         Point attackPos = this.getAttackOffset(this.fireLeftMachineGun);
         this.fireLeftMachineGun = !this.fireLeftMachineGun;
         if (this.isServer()) {
            this.attack(this.activeTarget.getX(), this.activeTarget.getY(), false);
            GameDamage damage = this.summonDamage;
            Mob followingMob = this.getFollowingMob();
            if (followingMob != null) {
               damage = damage.modDamage(followingMob.buffManager.getModifier(BuffModifiers.MAX_SUMMONS).intValue() / 2.0F);
            }

            Projectile machineGunProjectile = new JunkMechMachineGunProjectile(
               this.x + attackPos.x, this.y + attackPos.y - this.getCurrentAttackHeight(), targetX, targetY, 800.0F, 1000, damage, 5, this
            );
            machineGunProjectile.moveDist(32.0);
            this.getLevel().entityManager.projectiles.add(machineGunProjectile);
         } else {
            SoundManager.playSound(GameResources.handgun, SoundEffect.effect(player).volume(0.3F).pitch(GameRandom.globalRandom.getFloatBetween(0.15F, 0.25F)));
            SoundManager.playSound(
               GameResources.crystalHit2, SoundEffect.effect(player).pitch(GameRandom.globalRandom.getFloatBetween(1.4F, 1.6F)).volume(0.3F)
            );
            float xMove = GameRandom.globalRandom.getFloatBetween(-0.05F, 0.05F);
            float yStart = this.y + attackPos.y - this.getCurrentAttackHeight();
            float var10001 = this.x + attackPos.x;
            float var10002 = this.y + attackPos.y;
            this.getLevel()
               .entityManager
               .addParticle(var10001, var10002, Particle.GType.COSMETIC)
               .sprite(GameResources.bulletCasingParticles.sprite(0, 0, 6, 8))
               .moves((pos, delta, lifeTime, timeAlive, lifePercent) -> {
                  pos.x = pos.x + (xMove + attackPos.x / 200.0F * (1.0F - lifePercent));
                  pos.y = (float)(yStart + attackPos.y * lifePercent - 5.0 * Math.abs(1.3F * Math.cos(5.0F * lifePercent)));
               })
               .rotates(360.0F, 720.0F)
               .size((options, lifeTime, timeAlive, lifePercent) -> options.size(6, 8))
               .fadesAlpha(0.0F, 0.2F)
               .lifeTimeBetween(500, 1000);
         }
      } else {
         this.activeTarget = null;
      }

      return !isRunningClient || Control.TRINKET_ABILITY.isDown();
   }

   @Override
   public void onActiveMountAbilityUpdate(PlayerMob player, Packet content) {
   }

   @Override
   public void onActiveMountAbilityStopped(PlayerMob player) {
      this.activeTarget = null;
   }

   @Override
   public void init() {
      super.init();
      this.ai = new BehaviourTreeAI<>(
         this,
         new PlayerFollowerChaserAI<JunkMechMob>(576, 320, false, false, 640, 64) {
            public boolean attackTarget(JunkMechMob mob, Mob target) {
               if (mob.canAttack()) {
                  if (JunkMechMob.this.salvosRemaining <= 0) {
                     JunkMechMob.this.salvosRemaining = 10;
                  }

                  JunkMechMob.this.salvosRemaining--;
                  GameDamage damage = JunkMechMob.this.summonDamage;
                  Mob followingMob = JunkMechMob.this.getFollowingMob();
                  if (followingMob != null) {
                     damage = damage.modDamage(followingMob.buffManager.getModifier(BuffModifiers.MAX_SUMMONS).intValue());
                  }

                  mob.attack(target.getX(), target.getY(), true);
                  Point attackOffset = JunkMechMob.this.getAttackOffset(JunkMechMob.this.fireLeftRocket);
                  JunkMechMob.this.fireLeftRocket = !JunkMechMob.this.fireLeftRocket;
                  Projectile projectile = new MiniRocketProjectile(
                     JunkMechMob.this.getLevel(),
                     mob,
                     mob.x + attackOffset.x,
                     mob.y + attackOffset.y,
                     JunkMechMob.this.x,
                     JunkMechMob.this.y,
                     150.0F,
                     640,
                     damage,
                     10
                  );
                  projectile.setTargetPrediction(target, -20.0F);
                  projectile.moveDist(20.0);
                  mob.getLevel().entityManager.projectiles.add(projectile);
                  return true;
               } else {
                  return false;
               }
            }
         }
      );
   }

   @Override
   public void applySpawnPacket(PacketReader reader) {
      super.applySpawnPacket(reader);
      this.setEnchantment(EnchantmentRegistry.getEnchantment(reader.getNextShortUnsigned(), ToolItemEnchantment.class, ToolItemEnchantment.noEnchant));
   }

   @Override
   public void setupSpawnPacket(PacketWriter writer) {
      super.setupSpawnPacket(writer);
      writer.putNextShortUnsigned(this.summonEnchantment.getID());
   }

   @Override
   public void clientTick() {
      super.clientTick();
      if (this.activeSalvoCooldown > 0) {
         this.activeSalvoCooldown -= 50;
      }

      if (this.currentSpeed > 0.0F) {
         float idleToDriveLerp = GameMath.limit(this.currentSpeed / 100.0F, 0.0F, 1.0F);
         float pitchOffset = GameRandom.globalRandom.getFloatBetween(1.1F, 1.25F);
         SoundManager.playSound(
            GameResources.junkMechDriving, SoundEffect.effect(this).volume(idleToDriveLerp / 2.0F).pitch(1.0F + idleToDriveLerp / pitchOffset)
         );
      }
   }

   @Override
   public void serverTick() {
      super.serverTick();
      if (this.activeSalvoCooldown > 0) {
         this.activeSalvoCooldown -= 50;
      }
   }

   private void salvoTick() {
      if (this.activeSalvoCooldown <= 0 && this.activeTarget != null) {
         float targetX = this.activeTarget.x + GameRandom.globalRandom.getIntBetween(-16, 16);
         float targetY = this.activeTarget.y + GameRandom.globalRandom.getIntBetween(-16, 16);
         if (this.isServer()) {
            GameDamage damage = this.summonDamage;
            Mob followingMob = this.getFollowingMob();
            if (followingMob != null) {
               damage = damage.modDamage(followingMob.buffManager.getModifier(BuffModifiers.MAX_SUMMONS).intValue());
            }

            Point attackOffset = this.getAttackOffset(this.fireLeftRocket);
            this.fireLeftRocket = !this.fireLeftRocket;
            Projectile projectile = new MiniRocketProjectile(
               this.getLevel(), this, this.x + attackOffset.x, this.y + attackOffset.y, targetX, targetY, 150.0F, 640, damage, 10
            );
            projectile.setTargetPrediction(this.activeTarget, -20.0F);
            projectile.moveDist(20.0);
            this.getLevel().entityManager.projectiles.add(projectile);
         } else {
            SoundManager.playSound(GameResources.fizz, SoundEffect.effect(this).volume(0.4F).pitch(2.0F));
         }

         if (this.salvosRemaining < 1) {
            this.activeSalvoCooldown = 3000;
            this.salvosRemaining = 10;
         } else {
            this.salvosRemaining--;
            this.activeSalvoCooldown = 100;
         }
      }
   }

   private Point getParticleAttackOffset(boolean leftMachineGun) {
      if (this.activeTarget != null) {
         int offsetDist = 40;
         Float dirVector = GameMath.normalize(this.x - this.activeTarget.x, this.y - this.activeTarget.y);
         return leftMachineGun
            ? new Point((int)(-offsetDist * dirVector.y), (int)(offsetDist * dirVector.x) - this.getCurrentAttackHeight())
            : new Point((int)(offsetDist * dirVector.y), (int)(-offsetDist * dirVector.x) - this.getCurrentAttackHeight());
      } else {
         return new Point(0, 0);
      }
   }

   private Point getAttackOffset(boolean leftMachineGun) {
      switch (this.getFaceDirFromTargetPos()) {
         case 0:
            return new Point(leftMachineGun ? -38 : 38, 0);
         case 1:
            return new Point(leftMachineGun ? 10 : -10, leftMachineGun ? -14 : 6);
         case 2:
            return new Point(leftMachineGun ? 38 : -38, 0);
         case 3:
            return new Point(leftMachineGun ? 10 : -10, leftMachineGun ? 6 : -14);
         default:
            return new Point(0, 0);
      }
   }

   private Point getGunsDrawOffset(boolean leftMachineGun) {
      switch (this.getFaceDirFromTargetPos()) {
         case 0:
            return new Point(leftMachineGun ? -36 : -2, -39);
         case 1:
            return new Point(leftMachineGun ? 0 : -50, -39);
         case 2:
            return new Point(leftMachineGun ? 20 : -58, -49);
         case 3:
            return new Point(leftMachineGun ? 12 : -38, -39);
         default:
            return new Point(0, 0);
      }
   }

   private int getFaceDirFromTargetPos() {
      if (this.activeTarget == null) {
         return this.getDir();
      }

      Float dirVector = GameMath.normalize(this.x - this.activeTarget.x, this.y - this.activeTarget.y);
      float threshold = 0.2F;
      if (this.prioritizeVerticalDir) {
         if (Math.abs(dirVector.x) - Math.abs(dirVector.y) <= threshold) {
            return dirVector.y < 0.0F ? 2 : 0;
         } else {
            return dirVector.x < 0.0F ? 1 : 3;
         }
      } else if (Math.abs(dirVector.y) - Math.abs(dirVector.x) <= threshold) {
         return dirVector.x < 0.0F ? 1 : 3;
      } else {
         return dirVector.y < 0.0F ? 2 : 0;
      }
   }

   @Override
   public void spawnDeathParticles(float knockbackX, float knockbackY) {
      for (int i = 0; i < 4; i++) {
         this.getLevel()
            .entityManager
            .addParticle(
               new FleshParticle(
                  this.getLevel(), MobRegistry.Textures.ostrich, GameRandom.globalRandom.nextInt(5), 12, 32, this.x, this.y, 10.0F, knockbackX, knockbackY
               ),
               Particle.GType.IMPORTANT_COSMETIC
            );
      }
   }

   @Override
   protected void calcAcceleration(float speed, float friction, float moveX, float moveY, float delta) {
      super.calcAcceleration(speed, friction, moveX, moveY, delta);
      if (moveX != 0.0F || moveY != 0.0F) {
         this.setFacingDir(moveX, moveY);
      }
   }

   @Override
   public void updateAttackDir(int x, int y, boolean showAllDirections) {
      int lastDir = this.getDir();
      super.updateAttackDir(x, y, showAllDirections);
      this.setDir(lastDir);
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
      int drawX = camera.getDrawX(x) - 48;
      int drawY = camera.getDrawY(y) - 44 - 32 - 11;
      boolean hasTarget = this.activeTarget != null;
      int targetDir = this.getFaceDirFromTargetPos();
      Point bodySprite = this.getAnimSprite(x, y, targetDir);
      Point legSprite = this.getAnimSprite(x, y, this.getDir());
      drawY += level.getTile(getTileCoordinate(x), getTileCoordinate(y)).getMobSinkingAmount(this);
      final MaskShaderOptions swimMask = this.getSwimMaskShaderOptions(this.inLiquidFloat(x, y));
      final DrawOptions body = MobRegistry.Textures.junkMech
         .initDraw()
         .sprite(bodySprite.x, bodySprite.y, 96, 96)
         .addMaskShader(swimMask)
         .light(light)
         .pos(drawX, drawY);
      final DrawOptions legs = MobRegistry.Textures.junkMech_legs
         .initDraw()
         .sprite(legSprite.x, legSprite.y, 96, 96)
         .addMaskShader(swimMask)
         .light(light)
         .pos(drawX, drawY);
      final DrawOptions overlap;
      if (!this.isMounted()) {
         overlap = () -> {};
      } else {
         overlap = MobRegistry.Textures.junkMech
            .initDraw()
            .sprite(bodySprite.x, bodySprite.y + 4, 96, 96)
            .addMaskShader(swimMask)
            .light(light)
            .pos(drawX, drawY);
      }

      int anim = GameUtils.getAnim(this.getWorldEntity().getTime(), 2, 100);
      Point leftAttackOffset = this.getAttackOffset(true);
      Point rightAttackOffset = this.getAttackOffset(false);
      Point drawOffsetLeft = this.getGunsDrawOffset(true);
      Point drawOffsetRight = this.getGunsDrawOffset(false);
      final DrawOptions machineGunLeftLight;
      final DrawOptions machineGunRightLight;
      float leftRotateAngle;
      float rightRotateAngle;
      if (hasTarget) {
         Point leftToTargetVector = new Point((int)(x + leftAttackOffset.x - this.activeTarget.x), (int)(y + leftAttackOffset.y - this.activeTarget.y));
         leftRotateAngle = (float)Math.toDegrees(Math.atan2(leftToTargetVector.y, leftToTargetVector.x)) + 90.0F;
         machineGunLeftLight = MobRegistry.Textures.junkMech_guns
            .initDraw()
            .sprite(anim + 1, 1, 38, 66)
            .addMaskShader(swimMask)
            .light(light.minLevelCopy(150.0F))
            .rotate(leftRotateAngle, 8, 8)
            .pos(camera.getDrawX(x) + drawOffsetLeft.x, camera.getDrawY(y) + drawOffsetLeft.y);
         Point rightToTargetVector = new Point((int)(x + rightAttackOffset.x - this.activeTarget.x), (int)(y + rightAttackOffset.y - this.activeTarget.y));
         rightRotateAngle = (float)Math.toDegrees(Math.atan2(rightToTargetVector.y, rightToTargetVector.x)) + 90.0F;
         machineGunRightLight = MobRegistry.Textures.junkMech_guns
            .initDraw()
            .sprite(anim + 1, 0, 38, 66)
            .addMaskShader(swimMask)
            .light(light.minLevelCopy(150.0F))
            .rotate(rightRotateAngle, 30, 8)
            .pos(camera.getDrawX(x) + drawOffsetRight.x, camera.getDrawY(y) + drawOffsetRight.y);
      } else {
         machineGunLeftLight = () -> {};
         machineGunRightLight = () -> {};
         leftRotateAngle = this.getFaceDirFromTargetPos() * 90 + 180;
         rightRotateAngle = this.getFaceDirFromTargetPos() * 90 + 180;
      }

      final DrawOptions machineGunLeft = MobRegistry.Textures.junkMech_guns
         .initDraw()
         .sprite(0, 1, 38, 66)
         .addMaskShader(swimMask)
         .light(light)
         .rotate(leftRotateAngle, 8, 8)
         .pos(camera.getDrawX(x) + drawOffsetLeft.x, camera.getDrawY(y) + drawOffsetLeft.y);
      final DrawOptions machineGunRight = MobRegistry.Textures.junkMech_guns
         .initDraw()
         .sprite(0, 0, 38, 66)
         .addMaskShader(swimMask)
         .light(light)
         .rotate(rightRotateAngle, 30, 8)
         .pos(camera.getDrawX(x) + drawOffsetRight.x, camera.getDrawY(y) + drawOffsetRight.y);
      list.add(new MobDrawable() {
         @Override
         public void draw(TickManager tickManager) {
            swimMask.use();
            overlap.draw();
            if (JunkMechMob.this.getFaceDirFromTargetPos() == 1 || JunkMechMob.this.getFaceDirFromTargetPos() == 3) {
               if (JunkMechMob.this.getFaceDirFromTargetPos() == 1) {
                  machineGunRight.draw();
                  machineGunRightLight.draw();
               } else {
                  machineGunLeft.draw();
                  machineGunLeftLight.draw();
               }
            }

            swimMask.stop();
         }

         @Override
         public void drawBehindRider(TickManager tickManager) {
            swimMask.use();
            legs.draw();
            if (JunkMechMob.this.getFaceDirFromTargetPos() != 1 && JunkMechMob.this.getFaceDirFromTargetPos() != 3) {
               body.draw();
               machineGunLeft.draw();
               machineGunRight.draw();
               machineGunLeftLight.draw();
               machineGunRightLight.draw();
            } else if (JunkMechMob.this.getFaceDirFromTargetPos() == 1) {
               machineGunLeft.draw();
               machineGunLeftLight.draw();
               body.draw();
            } else {
               machineGunRight.draw();
               machineGunRightLight.draw();
               body.draw();
            }

            swimMask.stop();
         }
      });
      TextureDrawOptions shadow = MobRegistry.Textures.junkMech_shadow.initDraw().sprite(0, 0, 96).light(light).pos(drawX, drawY + 12);
      tileList.add(tm -> shadow.draw());
   }

   @Override
   public int getRockSpeed() {
      return 20;
   }

   @Override
   public Point getSpriteOffset(int spriteX, int spriteY) {
      Point p = new Point(0, 0);
      p.x = p.x + this.getRiderDrawXOffset();
      p.y = p.y + this.getRiderDrawYOffset();
      return p;
   }

   @Override
   public int getRiderDrawYOffset() {
      return this.getSwimMaskShaderOptions(this.inLiquidFloat(this.getDrawX(), this.getDrawY())).drawYOffset - 38;
   }

   @Override
   public GameTexture getRiderMask() {
      return MobRegistry.Textures.mountmask;
   }

   @Override
   protected void doMountedLogic() {
      if (!this.isServer()) {
         int particleCount = 40;

         for (int i = 0; i < particleCount; i++) {
            this.getLevel()
               .entityManager
               .addParticle(
                  this.x + (float)(GameRandom.globalRandom.nextGaussian() * 8.0),
                  this.y + 16.0F + (float)(GameRandom.globalRandom.nextGaussian() * 8.0),
                  Particle.GType.IMPORTANT_COSMETIC
               )
               .sprite(GameResources.smokePuff.sprite(0, 0, 32))
               .lifeTime(750)
               .fadesAlphaTime(100, 250)
               .movesFriction(16.0F * (float)GameRandom.globalRandom.nextGaussian(), 5.0F * (float)GameRandom.globalRandom.nextGaussian(), 1.0F)
               .sizeFades(14, 18)
               .heightMoves(20.0F, 64.0F);
         }
      }
   }

   @Override
   protected SoundSettings getAmbientSound() {
      return new SoundSettings(GameResources.ostrichAmbient).volume(1.1F);
   }

   @Override
   public void updateDamage(GameDamage damage) {
      this.summonDamage = damage;
   }

   @Override
   public void setEnchantment(ToolItemEnchantment enchantment) {
      this.summonEnchantment = enchantment;
      this.summonModifiers = new ModifierValue[]{
         new ModifierValue<>(BuffModifiers.ALL_DAMAGE, enchantment.getModifier(ToolItemModifiers.DAMAGE)),
         new ModifierValue<>(BuffModifiers.SPEED, enchantment.getModifier(ToolItemModifiers.SUMMONS_SPEED)),
         new ModifierValue<>(BuffModifiers.CHASER_RANGE, enchantment.getModifier(ToolItemModifiers.SUMMONS_TARGET_RANGE)),
         new ModifierValue<>(BuffModifiers.KNOCKBACK_OUT, enchantment.getModifier(ToolItemModifiers.KNOCKBACK))
      };
   }

   @Override
   public void setRemoveWhenNotInInventory(Item summonItem, CheckSlotType slotType) {
      this.removeWhenNotInInventoryItem = summonItem;
      this.removeWhenNotInInventorySlotType = slotType;
   }

   @Override
   public Stream<ModifierValue<?>> getDefaultModifiers() {
      return this.summonModifiers == null ? super.getDefaultModifiers() : Stream.of(this.summonModifiers);
   }

   @Override
   public boolean canAttack() {
      return !this.buffManager.getModifier(BuffModifiers.INTIMIDATED) && !this.buffManager.getModifier(BuffModifiers.PARALYZED)
         ? super.canAttack() || this.salvosRemaining > 0 && this.getTimeSinceLastAttack() >= 100L
         : false;
   }

   @Override
   public int getRiderDir(int startDir) {
      return this.getFaceDirFromTargetPos();
   }

   @Override
   public void updateRider() {
      super.updateRider();
   }
}
