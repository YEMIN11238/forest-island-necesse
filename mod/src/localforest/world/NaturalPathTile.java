package localforest.world;

import necesse.engine.localization.message.GameMessage;
import necesse.engine.localization.message.StaticMessage;
import necesse.level.gameTile.DirtTile;
import necesse.level.maps.Level;
import necesse.level.maps.regionSystem.SimulatePriorityList;

/** The maintained camp trail has native dirt interactions but does not erode into grass. */
public final class NaturalPathTile extends DirtTile {
    @Override public GameMessage getNewLocalization(){return new StaticMessage("林间小径");}
    @Override public void tick(Level level,int x,int y){}
    @Override public void addSimulateLogic(Level level,int x,int y,long ticks,SimulatePriorityList list,boolean sendChanges){}
}
