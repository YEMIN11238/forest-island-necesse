package localforest.companions;

import java.awt.Point;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map.Entry;
import java.util.function.Predicate;
import localforest.world.IslandWorld;
import necesse.engine.modLoader.annotations.ModMethodPatch;
import necesse.engine.util.GameLinkedList;
import necesse.inventory.InventoryItem;
import necesse.level.maps.levelData.settlementData.SettlementStoragePickupFuture;
import necesse.level.maps.levelData.settlementData.SettlementStoragePickupSlot;
import necesse.level.maps.levelData.settlementData.storage.ItemFinder;
import necesse.level.maps.levelData.settlementData.storage.SettlementStorageRecord;
import necesse.level.maps.levelData.settlementData.storage.SettlementStorageRecordsRegionData;
import net.bytebuddy.asm.Advice;

/** Fix the recovered kernel's missing linked-list cursor assignments without changing its archive.
 * Native validation, reservation and pickup callbacks remain authoritative. */
public final class NativeStorageFinderRepair {
    private static volatile boolean installed;
    private NativeStorageFinderRepair() {}
    public static void install() {
        installed=true;
        System.out.println("MOSSLUME_STORAGE_FINDER_REPAIR installed cursor-finally native-reservations");
    }
    public static boolean active() { return installed && IslandWorld.enabled(); }
    public record SearchResult<T>(T value, Point tile, int scanned) {}

    private static GameLinkedList<Entry<Point,GameLinkedList<SettlementStorageRecord>>>.Element nearest(
        GameLinkedList<Entry<Point,GameLinkedList<SettlementStorageRecord>>> records, Point tile, HashSet<Point> visited) {
        return records.streamElements().filter(e->!visited.contains(e.object.getKey()))
            .min(Comparator.comparingDouble(e->tile.distance(e.object.getKey()))).orElse(null);
    }

    public static SearchResult<SettlementStoragePickupSlot> first(SettlementStorageRecordsRegionData data, Point start,
        GameLinkedList<Entry<Point,GameLinkedList<SettlementStorageRecord>>> records, Predicate<InventoryItem> filter) {
        HashSet<Point> visited=new HashSet<>(); int scanned=0;
        // The budget is the complete input, not an arbitrary cap which could hide valid storage.
        int groupsRemaining=records.size();
        while(groupsRemaining-->0 && !records.isEmpty()) {
            var group=nearest(records,start,visited); if(group==null)break;
            Point tile=group.object.getKey(); visited.add(tile);
            var list=group.object.getValue(); var current=list.getLastElement();
            int remaining=list.size(); boolean found=false;
            while(current!=null && remaining-->0) {
                var next=current.prev(); scanned++;
                try {
                    InventoryItem item=data.validateItem(current);
                    if(item==null || (filter!=null && !filter.test(item)))continue;
                    SettlementStorageRecord record=current.object; found=true;
                    SettlementStoragePickupSlot pickup=record.storage.reserve(record.inventorySlot,item,1);
                    if(pickup==null)continue;
                    record.itemAmount--;
                    if(record.itemAmount<=0 && !current.isRemoved())current.remove();
                    return new SearchResult<>(pickup,tile,scanned);
                } finally { current=next; }
            }
            if(!found && !group.isRemoved())group.remove();
        }
        return new SearchResult<>(null,start,scanned);
    }

    public static SearchResult<LinkedList<SettlementStoragePickupSlot>> multiple(SettlementStorageRecordsRegionData data,
        Point start, GameLinkedList<Entry<Point,GameLinkedList<SettlementStorageRecord>>> records,
        int minAmount,int maxAmount,Predicate<InventoryItem> filter) {
        Point tileNow=start; HashSet<Point> visited=new HashSet<>();
        LinkedList<SettlementStoragePickupFuture> futures=new LinkedList<>();
        int amount=0,scanned=0,groupsRemaining=records.size();
        while(amount<maxAmount && groupsRemaining-->0 && !records.isEmpty()) {
            var group=nearest(records,tileNow,visited); if(group==null)break;
            Point tile=group.object.getKey(); visited.add(tile);
            var list=group.object.getValue(); var current=list.getLastElement();
            int remaining=list.size(); boolean found=false;
            while(current!=null && remaining-->0 && amount<maxAmount) {
                var next=current.prev(); scanned++;
                try {
                    InventoryItem item=data.validateItem(current); if(item==null)continue;
                    SettlementStorageRecord record=current.object; found=true;
                    if(filter!=null && !filter.test(item))continue;
                    var element=current;
                    SettlementStoragePickupFuture future=record.storage.getFutureReserve(record.inventorySlot,item,
                        Math.min(record.itemAmount,maxAmount-amount),accepted->{
                            record.itemAmount-=accepted.item.getAmount();
                            if(record.itemAmount<=0 && !element.isRemoved())element.remove();
                        });
                    if(future==null)continue;
                    futures.add(future); amount+=future.item.getAmount(); tileNow=tile;
                } finally { current=next; }
            }
            if(!found && !group.isRemoved())group.remove();
        }
        if(amount<minAmount)return new SearchResult<>(null,start,scanned);
        LinkedList<SettlementStoragePickupSlot> result=new LinkedList<>();
        for(var future:futures)result.add(future.accept(future.item.getAmount()));
        return new SearchResult<>(result,tileNow,scanned);
    }

    public static SearchResult<SettlementStoragePickupSlot> best(SettlementStorageRecordsRegionData data, Point start,
        GameLinkedList<Entry<Point,GameLinkedList<SettlementStorageRecord>>> records,Predicate<InventoryItem> filter,
        Comparator<SettlementStoragePickupFuture> comparator) {
        HashSet<Point> visited=new HashSet<>(); SettlementStoragePickupFuture winner=null;
        int scanned=0,groupsRemaining=records.size();
        while(groupsRemaining-->0 && !records.isEmpty()) {
            var group=nearest(records,start,visited); if(group==null)break;
            visited.add(group.object.getKey());
            var list=group.object.getValue(); var current=list.getLastElement(); int remaining=list.size();
            while(current!=null && remaining-->0) {
                var next=current.prev(); scanned++;
                try {
                    InventoryItem item=data.validateItem(current);
                    if(item==null || (filter!=null && !filter.test(item)))continue;
                    SettlementStorageRecord record=current.object; var element=current;
                    SettlementStoragePickupFuture candidate=record.storage.getFutureReserve(record.inventorySlot,item,1,accepted->{
                        record.itemAmount-=accepted.item.getAmount();
                        if(record.itemAmount<=0 && !element.isRemoved())element.remove();
                    });
                    if(candidate!=null && (winner==null || comparator.compare(winner,candidate)<0))winner=candidate;
                } finally { current=next; }
            }
        }
        return winner==null ? new SearchResult<>(null,start,scanned)
            : new SearchResult<>(winner.accept(1),new Point(winner.storage.tileX,winner.storage.tileY),scanned);
    }

    @ModMethodPatch(target=ItemFinder.class,name="findFirstItemPickup",arguments={Predicate.class})
    public static final class FirstPatch {
        @Advice.OnMethodEnter(skipOn=Advice.OnNonDefaultValue.class)
        public static SearchResult<SettlementStoragePickupSlot> before(
            @Advice.FieldValue("regionData") SettlementStorageRecordsRegionData data,
            @Advice.FieldValue("currentTile") Point tile,
            @Advice.FieldValue("records") GameLinkedList<Entry<Point,GameLinkedList<SettlementStorageRecord>>> records,
            @Advice.Argument(0) Predicate<InventoryItem> filter) {
            return active()?first(data,tile,records,filter):null;
        }
        @Advice.OnMethodExit public static void after(@Advice.Enter SearchResult<SettlementStoragePickupSlot> result,
            @Advice.Return(readOnly=false) SettlementStoragePickupSlot value,@Advice.FieldValue(value="currentTile",readOnly=false) Point tile) {
            if(result!=null){value=result.value();tile=result.tile();}
        }
    }
    @ModMethodPatch(target=ItemFinder.class,name="findPickupSlots",arguments={int.class,int.class,Predicate.class})
    public static final class MultiplePatch {
        @Advice.OnMethodEnter(skipOn=Advice.OnNonDefaultValue.class)
        public static SearchResult<LinkedList<SettlementStoragePickupSlot>> before(
            @Advice.FieldValue("regionData") SettlementStorageRecordsRegionData data,
            @Advice.FieldValue("currentTile") Point tile,
            @Advice.FieldValue("records") GameLinkedList<Entry<Point,GameLinkedList<SettlementStorageRecord>>> records,
            @Advice.Argument(0) int min,@Advice.Argument(1) int max,@Advice.Argument(2) Predicate<InventoryItem> filter) {
            return active()?multiple(data,tile,records,min,max,filter):null;
        }
        @Advice.OnMethodExit public static void after(@Advice.Enter SearchResult<LinkedList<SettlementStoragePickupSlot>> result,
            @Advice.Return(readOnly=false) LinkedList<SettlementStoragePickupSlot> value,@Advice.FieldValue(value="currentTile",readOnly=false) Point tile) {
            if(result!=null){value=result.value();tile=result.tile();}
        }
    }
    @ModMethodPatch(target=ItemFinder.class,name="findBestItem",arguments={Predicate.class,Comparator.class})
    public static final class BestPatch {
        @Advice.OnMethodEnter(skipOn=Advice.OnNonDefaultValue.class)
        public static SearchResult<SettlementStoragePickupSlot> before(
            @Advice.FieldValue("regionData") SettlementStorageRecordsRegionData data,
            @Advice.FieldValue("currentTile") Point tile,
            @Advice.FieldValue("records") GameLinkedList<Entry<Point,GameLinkedList<SettlementStorageRecord>>> records,
            @Advice.Argument(0) Predicate<InventoryItem> filter,@Advice.Argument(1) Comparator<SettlementStoragePickupFuture> comparator) {
            return active()?best(data,tile,records,filter,comparator):null;
        }
        @Advice.OnMethodExit public static void after(@Advice.Enter SearchResult<SettlementStoragePickupSlot> result,
            @Advice.Return(readOnly=false) SettlementStoragePickupSlot value,@Advice.FieldValue(value="currentTile",readOnly=false) Point tile) {
            if(result!=null){value=result.value();tile=result.tile();}
        }
    }
}
