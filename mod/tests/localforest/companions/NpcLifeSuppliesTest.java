package localforest.companions;

import java.util.ArrayList;
import java.util.List;
import necesse.engine.network.gameNetworkData.GNDItemMap;
import necesse.engine.registries.GNDRegistry;
import necesse.engine.save.SaveData;
import necesse.inventory.Inventory;
import necesse.inventory.InventoryItem;
import necesse.inventory.item.Item;

/** Actual inventory and native GND save markers: never overwrite player items or refill after reload. */
public final class NpcLifeSuppliesTest {
    private static void check(boolean value,String why){if(!value)throw new AssertionError(why);}
    private static InventoryItem item(int id,String name,int amount){Item item=new Item(250);item.idData.setData(id,name);return new InventoryItem(item,amount);}
    public static void main(String[] args){
        GNDRegistry.instance.registerCore();
        var data=new GNDItemMap();Inventory inventory=new Inventory(4);
        var keepsake=item(1,"playerkeepsake",3);inventory.setItem(0,keepsake);
        List<InventoryItem> supplies=List.of(item(2,"apple",12),item(3,"bread",6));
        check(NpcLifeSupplies.stockOnce(inventory,data,"pantry",supplies),"Initial physical pantry stock failed");
        check(inventory.getItem(0)==keepsake&&inventory.getItem(1).getAmount()==12&&inventory.getItem(2).getAmount()==6,"Player slot overwritten or wrong stock");
        inventory.setItem(1,null);inventory.getItem(2).setAmount(1);
        check(!NpcLifeSupplies.stockOnce(inventory,data,"pantry",supplies)&&inventory.getItem(1)==null&&inventory.getItem(2).getAmount()==1,"Consumed food was replenished");
        SaveData save=new SaveData("GND");data.addSaveData(save);var restored=new GNDItemMap(save.toLoadData());
        check(!NpcLifeSupplies.stockOnce(inventory,restored,"pantry",supplies),"Saved stock marker lost; reconnect duplicates food");
        Inventory crowded=new Inventory(2);crowded.setItem(0,keepsake);
        check(!NpcLifeSupplies.stockOnce(crowded,new GNDItemMap(),"blocked",supplies)&&crowded.getItem(1)==null&&crowded.getItem(0)==keepsake,"Partial full-pantry transaction mutated items");
        List<InventoryItem> carried=new ArrayList<>();
        check(NpcLifeSupplies.giveOnce(restored,"ration",item(4,"bread",1),carried::add),"Starting carried ration not issued");
        check(!NpcLifeSupplies.giveOnce(restored,"ration",item(4,"bread",1),carried::add)&&carried.size()==1,"Ration duplicated by repeated binding");
        SaveData second=new SaveData("GND");restored.addSaveData(second);
        check(!NpcLifeSupplies.giveOnce(new GNDItemMap(second.toLoadData()),"ration",item(4,"bread",1),carried::add),"Ration regenerated after load");
        check(NpcLifeSupplies.PANTRY.stream().filter(s->s.id().equals("apple")).mapToInt(NpcLifeSupplies.Supply::amount).sum()==12
            &&NpcLifeSupplies.PANTRY.stream().filter(s->s.id().equals("bread")).mapToInt(NpcLifeSupplies.Supply::amount).sum()==6,"Authored finite pantry kit changed");
        java.util.Set<java.awt.Point> occupied=new java.util.HashSet<>();
        for(var furniture:NpcLifeSupplies.INTERIOR){
            check(occupied.add(new java.awt.Point(furniture.x(),furniture.y())),"Dining objects overlap");
            check(furniture.x()>0&&furniture.x()<6&&furniture.y()>=3&&furniture.y()<5,"Furniture overlaps native wall/bed rows");
            check(furniture.x()!=3,"Furniture blocks the center aisle to the door");
        }
        check(NpcLifeSupplies.INTERIOR.stream().filter(f->f.object().equals("oakchair")).count()==3,"Three residents lack separate native seats");
        GNDItemMap lifecycle=new GNDItemMap();
        for(var resident:NpcDefinition.INITIAL)lifecycle.setBoolean(NpcLifeSupplies.PREFIX+"ration_"+resident.id(),true);
        for(int district=1;district<=3;district++)for(var furniture:NpcLifeSupplies.INTERIOR)
            lifecycle.setBoolean(NpcLifeSupplies.PREFIX+district+"_"+furniture.key(),true);
        check(!NpcPopulationService.lifeReadyData(lifecycle),"Furniture-only readiness skipped pantry stock/registration retry");
        for(int district=1;district<=3;district++)lifecycle.setBoolean(NpcLifeSupplies.PREFIX+district+"_pantryStock",true);
        check(!NpcPopulationService.lifeReadyData(lifecycle),"Pantry registration failure incorrectly marked life ready");
        for(int district=1;district<=3;district++)lifecycle.setBoolean(NpcLifeSupplies.PREFIX+district+"_storageAssigned",true);
        check(NpcPopulationService.lifeReadyData(lifecycle),"Successful finite supply setup never completes");
        inventory.setItem(2,null); // Later consumption/removal is intentionally not an initialization failure.
        check(NpcPopulationService.lifeReadyData(lifecycle),"Initialization checks reset after player removal/consumption");
        System.out.println("NpcLifeSuppliesTest PASS nativeInventory=true nativeMarkerSave=true onceOnlyRationAndPantry=true preservePlayerItems=true atomicInsufficientSpace=true bedsAndDoorClear=true failedStockAndRegistrationRetry=true");
    }
}
