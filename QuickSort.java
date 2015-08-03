import java.util.Vector;

public class QuickSort {
    private static long comparisons = 0;
    private static long exchanges   = 0;

   /***********************************************************************
    *  Quicksort code from Sedgewick 7.1, 7.2. (Ascending order)
    ***********************************************************************/
    public static void quicksort(double[] a) {
        shuffle(a);                        // to guard against worst-case
        quicksort(a, 0, a.length - 1);
    }

    public static void quicksort(double [] a, int [] ind){
	shuffle(a,ind);
	quicksort(a, 0, a.length - 1, ind);
    }

    public static void quicksort(double[] a, int left, int right) {
        if (right <= left) return;
        int i = partition(a, left, right);
        quicksort(a, left, i-1);
        quicksort(a, i+1, right);
    }


    public static void quicksort(double[] a, int left, int right, int [] ind) {
        if (right <= left) return;
        int i = partition(a, left, right, ind);
        quicksort(a, left, i-1, ind);
        quicksort(a, i+1, right, ind);
    }

    private static int partition(double[] a, int left, int right) {
        int i = left - 1;
        int j = right;
        while (true) {
            while (less(a[++i], a[right]))      // find item on left to swap
                ;                               // a[right] acts as sentinel
            while (less(a[right], a[--j]))      // find item on right to swap
                if (j == left) break;           // don't go out-of-bounds
            if (i >= j) break;                  // check if pointers cross
            exch(a, i, j);                      // swap two elements into place
        }
        exch(a, i, right);                      // swap with partition element
        return i;
    }


    private static int partition(double[] a, int left, int right, int [] ind) {
        int i = left - 1;
        int j = right;
        while (true) {
            while (less(a[++i], a[right]))      // find item on left to swap
                ;                               // a[right] acts as sentinel
            while (less(a[right], a[--j]))      // find item on right to swap
                if (j == left) break;           // don't go out-of-bounds
            if (i >= j) break;                  // check if pointers cross
            exch(a, i, j, ind);                      // swap two elements into place
        }
        exch(a, i, right, ind);                      // swap with partition element
        return i;
    }


    // is x < y ?
    private static boolean less(double x, double y) {
        comparisons++;
        return (x < y);
    }

    // is x < y ?
    private static boolean lessString(String x, String y) {
        comparisons++;
        return ((x.compareTo(y)) < 0);
    }

    private static void reverse(double[] a){
	double t;
	int i;
	for (i=0;i<(a.length / 2);i++){
	    t = a[i];
	    a[i] = a[a.length - i];
	    a[a.length - i] = t;
	}
    }

    // exchange a[i] and a[j]
    private static void exch(double[] a, int i, int j) {
        exchanges++;
        double swap = a[i];
        a[i] = a[j];
        a[j] = swap;
    }
    
    private static void exch(double[] a, int i, int j, int [] ind) {
        exchanges++;
        double swap = a[i];
	int t = ind[i];
        a[i] = a[j];
	ind[i] = ind[j];
        a[j] = swap;
	ind[j] = t;
    }

    // shuffle the array a
    private static void shuffle(double[] a) {
        int N = a.length;
        for (int i = 0; i < N; i++) {
            int r = i + (int) (Math.random() * (N-i));   // between i and N-1
            exch(a, i, r);
        }
    }


    private static void shuffle(double[] a, int [] ind) {
        int N = a.length;
        for (int i = 0; i < N; i++) {
            int r = i + (int) (Math.random() * (N-i));   // between i and N-1
            exch(a, i, r, ind);
        }
    }

}


