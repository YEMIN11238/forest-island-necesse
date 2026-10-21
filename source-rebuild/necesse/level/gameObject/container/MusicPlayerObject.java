package necesse.level.gameObject.container;

import java.awt.Color;
import java.awt.Rectangle;
import java.util.Collections;
import java.util.List;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.localization.Localization;
import necesse.engine.localization.message.LocalMessage;
import necesse.engine.network.server.ServerClient;
import necesse.engine.registries.ContainerRegistry;
import necesse.engine.util.GameRandom;
import necesse.engine.world.worldData.SettlementsWorldData;
import necesse.entity.mobs.Attacker;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.objectEntity.MusicPlayerObjectEntity;
import necesse.entity.objectEntity.ObjectEntity;
import necesse.entity.particle.Particle;
import necesse.entity.particle.ParticleOption;
import necesse.gfx.GameResources;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.texture.TextureDrawOptions;
import necesse.gfx.drawables.LevelSortedDrawable;
import necesse.gfx.drawables.OrderableDrawables;
import necesse.gfx.gameTexture.GameTexture;
import necesse.gfx.gameTexture.GameTextureSection;
import necesse.gfx.gameTooltips.ListGameTooltips;
import necesse.inventory.InventoryItem;
import necesse.inventory.container.object.OEInventoryContainer;
import necesse.inventory.item.Item;
import necesse.inventory.item.toolItem.ToolType;
import necesse.level.gameObject.GameObject;
import necesse.level.gameObject.furniture.RoomFurniture;
import necesse.level.maps.Level;
import necesse.level.maps.levelData.jobs.ListenToMusicTileLevelJob;
import necesse.level.maps.levelData.jobs.TileLevelJob;
import necesse.level.maps.light.GameLight;

public class MusicPlayerObject extends GameObject implements RoomFurniture {
   public GameTexture texture;
   public GameTextureSection musicNotesTexture;

   public MusicPlayerObject() {
      super(new Rectangle(32, 32));
      this.displayMapTooltip = true;
      this.objectHealth = 50;
      this.isLightTransparent = true;
      this.rarity = Item.Rarity.RARE;
      this.toolType = ToolType.ALL;
      this.hoverHitbox = new Rectangle(0, -32, 32, 64);
      this.roomProperties.add("musicplayer");
      this.setItemCategory("objects", "misc");
      this.setCraftingCategory("objects", "misc");
   }

   @Override
   public void loadTextures() {
      super.loadTextures();
      this.texture = GameTexture.fromFile("objects/musicplayer");
      this.musicNotesTexture = GameResources.musicNotesParticle;
   }

   @Override
   protected Rectangle getCollision(Level level, int x, int y, int rotation) {
      if (rotation == 0) {
         return new Rectangle(x * 32 + 2, y * 32 + 10, 28, 20);
      } else if (rotation == 1) {
         return new Rectangle(x * 32 + 4, y * 32 + 6, 26, 22);
      } else {
         return rotation == 2 ? new Rectangle(x * 32 + 2, y * 32 + 4, 28, 20) : new Rectangle(x * 32 + 2, y * 32 + 6, 26, 22);
      }
   }

   @Override
   public void tickEffect(Level level, int layerID, int tileX, int tileY) {
      super.tickEffect(level, layerID, tileX, tileY);
      ObjectEntity objectEntity = level.entityManager.getObjectEntity(tileX, tileY);
      if (objectEntity instanceof MusicPlayerObjectEntity) {
         MusicPlayerObjectEntity musicPlayer = (MusicPlayerObjectEntity)objectEntity;
         if (musicPlayer.getCurrentMusic() != null && !musicPlayer.isPaused() && GameRandom.globalRandom.getEveryXthChance(10)) {
            int sprite = GameRandom.globalRandom.nextInt(4);
            int sway = GameRandom.globalRandom.getIntBetween(5, 15) * GameRandom.globalRandom.getOneOf(1, -1);
            int dx = GameRandom.globalRandom.getIntBetween(-10, 10);
            byte rotation = level.getObjectRotation(tileX, tileY);
            int startX;
            int startY;
            int startHeight;
            switch (rotation) {
               case 0:
                  startX = tileX * 32 + 16;
                  startY = tileY * 32 + 12;
                  startHeight = 40;
                  break;
               case 1:
                  startX = tileX * 32 + 20;
                  startY = tileY * 32 + 16;
                  startHeight = 40;
                  break;
               case 2:
                  startX = tileX * 32 + 16;
                  startY = tileY * 32 + 20;
                  startHeight = 45;
                  break;
               default:
                  startX = tileX * 32 + 12;
                  startY = tileY * 32 + 16;
                  startHeight = 40;
            }

            int heightIncrease = GameRandom.globalRandom.getIntBetween(20, 50);
            int totalLifeTime = GameRandom.globalRandom.getIntBetween(1000, 3000);
            level.entityManager
               .addParticle(ParticleOption.base(startX, startY), Particle.GType.COSMETIC)
               .moves((pos, delta, lifeTime, timeAlive, lifePercent) -> {
                  pos.x = startX + (float)Math.sin(timeAlive / 500.0) * sway + dx * lifePercent;
                  pos.y = startY;
               })
               .sizeFadesInAndOut(12, 18, 100, 500)
               .lifeTime(totalLifeTime)
               .heightMoves(startHeight, startHeight + heightIncrease)
               .sprite(this.musicNotesTexture.sprite(sprite % 2, sprite / 2, 10))
               .color(Color.BLACK);
         }
      }
   }

   @Override
   public void addDrawables(
      List<LevelSortedDrawable> list,
      OrderableDrawables tileList,
      Level level,
      int tileX,
      int tileY,
      TickManager tickManager,
      GameCamera camera,
      PlayerMob perspective
   ) {
      GameLight light = level.getLightLevel(tileX, tileY);
      int drawX = camera.getTileDrawX(tileX);
      int drawY = camera.getTileDrawY(tileY);
      byte rotation = level.getObjectRotation(tileX, tileY);
      final TextureDrawOptions base = this.texture
         .initDraw()
         .sprite(rotation, 0, 32, this.texture.getHeight())
         .addObjectDamageOverlay(this, level, tileX, tileY)
         .light(light)
         .pos(drawX, drawY - (this.texture.getHeight() - 32));
      list.add(new LevelSortedDrawable(this, tileX, tileY) {
         @Override
         public int getSortY() {
            return 16;
         }

         @Override
         public void draw(TickManager tickManager) {
            base.draw();
         }
      });
   }

   @Override
   public void drawPreview(Level level, int tileX, int tileY, int rotation, float alpha, PlayerMob player, GameCamera camera) {
      int drawX = camera.getTileDrawX(tileX);
      int drawY = camera.getTileDrawY(tileY);
      this.texture.initDraw().sprite(rotation, 0, 32, this.texture.getHeight()).alpha(alpha).draw(drawX, drawY - (this.texture.getHeight() - 32));
   }

   @Override
   public String getInteractTip(Level level, int x, int y, PlayerMob perspective, boolean debug) {
      return Localization.translate("controls", "opentip");
   }

   @Override
   public boolean canInteract(Level level, int x, int y, PlayerMob player) {
      return true;
   }

   @Override
   public void interact(Level level, int x, int y, PlayerMob player) {
      super.interact(level, x, y, player);
      if (level.isServer()) {
         OEInventoryContainer.openAndSendContainer(ContainerRegistry.MUSIC_PLAYER_CONTAINER, player.getServerClient(), level, x, y);
      }
   }

   @Override
   public ObjectEntity getNewObjectEntity(Level level, int x, int y) {
      return new MusicPlayerObjectEntity(level, x, y);
   }

   @Override
   public void doExplosionDamage(Level level, int layerID, int tileX, int tileY, int damage, float toolTier, Attacker attacker, ServerClient client) {
      boolean hasSettlement = SettlementsWorldData.getSettlementsData(level).hasSettlementAtTile(level, tileX, tileY);
      if (!hasSettlement) {
         super.doExplosionDamage(level, layerID, tileX, tileY, damage, toolTier, attacker, client);
      }
   }

   @Override
   public void onWireUpdate(Level level, int layerID, int tileX, int tileY, int wireID, boolean active) {
      super.onWireUpdate(level, layerID, tileX, tileY, wireID, active);
      ObjectEntity objectEntity = level.entityManager.getObjectEntity(tileX, tileY);
      if (objectEntity instanceof MusicPlayerObjectEntity) {
         ((MusicPlayerObjectEntity)objectEntity).onWireUpdated();
      }
   }

   @Override
   public ListGameTooltips getItemTooltips(InventoryItem item, PlayerMob perspective) {
      ListGameTooltips tooltips = super.getItemTooltips(item, perspective);
      tooltips.add(Localization.translate("itemtooltip", "musicplayertip"));
      tooltips.add(new LocalMessage("itemtooltip", "playingentrec"), 350);
      return tooltips;
   }

   @Override
   public String getFurnitureType() {
      return "musicplayer";
   }

   @Override
   protected boolean shouldPlayInteractSound(Level level, int tileX, int tileY) {
      return true;
   }

   @Override
   public List<TileLevelJob> getLevelJobs(Level level, int tileX, int tileY) {
      MusicPlayerObjectEntity musicPlayer = this.getCurrentObjectEntity(level, tileX, tileY, MusicPlayerObjectEntity.class);
      return musicPlayer != null && !musicPlayer.isPaused() && musicPlayer.getMusicManager().hasAnyVinyls()
         ? Collections.singletonList(new ListenToMusicTileLevelJob(tileX, tileY, this))
         : Collections.emptyList();
   }
}
