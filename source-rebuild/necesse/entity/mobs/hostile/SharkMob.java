package necesse.entity.mobs.hostile;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.List;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.network.server.Server;
import necesse.engine.network.server.ServerClient;
import necesse.engine.registries.MobRegistry;
import necesse.engine.registries.TileRegistry;
import necesse.engine.sound.SoundManager;
import necesse.engine.sound.SoundSettings;
import necesse.engine.world.worldData.SettlementsWorldData;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.MobDrawable;
import necesse.entity.mobs.MobSpawnLocation;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.ai.behaviourTree.BehaviourTreeAI;
import necesse.entity.mobs.ai.behaviourTree.trees.SharkAI;
import necesse.entity.mobs.ai.behaviourTree.util.AIMover;
import necesse.entity.mobs.networkField.BooleanNetworkField;
import necesse.entity.mobs.summon.WoodBoatMob;
import necesse.entity.particle.FleshParticle;
import necesse.entity.particle.Particle;
import necesse.gfx.GameResources;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.DrawOptions;
import necesse.gfx.drawOptions.texture.TextureDrawOptions;
import necesse.gfx.drawables.OrderableDrawables;
import necesse.gfx.gameTexture.GameTexture;
import necesse.inventory.lootTable.LootTable;
import necesse.inventory.lootTable.lootItem.ChanceLootItem;
import necesse.inventory.lootTable.lootItem.LootItem;
import necesse.inventory.lootTable.lootItem.OneOfLootItems;
import necesse.level.maps.CollisionFilter;
import necesse.level.maps.Level;
import necesse.level.maps.TilePosition;
import necesse.level.maps.biomes.Biome;
import necesse.level.maps.light.GameLight;

public class SharkMob extends HostileMob {
   protected double deltaCounter = 0.0;
   public BooleanNetworkField isChasing;
   protected int swimParticleOffset = 13;
   public static OneOfLootItems randomFishLoot = new OneOfLootItems(
      new ChanceLootItem(0.6F, "mackerel"),
      new ChanceLootItem(0.6F, "herring"),
      new ChanceLootItem(0.6F, "tuna"),
      new ChanceLootItem(0.6F, "salmon"),
      new ChanceLootItem(0.6F, "cod")
   );
   public static LootTable lootTable = new LootTable(
      randomMapDrop, randomFishLoot, new LootItem("sharkscales"), new LootItem("sharkmeat"), new ChanceLootItem(0.4F, "sharktooth")
   );

   public SharkMob() {
      super(500);
      this.setSpeed(30.0F);
      this.setSwimSpeed(1.0F);
      this.setFriction(1.0F);
      this.collision = new Rectangle(-16, -14, 32, 28);
      this.hitBox = new Rectangle(-20, -16, 40, 32);
      this.selectBox = new Rectangle(-20, -20, 40, 40);
      this.swimMaskMove = 0;
      this.swimMaskOffset = 0;
      this.swimSinkOffset = 0;
      this.isChasing = this.registerNetworkField(new BooleanNetworkField(false));
      this.setKnockbackModifier(0.5F);
      this.setTeam(-1);
   }

   @Override
   public LootTable getLootTable() {
      return lootTable;
   }

   @Override
   public void init() {
      super.init();
      this.ai = new BehaviourTreeAI<>(this, new SharkAI<>(640), new AIMover());
      this.updateSpeed();
   }

   @Override
   public float getFullInLiquidAtPercent(int x, int y) {
      return 0.4F;
   }

   @Override
   public void tickMovement(float delta) {
      super.tickMovement(delta);
      if (this.isClient()) {
         if (this.dx != 0.0F || this.dy != 0.0F) {
            this.deltaCounter = this.deltaCounter + delta * Math.max(0.2F, this.getCurrentSpeed() / 30.0F);
            if (this.deltaCounter >= 50.0) {
               this.deltaCounter -= 50.0;
               WoodBoatMob.addParticleEffects(this, 0.0F, 0.0F, this.swimParticleOffset);
            }
         }
      }
   }

   @Override
   public void clientTick() {
      super.clientTick();
      this.updateSpeed();
   }

   @Override
   public void serverTick() {
      super.serverTick();
      Object chaserTarget = this.ai.blackboard.get("chaserTarget");
      this.isChasing.set(chaserTarget != null);
      this.updateSpeed();
   }

   public void updateSpeed() {
      if (this.isChasing.get()) {
         this.setSpeed(65.0F);
      } else {
         this.setSpeed(30.0F);
      }
   }

   @Override
   public boolean isValidSpawnLocation(Server server, ServerClient client, int targetX, int targetY) {
      return new MobSpawnLocation(this, targetX, targetY)
         .checkMobSpawnLocation()
         .checkInLiquid()
         .checkMaxMobsAround(1, Mob.MOB_SPAWN_AREA.maxSpawnDistance * 3, m -> m instanceof SharkMob, client)
         .validAndApply();
   }

   @Override
   public MobSpawnLocation checkSpawnLocation(MobSpawnLocation location) {
      return location.checkNotLevelCollides()
         .checkTile((tileX, tileY) -> this.getLevel().liquidManager.getHeight(tileX, tileY) <= -10)
         .checkTile((tileX, tileY) -> !SettlementsWorldData.getSettlementsData(this).hasSettlementAtTile(this.getLevel(), tileX, tileY));
   }

   @Override
   public void showAttack(int x, int y, int seed, boolean showAllDirections) {
      super.showAttack(x, y, seed, showAllDirections);
      if (this.isClient()) {
         SoundManager.playSound(new SoundSettings(GameResources.sharkAttack).volume(0.4F), this);
      }
   }

   @Override
   public void updateAttackDir(int x, int y, boolean showAllDirections) {
   }

   @Override
   public CollisionFilter getLevelCollisionFilter() {
      return super.getLevelCollisionFilter().allLandTiles();
   }

   @Override
   public boolean estimateCanMoveTo(int tileX, int tileY, boolean acceptAdjacentTile) {
      return !this.getLevel().isLiquidTile(tileX, tileY) ? false : super.estimateCanMoveTo(tileX, tileY, acceptAdjacentTile);
   }

   @Override
   public void spawnDeathParticles(float knockbackX, float knockbackY) {
      for (int i = 0; i < 3; i++) {
         this.getLevel()
            .entityManager
            .addParticle(
               new FleshParticle(this.getLevel(), MobRegistry.Textures.shark, i, 12, 32, this.x, this.y, 20.0F, knockbackX, knockbackY),
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
      int drawX = camera.getDrawX(x) - 48;
      int drawY = camera.getDrawY(y) - 48;
      Point sprite = this.getAnimSprite(x, y, this.getDir());
      final DrawOptions options = this.getSharkTexture()
         .initDraw()
         .sprite(sprite.x, sprite.y, 96)
         .startGlowOptions(level, this.getID())
         .light(light)
         .applyEnemyTracker(this, perspective)
         .pos(drawX, drawY);
      list.add(new MobDrawable() {
         @Override
         public void draw(TickManager tickManager) {
            options.draw();
         }
      });
   }

   protected GameTexture getSharkTexture() {
      return MobRegistry.Textures.shark;
   }

   @Override
   protected TextureDrawOptions getShadowDrawOptions(Level level, int x, int y, GameLight light, GameCamera camera) {
      GameTexture shadowTexture = MobRegistry.Textures.human_baby_shadow;
      int res = shadowTexture.getHeight();
      int drawX = camera.getDrawX(x) - res / 2;
      int drawY = camera.getDrawY(y) - res / 2;
      drawY += this.getBobbing(x, y);
      return shadowTexture.initDraw().sprite(this.getDir(), 0, res).light(light).pos(drawX, drawY);
   }

   @Override
   public Point getAnimSprite(int x, int y, int dir) {
      Point p = new Point(0, dir);
      if (Math.abs(this.dx) <= 0.01F && Math.abs(this.dy) <= 0.01F) {
         p.x = 0;
      } else {
         p.x = (int)(this.getDistanceRan() / this.getRockSpeed()) % 4 + 1;
      }

      return p;
   }

   @Override
   public int getRockSpeed() {
      return 14;
   }

   @Override
   public int getTileWanderPriority(TilePosition pos, Biome baseBiome) {
      if (pos.tileID() == TileRegistry.waterID) {
         int height = pos.level.liquidManager.getHeight(pos.tileX, pos.tileY);
         return height < -7 ? 1000 : Math.abs(height) * 100;
      } else {
         return -1000;
      }
   }

   @Override
   protected SoundSettings getHurtSound() {
      return new SoundSettings(GameResources.sharkHurt).volume(0.2F);
   }

   @Override
   protected SoundSettings getDeathSound() {
      return new SoundSettings(GameResources.sharkDeath).volume(0.7F);
   }
}
