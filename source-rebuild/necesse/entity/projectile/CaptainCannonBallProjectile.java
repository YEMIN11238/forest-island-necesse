package necesse.entity.projectile;

import java.awt.Color;
import java.util.List;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.entity.levelEvent.explosionEvent.CaptainCannonBallExplosionEvent;
import necesse.entity.mobs.DeathMessageTable;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.trails.Trail;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.texture.TextureDrawOptions;
import necesse.gfx.drawables.EntityDrawable;
import necesse.gfx.drawables.LevelSortedDrawable;
import necesse.gfx.drawables.OrderableDrawables;
import necesse.level.maps.Level;
import necesse.level.maps.LevelObjectHit;
import necesse.level.maps.light.GameLight;

public class CaptainCannonBallProjectile extends Projectile {
   @Override
   public void setupSpawnPacket(PacketWriter writer) {
      super.setupSpawnPacket(writer);
      writer.putNextBoolean(this.isSolid);
   }

   @Override
   public void applySpawnPacket(PacketReader reader) {
      super.applySpawnPacket(reader);
      this.isSolid = reader.getNextBoolean();
   }

   @Override
   public void init() {
      super.init();
      this.setWidth(15.0F);
      this.height = 18.0F;
      this.heightBasedOnDistance = true;
      this.spawnTime = this.getWorldEntity().getTime();
      this.doesImpactDamage = false;
      this.trailOffset = 0.0F;
   }

   @Override
   public Trail getTrail() {
      return new Trail(this, this.getLevel(), new Color(30, 30, 30), 14.0F, 250, 18.0F);
   }

   @Override
   public void clientTick() {
      super.clientTick();
   }

   @Override
   public void addDrawables(
      List<LevelSortedDrawable> list,
      OrderableDrawables tileList,
      OrderableDrawables topList,
      OrderableDrawables overlayList,
      Level level,
      TickManager tickManager,
      GameCamera camera,
      PlayerMob perspective
   ) {
      if (!this.removed()) {
         GameLight light = level.getLightLevel(this);
         int drawX = camera.getDrawX(this.x) - this.texture.getWidth() / 2;
         int drawY = camera.getDrawY(this.y) - this.texture.getHeight() / 2;
         final TextureDrawOptions options = this.texture
            .initDraw()
            .light(light)
            .rotate(this.getAngle(), this.texture.getWidth() / 2, this.texture.getHeight() / 2)
            .pos(drawX, drawY - (int)this.getHeight());
         list.add(new EntityDrawable(this) {
            @Override
            public void draw(TickManager tickManager) {
               options.draw();
            }
         });
         this.addShadowDrawables(tileList, drawX, drawY, light, this.getAngle(), this.texture.getHeight() / 2);
      }
   }

   @Override
   public float getAngle() {
      return (float)(this.getWorldEntity().getTime() - this.spawnTime);
   }

   @Override
   public void doHitLogic(Mob mob, LevelObjectHit object, float x, float y) {
      super.doHitLogic(mob, object, x, y);
      if (this.isServer()) {
         CaptainCannonBallExplosionEvent event = new CaptainCannonBallExplosionEvent(x, y, this.getDamage(), this.getOwner());
         this.getLevel().entityManager.events.add(event);
      }
   }

   @Override
   public DeathMessageTable getDeathMessages() {
      return this.getDeathMessages("piratecap", 4);
   }
}
