package localforest.world;

import java.awt.Color;
import java.util.LinkedHashMap;
import java.util.Map;
import necesse.engine.localization.message.GameMessage;
import necesse.engine.localization.message.StaticMessage;
import necesse.engine.registries.BiomeRegistry;
import necesse.engine.registries.ObjectRegistry;
import necesse.engine.registries.TileRegistry;
import necesse.engine.util.GameRandom;
import necesse.engine.world.biomeGenerator.BiomeGeneratorStack;
import necesse.level.gameTile.GameTile;
import necesse.level.maps.Level;
import necesse.level.maps.biomes.Biome;
import necesse.level.maps.presets.set.VillageSet;
import necesse.level.maps.regionSystem.Region;

/** Native Biome extension, selected only by our v4 stack. Zero global weight
 * keeps existing/vanilla worlds unchanged. No second terrain/storage engine. */
public final class MosslumeContinuousBiome extends Biome {
    private static final Map<String,MosslumeContinuousBiome> registered=new LinkedHashMap<>();
    public final String habitat;
    private final Biome climate;
    private final String ground;
    private MosslumeContinuousBiome(String habitat,Biome climate,String ground){this.habitat=habitat;this.climate=climate;this.ground=ground;}
    public static void register(){
        if(!registered.isEmpty())return;
        WorldContent content=WorldContent.load(4);
        for(String habitat:content.biomes){
            var biome=new MosslumeContinuousBiome(habitat,BiomeRegistry.getBiome(content.text(habitat+".nativeBiome")),content.text(habitat+".ground"));
            registered.put(habitat,BiomeRegistry.registerBiome("mosslume_"+habitat,biome,false));
        }
    }
    public static MosslumeContinuousBiome get(String habitat){
        MosslumeContinuousBiome result=registered.get(habitat);
        if(result==null)throw new IllegalStateException("Continuous biome not registered: "+habitat);return result;
    }
    public static void initialize(BiomeGeneratorStack stack){for(var biome:registered.values())biome.initializeGeneratorStack(stack);}
    @Override public GameMessage getNewLocalization(){return new StaticMessage(switch(habitat){case "wetland"->"湿地";case "mushroom"->"蘑菇林";case "ruins"->"古代遗迹";case "snowfield"->"雪原";case "desert"->"沙漠";case "volcano"->"火山";default->"林间荒野";});}
    @Override public Color getDebugBiomeColor(){return climate.getDebugBiomeColor();}
    @Override public int getGenerationTerrainTileID(){return TileRegistry.getTileID(ground);}
    @Override public int getGenerationWaterTileID(){return climate.getGenerationWaterTileID();}
    @Override public int getGenerationBeachTileID(){return climate.getGenerationBeachTileID();}
    @Override public GameTile getUnderLiquidTile(Level level,int x,int y){return climate.getUnderLiquidTile(level,x,y);}
    // Native swamp uses null to reject vanilla villages before scanning a whole
    // candidate rectangle. Our small outposts use the v4 regional content rules.
    @Override public VillageSet[] getVillageSets(){return null;}
    @Override public void initializeGeneratorStack(BiomeGeneratorStack stack){stack.addRandomSimplexVeinsBranch("mosslume_"+habitat+"_canopy",1.7f,.28f,1f,1);}
    @Override public void generateRegionSurfaceTerrain(Region region,BiomeGeneratorStack stack,GameRandom random){
        if(!(stack instanceof MosslumeContinuousStack ours))return;
        int terrain=getGenerationTerrainTileID();
        var sites=stack.startPlaceOnVein(this,region,random,"mosslume_"+habitat+"_canopy")
            .onlyOnTile(terrain).test((r,g,rx,ry,l,x,y)->!ours.coast.owned(x,y)&&!ours.layout.reserved(x,y));
        // Native per-region RNG and biome gates preserve visit-order determinism.
        if(habitat.equals("forest")){
            sites.chance(.10).placeObject("oaktree");
            stack.startPlace(this,region,random).onlyOnTile(terrain).chance(.018)
                .test((r,g,rx,ry,l,x,y)->!ours.coast.owned(x,y)&&!ours.layout.reserved(x,y)).placeObject("birchtree");
            stack.startPlace(this,region,random).onlyOnTile(terrain).onlyOnObject(0).chance(.12)
                .test((r,g,rx,ry,l,x,y)->!ours.coast.owned(x,y)&&!ours.layout.reserved(x,y)).placeObject("grass");
            stack.startPlace(this,region,random).onlyOnTile(terrain).onlyOnObject(0).chance(.014)
                .test((r,g,rx,ry,l,x,y)->!ours.coast.owned(x,y)&&!ours.layout.reserved(x,y)).placeObject("blueberrybush");
            stack.startPlace(this,region,random).onlyOnTile(terrain).onlyOnObject(0).chance(.024)
                .test((r,g,rx,ry,l,x,y)->!ours.coast.owned(x,y)&&!ours.layout.reserved(x,y)).placeObject("yellowflowerpatch");
        }else{
            if(!habitat.equals("ruins"))sites.chance(.075).placeObject("mlbiome_"+habitat+"_tree");
            stack.startPlace(this,region,random).onlyOnTile(terrain).onlyOnObject(0).chance(.13)
                .test((r,g,rx,ry,l,x,y)->!ours.coast.owned(x,y)&&!ours.layout.reserved(x,y))
                .customPlace((r,g,rx,ry,l,x,y)->{
                    int variant=(int)(ours.layout.random(x,y,49)*9);
                    ObjectRegistry.getObject("mlbiome_"+habitat+"_flora"+variant).placeObject(l,x,y,0,false);
                });
        }
        stack.startPlace(this,region,random).onlyOnTile(terrain).onlyOnObject(0).chance(.012)
            .test((r,g,rx,ry,l,x,y)->!ours.coast.owned(x,y)&&!ours.layout.reserved(x,y)).placeObject("surfacerocksmall");
    }
    @Override public void generateRegionCaveTerrain(Region region,BiomeGeneratorStack stack,GameRandom random){
        stack.startPlace(this,region,random).onlyOnObject(ObjectRegistry.rockID).chance(.025).placeObjectForced("copperorerock");
        stack.startPlace(this,region,random).onlyOnObject(ObjectRegistry.rockID).chance(.018).placeObjectForced("ironorerock");
        stack.startPlace(this,region,random).onlyOnObject(0).chance(.015).placeObject("caverocksmall");
    }
}
