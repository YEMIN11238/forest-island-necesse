package necesse.entity.mobs;

import java.awt.Point;
import java.util.List;
import java.util.stream.Stream;
import necesse.engine.gameLoop.tickManager.Performance;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.localization.message.GameMessage;
import necesse.engine.localization.message.StaticMessage;
import necesse.engine.modifiers.ModifierValue;
import necesse.engine.network.Packet;
import necesse.engine.registries.MobRegistry;
import necesse.engine.save.LoadData;
import necesse.engine.save.SaveData;
import necesse.engine.sound.SimpleSoundCooldown;
import necesse.engine.sound.SoundEffect;
import necesse.engine.sound.SoundManager;
import necesse.engine.sound.gameSound.GameSound;
import necesse.engine.util.GameRandom;
import necesse.entity.mobs.ability.EmptyMobAbility;
import necesse.entity.mobs.ai.behaviourTree.AINode;
import necesse.entity.mobs.ai.behaviourTree.AINodeResult;
import necesse.entity.mobs.ai.behaviourTree.BehaviourTreeAI;
import necesse.entity.mobs.ai.behaviourTree.Blackboard;
import necesse.entity.mobs.ai.behaviourTree.composites.SequenceAINode;
import necesse.entity.mobs.ai.behaviourTree.event.AIEvent;
import necesse.entity.mobs.ai.behaviourTree.leaves.ConfusedWandererAINode;
import necesse.entity.mobs.ai.behaviourTree.util.AIMover;
import necesse.entity.mobs.buffs.BuffModifiers;
import necesse.entity.mobs.buffs.staticBuffs.Buff;
import necesse.entity.particle.Particle;
import necesse.entity.particle.SmokePuffParticle;
import necesse.gfx.ThemeColorRegistry;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.DrawOptions;
import necesse.gfx.drawOptions.texture.TextureDrawOptions;
import necesse.gfx.drawables.OrderableDrawables;
import necesse.inventory.InventoryItem;
import necesse.level.maps.Level;
import necesse.level.maps.light.GameLight;

public abstract class PolymorphMob extends Mob implements MountAbility {
   public long removeAtTime;
   protected EmptyMobAbility spaceAbility;
   protected long spaceAbilityLastUseTime;
   protected int spaceAbilityCooldown = 700;

   public PolymorphMob() {
      super(100);
      this.registerAbility(this.spaceAbility = new EmptyMobAbility() {
         @Override
         protected void run() {
            if (PolymorphMob.this.getTime() >= PolymorphMob.this.spaceAbilityLastUseTime && PolymorphMob.this.isClient()) {
               PolymorphMob.this.spaceAbilityLastUseTime = PolymorphMob.this.getTime() + PolymorphMob.this.spaceAbilityCooldown;
               PolymorphMob.this.spaceAbility();
            }
         }
      });
   }

   protected void spaceAbility() {
      SoundManager.playSound(
         this.getMobSound(),
         SoundEffect.effect(this).pitch(GameRandom.globalRandom.getFloatBetween(1.0F, 1.25F)).volume(this.getMobSoundVolume()),
         new SimpleSoundCooldown(this.spaceAbilityCooldown)
      );
   }

   protected float getMobSoundVolume() {
      return 1.0F;
   }

   protected abstract GameSound getMobSound();

   protected abstract Buff getPolymorphBuff();

   @Override
   public void addSaveData(SaveData save) {
      super.addSaveData(save);
      save.addLong("removeAtTime", this.removeAtTime);
   }

   @Override
   public void applyLoadData(LoadData save) {
      super.applyLoadData(save);
      this.removeAtTime = save.getLong("removeAtTime");
   }

   @Override
   public void init() {
      super.init();
      SequenceAINode<PolymorphMob> sequenceAI = new SequenceAINode<>();
      this.ai = new BehaviourTreeAI<>(this, sequenceAI, new AIMover());
      sequenceAI.addChild(new AINode<PolymorphMob>() {
         private long ticksToNextBork = 0L;

         protected void onRootSet(AINode<PolymorphMob> root, PolymorphMob mob, Blackboard<PolymorphMob> blackboard) {
            this.setNextAbilityTime();
         }

         private void setNextAbilityTime() {
            this.ticksToNextBork = GameRandom.globalRandom.getIntBetween(40, 80);
         }

         public void init(PolymorphMob mob, Blackboard<PolymorphMob> blackboard) {
         }

         public AINodeResult tick(PolymorphMob mob, Blackboard<PolymorphMob> blackboard) {
            if (this.ticksToNextBork-- <= 0L) {
               this.setNextAbilityTime();
               PolymorphMob.this.spaceAbility.runAndSend();
            }

            return AINodeResult.SUCCESS;
         }
      });
      ConfusedWandererAINode<PolymorphMob> confusedWandererAINode = new ConfusedWandererAINode<>();
      confusedWandererAINode.confusionTimer = Long.MAX_VALUE;
      sequenceAI.addChild(confusedWandererAINode);
      if (this.isClient()) {
         Level level = this.getLevel();
         level.entityManager
            .addParticle(new SmokePuffParticle(level, this.x, this.y + 5.0F, ThemeColorRegistry.POLYMORPH.getRandomColor()), Particle.GType.IMPORTANT_COSMETIC);
      }
   }

   @Override
   protected boolean shouldSendMovementPacketWithRider(Mob rider) {
      return !rider.isPlayer;
   }

   @Override
   public void tickCurrentMovement(float delta) {
      this.moveX = 0.0F;
      this.moveY = 0.0F;
      Mob mounted = this.getRider();
      if (this.isMounted() && mounted != null && mounted.isPlayer) {
         this.setDir(mounted.getDir());
         this.moveX = mounted.moveX;
         this.moveY = mounted.moveY;
      } else if (this.currentMovement != null) {
         boolean lastArrivedAtTarget = this.hasArrivedAtTarget;
         this.hasArrivedAtTarget = this.currentMovement.tick(this);
         if (this.hasArrivedAtTarget) {
            if (!lastArrivedAtTarget) {
               this.onArrivedAtTarget();
            }

            if (this.stopMoveWhenArrive) {
               this.stopMoving();
            }
         }
      } else {
         this.hasArrivedAtTarget = true;
      }
   }

   @Override
   public void serverTick() {
      super.serverTick();
      Performance.record(this.getLevel().tickManager(), "ai", () -> {
         if (!this.isMounted() || this.getRider() == null || !this.getRider().isPlayer) {
            this.ai.tick();
         }
      });
      if (this.getTime() <= this.removeAtTime && this.isMounted()) {
         Mob rider = this.getRider();
         if (rider != null && !rider.buffManager.hasBuff(this.getPolymorphBuff())) {
            this.remove();
         }
      } else {
         Mob rider = this.getRider();
         if (rider != null && !rider.isPlayer && rider.ai != null) {
            rider.ai.blackboard.mover.stopMoving(rider);
            rider.ai.blackboard.submitEvent("resetPathTime", new AIEvent());
         }

         this.remove();
      }
   }

   @Override
   public void remove(float knockbackX, float knockbackY, Attacker attacker, boolean isDeath) {
      Level level = this.getLevel();
      level.entityManager
         .addParticle(new SmokePuffParticle(level, this.x, this.y + 5.0F, ThemeColorRegistry.POLYMORPH.getRandomColor()), Particle.GType.IMPORTANT_COSMETIC);
      super.remove(knockbackX, knockbackY, attacker, isDeath);
   }

   @Override
   public boolean isVisible() {
      return this.isMounted();
   }

   @Override
   public boolean canLevelInteract() {
      return this.isMounted();
   }

   @Override
   public boolean canPushMob(Mob other) {
      return this.isMounted();
   }

   @Override
   public boolean canBePushed(Mob other) {
      return this.isMounted();
   }

   @Override
   public boolean canTakeDamage() {
      return false;
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
      if (this.isMounted()) {
         GameLight light = level.getLightLevel(getTileCoordinate(x), getTileCoordinate(y));
         int drawX = camera.getDrawX(x) - 22 - 10;
         int drawY = camera.getDrawY(y) - 44 - 7;
         int dir = this.getDir();
         Point sprite = this.getAnimSprite(x, y, dir);
         drawY += this.getBobbing(x, y);
         TextureDrawOptions shadow = MobRegistry.Textures.chicken_shadow.initDraw().sprite(0, dir, 64).light(light).pos(drawX, drawY);
         tileList.add(tm -> shadow.draw());
         drawY += level.getTile(getTileCoordinate(x), getTileCoordinate(y)).getMobSinkingAmount(this);
         final MaskShaderOptions swimMask = this.getSwimMaskShaderOptions(this.inLiquidFloat(x, y));
         final DrawOptions options = MobRegistry.Textures.chicken
            .initDraw()
            .sprite(sprite.x, sprite.y, 64)
            .light(light)
            .addMaskShader(swimMask)
            .pos(drawX, drawY);
         list.add(new MobDrawable() {
            @Override
            public void draw(TickManager tickManager) {
               swimMask.use();
               options.draw();
               swimMask.stop();
            }
         });
      }
   }

   @Override
   protected int getRockSpeed() {
      return 10;
   }

   @Override
   public boolean shouldDrawRider() {
      return false;
   }

   @Override
   public boolean forceFollowRiderLevelChange(Mob rider) {
      return true;
   }

   @Override
   public GameMessage getMountDismountError(Mob rider, InventoryItem item) {
      return new StaticMessage("");
   }

   @Override
   public Stream<ModifierValue<?>> getDefaultRiderModifiers() {
      return Stream.of(new ModifierValue<>(BuffModifiers.INTIMIDATED, true));
   }

   @Override
   public Stream<ModifierValue<?>> getDefaultModifiers() {
      if (this.isMounted()) {
         Mob rider = this.getRider();
         if (rider != null) {
            return Stream.concat(
               super.getDefaultRiderModifiers(),
               Stream.of(
                  new ModifierValue<>(BuffModifiers.GROUNDED, rider.buffManager.getModifier(BuffModifiers.GROUNDED)),
                  new ModifierValue<>(BuffModifiers.PARALYZED, rider.buffManager.getModifier(BuffModifiers.PARALYZED))
               )
            );
         }
      }

      return super.getDefaultModifiers();
   }

   @Override
   public void runMountAbility(PlayerMob player, Packet content) {
      if (this.isServer()) {
         this.spaceAbility.runAndSend();
      }
   }

   @Override
   public boolean canRunMountAbility(PlayerMob player, Packet content) {
      return true;
   }
}
