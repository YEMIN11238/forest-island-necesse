package localforest.visual;

import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.imageio.ImageIO;
import necesse.gfx.drawOptions.texture.SharedTextureDrawOptions;
import necesse.gfx.drawables.LevelTileTerrainDrawOptions;
import necesse.gfx.gameTexture.GameTexture;
import necesse.gfx.res.ResourceEncoder;
import necesse.level.maps.Level;

/** Connected dirt trails rendered from the existing ground and dirt art.
 * Native tile IDs, digging, farmland, collision and saved terrain remain authoritative.
 */
public final class NaturalPathRenderer {
    private static final int TILE=32, PAGE_TILES=16, PAGE_SIZE=TILE*PAGE_TILES;
    private static final List<GameTexture> pages=new ArrayList<>();
    private static final Map<Long,Entry> tiles=new HashMap<>();
    private static BufferedImage grass,dirt;
    private static boolean failed;
    public static long draws,rebuilds;
    private static final class Entry {
        final int page,x,y;
        int neighbours=-1;
        Entry(int index){page=index/(PAGE_TILES*PAGE_TILES);int slot=index%(PAGE_TILES*PAGE_TILES);x=(slot%PAGE_TILES)*TILE;y=(slot/PAGE_TILES)*TILE;}
    }

    public static boolean isPath(Level level,int x,int y) {
        if(x<0||y<0||x>=level.tileWidth||y>=level.tileHeight)return false;
        String id=level.getTile(x,y).getStringID();
        return !id.contains("farmland")&&(id.contains("dirt")||id.contains("path")||id.contains("gravel"));
    }

    public static synchronized boolean draw(LevelTileTerrainDrawOptions under,Level level,int tx,int ty,int dx,int dy) {
        if(Boolean.getBoolean("mosslume.visual.legacy")||failed)return false;
        boolean road=isPath(level,tx,ty);
        if(!road&&(!"grasstile".equals(level.getTile(tx,ty).getStringID())||level.tileLayer.isPlayerPlaced(tx,ty)))return false;
        int neighbours=(isPath(level,tx,ty-1)?1:0)|(isPath(level,tx+1,ty)?2:0)
            |(isPath(level,tx,ty+1)?4:0)|(isPath(level,tx-1,ty)?8:0)
            |(isPath(level,tx-1,ty-1)?16:0)|(isPath(level,tx+1,ty-1)?32:0)
            |(isPath(level,tx+1,ty+1)?64:0)|(isPath(level,tx-1,ty+1)?128:0);
        if(!road&&!NaturalPathShape.hasCorner(neighbours))return false;
        if(grass==null)try {
            try(InputStream g=ResourceEncoder.getResourceInputStream("localforest/ground.png");
                InputStream d=ResourceEncoder.getResourceInputStream("localforest/dirt.png")) {
                grass=ImageIO.read(g);dirt=ImageIO.read(d);
                if(grass==null||dirt==null)throw new IllegalStateException("Missing path source texture");
            }
        } catch(Exception e) {
            failed=true;System.err.println("MOSSLUME_NATURAL_PATH_UNAVAILABLE "+e);return false;
        }
        int signature=neighbours|(road?256:0);
        long key=((long)tx<<32)^(ty&0xffffffffL);
        Entry entry=tiles.get(key);
        if(entry==null) {
            entry=new Entry(tiles.size());tiles.put(key,entry);
            while(pages.size()<=entry.page) {
                GameTexture texture=new GameTexture("mosslume-natural-path-"+pages.size(),PAGE_SIZE,PAGE_SIZE);
                texture.setBlendQuality(GameTexture.BlendQuality.NEAREST);pages.add(texture);
            }
        }
        if(entry.neighbours!=signature) {
            GameTexture page=pages.get(entry.page);
            byte[] row=new byte[TILE*4];
            for(int y=0;y<TILE;y++) {
                for(int x=0;x<TILE;x++) {
                    int wx=tx*TILE+x,wy=ty*TILE+y;
                    int alpha=road?NaturalPathShape.coverage(x,y,tx*TILE,ty*TILE,neighbours)
                        :NaturalPathShape.cornerCoverage(x,y,tx*TILE,ty*TILE,neighbours);
                    int g=grass.getRGB(Math.floorMod(wx,grass.getWidth()),Math.floorMod(wy,grass.getHeight()));
                    int d=dirt.getRGB(Math.floorMod(wx,dirt.getWidth()),Math.floorMod(wy,dirt.getHeight()));
                    row[x*4]=(byte)mix((g>>>16)&255,(d>>>16)&255,alpha);
                    row[x*4+1]=(byte)mix((g>>>8)&255,(d>>>8)&255,alpha);
                    row[x*4+2]=(byte)mix(g&255,d&255,alpha);row[x*4+3]=(byte)255;
                }
                // Dirty the shared atlas once per changed row. No per-frame image allocation or upload.
                page.putData(((entry.y+y)*PAGE_SIZE+entry.x)*4,row);
            }
            entry.neighbours=signature;rebuilds++;
        }
        final GameTexture page=pages.get(entry.page);
        SharedTextureDrawOptions batch=under.specialDrawables.getOrCreate("mosslume-natural-path-"+entry.page,0,()->new SharedTextureDrawOptions(page));
        batch.addSection(entry.x,entry.x+TILE,entry.y,entry.y+TILE).pos(dx,dy);
        draws++;return true;
    }
    private static int mix(int a,int b,int alpha){return (a*(255-alpha)+b*alpha+127)/255;}
    private NaturalPathRenderer(){}
}
