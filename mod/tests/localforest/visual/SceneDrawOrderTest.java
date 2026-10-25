package localforest.visual;

import java.util.*;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.gfx.drawables.LevelSortedDrawable;
import necesse.level.gameObject.GameObject;

/** Compare the native comparator to buffered/native boundaries, including ties. */
public final class SceneDrawOrderTest {
    public static void main(String[] args) {
        List<Integer> expected=new ArrayList<>(),actual=new ArrayList<>();
        List<LevelSortedDrawable> reference=new ArrayList<>(),buffered=new ArrayList<>();
        Random random=new Random(4212026);
        for(int i=0;i<300;i++){
            final int id=i,x=random.nextInt(80)-40,y=random.nextInt(80)-40,sort=random.nextInt(32);
            GameObject object=null;
            reference.add(new LevelSortedDrawable(object,x,y){
                public int getSortY(){return sort;}
                public void draw(TickManager tick){expected.add(id);}
            });
            if(i%3!=0)buffered.add(CrownSpriteBatch.sceneDrawable(object,x,y,sort,()->actual.add(id)));
            else buffered.add(new LevelSortedDrawable(object,x,y){
                public int getSortY(){return sort;}
                public void draw(TickManager tick){actual.add(id);}
            });
        }
        reference.sort(null);
        List<LevelSortedDrawable> wrapped=CrownSpriteBatch.boundaries(buffered);wrapped.sort(null);
        for(LevelSortedDrawable draw:reference)draw.draw(null);
        for(LevelSortedDrawable draw:wrapped)draw.draw(null);
        if(!expected.equals(actual))throw new AssertionError("Batch boundary changed native Y/tie order");
        System.out.println("SceneDrawOrderTest PASS: native ordering, mixed native/buffered draws, signed coordinates and ties");
    }
}
