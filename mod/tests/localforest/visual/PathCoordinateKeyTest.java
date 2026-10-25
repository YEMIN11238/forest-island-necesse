package localforest.visual;

import java.util.HashSet;
import java.util.Set;

/** Detect coordinate aliases and the diagonal Long.hashCode collision pattern. */
public final class PathCoordinateKeyTest {
    public static void main(String[] args) {
        Set<Long> keys=new HashSet<>();
        Set<Integer> buckets=new HashSet<>();
        for(int i=-4096;i<4096;i++) {
            long key=NaturalPathRenderer.coordinateKey(i,i);
            if(!keys.add(key))throw new AssertionError("Diagonal coordinates alias");
            int h=Long.hashCode(key);buckets.add((h^(h>>>16))&8191);
        }
        if(buckets.size()<4000)throw new AssertionError("Diagonal path still collapses into collision buckets");
        for(int x:new int[]{Integer.MIN_VALUE,-1000000,-1,0,1,1000000,Integer.MAX_VALUE})
            for(int y:new int[]{Integer.MIN_VALUE,-1000000,-1,0,1,1000000,Integer.MAX_VALUE})
                if(x!=y&&!keys.add(NaturalPathRenderer.coordinateKey(x,y)))throw new AssertionError("Signed coordinates alias");
        System.out.println("PathCoordinateKeyTest PASS: diagonal distribution, signed/extreme coordinate identity");
    }
}
