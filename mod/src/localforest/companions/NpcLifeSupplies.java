package localforest.companions;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import necesse.engine.network.gameNetworkData.GNDItemMap;
import necesse.engine.registries.ObjectRegistry;
import necesse.entity.mobs.friendly.human.humanShop.HumanShop;
import necesse.entity.objectEntity.InventoryObjectEntity;
import necesse.inventory.Inventory;
import necesse.inventory.InventoryItem;
import necesse.level.maps.Level;
import necesse.level.maps.levelData.settlementData.ServerSettlementData;

/** Finite starting belongings and real native dining furniture. Never replenishes consumed food. */
final class NpcLifeSupplies {
    static final String PREFIX="mosslumeLifeV1_";
    record Supply(String id,int amount) {}
    static final List<Supply> PANTRY=List.of(new Supply("apple",12),new Supply("bread",6));
    record Furniture(String key,String object,int x,int y,int rotation) {}
    // Beds occupy x=1,3,5 at rows 1/2. Row 4's center stays free to reach the south door.
    static final List<Furniture> INTERIOR=List.of(
        new Furniture("pantry","oakchest",1,4,0),
        new Furniture("chairA","oakchair",2,3,1),
        new Furniture("chairB","oakchair",4,3,3),
        new Furniture("chairC","oakchair",5,4,0));
    private NpcLifeSupplies() {}

    static void ration(Level level,HumanShop mob,NpcDefinition definition) {
        // Native WorkInventory.add ignores non-settlers, so grant only after moveIn/onRecruited.
        if(definition.tier()==NpcDefinition.Tier.VISITOR||!mob.isSettler())return;
        String key=PREFIX+"ration_"+definition.id();
        if(level.gndData.getBoolean(key)||mob.getWorkInventory().isFull())return;
        giveOnce(level.gndData,key,new InventoryItem("bread",1),item->mob.getWorkInventory().add(item));
    }
    static boolean giveOnce(GNDItemMap data,String key,InventoryItem item,Consumer<InventoryItem> destination) {
        if(data.getBoolean(key))return false;
        destination.accept(item);data.setBoolean(key,true);return true;
    }
    static void residence(Level level,ServerSettlementData settlement,int district,int left,int top) {
        for(Furniture item:INTERIOR) {
            int x=left+item.x(),y=top+item.y();
            if(!level.regionManager.isTileLoaded(x,y))continue;
            String owned=PREFIX+district+"_"+item.key();
            if(!level.gndData.getBoolean(owned)) {
                // An old world's empty interior may gain furniture. Existing player objects win.
                if(level.getObjectID(x,y)!=0||level.isLiquidTile(x,y)||level.entityManager.mobs.stream()
                    .anyMatch(m->!m.removed()&&m.getTileX()==x&&m.getTileY()==y))continue;
                ObjectRegistry.getObject(item.object()).placeObject(level,x,y,item.rotation(),false);
                if(!item.object().equals(level.getObject(x,y).getStringID()))continue;
                level.gndData.setBoolean(owned,true);
            }
            // The marker records that we supplied it once, not permission to recreate it after removal.
            if(!item.object().equals(level.getObject(x,y).getStringID()))continue;
            if(item.key().equals("pantry")&&level.entityManager.getObjectEntity(x,y) instanceof InventoryObjectEntity chest) {
                String stockKey=PREFIX+district+"_pantryStock";
                if(!level.gndData.getBoolean(stockKey))stockOnce(chest.getInventory(),level.gndData,stockKey,
                    PANTRY.stream().map(supply->new InventoryItem(supply.id(),supply.amount())).toList());
                String assigned=PREFIX+district+"_storageAssigned";
                if(!level.gndData.getBoolean(assigned)&&settlement.storageManager.assignStorage(x,y,true)!=null)
                    level.gndData.setBoolean(assigned,true);
            }
        }
    }
    /** Plan all empty slots before mutating: an obstructed/full old pantry cannot be partially refilled. */
    static boolean stockOnce(Inventory inventory,GNDItemMap data,String key,List<InventoryItem> supplies) {
        if(data.getBoolean(key))return false;
        List<Integer> free=new ArrayList<>();
        for(int slot=0;slot<inventory.getSize()&&free.size()<supplies.size();slot++)if(inventory.getItem(slot)==null)free.add(slot);
        if(free.size()<supplies.size())return false;
        for(int i=0;i<supplies.size();i++)inventory.setItem(free.get(i),supplies.get(i));
        data.setBoolean(key,true);return true;
    }
}
