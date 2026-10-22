package localforest.scene;

/** Half-open tile bounds. Camera-relative receivers remain correct away from world origin. */
public final class TileBounds {
    public final int left, top, right, bottom;
    public TileBounds(int left, int top, int right, int bottom) {
        this.left = left;
        this.top = top;
        this.right = Math.max(left, right);
        this.bottom = Math.max(top, bottom);
    }
    public static TileBounds fromPixels(float x, float y, int width, int height, int margin) {
        return new TileBounds((int)Math.floor(x / 32.0) - margin,
            (int)Math.floor(y / 32.0) - margin,
            (int)Math.ceil((x + width) / 32.0) + margin,
            (int)Math.ceil((y + height) / 32.0) + margin);
    }
    public TileBounds clip(int width, int height) {
        return new TileBounds(width > 0 ? Math.max(0, Math.min(width, left)) : left,
            height > 0 ? Math.max(0, Math.min(height, top)) : top,
            width > 0 ? Math.max(0, Math.min(width, right)) : right,
            height > 0 ? Math.max(0, Math.min(height, bottom)) : bottom);
    }
    public int width() { return right - left; }
    public int height() { return bottom - top; }
    public boolean contains(int x, int y) { return x >= left && x < right && y >= top && y < bottom; }
    @Override public boolean equals(Object obj) {
        if (!(obj instanceof TileBounds)) return false;
        TileBounds b = (TileBounds)obj;
        return left == b.left && top == b.top && right == b.right && bottom == b.bottom;
    }
    @Override public int hashCode() { return ((left * 31 + top) * 31 + right) * 31 + bottom; }
}
