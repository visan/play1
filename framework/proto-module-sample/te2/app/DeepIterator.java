import java.util.Iterator;

/**
 * Created by adg on 27.01.2015.
 */
public class DeepIterator implements Iterator {
    private final int[][] data;
    private int col;
    private int row;

    public DeepIterator(final int[][] source) {
        this.data = source;
    }

    public static void main(String[] args) {
        int[][] source = new int[][]{
                {5, 2, 8},
                {1, 4},
                {43, 23}
        };

        DeepIterator deepIterator = new DeepIterator(source);
        while (deepIterator.hasNext()) {
            System.out.println(deepIterator.next());
        }
    }

    @Override
    public boolean hasNext() {
        if (col + 1 < data[row].length) return true;
        if (col + 1 >= data[row].length && row + 1 < data.length) return true;
        return false;
    }

    @Override
    public Integer next() {
        java.lang.Integer res = data[row][col];
        if (col + 1 < data[row].length) {
            col++;
        } else if (col + 1 >= data[row].length && row + 1 < data.length) {
            col = 0;
            row ++;
        }else {
            throw new IllegalStateException("out of collection");
        }

        return res;
    }
}
