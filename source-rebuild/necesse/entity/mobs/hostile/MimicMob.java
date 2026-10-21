package necesse.entity.mobs.hostile;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Point2D.Float;
import java.util.ArrayList;
import java.util.List;
import necesse.engine.CameraShake;
import necesse.engine.GameLog;
import necesse.engine.Settings;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.input.Control;
import necesse.engine.localization.Localization;
import necesse.engine.registries.MobRegistry;
import necesse.engine.save.LoadData;
import necesse.engine.save.SaveData;
import necesse.engine.sound.SoundSettings;
import necesse.engine.util.GameBlackboard;
import necesse.engine.util.GameRandom;
import necesse.engine.util.gameAreaSearch.GameAreaStream;
import necesse.engine.window.GameWindow;
import necesse.entity.mobs.DeathMessageTable;
import necesse.entity.mobs.GameDamage;
import necesse.entity.mobs.MaskShaderOptions;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.MobDrawable;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.ai.behaviourTree.BehaviourTreeAI;
import necesse.entity.mobs.ai.behaviourTree.composites.SequenceAINode;
import necesse.entity.mobs.ai.behaviourTree.leaves.CollisionChaserAINode;
import necesse.entity.mobs.ai.behaviourTree.leaves.TargetFinderAINode;
import necesse.entity.mobs.ai.behaviourTree.util.TargetFinderDistance;
import necesse.entity.particle.FleshParticle;
import necesse.entity.particle.Particle;
import necesse.gfx.GameResources;
import necesse.gfx.Renderer;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.DrawOptions;
import necesse.gfx.drawables.OrderableDrawables;
import necesse.gfx.gameTooltips.GameTooltipManager;
import necesse.gfx.gameTooltips.InputTooltip;
import necesse.gfx.gameTooltips.ListGameTooltips;
import necesse.gfx.gameTooltips.TooltipLocation;
import necesse.inventory.InventoryItem;
import necesse.inventory.lootTable.LootItemInterface;
import necesse.inventory.lootTable.LootList;
import necesse.inventory.lootTable.LootTable;
import necesse.inventory.lootTable.lootItem.LootItem;
import necesse.level.maps.Level;
import necesse.level.maps.light.GameLight;

public class MimicMob extends HostileMob {
   public final int damageDiceCount = 14;
   public int minDamageRoll = 14;
   public int maxDamageRoll = 112;
   public boolean isDisguised = true;
   public CameraShake disguisedShake;
   public long lastShakeTime;
   public ArrayList<InventoryItem> loot = new ArrayList<>();

   public MimicMob() {
      super(600);
      this.setSpeed(40.0F);
      this.setFriction(3.0F);
      this.setKnockbackModifier(0.4F);
      this.setArmor(20);
      this.collision = new Rectangle(-10, -7, 20, 14);
      this.hitBox = new Rectangle(-14, -12, 28, 24);
      this.selectBox = new Rectangle(-16, -24, 32, 32);
      this.swimMaskMove = 16;
      this.swimMaskOffset = -2;
      this.swimSinkOffset = -4;
   }

   @Override
   public void addSaveData(SaveData save) {
      super.addSaveData(save);
      if (!this.loot.isEmpty()) {
         SaveData lootSave = new SaveData("LOOT");

         for (InventoryItem item : this.loot) {
            SaveData itemSave = new SaveData("item");
            item.addSaveData(itemSave);
            lootSave.addSaveData(itemSave);
         }

         if (!lootSave.isEmpty()) {
            save.addSaveData(lootSave);
         }
      }
   }

   @Override
   public void applyLoadData(LoadData save) {
      super.applyLoadData(save);
      this.loot.clear();
      LoadData lootSave = save.getFirstLoadDataByName("LOOT");
      if (lootSave != null) {
         for (LoadData itemSave : lootSave.getLoadDataByName("item")) {
            try {
               InventoryItem item = InventoryItem.fromLoadData(itemSave);
               if (item != null) {
                  this.loot.add(item);
               } else {
                  GameLog.warn.println("Error loading mimic loot table - invalid item: " + itemSave.getScript());
               }
            } catch (Exception e) {
               System.err.println("Error loading mimic loot item: " + e.getMessage());
            }
         }
      }
   }

   @Override
   public void init() {
      super.init();
      this.ai = new BehaviourTreeAI<>(this, new MimicMob.MimicAI<>(640));
      if (this.isDisguised) {
         this.x = getTileCoordinate(this.x) * 32 + 16;
         this.y = getTileCoordinate(this.y) * 32 + 16;
      }
   }

   @Override
   public void clientTick() {
      super.clientTick();
      this.tickIsDisguised();
   }

   @Override
   public void serverTick() {
      super.serverTick();
      this.tickIsDisguised();
   }

   public void tickIsDisguised() {
      boolean nextIsDisguised = !this.isAccelerating() && this.dx == 0.0F && this.dy == 0.0F;
      if (this.isDisguised != nextIsDisguised) {
         this.isDisguised = nextIsDisguised;
         if (this.isDisguised) {
            this.x = getTileCoordinate(this.x) * 32 + 16;
            this.y = getTileCoordinate(this.y) * 32 + 16;
            this.sendMovementPacket(false);
         }

         if (this.isClient()) {
            for (int i = 0; i < 20; i++) {
               int startHeight = GameRandom.globalRandom.getIntBetween(2, 10);
               this.getLevel()
                  .entityManager
                  .addParticle(
                     this.x + GameRandom.globalRandom.floatGaussian() * 8.0F,
                     this.y - 4.0F + GameRandom.globalRandom.floatGaussian() * 4.0F,
                     Particle.GType.IMPORTANT_COSMETIC
                  )
                  .movesFriction(GameRandom.globalRandom.floatGaussian() * 20.0F, GameRandom.globalRandom.floatGaussian() * 16.0F, 2.0F)
                  .heightMoves(startHeight, startHeight + 20)
                  .lifeTime(1000);
            }
         }
      }
   }

   @Override
   public boolean onMouseHover(GameCamera camera, PlayerMob perspective, GameBlackboard blackboard, boolean debug) {
      if (this.isDisguised) {
         if (!debug && !this.isVisible()) {
            return false;
         }

         ListGameTooltips tips = new ListGameTooltips();
         if (this.canInteract(perspective)) {
            Renderer.setCursor(GameWindow.CURSOR.INTERACT);
            if (Settings.showControlTips) {
               String controlMsg = this.getInteractTip(perspective, debug);
               if (controlMsg != null) {
                  tips.add(new InputTooltip(Control.MOUSE2, controlMsg, this.inInteractRange(perspective) ? 1.0F : 0.7F));
               }
            }
         }

         GameTooltipManager.addTooltip(tips, TooltipLocation.INTERACT_FOCUS);
         return true;
      } else {
         return super.onMouseHover(camera, perspective, blackboard, debug);
      }
   }

   @Override
   public boolean canInteract(Mob mob) {
      return true;
   }

   @Override
   protected String getInteractTip(PlayerMob perspective, boolean debug) {
      return Localization.translate("controls", "opentip");
   }

   @Override
   public LootTable getLootTable() {
      LootTable lootTable = new LootTable();
      lootTable.items.add(new LootItem("mimicchest").preventLootMultiplier());
      lootTable.items.add(new LootItemInterface() {
         @Override
         public void addPossibleLoot(LootList list, Object... extra) {
            MimicMob self = LootTable.expectExtra(MimicMob.class, extra, 0);
            if (self != null && self.loot != null) {
               for (InventoryItem item : self.loot) {
                  list.addCustom(item);
               }
            }
         }

         @Override
         public void addItems(List<InventoryItem> list, GameRandom random, float lootMultiplier, Object... extra) {
            MimicMob self = LootTable.expectExtra(MimicMob.class, extra, 0);
            if (self != null && self.loot != null) {
               for (InventoryItem item : self.loot) {
                  list.add(item.copy());
               }
            }
         }
      });
      return lootTable;
   }

   @Override
   public DeathMessageTable getDeathMessages() {
      return this.getDeathMessages("mimic", 3);
   }

   @Override
   protected SoundSettings getHitSound() {
      return new SoundSettings(GameResources.crack).volume(1.6F);
   }

   @Override
   protected SoundSettings getHitDeathSound() {
      return new SoundSettings(GameResources.crackdeath).volume(0.8F);
   }

   @Override
   public void spawnDeathParticles(float knockbackX, float knockbackY) {
      for (int i = 0; i < 4; i++) {
         this.getLevel()
            .entityManager
            .addParticle(
               new FleshParticle(
                  this.getLevel(), MobRegistry.Textures.skeleton.body, GameRandom.globalRandom.nextInt(5), 8, 32, this.x, this.y, 20.0F, knockbackX, knockbackY
               ),
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
      int drawY = camera.getDrawY(y) - 42;
      Point sprite = this.getAnimSprite(x, y, this.getDir());
      if (this.isDisguised) {
         if (this.lastShakeTime < this.getLocalTime() - 5000L && (this.disguisedShake == null || this.disguisedShake.isOver(this.getLocalTime()))) {
            this.disguisedShake = new CameraShake(this.getLocalTime(), 1000, 75, 1.5F, 1.5F, true);
            this.lastShakeTime = this.getLocalTime();
         }

         if (this.disguisedShake != null) {
            Float shake = this.disguisedShake.getCurrentShake(this.getLocalTime());
            drawX = (int)(drawX + shake.x);
            drawY = (int)(drawY + shake.y);
         }
      }

      drawY += this.getBobbing(x, y);
      drawY += level.getTile(getTileCoordinate(x), getTileCoordinate(y)).getMobSinkingAmount(this);
      MaskShaderOptions swimMask = this.getSwimMaskShaderOptions(this.inLiquidFloat(x, y));
      final DrawOptions drawOptions = MobRegistry.Textures.mimic
         .initDraw()
         .sprite(sprite.x, sprite.y, 64)
         .addMaskShader(swimMask)
         .startGlowOptions(level, this.getID())
         .light(light)
         .applyTreasure(perspective)
         .pos(drawX, drawY);
      list.add(new MobDrawable() {
         @Override
         public void draw(TickManager tickManager) {
            drawOptions.draw();
         }
      });
   }

   @Override
   public int getRockSpeed() {
      return 20;
   }

   public static class MimicAI<T extends MimicMob> extends SequenceAINode<T> {
      public final TargetFinderAINode<T> targetFinderNode;
      public final CollisionChaserAINode<T> chaserNode;
      public final int tileActivationRange = 3;

      public MimicAI(int searchDistance) {
         TargetFinderDistance<T> targetFinder = new TargetFinderDistance<>(searchDistance);
         targetFinder.targetLostAddedDistance = searchDistance * 2;
         this.addChild(this.targetFinderNode = new TargetFinderAINode<T>(targetFinder) {
            public GameAreaStream<? extends Mob> streamPossibleTargets(T mob, Point base, TargetFinderDistance<T> distance) {
               return TargetFinderAINode.streamPlayersAndHumans(mob, base, distance).filter(m -> m.getDistance(mob) < 96.0F);
            }
         });
         this.addChild(this.chaserNode = new CollisionChaserAINode<T>() {
            public boolean attackTarget(T mob, Mob target) {
               GameDamage damage = new GameDamage(GameRandom.globalRandom.getIntBetween(mob.minDamageRoll, mob.maxDamageRoll));
               return simpleAttack(mob, target, damage, 50);
            }
         });
      }
   }
}
