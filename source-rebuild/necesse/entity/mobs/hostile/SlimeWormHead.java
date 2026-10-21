package necesse.entity.mobs.hostile;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.List;
import java.util.stream.Stream;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.modifiers.ModifierValue;
import necesse.engine.network.server.ServerClient;
import necesse.engine.registries.MobRegistry;
import necesse.engine.sound.SoundEffect;
import necesse.engine.sound.SoundManager;
import necesse.engine.util.GameMath;
import necesse.engine.util.GameRandom;
import necesse.entity.mobs.GameDamage;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.MobDrawable;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.WormMoveLine;
import necesse.entity.mobs.ai.behaviourTree.BehaviourTreeAI;
import necesse.entity.mobs.ai.behaviourTree.trees.PlayerChargingCirclingChaserAI;
import necesse.entity.mobs.ai.behaviourTree.util.FlyingAIMover;
import necesse.entity.mobs.buffs.BuffModifiers;
import necesse.gfx.GameResources;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.texture.TextureDrawOptions;
import necesse.gfx.drawables.OrderableDrawables;
import necesse.gfx.gameTexture.GameTexture;
import necesse.inventory.lootTable.LootTable;
import necesse.level.maps.Level;
import necesse.level.maps.light.GameLight;

public class SlimeWormHead extends HostileWormMobHead<SlimeWormBody, SlimeWormHead> {
   public static LootTable lootTable = new LootTable();
   public static float lengthPerBodyPart = 20.0F;
   public static float waveLength = 350.0F;
   public static final int totalBodyParts = 30;
   public static GameDamage headCollisionDamage = new GameDamage(130.0F);
   public static GameDamage bodyCollisionDamage = new GameDamage(100.0F);

   public SlimeWormHead() {
      super(1800, waveLength, 70.0F, 30, 0.0F, -24.0F);
      this.moveAccuracy = 120;
      this.setSpeed(50.0F);
      this.setArmor(25);
      this.accelerationMod = 1.0F;
      this.decelerationMod = 1.0F;
      this.collision = new Rectangle(-16, -14, 32, 28);
      this.hitBox = new Rectangle(-20, -16, 40, 32);
      this.selectBox = new Rectangle(-20, -35, 40, 40);
   }

   protected float getDistToBodyPart(SlimeWormBody bodyPart, int index, float lastDistance) {
      float length = lengthPerBodyPart;
      if (index >= 24) {
         int sprite = index - 24 + 1;
         length = Math.max(length - sprite * 2, 5.0F);
      }

      float movedDist = 0.0F;
      WormMoveLine first = this.moveLines.getFirst();
      if (first != null) {
         movedDist = first.movedDist;
      }

      movedDist /= 0.77F;
      return length - (float)((Math.sin((index - movedDist / (lengthPerBodyPart * 0.8F)) / 2.0F) + 1.0) / 2.0) * (length * 0.6F);
   }

   protected void onUpdatedBodyPartPos(SlimeWormBody bodyPart, int index, float distToBodyPart) {
      super.onUpdatedBodyPartPos(bodyPart, index, distToBodyPart);
      int minSprite = 0;
      if (index >= 24) {
         minSprite = index - 24 + 1;
      }

      float movedDist = 0.0F;
      WormMoveLine first = this.moveLines.getFirst();
      if (first != null) {
         movedDist = first.movedDist;
      }

      movedDist /= 0.77F;
      float percent = 1.0F - (float)((Math.sin((index - movedDist / (lengthPerBodyPart * 0.8F)) / 2.0F) + 1.0) / 2.0);
      int lerp = GameMath.limit(GameMath.lerp(percent, 0, 4), minSprite, Math.max(minSprite, 4));
      bodyPart.sprite = new Point(0, lerp);
   }

   protected SlimeWormBody createNewBodyPart(int index) {
      SlimeWormBody bodyPart = new SlimeWormBody();
      bodyPart.sharesHitCooldownWithNext = index % 3 < 2;
      bodyPart.relaysBuffsToNext = index % 3 < 2;
      bodyPart.bodyIndex = index;
      if (index >= 24) {
         int sprite = index - 24 + 1;
         bodyPart.sprite = new Point(0, sprite);
      } else {
         bodyPart.sprite = new Point(0, 0);
      }

      return bodyPart;
   }

   @Override
   protected void playMoveSound() {
      SoundManager.playSound(GameResources.slimeSplash2, SoundEffect.effect(this).volume(0.2F).pitch(GameRandom.globalRandom.getFloatBetween(0.9F, 1.2F)));
   }

   @Override
   public void init() {
      super.init();
      this.ai = new BehaviourTreeAI<>(this, new PlayerChargingCirclingChaserAI<>(null, 2560, 500, 20), new FlyingAIMover());
   }

   @Override
   public float getTurnSpeed(float delta) {
      return super.getTurnSpeed(delta) * 1.2F;
   }

   @Override
   public LootTable getLootTable() {
      return lootTable;
   }

   @Override
   public GameDamage getCollisionDamage(Mob target, boolean fromPacket, ServerClient packetSubmitter) {
      return null;
   }

   @Override
   public void spawnDeathParticles(float knockbackX, float knockbackY) {
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
   }

   @Override
   protected TextureDrawOptions getShadowDrawOptions(Level level, int x, int y, GameLight light, GameCamera camera) {
      GameTexture shadowTexture = MobRegistry.Textures.slimeWorm.shadow;
      int drawX = camera.getDrawX(x) - 32;
      int drawY = camera.getDrawY(y) - 32;
      drawY += this.getBobbing(x, y);
      return shadowTexture.initDraw().sprite(0, 1, 64).light(light).pos(drawX, drawY);
   }

   @Override
   public Stream<ModifierValue<?>> getDefaultModifiers() {
      return Stream.of(new ModifierValue<>(BuffModifiers.SLOW, 0.0F).max(0.2F), new ModifierValue<>(BuffModifiers.FIRE_DAMAGE, 0.0F).max(0.0F));
   }

   @Override
   public boolean isSlimeImmune() {
      return true;
   }
}
