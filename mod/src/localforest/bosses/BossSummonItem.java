package localforest.bosses;

import java.awt.Point;
import java.awt.geom.Line2D;
import javax.imageio.ImageIO;
import localforest.world.WorldDefinition;
import necesse.engine.localization.message.GameMessage;
import necesse.engine.localization.message.StaticMessage;
import necesse.engine.network.gameNetworkData.GNDItemMap;
import necesse.engine.network.packet.PacketChatMessage;
import necesse.engine.registries.MobRegistry;
import necesse.engine.util.GameBlackboard;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.PlayerMob;
import necesse.gfx.gameTexture.GameTexture;
import necesse.gfx.gameTooltips.ListGameTooltips;
import necesse.gfx.res.ResourceEncoder;
import necesse.inventory.InventoryItem;
import necesse.inventory.item.Item;
import necesse.inventory.item.placeableItem.consumableItem.ConsumableItem;
import necesse.level.maps.Level;

/** Workstation-crafted summon consumable, validated and consumed only by the native server. */
public final class BossSummonItem extends ConsumableItem {
    private final MosslumeBosses.Definition definition;
    public BossSummonItem(MosslumeBosses.Definition definition){super(10,true);this.definition=definition;itemCooldownTime.setBaseValue(2000);setItemCategory("consumable","bossitems");rarity=Item.Rarity.RARE;worldDrawSize=32;}
    @Override public GameMessage getNewLocalization(){return new StaticMessage(definition.summonName());}
    @Override protected void loadItemTextures(){
        String path="mosslume/bosses/"+(definition==MosslumeBosses.STONE?"stone-echo":"moon-seal");
        try(var input=ResourceEncoder.getResourceInputStream(path+".png")){
            var image=ImageIO.read(input);if(image==null||image.getWidth()!=32||image.getHeight()!=32)throw new IllegalStateException("Expected 32px original summon icon");
            itemTexture=GameTexture.fromFile(path);itemTexture.setBlendQuality(GameTexture.BlendQuality.NEAREST);
        }catch(Exception error){throw new IllegalStateException("Missing or invalid Boss summon art: "+path,error);}
    }
    @Override public ListGameTooltips getTooltips(InventoryItem item,PlayerMob player,GameBlackboard board){ListGameTooltips out=super.getTooltips(item,player,board);out.add("召唤："+definition.name());out.add("离营地至少24格，在空旷地表使用。");out.add("附近5×5格需要无树木、建筑与水面。");out.add("生命降至一半时，攻势会增强。");return out;}
    @Override public String canPlace(Level level,int x,int y,PlayerMob player,Line2D line,InventoryItem item,GNDItemMap data){
        if(!level.isServer())return null;
        if(level.isCave||!level.isSurfaceLevel())return "surface";
        WorldDefinition world=WorldDefinition.forLevel(level);
        int cx=world==null?level.gndData.getInt("mosslumeCampX",51):world.campX,cy=world==null?level.gndData.getInt("mosslumeCampY",36):world.campY;
        if(Math.hypot(player.getTileX()-cx,player.getTileY()-cy)<24)return "camp";
        if(level.entityManager.mobs.stream().anyMatch(m->m instanceof MosslumeBossMob&&!m.removed()&&!m.hasDied()&&player.getDistance(m)<1800))return "active";
        return spawnPoint(level,player)==null?"space":null;
    }
    public Point spawnPoint(Level level,PlayerMob player){
        int px=player.getTileX(),py=player.getTileY();
        for(int distance=5;distance<=8;distance++)for(int step=0;step<16;step++){
            double angle=Math.PI*2*step/16;int x=px+(int)Math.round(Math.cos(angle)*distance),y=py+(int)Math.round(Math.sin(angle)*distance);
            boolean clear=true;
            for(int dy=-2;dy<=2&&clear;dy++)for(int dx=-2;dx<=2;dx++)if(!level.regionManager.isTileLoaded(x+dx,y+dy)||level.isLiquidTile(x+dx,y+dy)||level.getObject(x+dx,y+dy).isSolid){clear=false;break;}
            if(clear&&!level.collides(new Line2D.Float(player.x,player.y,x*32+16,y*32+16),player.getLevelCollisionFilter()))return new Point(x,y);
        }
        return null;
    }
    @Override public InventoryItem onPlace(Level level,int x,int y,PlayerMob player,int seed,InventoryItem item,GNDItemMap data){
        if(!level.isServer())return item;
        String error=canPlace(level,x,y,player,null,item,data);if(error!=null){onAttemptPlace(level,x,y,player,item,data,error);return item;}
        Point point=spawnPoint(level,player);if(point==null)return item;
        Mob mob=MobRegistry.getMob(definition.id(),level);mob.setSpawnTilePosition(point.x,point.y);
        level.entityManager.addMob(mob,point.x*32+16,point.y*32+16);MosslumeBossMob.encountersStarted++;
        level.getServer().network.sendToClientsWithEntity(new PacketChatMessage(new StaticMessage(definition.name()+"回应了你的召唤。注意地面上的蓄力预警。")),mob);
        if(isSingleUse(player))item.setAmount(item.getAmount()-1);return item;
    }
    @Override public InventoryItem onAttemptPlace(Level level,int x,int y,PlayerMob player,InventoryItem item,GNDItemMap data,String error){
        if(level.isServer()&&player!=null&&player.isServerClient())player.getServerClient().sendChatMessage(new StaticMessage(switch(error){case "surface"->"请在地表召唤。";case "camp"->"离营地至少24格再召唤，别把危险带回家。";case "active"->"附近已有一位首领，请先结束这场战斗。";default->"附近没有5×5格的空地。请先清理树木，远离建筑和水边。";}));return item;
    }
}
