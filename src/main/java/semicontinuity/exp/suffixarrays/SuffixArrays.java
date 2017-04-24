package semicontinuity.exp.suffixarrays;

import java.util.function.Function;

import semicontinuity.exp.offheap.LongArray;

/**
 * Adopted from jsuffixarrays
 * https://github.com/carrotsearch/jsuffixarrays
 */
public class SuffixArrays {
    /**
     * Calculate longest prefix (LCP) array for an existing suffix array and input. Index
     * <code>i</code> of the returned array indicates the length of the common prefix
     * between suffix <code>i</code> and <code>i-1<code>. The 0-th
     * index has a constant value of <code>-1</code>.
     * <p>
     * The algorithm used to compute the LCP comes from
     * <tt>T. Kasai, G. Lee, H. Arimura, S. Arikawa, and K. Park. Linear-time longest-common-prefix
     * computation in suffix arrays and its applications. In Proc. 12th Symposium on Combinatorial
     * Pattern Matching (CPM ’01), pages 181–192. Springer-Verlag LNCS n. 2089, 2001.</tt>
     */
    public static LongArray computeLCP(
            LongArray input, long start, long length, LongArray sa, LongArray lcp,
            Function<Long, LongArray> arrayFactory)
    {
        try (LongArray rank = arrayFactory.apply(length)) {
            for (long i = 0; i < length; i++) {
                rank.set(sa.get(i), i);
            }
            int h = 0;
            for (long i = 0; i < length; i++) {
                long k = rank.get(i);
                if (k == 0) {
                    lcp.set(k, -1);
                } else {
                    final long j = sa.get(k - 1);
                    while (i + h < length && j + h < length && input.get(start + i + h) == input.get(start + j + h)) {
                        h++;
                    }
                    lcp.set(k, h);
                }
                if (h > 0) {
                    h--;
                }
            }
            return lcp;
        }
    }
}
