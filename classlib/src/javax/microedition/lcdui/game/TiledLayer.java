package javax.microedition.lcdui.game;

import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;

public class TiledLayer extends Layer {
    private final int columns;
    private final int rows;
    private final int[] cells;
    private Image image;
    private int tileWidth;
    private int tileHeight;
    private int tilesPerRow;
    private int tileCount;
    /* animated[i] is the static tile shown for animated tile -(i + 1). */
    private int[] animated = new int[4];
    private int animatedCount;

    public TiledLayer(int columns, int rows, Image image, int tileWidth, int tileHeight) {
        super(0, 0);
        if (columns < 1 || rows < 1) {
            throw new IllegalArgumentException();
        }
        this.columns = columns;
        this.rows = rows;
        cells = new int[columns * rows];
        setTiles(image, tileWidth, tileHeight);
    }

    private void setTiles(Image img, int tw, int th) {
        if (img == null) {
            throw new NullPointerException();
        }
        int iw = img.getWidth();
        int ih = img.getHeight();
        if (tw < 1 || th < 1 || iw % tw != 0 || ih % th != 0) {
            throw new IllegalArgumentException();
        }
        image = img;
        tileWidth = tw;
        tileHeight = th;
        tilesPerRow = iw / tw;
        tileCount = tilesPerRow * (ih / th);
        setSize(columns * tw, rows * th);
    }

    public int createAnimatedTile(int staticTileIndex) {
        if (staticTileIndex < 0 || staticTileIndex > tileCount) {
            throw new IndexOutOfBoundsException();
        }
        if (animatedCount == animated.length) {
            int[] a = new int[animated.length * 2];
            System.arraycopy(animated, 0, a, 0, animatedCount);
            animated = a;
        }
        animated[animatedCount++] = staticTileIndex;
        return -animatedCount;
    }

    public void setAnimatedTile(int animatedTileIndex, int staticTileIndex) {
        if (staticTileIndex < 0 || staticTileIndex > tileCount) {
            throw new IndexOutOfBoundsException();
        }
        animated[animatedIndex(animatedTileIndex)] = staticTileIndex;
    }

    public int getAnimatedTile(int animatedTileIndex) {
        return animated[animatedIndex(animatedTileIndex)];
    }

    private int animatedIndex(int animatedTileIndex) {
        int i = -animatedTileIndex - 1;
        if (animatedTileIndex >= 0 || i >= animatedCount) {
            throw new IndexOutOfBoundsException();
        }
        return i;
    }

    private void checkTile(int tileIndex) {
        if (tileIndex > tileCount || tileIndex < -animatedCount) {
            throw new IndexOutOfBoundsException();
        }
    }

    public void setCell(int col, int row, int tileIndex) {
        if (col < 0 || col >= columns || row < 0 || row >= rows) {
            throw new IndexOutOfBoundsException();
        }
        checkTile(tileIndex);
        cells[row * columns + col] = tileIndex;
    }

    public int getCell(int col, int row) {
        if (col < 0 || col >= columns || row < 0 || row >= rows) {
            throw new IndexOutOfBoundsException();
        }
        return cells[row * columns + col];
    }

    public void fillCells(int col, int row, int numCols, int numRows, int tileIndex) {
        if (numCols < 0 || numRows < 0) {
            throw new IllegalArgumentException();
        }
        if (col < 0 || row < 0 || col + numCols > columns || row + numRows > rows) {
            throw new IndexOutOfBoundsException();
        }
        checkTile(tileIndex);
        for (int r = row; r < row + numRows; r++) {
            for (int c = col; c < col + numCols; c++) {
                cells[r * columns + c] = tileIndex;
            }
        }
    }

    public final int getCellWidth() {
        return tileWidth;
    }

    public final int getCellHeight() {
        return tileHeight;
    }

    public final int getColumns() {
        return columns;
    }

    public final int getRows() {
        return rows;
    }

    public void setStaticTileSet(Image image, int tileWidth, int tileHeight) {
        int oldCount = tileCount;
        setTiles(image, tileWidth, tileHeight);
        if (tileCount < oldCount) {
            for (int i = 0; i < cells.length; i++) {
                cells[i] = 0;
            }
            animatedCount = 0;
        }
    }

    /* ---- Shared with Sprite ---- */

    /* The static tile shown in a cell (animated tiles resolved), 0 if empty. */
    int staticTileAt(int col, int row) {
        int t = cells[row * columns + col];
        return t < 0 ? animated[-t - 1] : t;
    }

    Image tileImage() {
        return image;
    }

    int tileSourceX(int tile) {
        return ((tile - 1) % tilesPerRow) * tileWidth;
    }

    int tileSourceY(int tile) {
        return ((tile - 1) / tilesPerRow) * tileHeight;
    }

    public final void paint(Graphics g) {
        if (g == null) {
            throw new NullPointerException();
        }
        if (!visible) {
            return;
        }
        int x1 = Math.max(g.getClipX(), x);
        int y1 = Math.max(g.getClipY(), y);
        int x2 = Math.min(g.getClipX() + g.getClipWidth(), x + width);
        int y2 = Math.min(g.getClipY() + g.getClipHeight(), y + height);
        if (x1 >= x2 || y1 >= y2) {
            return;
        }
        int c1 = (x1 - x) / tileWidth;
        int c2 = (x2 - 1 - x) / tileWidth;
        int r1 = (y1 - y) / tileHeight;
        int r2 = (y2 - 1 - y) / tileHeight;
        for (int row = r1; row <= r2; row++) {
            int ty = y + row * tileHeight;
            for (int col = c1; col <= c2; col++) {
                int tile = staticTileAt(col, row);
                if (tile != 0) {
                    g.drawRegion(image, tileSourceX(tile), tileSourceY(tile), tileWidth, tileHeight,
                            Sprite.TRANS_NONE, x + col * tileWidth, ty, Graphics.TOP | Graphics.LEFT);
                }
            }
        }
    }
}
