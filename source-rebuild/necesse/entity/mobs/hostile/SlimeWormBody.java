package necesse.entity.mobs.hostile;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.List;
import java.util.stream.Stream;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.localization.message.GameMessage;
import necesse.engine.localization.message.LocalMessage;
import necesse.engine.modifiers.ModifierValue;
import necesse.engine.network.server.ServerClient;
import necesse.engine.registries.MobRegistry;
import necesse.engine.util.GameRandom;
import necesse.entity.mobs.GameDamage;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.MobDrawable;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.WormMobHead;
import necesse.entity.mobs.buffs.BuffModifiers;
import necesse.entity.particle.FleshParticle;
import necesse.entity.particle.Particle;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.texture.TextureDrawOptions;
import necesse.gfx.drawables.OrderableDrawables;
import necesse.gfx.gameTexture.GameSprite;
import necesse.gfx.gameTexture.GameTexture;
import necesse.level.maps.Level;
import necesse.level.maps.light.GameLight;

public class SlimeWormBody extends HostileWormMobBody<SlimeWormHead, SlimeWormBody> {
   public int bodyIndex;
   public Point sprite = new Point(0, 0);

   public SlimeWormBody() {
      super(1200);
      this.setArmor(25);
      this.collision = new Rectangle(-20, -15, 40, 30);
      this.hitBox = new Rectangle(-25, -20, 50, 40);
      this.selectBox = new Rectangle(-32, -60, 64, 64);
   }

   @Override
   public GameMessage getLocalization() {
      return new LocalMessage("mob", "slimeworm");
   }

   @Override
   public GameDamage getCollisionDamage(Mob target, boolean fromPacket, ServerClient packetSubmitter) {
      return this.bodyIndex == 0 ? SlimeWormHead.headCollisionDamage : SlimeWormHead.bodyCollisionDamage;
   }

   @Override
   public void spawnDeathParticles(float knockbackX, float knockbackY) {
      if (this.isVisible()) {
         for (int i = 0; i < 2; i++) {
            this.getLevel()
               .entityManager
               .addParticle(
                  new FleshParticle(
                     this.getLevel(),
                     MobRegistry.Textures.slimeWorm.body,
                     2,
                     GameRandom.globalRandom.nextInt(5),
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
      if (this.isVisible()) {
         GameLight light = level.getLightLevel(this);
         int drawX = camera.getDrawX(x) - 32;
         int drawY = camera.getDrawY(y);
         WormMobHead.addDrawable(
            list,
            this,
            new GameSprite(MobRegistry.Textures.slimeWorm.body, this.sprite.x, this.sprite.y, 64),
            MobRegistry.Textures.slimeWorm_mask,
            light,
            (int)this.height,
            drawX,
            drawY,
            64,
            perspective
         );
         this.addShadowDrawables(tileList, level, x, y, light, camera);
      }
   }

   @Override
   protected TextureDrawOptions getShadowDrawOptions(Level level, int x, int y, GameLight light, GameCamera camera) {
      GameTexture shadowTexture = MobRegistry.Textures.slimeWorm.shadow;
      int drawX = camera.getDrawX(x) - 32;
      int drawY = camera.getDrawY(y) - 32;
      drawY += this.getBobbing(x, y);
      return shadowTexture.initDraw().sprite(this.sprite.x, this.sprite.y, 64).light(light).pos(drawX, drawY);
   }

   @Override
   public Stream<ModifierValue<?>> getDefaultModifiers() {
      return Stream.of(new ModifierValue<>(BuffModifiers.FIRE_DAMAGE, 0.0F).max(0.0F));
   }

   @Override
   public boolean isSlimeImmune() {
      return true;
   }
}
