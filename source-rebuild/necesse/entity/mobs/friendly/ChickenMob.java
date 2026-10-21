package necesse.entity.mobs.friendly;

import java.awt.Color;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;
import necesse.engine.GameTileRange;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.localization.message.GameMessage;
import necesse.engine.localization.message.LocalMessage;
import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.engine.registries.MobRegistry;
import necesse.engine.save.LoadData;
import necesse.engine.save.SaveData;
import necesse.engine.sound.SoundEffect;
import necesse.engine.sound.SoundManager;
import necesse.engine.sound.SoundSettings;
import necesse.engine.util.GameMath;
import necesse.engine.util.GameRandom;
import necesse.engine.util.GameUtils;
import necesse.engine.util.ItemCostList;
import necesse.entity.mobs.MaskShaderOptions;
import necesse.entity.mobs.MobDrawable;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.ai.behaviourTree.AINode;
import necesse.entity.mobs.ai.behaviourTree.AINodeResult;
import necesse.entity.mobs.ai.behaviourTree.BehaviourTreeAI;
import necesse.entity.mobs.ai.behaviourTree.Blackboard;
import necesse.entity.mobs.ai.behaviourTree.decorators.MoveTaskAINode;
import necesse.entity.mobs.ai.behaviourTree.event.AIEvent;
import necesse.entity.mobs.ai.behaviourTree.trees.HusbandryImpregnateWandererAI;
import necesse.entity.mobs.ai.behaviourTree.util.AIMover;
import necesse.entity.mobs.misc.MobProcessObjectHandler;
import necesse.entity.mobs.misc.StartMobProcessObjectMobAbility;
import necesse.entity.particle.FleshParticle;
import necesse.entity.particle.Particle;
import necesse.gfx.GameResources;
import necesse.gfx.HumanGender;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.DrawOptions;
import necesse.gfx.drawOptions.texture.TextureDrawOptions;
import necesse.gfx.drawables.OrderableDrawables;
import necesse.gfx.gameTexture.GameTexture;
import necesse.gfx.gameTooltips.ListGameTooltips;
import necesse.inventory.InventoryItem;
import necesse.inventory.lootTable.LootTable;
import necesse.inventory.lootTable.lootItem.LootItem;
import necesse.level.gameObject.EggNestObjectInterface;
import necesse.level.gameObject.GameObject;
import necesse.level.gameObject.ProcessObjectHandler;
import necesse.level.maps.Level;
import necesse.level.maps.light.GameLight;
import necesse.level.maps.multiTile.MultiTile;

public class ChickenMob extends HusbandryMob {
   public static GameTileRange LAY_EGG_TILE_RANGE = new GameTileRange(15);
   public static int LAY_EGG_COOLDOWN_SECONDS_MIN = 600;
   public static int LAY_EGG_COOLDOWN_SECONDS_MAX = 1200;
   public static int EGG_HATCH_SECONDS_MIN = 600;
   public static int EGG_HATCH_SECONDS_MAX = 1200;
   public static LootTable lootTable = new LootTable(LootItem.between("rawchickenleg", 1, 2));
   public long nextEggLayingTime;
   public boolean nextEggIsFertilized;
   protected MobProcessObjectHandler layEggHandler = new MobProcessObjectHandler(this) {
      @Override
      public void tickInProgress() {
         super.tickInProgress();
         if (!ChickenMob.this.isServer() && GameRandom.globalRandom.nextInt(20) == 0) {
            ChickenMob.this.spawnLayEggParticles();
         }
      }

      @Override
      public void onCompleted() {
         super.onCompleted();
         if (!ChickenMob.this.isClient() && this.target != null && this.target.canProcess()) {
            this.target.process();
            ChickenMob.this.refreshNextEggLayingTime();
            ChickenMob.this.ai.blackboard.submitEvent("wanderNow", new AIEvent());
         }

         if (!ChickenMob.this.isServer()) {
            SoundManager.playSound(GameResources.pop, SoundEffect.effect(ChickenMob.this).volume(0.3F).pitch(0.7F));

            for (int i = 0; i < 5; i++) {
               ChickenMob.this.spawnLayEggParticles();
            }
         }
      }
   };
   public StartMobProcessObjectMobAbility layEggAbility;

   public ChickenMob() {
      super(50);
      this.setSpeed(12.0F);
      this.setFriction(3.0F);
      this.collision = new Rectangle(-12, -9, 24, 18);
      this.hitBox = new Rectangle(-16, -12, 32, 24);
      this.selectBox = new Rectangle(-18, -30, 36, 36);
      this.swimMaskMove = 8;
      this.swimMaskOffset = 0;
      this.swimSinkOffset = 0;
      this.timeToGrowUp = 600000;
      this.layEggAbility = this.registerAbility(new StartMobProcessObjectMobAbility(this.layEggHandler));
   }

   @Override
   public void addSaveData(SaveData save) {
      super.addSaveData(save);
      save.addLong("nextEggLayingTime", this.nextEggLayingTime);
      save.addBoolean("nextEggIsFertilized", this.nextEggIsFertilized);
   }

   @Override
   public void applyLoadData(LoadData save) {
      super.applyLoadData(save);
      this.nextEggLayingTime = save.getLong("nextEggLayingTime", 0L, false);
      this.nextEggIsFertilized = save.getBoolean("nextEggIsFertilized", this.nextEggIsFertilized, false);
   }

   @Override
   public void applyMovementPacket(PacketReader reader, boolean isDirect) {
      super.applyMovementPacket(reader, isDirect);
      this.nextEggLayingTime = reader.getNextLong();
      this.nextEggIsFertilized = reader.getNextBoolean();
   }

   @Override
   public void setupMovementPacket(PacketWriter writer) {
      super.setupMovementPacket(writer);
      writer.putNextLong(this.nextEggLayingTime);
      writer.putNextBoolean(this.nextEggIsFertilized);
   }

   @Override
   public void init() {
      super.init();
      this.ai = new BehaviourTreeAI<>(this, new ChickenMob.ChickenAI<>(30000), new AIMover());
      if (this.nextEggLayingTime == 0L) {
         this.refreshNextEggLayingTime();
      }
   }

   @Override
   public void clientTick() {
      super.clientTick();
      this.layEggHandler.tick();
   }

   @Override
   public void serverTick() {
      super.serverTick();
      this.layEggHandler.tick();
   }

   public void spawnLayEggParticles() {
      if (this.layEggHandler.isTargetOnTile) {
         spawnEggParticles(this.getLevel(), this.layEggHandler.tileX, this.layEggHandler.tileY);
      } else {
         spawnEggParticles(this.getLevel(), this.x + GameRandom.globalRandom.getIntBetween(-10, 10), this.y + GameRandom.globalRandom.getIntBetween(-5, 5));
      }
   }

   public static void spawnEggParticles(Level level, int tileX, int tileY) {
      MultiTile multiTile = level.getObject(tileX, tileY).getMultiTile(level, 0, tileX, tileY);
      Point offset = new Point(multiTile.getCenterXOffset() * 16, multiTile.getCenterYOffset() * 16);
      spawnEggParticles(level, tileX * 32.0F + offset.x + GameRandom.globalRandom.nextInt(32), tileY * 32.0F + offset.y + 32.0F);
   }

   public static void spawnEggParticles(Level level, float posX, float posY) {
      int startHeight = 8 + GameRandom.globalRandom.nextInt(24);
      level.entityManager
         .addParticle(posX, posY, Particle.GType.COSMETIC)
         .color(new Color(255, 239, 157, 255))
         .heightMoves(startHeight, startHeight + 40)
         .lifeTime(2000);
   }

   @Override
   public LootTable getLootTable() {
      return !this.isGrown() ? new LootTable() : lootTable;
   }

   @Override
   public void setDefaultBuyPrice(GameRandom random) {
      this.buyPrice = new ItemCostList();
      this.buyPrice.addItem("coin", random.getIntBetween(500, 600));
   }

   @Override
   protected SoundSettings getAmbientSound() {
      return new SoundSettings(GameResources.chickenAmbient).volume(0.6F);
   }

   @Override
   protected SoundSettings getHurtSound() {
      return new SoundSettings(GameResources.chickenHurt);
   }

   @Override
   protected SoundSettings getDeathSound() {
      return new SoundSettings(GameResources.chickenDeath).volume(0.3F);
   }

   @Override
   public GameMessage getLocalization() {
      return this.isGrown() ? super.getLocalization() : new LocalMessage("mob", "chick");
   }

   @Override
   public void spawnDeathParticles(float knockbackX, float knockbackY) {
      GameTexture texture = this.getTexture();

      for (int i = 0; i < 4; i++) {
         this.getLevel()
            .entityManager
            .addParticle(
               new FleshParticle(this.getLevel(), texture, GameRandom.globalRandom.nextInt(5), 8, 32, this.x, this.y, 10.0F, knockbackX, knockbackY),
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
      TextureDrawOptions shadow = this.getShadowTexture().initDraw().sprite(0, dir, 64).light(light).pos(drawX, drawY);
      tileList.add(tm -> shadow.draw());
      drawY += level.getTile(getTileCoordinate(x), getTileCoordinate(y)).getMobSinkingAmount(this);
      float eggProgress = this.layEggHandler.getProgressPercent();
      final MaskShaderOptions swimMask;
      if (eggProgress > 0.0F) {
         swimMask = this.getSwimMaskShaderOptions(Math.min(eggProgress * 2.0F, 0.7F));
      } else {
         swimMask = this.getSwimMaskShaderOptions(this.inLiquidFloat(x, y));
      }

      final DrawOptions options = this.getTexture()
         .initDraw()
         .sprite(sprite.x, sprite.y, 64)
         .startGlowOptions(level, this.getID())
         .light(light)
         .applyEnemyTracker(this, perspective)
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

   private GameTexture getTexture() {
      return this.isGrown() ? MobRegistry.Textures.chicken : MobRegistry.Textures.chick;
   }

   private GameTexture getShadowTexture() {
      return this.isGrown() ? MobRegistry.Textures.chicken_shadow : MobRegistry.Textures.chick_shadow;
   }

   @Override
   protected int getRockSpeed() {
      return this.isGrown() ? 10 : 7;
   }

   @Override
   public HumanGender getGender() {
      return HumanGender.FEMALE;
   }

   @Override
   public boolean canBirth() {
      return super.canBirth() && !this.nextEggIsFertilized;
   }

   @Override
   public void onImpregnated(HusbandryMob father) {
      this.nextEggIsFertilized = true;
   }

   public boolean isLayingEgg() {
      return this.layEggHandler.isInProgress();
   }

   public boolean canLayEgg() {
      return this.isGrown() && this.nextEggLayingTime <= this.getWorldTime() && !this.isLayingEgg();
   }

   public void refreshNextEggLayingTime() {
      this.nextEggLayingTime = this.getWorldTime() + GameRandom.globalRandom.getIntBetween(LAY_EGG_COOLDOWN_SECONDS_MIN, LAY_EGG_COOLDOWN_SECONDS_MAX) * 1000L;
   }

   @Override
   protected void addDebugTooltips(ListGameTooltips tooltips) {
      super.addDebugTooltips(tooltips);
      if (this.isGrown()) {
         tooltips.add("Next egg in: " + GameUtils.getTimeStringMillis(this.nextEggLayingTime - this.getWorldTime()));
         tooltips.add("Next egg is fertilized: " + this.nextEggIsFertilized);
      }
   }

   public static class ChickenAI<T extends ChickenMob> extends HusbandryImpregnateWandererAI<T> {
      public final ChickenMob.ChickenLayEggAINode<T> eggLayingNode;

      public ChickenAI(int wanderFrequency) {
         super(wanderFrequency);
         this.addChildFirst(this.eggLayingNode = new ChickenMob.ChickenLayEggAINode<>());
      }
   }

   public static class ChickenLayEggAINode<T extends ChickenMob> extends MoveTaskAINode<T> {
      public ProcessObjectHandler target;

      protected void onRootSet(AINode<T> root, T mob, Blackboard<T> blackboard) {
      }

      public void init(T mob, Blackboard<T> blackboard) {
      }

      public AINodeResult tick(T mob, Blackboard<T> blackboard) {
         if (this.target != null && this.target.reservable != null) {
            this.target.reservable.reserve(mob);
         }

         return super.tick(mob, blackboard);
      }

      public AINodeResult tickNode(T mob, Blackboard<T> blackboard) {
         if (blackboard.mover.isCurrentlyMovingFor(this)) {
            return AINodeResult.SUCCESS;
         }

         if (this.target == null && mob.canLayEgg()) {
            List<HusbandryMob> nearbyMobs = mob.getNearbyHusbandryMobs();
            final boolean instant = mob.getRopeMob() != null || mob.isBeingInteractedWith();
            if (!instant && mob.nextEggIsFertilized && nearbyMobs.size() <= HusbandryMob.maxCloseMobsToBirth) {
               Point baseTile = new Point(mob.getTileX(), mob.getTileY());
               GameTileRange range = ChickenMob.LAY_EGG_TILE_RANGE;
               ArrayList<ProcessObjectHandler> validTiles = new ArrayList<>();
               Point pathOffset = mob.getPathMoveOffset();

               for (Point tile : range.getValidTiles(baseTile.x, baseTile.y)) {
                  GameObject object = mob.getLevel().getObject(tile.x, tile.y);
                  if (!mob.getLevel().isSolidTile(tile.x, tile.y)
                     && object instanceof EggNestObjectInterface
                     && !mob.collidesWith(mob.getLevel(), tile.x * 32 + pathOffset.x, tile.y * 32 + pathOffset.y)
                     && mob.estimateCanMoveTo(tile.x, tile.y, false)) {
                     ProcessObjectHandler handler = ((EggNestObjectInterface)object).getLayEggHandler(mob.getLevel(), tile.x, tile.y);
                     if (handler != null && handler.canProcess() && (handler.reservable == null || handler.reservable.isAvailable(mob))) {
                        validTiles.add(handler);
                     }
                  }
               }

               if (!validTiles.isEmpty()) {
                  this.target = GameRandom.globalRandom.getOneOf(validTiles);
                  if (this.target.reservable != null) {
                     this.target.reservable.reserve(mob);
                  }

                  return this.moveToTileTask(
                     this.target.tileX, this.target.tileY, null, path -> path.moveIfWithin(-1, 0, null) ? AINodeResult.SUCCESS : AINodeResult.FAILURE
                  );
               }

               this.target = new ProcessObjectHandler(mob.getTileX(), mob.getTileY(), null) {
                  @Override
                  public boolean canProcess() {
                     return true;
                  }

                  @Override
                  public void process() {
                     InventoryItem product = new InventoryItem("egg");
                     Level level = ((ChickenMob)ChickenLayEggAINode.this.mob()).getLevel();
                     level.entityManager
                        .pickups
                        .add(product.getPickupEntity(level, ((ChickenMob)ChickenLayEggAINode.this.mob()).x, ((ChickenMob)ChickenLayEggAINode.this.mob()).y));
                  }

                  @Override
                  public boolean isValid() {
                     return true;
                  }

                  @Override
                  public int getTimeItTakesInMilliseconds() {
                     return 4000;
                  }
               };
            } else {
               this.target = new ProcessObjectHandler(mob.getTileX(), mob.getTileY(), null) {
                  @Override
                  public boolean canProcess() {
                     return true;
                  }

                  @Override
                  public void process() {
                     InventoryItem product = new InventoryItem("egg");
                     Level level = ((ChickenMob)ChickenLayEggAINode.this.mob()).getLevel();
                     level.entityManager
                        .pickups
                        .add(product.getPickupEntity(level, ((ChickenMob)ChickenLayEggAINode.this.mob()).x, ((ChickenMob)ChickenLayEggAINode.this.mob()).y));
                  }

                  @Override
                  public boolean isValid() {
                     return true;
                  }

                  @Override
                  public int getTimeItTakesInMilliseconds() {
                     return instant ? 0 : 4000;
                  }
               };
            }
         }

         if (this.target != null && this.target.isValid()) {
            if (this.target.canProcess()
               && !mob.isLayingEgg()
               && mob.canLayEgg()
               && GameMath.diagonalMoveDistance(this.target.tileX, this.target.tileY, mob.getTileX(), mob.getTileY()) <= 1.0) {
               mob.layEggAbility.runAndSend(this.target);
            }

            if (mob.isLayingEgg()) {
               return AINodeResult.SUCCESS;
            }

            this.target = null;
            return AINodeResult.SUCCESS;
         } else {
            this.target = null;
            return AINodeResult.FAILURE;
         }
      }
   }
}
