package localforest.scene;

/** Headless invalidation checks for the camera observation cache. */
public final class SceneViewportTest {
    public static void main(String[] args) {
        RegionSceneQuery.ObservedView first=update(null,2048,1024,1200,900,192,160);
        for(int tile=0;tile<2000;tile++)
            check(update(first,2048,1024,1200,900,192,160)==first,"Unchanged viewport allocated a new observation");

        RegionSceneQuery.ObservedView moved=update(first,2080,1056,1200,900,192,160);
        check(moved!=first&&moved.bounds.left==first.bounds.left+1&&moved.bounds.top==first.bounds.top+1,
            "Camera movement did not update query origin");
        RegionSceneQuery.ObservedView resized=update(moved,2080,1056,1600,1000,192,160);
        check(resized.bounds.right>moved.bounds.right&&resized.bounds.bottom>moved.bounds.bottom,
            "Window resize did not expand query extent");
        RegionSceneQuery.ObservedView clipped=update(resized,2080,1056,1600,1000,96,64);
        check(clipped!=resized&&clipped.bounds.right==96&&clipped.bounds.bottom==64,
            "Level dimensions did not invalidate clipping");

        // Each Level key owns an independent observation. Visiting a second
        // level must not replace the previous level's camera state or bounds.
        RegionSceneQuery.ObservedView other=update(null,-32,-64,640,480,80,80);
        check(other.bounds.left==0&&other.bounds.top==0,"Second level origin was inherited");
        check(update(first,2048,1024,1200,900,192,160)==first,"Returning to the first level lost its viewport");
        check(update(other,-32,-64,640,480,80,80)==other,"Second level cache was overwritten");
        System.out.println("SceneViewportTest PASS: unchanged inputs reused; move, resize and level bounds invalidate independently");
    }
    private static RegionSceneQuery.ObservedView update(RegionSceneQuery.ObservedView old,
            int x,int y,int width,int height,int levelWidth,int levelHeight) {
        RegionSceneQuery.ObservedView next=RegionSceneQuery.ObservedView.update(old,x,y,width,height,levelWidth,levelHeight);
        check(next.bounds.equals(TileBounds.fromPixels(x,y,width,height,12).clip(levelWidth,levelHeight)),
            "Cached query differs from the original margin/clipping algorithm");
        return next;
    }
    private static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
}
