package localforest.companions;

import java.awt.Point;
import java.lang.reflect.Field;
import java.util.AbstractMap.SimpleEntry;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.Map.Entry;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.matcher.ElementMatchers;
import necesse.engine.localization.message.GameMessage;
import necesse.engine.util.GameLinkedList;
import necesse.engine.world.WorldEntity;
import necesse.inventory.Inventory;
import necesse.inventory.InventoryItem;
import necesse.inventory.InventoryRange;
import necesse.inventory.item.Item;
import necesse.inventory.itemFilter.ItemCategoriesFilter;
import necesse.level.maps.Level;
import necesse.level.maps.levelData.settlementData.LevelStorage;
import necesse.level.maps.levelData.settlementData.SettlementStoragePickupFuture;
import necesse.level.maps.levelData.settlementData.SettlementStoragePickupSlot;
import necesse.level.maps.levelData.settlementData.storage.ItemFinder;
import necesse.level.maps.levelData.settlementData.storage.SettlementStorageRecord;
import necesse.level.maps.levelData.settlementData.storage.SettlementStorageRecordsRegionData;

/** Applies production Advice to real native finder bytecode. Sparse clock/level fixtures avoid GL;
 * validation, list removal, native future reservations and accept callbacks are not mocked. */
public final class NativeStorageFinderRepairTest {
    private static final sun.misc.Unsafe UNSAFE;
    static { try { Field f=sun.misc.Unsafe.class.getDeclaredField("theUnsafe");f.setAccessible(true);UNSAFE=(sun.misc.Unsafe)f.get(null); }
        catch(Exception e){throw new ExceptionInInitializerError(e);} }
    private static Class<?> finderType;
    private static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    private static final class Storage extends LevelStorage {
        final Inventory inventory=new Inventory(1); boolean valid=true;
        Storage(Level level,int x,int amount,int itemId) {
            super(level,x,10);
            Item item=new Item(100);item.idData.setData(itemId,"fixture"+itemId);
            inventory.setItem(0,new InventoryItem(item,amount));
        }
        @Override public InventoryRange getInventoryRange(){return valid?new InventoryRange(inventory):null;}
        @Override public ItemCategoriesFilter getFilter(){return null;}
        @Override public GameMessage getInventoryName(){return null;}
        @Override public boolean isStorageValid(){return valid;}
        int reserved(){return pickupSlots.get(0).stream().mapToInt(s->s.item.getAmount()).sum();}
    }
    private static Level level()throws Exception {
        Level level=(Level)UNSAFE.allocateInstance(Level.class);
        level.setWorldEntity((WorldEntity)UNSAFE.allocateInstance(WorldEntity.class));return level;
    }
    private static final class Fixture {
        final Level level;
        final SettlementStorageRecordsRegionData data=new SettlementStorageRecordsRegionData(null,item->true);
        final GameLinkedList<Entry<Point,GameLinkedList<SettlementStorageRecord>>> groups=new GameLinkedList<>();
        final Point start=new Point(0,10);
        Fixture()throws Exception{level=level();}
        Storage storage(int x,int amount,int id){return new Storage(level,x,amount,id);}
        GameLinkedList<SettlementStorageRecord> group(int x,Storage... storages){
            GameLinkedList<SettlementStorageRecord> records=new GameLinkedList<>();
            for(Storage storage:storages){InventoryItem item=storage.inventory.getItem(0);records.addLast(new SettlementStorageRecord(storage,0,item,item.getAmount()));}
            groups.addLast(new SimpleEntry<>(new Point(x,10),records));return records;
        }
        Object finder()throws Exception{return finderType.getConstructor(SettlementStorageRecordsRegionData.class,Point.class,GameLinkedList.class,Predicate.class).newInstance(data,start,groups,null);}
        SettlementStoragePickupSlot first(Predicate<InventoryItem> p)throws Exception{return (SettlementStoragePickupSlot)finderType.getMethod("findFirstItemPickup",Predicate.class).invoke(finder(),p);}
        @SuppressWarnings("unchecked") LinkedList<SettlementStoragePickupSlot> multiple(Object finder,int min,int max,Predicate<InventoryItem> p)throws Exception{
            return (LinkedList<SettlementStoragePickupSlot>)finderType.getMethod("findPickupSlots",int.class,int.class,Predicate.class).invoke(finder,min,max,p);
        }
        SettlementStoragePickupSlot best(Predicate<InventoryItem> p,Comparator<SettlementStoragePickupFuture> order)throws Exception{
            return (SettlementStoragePickupSlot)finderType.getMethod("findBestItem",Predicate.class,Comparator.class).invoke(finder(),p,order);
        }
    }
    private static void cases()throws Exception {
        System.setProperty("localforest.islandprofile","true");NativeStorageFinderRepair.install();
        necesse.engine.registries.GNDRegistry.instance.registerCore();
        check(NativeStorageFinderRepair.active(),"Mosslume installation scope inactive");
        finderType=new ByteBuddy().redefine(ItemFinder.class).name("localforest.companions.RepairedNativeItemFinderForTest")
            .visit(Advice.to(NativeStorageFinderRepair.FirstPatch.class).on(ElementMatchers.named("findFirstItemPickup")))
            .visit(Advice.to(NativeStorageFinderRepair.MultiplePatch.class).on(ElementMatchers.named("findPickupSlots")))
            .visit(Advice.to(NativeStorageFinderRepair.BestPatch.class).on(ElementMatchers.named("findBestItem")))
            .make().load(NativeStorageFinderRepairTest.class.getClassLoader(),ClassLoadingStrategy.Default.WRAPPER).getLoaded();
        Fixture invalid=new Fixture();Storage valid=invalid.storage(2,3,1),gone=invalid.storage(2,4,2);gone.valid=false;
        var invalidRecords=invalid.group(2,valid,gone);var slot=invalid.first(null);
        check(slot!=null&&slot.storage==valid&&slot.item.getAmount()==1,"Removed current record did not advance to previous valid item");
        check(invalidRecords.size()==1&&valid.reserved()==1,"Native invalid removal/reservation lost");

        Fixture rejected=new Fixture();Storage accepted=rejected.storage(3,2,3),filtered=rejected.storage(3,9,4);
        rejected.group(3,accepted,filtered);slot=rejected.first(item->item.item.getID()==3);
        check(slot!=null&&slot.storage==accepted&&filtered.reserved()==0,"Rejected last item prevented earlier item");
        Fixture exhausted=new Fixture();Storage available=exhausted.storage(4,3,5),reserved=exhausted.storage(4,1,6);
        reserved.reserve(0,reserved.inventory.getItem(0),1);exhausted.group(4,available,reserved);
        check(exhausted.first(null).storage==available,"Null native reserve did not advance");
        Fixture none=new Fixture();Storage n=none.storage(5,1,7);none.group(5,n);
        check(none.first(item->false)==null&&none.groups.isEmpty(),"Fully rejected search did not terminate/remove local finder group");

        Fixture combined=new Fixture();Storage near=combined.storage(2,4,8),far=combined.storage(9,5,9);
        near.reserve(0,near.inventory.getItem(0),2);far.reserve(0,far.inventory.getItem(0),1);
        var nearRecords=combined.group(2,near);var farRecords=combined.group(9,far);
        Object combinedFinder=combined.finder();var pickups=combined.multiple(combinedFinder,4,5,null);
        check(pickups.size()==2&&pickups.get(0).item.getAmount()==2&&pickups.get(1).item.getAmount()==3,"Partial native reservations/nearest ordering/max amounts changed");
        check(near.reserved()==4&&far.reserved()==4,"Accept used requested amount instead of actual future quantity");
        check(nearRecords.getFirst().itemAmount==2&&farRecords.getFirst().itemAmount==2,"Callback did not decrement actual accepted quantities");
        Field tileField=finderType.getDeclaredField("currentTile");tileField.setAccessible(true);
        check(tileField.get(combinedFinder).equals(new Point(9,10)),"Successful multi search cursor not updated");

        Fixture shortage=new Fixture();Storage shortStore=shortage.storage(2,3,10);var shortRecords=shortage.group(2,shortStore);
        Object shortFinder=shortage.finder();check(shortage.multiple(shortFinder,5,8,null)==null,"Minimum shortage did not fail");
        check(shortStore.reserved()==0&&shortRecords.getFirst().itemAmount==3&&tileField.get(shortFinder).equals(shortage.start),"Failed minimum accepted futures or changed cursor");
        Fixture multiReject=new Fixture();Storage earlier=multiReject.storage(2,2,11),later=multiReject.storage(2,5,12);multiReject.group(2,earlier,later);
        check(multiReject.multiple(multiReject.finder(),1,2,item->item.item.getID()==11).getFirst().storage==earlier,"Multi filter cursor did not advance");
        Fixture multiInvalid=new Fixture();Storage earlierValid=multiInvalid.storage(2,2,13),laterInvalid=multiInvalid.storage(2,3,14);laterInvalid.valid=false;multiInvalid.group(2,earlierValid,laterInvalid);
        check(multiInvalid.multiple(multiInvalid.finder(),1,2,null).getFirst().storage==earlierValid,"Multi validation removal cursor failed");

        Fixture best=new Fixture();Storage lower=best.storage(2,4,15),winner=best.storage(2,2,19),unavailable=best.storage(2,1,99),ignored=best.storage(2,5,100),invalidBest=best.storage(2,1,101);
        unavailable.reserve(0,unavailable.inventory.getItem(0),1);invalidBest.valid=false;
        var bestRecords=best.group(2,lower,winner,unavailable,ignored,invalidBest);
        slot=best.best(item->item.item.getID()!=100,Comparator.comparingInt(future->future.item.item.getID()));
        check(slot!=null&&slot.storage==winner,"Best comparator received null or did not scan every valid candidate");
        check(winner.reserved()==1&&lower.reserved()==0&&ignored.reserved()==0,"Losing best futures were accepted");
        check(bestRecords.size()==4,"Best validation removal changed native local records");
        check(winner.inventory.getItem(0).getAmount()==2&&lower.inventory.getItem(0).getAmount()==4,"Reservation mutated inventory before actual pickup");
        Fixture bestNone=new Fixture();Storage allBusy=bestNone.storage(2,1,20);allBusy.reserve(0,allBusy.inventory.getItem(0),1);bestNone.group(2,allBusy);
        check(bestNone.best(null,Comparator.comparingInt(future->future.item.item.getID()))==null,"All-unavailable best did not terminate");
        System.clearProperty("localforest.islandprofile");check(!NativeStorageFinderRepair.active(),"Repair leaked outside Mosslume scope");
    }
    public static void main(String[] args)throws Exception {
        var executor=Executors.newSingleThreadExecutor(r->{Thread thread=new Thread(r,"finder-regression");thread.setDaemon(true);return thread;});
        try { executor.submit(()->{try{cases();}catch(Exception e){throw new RuntimeException(e);}}).get(12,TimeUnit.SECONDS); }
        finally {executor.shutdownNow();}
        System.out.println("NativeStorageFinderRepairTest PASS actualAdvice=true nativeValidation=true nativeReservation=true cursorRemoval/filter/nullFuture=true minMax/order/callback/cursor=true scope=Mosslume");
    }
}
