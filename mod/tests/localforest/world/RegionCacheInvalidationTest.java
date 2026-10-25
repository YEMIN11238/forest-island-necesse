package localforest.world;

import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicInteger;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.matcher.ElementMatchers;
import necesse.engine.network.gameNetworkData.GNDItemMap;
import necesse.engine.registries.GNDRegistry;
import necesse.entity.manager.EntityManager;
import necesse.level.maps.Level;
import necesse.level.maps.regionSystem.Region;
import necesse.level.maps.regionSystem.RegionManager;
import necesse.level.maps.regionSystem.regionsStructure.RegionStructureDataAbstract;
import necesse.level.maps.regionSystem.regionsStructure.RegionStructureDataMap;

/** Exercise actual kernel lookup bytecode before/after the production Advice.
 * Sparse test fixtures deliberately skip constructors; no game or GL context. */
public final class RegionCacheInvalidationTest {
    private static final sun.misc.Unsafe UNSAFE;
    static{try{Field field=sun.misc.Unsafe.class.getDeclaredField("theUnsafe");field.setAccessible(true);UNSAFE=(sun.misc.Unsafe)field.get(null);}catch(Exception e){throw new ExceptionInInitializerError(e);}}
    private static void require(boolean value,String message){if(!value)throw new AssertionError(message);}
    private static void set(Object target,Class<?> owner,String name,Object value)throws Exception{Field field=owner.getDeclaredField(name);field.setAccessible(true);field.set(target,value);}
    private static Region region(){try{return (Region)UNSAFE.allocateInstance(Region.class);}catch(Exception e){throw new RuntimeException(e);}}
    public static void main(String[] args)throws Exception{
        System.setProperty("localforest.islandprofile","true");GNDRegistry.instance.registerCore();
        Level level=(Level)UNSAFE.allocateInstance(Level.class);
        GNDItemMap data=new GNDItemMap();data.setBoolean("localforestisland",true);set(level,Level.class,"gndData",data);
        EntityManager entities=(EntityManager)UNSAFE.allocateInstance(EntityManager.class);set(entities,EntityManager.class,"lock",new Object());set(level,Level.class,"entityManager",entities);
        RegionManager manager=(RegionManager)UNSAFE.allocateInstance(RegionManager.class);set(manager,RegionManager.class,"level",level);
        AtomicInteger constructions=new AtomicInteger();
        RegionStructureDataAbstract.NewRegionConstructor constructor=(x,y)->{constructions.incrementAndGet();return region();};
        RegionStructureDataAbstract.NewRegionLoader loader=(r,skip)->{};
        var old=new RegionStructureDataMap(manager,constructor,loader);
        Region prior=old.getRegion(-3,5,true,false);old.removeRegion(-3,5);
        require(old.getRegion(-3,5,false,false)==prior,"Kernel no longer reproduces stale-cache bug; reassess patch");
        Class<?> patched=new ByteBuddy().redefine(RegionStructureDataMap.class).name("localforest.world.PatchedRegionMapForTest")
            .visit(Advice.to(RegionCacheInvalidationPatch.class).on(ElementMatchers.named("removeRegion")))
            .make().load(RegionCacheInvalidationTest.class.getClassLoader(),ClassLoadingStrategy.Default.WRAPPER).getLoaded();
        RegionStructureDataAbstract map=(RegionStructureDataAbstract)patched.getConstructor(RegionManager.class,RegionStructureDataAbstract.NewRegionConstructor.class,RegionStructureDataAbstract.NewRegionLoader.class).newInstance(manager,constructor,loader);
        Region first=map.getRegion(-3,5,true,false);map.removeRegion(-3,5);
        require(map.getRegion(-3,5,false,false)==null,"Unloaded region remained visible");
        Region second=map.getRegion(-3,5,true,false);require(first!=second,"Reload returned stale Region");
        require(map.isRegionLoaded(-3,5)&&map.getLoadedRegionsSize()==1,"Map bookkeeping lost reload");
        require(RegionDiagnostics.invalidatedCaches()>0,"Production Advice was not applied");
        System.out.println("RegionCacheInvalidationTest PASS originalBug=reproduced patchedBytecode=true unloadedLookup=null reload=fresh negativeCoordinates=true");
    }
}
