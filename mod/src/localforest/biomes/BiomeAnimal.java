package localforest.biomes;

import java.awt.Rectangle;
import java.util.List;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.entity.mobs.MobDrawable;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.friendly.critters.CritterMob;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawables.OrderableDrawables;
import necesse.inventory.lootTable.LootTable;
import necesse.inventory.lootTable.lootItem.LootItem;
import necesse.level.maps.Level;

/** Reuses native CritterAI wandering/fleeing and native hunt/damage/save lifecycle. */
public class BiomeAnimal extends CritterMob {
    public final BiomeCreatures.Species species;
    protected BiomeAnimal(String name){
        super(BiomeCreatures.species(name).health());species=BiomeCreatures.species(name);
        setSpeed(species.speed());setFriction(3);
        collision=new Rectangle(-9,-6,18,12);hitBox=new Rectangle(-13,-24,26,30);selectBox=new Rectangle(-18,-34,36,40);
    }
    @Override public LootTable getLootTable(){return species.drop()==null?new LootTable():new LootTable(new LootItem(species.drop()));}
    @Override public void spawnDeathParticles(float x,float y){BiomeCreatureSprites.death(this,species);}
    @Override public void addDrawables(List<MobDrawable> list,OrderableDrawables tiles,OrderableDrawables top,Level level,int x,int y,TickManager tick,GameCamera camera,PlayerMob viewer){
        super.addDrawables(list,tiles,top,level,x,y,tick,camera,viewer);
        BiomeCreatureSprites.draw(this,species,list,tiles,level,x,y,camera,viewer);
    }
}
