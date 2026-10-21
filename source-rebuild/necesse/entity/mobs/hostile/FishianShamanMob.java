package necesse.entity.mobs.hostile;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Stream;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.modifiers.ModifierValue;
import necesse.engine.registries.MobRegistry;
import necesse.engine.registries.TileRegistry;
import necesse.engine.util.GameRandom;
import necesse.entity.mobs.DeathMessageTable;
import necesse.entity.mobs.GameDamage;
import necesse.entity.mobs.MaskShaderOptions;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.MobDrawable;
import necesse.entity.mobs.PathDoorOption;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.ai.behaviourTree.BehaviourTreeAI;
import necesse.entity.mobs.ai.behaviourTree.trees.PlayerChaserWandererAI;
import necesse.entity.mobs.buffs.BuffModifiers;
import necesse.entity.particle.FleshParticle;
import necesse.entity.particle.Particle;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.DrawOptions;
import necesse.gfx.drawOptions.human.HumanDrawOptions;
import necesse.gfx.drawables.OrderableDrawables;
import necesse.inventory.lootTable.LootTable;
import necesse.inventory.lootTable.lootItem.LootItem;
import necesse.level.maps.Level;
import necesse.level.maps.TilePosition;
import necesse.level.maps.biomes.Biome;
import necesse.level.maps.light.GameLight;

public class FishianShamanMob extends HostileMob {
   public static LootTable lootTable = new LootTable(LootItem.between("coin", 10, 40).splitItems(4), LootItem.between("bamboo", 5, 10).splitItems(4));
   public static GameDamage damage = new GameDamage(55.0F);

   public FishianShamanMob() {
      super(300);
      this.setSpeed(40.0F);
      this.setFriction(3.0F);
      this.setKnockbackModifier(0.5F);
      this.setArmor(30);
      this.collision = new Rectangle(-10, -7, 20, 14);
      this.hitBox = new Rectangle(-14, -12, 28, 24);
      this.selectBox = new Rectangle(-14, -41, 28, 48);
      this.swimMaskMove = 16;
      this.swimMaskOffset = -2;
      this.swimSinkOffset = -4;
   }

   @Override
   public void init() {
      super.init();
      this.ai = new BehaviourTreeAI<>(this, new PlayerChaserWandererAI<Mob>(null, 384, 256, 40000, false, false) {
         @Override
         public boolean attackTarget(Mob mob, Mob target) {
            return this.shootSimpleProjectile(mob, target, "fishianwarriorhook", FishianShamanMob.damage, 200, 480);
         }
      });
   }

   @Override
   public LootTable getLootTable() {
      return lootTable;
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
   public DeathMessageTable getDeathMessages() {
      return this.getDeathMessages("fishian", 3);
   }

   @Override
   public void spawnDeathParticles(float knockbackX, float knockbackY) {
      for (int i = 0; i < 4; i++) {
         this.getLevel()
            .entityManager
            .addParticle(
               new FleshParticle(
                  this.getLevel(),
                  MobRegistry.Textures.fishianShaman.body,
                  GameRandom.globalRandom.nextInt(5),
                  8,
                  32,
                  this.x,
                  this.y,
                  20.0F,
                  knockbackX,
                  knockbackY
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
      int drawY = camera.getDrawY(y) - 44 - 7;
      int dir = this.getDir();
      Point sprite = this.getAnimSprite(x, y, dir);
      drawY += this.getBobbing(x, y);
      drawY += level.getTile(getTileCoordinate(x), getTileCoordinate(y)).getMobSinkingAmount(this);
      MaskShaderOptions swimMask = this.getSwimMaskShaderOptions(this.inLiquidFloat(x, y));
      HumanDrawOptions humanDrawOptions = new HumanDrawOptions(level, MobRegistry.Textures.fishianShaman)
         .sprite(sprite)
         .dir(dir)
         .mask(swimMask)
         .light(light)
         .applyEnemyTracker(this, perspective);
      final DrawOptions drawOptions = humanDrawOptions.pos(drawX, drawY);
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
      return 20;
   }

   @Override
   public Stream<ModifierValue<?>> getDefaultModifiers() {
      return Stream.of(new ModifierValue<>(BuffModifiers.FRICTION, 0.0F).min(0.75F));
   }

   @Override
   public int getTileWanderPriority(TilePosition pos, Biome baseBiome) {
      return pos.tileID() == TileRegistry.puddleCobble ? 1000 : super.getTileWanderPriority(pos, baseBiome);
   }
}
