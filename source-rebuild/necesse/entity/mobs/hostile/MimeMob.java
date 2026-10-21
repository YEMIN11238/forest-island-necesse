package necesse.entity.mobs.hostile;

import java.awt.Color;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.List;
import java.util.function.Supplier;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.engine.network.gameNetworkData.GNDItemGameDamage;
import necesse.engine.save.LoadData;
import necesse.engine.save.SaveData;
import necesse.engine.sound.SoundEffect;
import necesse.engine.sound.SoundManager;
import necesse.engine.util.GameMath;
import necesse.engine.util.GameRandom;
import necesse.entity.levelEvent.mobAbilityLevelEvent.ToolItemMobAbilityEvent;
import necesse.entity.mobs.DeathMessageTable;
import necesse.entity.mobs.GameDamage;
import necesse.entity.mobs.MaskShaderOptions;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.MobDrawable;
import necesse.entity.mobs.PathDoorOption;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.ability.IntMobAbility;
import necesse.entity.mobs.ai.behaviourTree.BehaviourTreeAI;
import necesse.entity.mobs.ai.behaviourTree.event.AIEvent;
import necesse.entity.mobs.ai.behaviourTree.trees.ConfusedPlayerChaserWandererAI;
import necesse.entity.mobs.buffs.BuffModifiers;
import necesse.entity.mobs.friendly.human.HumanMob;
import necesse.entity.mobs.itemAttacker.CheckSlotType;
import necesse.entity.mobs.itemAttacker.ItemAttackSlot;
import necesse.entity.particle.Particle;
import necesse.gfx.GameHair;
import necesse.gfx.GameResources;
import necesse.gfx.HumanGender;
import necesse.gfx.HumanLook;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.DrawOptions;
import necesse.gfx.drawOptions.human.HumanDrawOptions;
import necesse.gfx.drawables.OrderableDrawables;
import necesse.inventory.InventoryItem;
import necesse.inventory.item.Item;
import necesse.inventory.lootTable.LootTable;
import necesse.inventory.lootTable.lootItem.LootItem;
import necesse.level.maps.Level;
import necesse.level.maps.light.GameLight;

public class MimeMob extends HostileItemAttackerMob {
   public static LootTable lootTable = new LootTable(new LootItem("mimeberet"), new LootItem("mimeshirt"), new LootItem("mimeboots"));
   public static GameDamage damage = new GameDamage(40.0F);
   public int lookSeed;
   public HumanLook look = new HumanLook();
   public InventoryItem helmet;
   public InventoryItem chest;
   public InventoryItem boots;
   protected long startSwingAttackTime;
   protected int swingAttackDir;
   protected final IntMobAbility startSwingAttackAbility;
   protected final IntMobAbility fireSwingAttackAbility;
   protected int swingAttackChargeUpTime = 750;
   protected int swingAttackSlashTime = 200;

   public MimeMob() {
      super(300);
      this.setSpeed(20.0F);
      this.setFriction(3.0F);
      this.setKnockbackModifier(0.5F);
      this.setArmor(12);
      this.getLookSeed();
      this.collision = new Rectangle(-10, -7, 20, 14);
      this.hitBox = new Rectangle(-14, -12, 28, 24);
      this.selectBox = new Rectangle(-14, -41, 28, 48);
      this.swimMaskMove = 16;
      this.swimMaskOffset = -2;
      this.swimSinkOffset = -4;
      this.startSwingAttackAbility = this.registerAbility(new IntMobAbility() {
         @Override
         protected void run(int value) {
            MimeMob.this.startSwingAttackTime = MimeMob.this.getTime();
            MimeMob.this.attackAnimTime = MimeMob.this.swingAttackChargeUpTime + 500;
            MimeMob.this.attackCooldown = MimeMob.this.swingAttackChargeUpTime + 1000;
            MimeMob.this.swingAttackDir = value;
            MimeMob.this.setDir(value);
            Point vector = MimeMob.this.getDirVector();
            MimeMob.this.attack(MimeMob.this.getX() + vector.x * 100, MimeMob.this.getY() + vector.y * 100, true);
            MimeMob.this.stopMoving();
         }
      });
      this.fireSwingAttackAbility = this.registerAbility(
         new IntMobAbility() {
            @Override
            protected void run(int value) {
               MimeMob.this.startSwingAttackTime = 0L;
               MimeMob.this.attackAnimTime = MimeMob.this.swingAttackSlashTime;
               MimeMob.this.attackCooldown = MimeMob.this.swingAttackSlashTime + 500;
               MimeMob.this.setDir(value);
               Point vector = MimeMob.this.getDirVector();
               int aimX = vector.x * 100;
               int aimY = vector.y * 100;
               MimeMob.this.attack(MimeMob.this.getX() + aimX, MimeMob.this.getY() + aimY, true);
               if (MimeMob.this.isServer()) {
                  InventoryItem attackItem = new InventoryItem("invisiblesword");
                  attackItem.getGndData().setItem("damage", new GNDItemGameDamage(MimeMob.damage));
                  MimeMob.this.getLevel()
                     .entityManager
                     .events
                     .add(
                        new ToolItemMobAbilityEvent(
                           MimeMob.this,
                           GameRandom.globalRandom.nextInt(),
                           attackItem,
                           aimX,
                           aimY,
                           MimeMob.this.swingAttackSlashTime,
                           MimeMob.this.swingAttackSlashTime
                        )
                     );
               } else {
                  SoundManager.playSound(GameResources.woodGreatsword3, SoundEffect.effect(MimeMob.this).volume(0.7F));
               }

               MimeMob.this.ai.blackboard.submitEvent("confuseAfterAttack", new AIEvent());
            }
         }
      );
   }

   @Override
   public void addSaveData(SaveData save) {
      super.addSaveData(save);
      save.addInt("lookSeed", this.lookSeed);
   }

   @Override
   public void applyLoadData(LoadData save) {
      super.applyLoadData(save);
      this.lookSeed = save.getInt("lookSeed", this.lookSeed);
      this.getLookSeed();
      this.updateLook();
   }

   @Override
   public void applySpawnPacket(PacketReader reader) {
      super.applySpawnPacket(reader);
      this.lookSeed = reader.getNextInt();
      this.updateLook();
   }

   @Override
   public void setupSpawnPacket(PacketWriter writer) {
      super.setupSpawnPacket(writer);
      writer.putNextInt(this.lookSeed);
   }

   @Override
   public void init() {
      super.init();
      this.updateLook();
      this.ai = new BehaviourTreeAI<>(this, new ConfusedPlayerChaserWandererAI<MimeMob>(null, 384, 60, 40000, false, false) {
         public boolean attackTarget(MimeMob mob, Mob target) {
            if (mob.canAttack()) {
               mob.showAttack(target.getX(), target.getY(), true);
               mob.startSwingAttackAbility.runAndSend(mob.getDir());
               this.wanderAfterAttack = true;
               return true;
            } else {
               return false;
            }
         }

         @Override
         protected int getRandomConfuseTime() {
            return GameRandom.globalRandom.getIntBetween(2000, 3000);
         }
      });
   }

   @Override
   public void clientTick() {
      super.clientTick();
   }

   @Override
   public void serverTick() {
      super.serverTick();
      if (this.startSwingAttackTime != 0L) {
         long timeSinceStartAttack = this.getTime() - this.startSwingAttackTime;
         if (timeSinceStartAttack >= this.swingAttackChargeUpTime) {
            this.fireSwingAttackAbility.runAndSend(this.swingAttackDir);
         }
      }
   }

   @Override
   public PathDoorOption getPathDoorOption() {
      if (this.getLevel() != null) {
         return this.buffManager.getModifier(BuffModifiers.CAN_BREAK_OBJECTS)
            ? this.getLevel().regionManager.CAN_BREAK_OBJECTS_OPTIONS
            : this.getLevel().regionManager.CAN_OPEN_DOORS_OPTIONS;
      } else {
         return null;
      }
   }

   @Override
   public LootTable getLootTable() {
      return lootTable;
   }

   @Override
   public DeathMessageTable getDeathMessages() {
      return this.getDeathMessages("mime", 3);
   }

   public void updateLook() {
      GameRandom random = new GameRandom(this.lookSeed);
      HumanGender gender = random.getOneOfWeighted(HumanGender.class, 60, HumanGender.MALE, 30, HumanGender.FEMALE, 10, HumanGender.NEUTRAL);
      this.look.setSkin(9);
      this.look.setEyeType(random.getOneOf(0, 1, 3, 5, 6, 7, 8, 9, 10));
      this.look.setEyeColor(random.getIntBetween(0, 10));
      this.look.setHair(GameHair.getRandomHairBasedOnGender(random, gender));
      this.look.setHairColor(random.getOneOf(0, 2, 4, 5, 10, 12));
      this.helmet = new InventoryItem("mimeberet");
      this.chest = new InventoryItem("mimeshirt");
      this.boots = new InventoryItem("mimeboots");
   }

   public void getLookSeed() {
      if (this.lookSeed == 0) {
         this.lookSeed = GameRandom.globalRandom.nextInt();
      }
   }

   @Override
   public void spawnDeathParticles(float knockbackX, float knockbackY) {
      for (int i = 0; i < 40; i++) {
         this.getLevel()
            .entityManager
            .addParticle(
               this.x + GameRandom.globalRandom.getIntBetween(-20, 20), this.y + GameRandom.globalRandom.getIntBetween(-20, 20), Particle.GType.COSMETIC
            )
            .sprite(GameResources.debrisParticles.sprite(GameRandom.globalRandom.nextInt(6), 0, 20))
            .color(GameRandom.globalRandom.nextBoolean() ? new Color(245, 240, 230) : new Color(225, 50, 75))
            .heightMoves(0.0F, 64.0F)
            .movesFriction(GameRandom.globalRandom.getIntBetween(-2, 2), GameRandom.globalRandom.getIntBetween(0, -5), -0.1F)
            .sizeFades(40, 20)
            .fadesAlpha(0.0F, 0.75F)
            .lifeTime(2000 + GameRandom.globalRandom.getIntBetween(0, 2000));
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
      boolean inLiquid = this.inLiquid(x, y);
      if (inLiquid) {
         sprite.x = 0;
      }

      drawY += this.getBobbing(x, y);
      drawY += level.getTile(getTileCoordinate(x), getTileCoordinate(y)).getMobSinkingAmount(this);
      MaskShaderOptions swimMask = this.getSwimMaskShaderOptions(this.inLiquidFloat(x, y));
      HumanDrawOptions humanOptions = new HumanDrawOptions(level, this.look, false)
         .sprite(sprite)
         .mask(swimMask)
         .dir(dir)
         .light(light)
         .applyEnemyTracker(this, perspective);
      float attackProgress = this.getAttackAnimProgress();
      if (this.isAttacking) {
         InventoryItem showItem = new InventoryItem("invisiblesword");
         if (this.startSwingAttackTime != 0L) {
            long timeSinceStartAttack = this.getTime() - this.startSwingAttackTime;
            float progress = GameMath.limit((float)timeSinceStartAttack / this.swingAttackChargeUpTime, 0.0F, 1.0F);
            showItem.getGndData().setFloat("chargePercent", progress);
            showItem.getGndData().setBoolean("charging", true);
         }

         humanOptions.itemAttack(showItem, null, attackProgress, this.attackDir.x, this.attackDir.y);
      }

      humanOptions.helmet(this.helmet);
      humanOptions.chestplate(this.chest);
      humanOptions.boots(this.boots);
      this.setupAttackDraw(humanOptions);
      final DrawOptions drawOptions = humanOptions.pos(drawX, drawY);
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
      return 16;
   }

   @Override
   public float getAttackingMovementModifier() {
      return 0.5F;
   }

   @Override
   public ItemAttackSlot getCurrentSelectedAttackSlot() {
      return null;
   }

   @Override
   public boolean hasValidSummonItem(Item item, CheckSlotType slotType) {
      return true;
   }

   @Override
   public boolean canBeTargetedByHumans(HumanMob humanMob) {
      return humanMob.isSettler();
   }
}
