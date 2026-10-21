package necesse.entity.mobs.hostile;

import java.awt.Rectangle;
import java.util.List;
import java.util.stream.Stream;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.modifiers.ModifierValue;
import necesse.engine.network.server.Server;
import necesse.engine.network.server.ServerClient;
import necesse.engine.registries.MobRegistry;
import necesse.engine.sound.SoundSettings;
import necesse.engine.util.GameUtils;
import necesse.entity.mobs.GameDamage;
import necesse.entity.mobs.MaskShaderOptions;
import necesse.entity.mobs.MobDrawable;
import necesse.entity.mobs.MobSpawnLocation;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.ai.behaviourTree.BehaviourTreeAI;
import necesse.entity.mobs.ai.behaviourTree.trees.ConfusedCollisionPlayerChaserWandererAI;
import necesse.entity.mobs.buffs.BuffModifiers;
import necesse.entity.particle.FleshParticle;
import necesse.entity.particle.Particle;
import necesse.gfx.GameResources;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.DrawOptions;
import necesse.gfx.drawOptions.texture.TextureDrawOptions;
import necesse.gfx.drawables.OrderableDrawables;
import necesse.inventory.lootTable.LootTable;
import necesse.level.maps.Level;
import necesse.level.maps.light.GameLight;

public class SwampSlimeMob extends HostileSlimeMob {
   public static LootTable lootTable = new LootTable(HostileMob.randomMapDrop);

   public SwampSlimeMob() {
      super(240);
      this.setSpeed(25.0F);
      this.setFriction(2.0F);
      this.collision = new Rectangle(-10, -7, 20, 14);
      this.hitBox = new Rectangle(-12, -14, 24, 24);
      this.selectBox = new Rectangle(-16, -24, 32, 32);
      this.swimMaskMove = 8;
      this.swimMaskOffset = 28;
      this.swimSinkOffset = 0;
   }

   @Override
   public void init() {
      super.init();
      this.ai = new BehaviourTreeAI<>(this, new ConfusedCollisionPlayerChaserWandererAI<>(null, 160, new GameDamage(38.0F), 100, 40000));
   }

   @Override
   public void spawnDeathParticles(float knockbackX, float knockbackY) {
      for (int i = 0; i < 4; i++) {
         this.getLevel()
            .entityManager
            .addParticle(
               new FleshParticle(this.getLevel(), MobRegistry.Textures.swampSlime, i, 2, 32, this.x, this.y, 20.0F, knockbackX, knockbackY),
               Particle.GType.IMPORTANT_COSMETIC
            );
      }
   }

   @Override
   public LootTable getLootTable() {
      return lootTable;
   }

   @Override
   public boolean isValidSpawnLocation(Server server, ServerClient client, int targetX, int targetY) {
      MobSpawnLocation location = new MobSpawnLocation(this, targetX, targetY).checkMobSpawnLocation();
      if (this.getLevel().isCave) {
         location = location.checkLightThreshold(client);
      } else {
         location = location.checkMaxStaticLightThreshold(10);
      }

      return location.validAndApply();
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
      int drawX = camera.getDrawX(x) - 16;
      int drawY = camera.getDrawY(y) - 26;
      boolean inLiquid = this.inLiquid(x, y);
      int spriteX;
      if (inLiquid) {
         spriteX = GameUtils.getAnim(this.getWorldEntity().getTime(), 2, 1000);
      } else {
         spriteX = this.getJumpAnimationFrame(6);
      }

      drawY += this.getBobbing(x, y);
      drawY += level.getTile(getTileCoordinate(x), getTileCoordinate(y)).getMobSinkingAmount(this);
      final MaskShaderOptions swimMask = this.getSwimMaskShaderOptions(this.inLiquidFloat(x, y));
      final DrawOptions options = MobRegistry.Textures.swampSlime
         .initDraw()
         .sprite(spriteX, inLiquid ? 1 : 0, 32)
         .addMaskShader(swimMask)
         .startGlowOptions(this, this.getID())
         .light(light)
         .applyEnemyTracker(this, perspective)
         .pos(drawX, drawY);
      list.add(new MobDrawable() {
         @Override
         public void draw(TickManager tickManager) {
            swimMask.use();
            options.draw();
            swimMask.stop();
         }
      });
      TextureDrawOptions shadow = MobRegistry.Textures.swampSlime_shadow.initDraw().sprite(spriteX, 0, 32).light(light).pos(drawX, drawY);
      tileList.add(tm -> shadow.draw());
   }

   @Override
   public boolean isSlimeImmune() {
      return true;
   }

   @Override
   public Stream<ModifierValue<?>> getDefaultModifiers() {
      return this.getLevel() != null && this.getLevel().isCave
         ? Stream.of(new ModifierValue<>(BuffModifiers.SPEED, 1.0F), new ModifierValue<>(BuffModifiers.CHASER_RANGE, 2.0F))
         : super.getDefaultModifiers();
   }

   @Override
   protected SoundSettings getHurtSound() {
      return new SoundSettings(GameResources.slimeSplash2);
   }
}
