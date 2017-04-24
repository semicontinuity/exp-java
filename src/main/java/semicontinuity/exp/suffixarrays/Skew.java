package semicontinuity.exp.suffixarrays;

import semicontinuity.exp.offheap.LongArray;
import semicontinuity.exp.offheap.OffheapByteArrayAsFiveByteLongArrayLsb;

/**
 * Adopted from jsuffixarrays to support long datasets.
 * https://github.com/carrotsearch/jsuffixarrays
 * <p>
 * Straightforward reimplementation of the recursive algorithm given in: <tt>
 * J. Kärkkäinen and P. Sanders. Simple linear work suffix array construction.
 * In Proc. 13th International Conference on Automata, Languages and Programming,
 * Springer, 2003
 * </tt>
 * <p>
 * This implementation is basically a translation of the C++ version given by Juha
 * Kärkkäinen and Peter Sanders.
 * <p>
 * The implementation of this algorithm makes some assumptions about the input.
 */
public final class Skew {
    /**
     * Lexicographic order for pairs.
     */
    private static boolean leq(long a1, long a2, long b1, long b2) {
        return a1 < b1 || (a1 == b1 && a2 <= b2);
    }

    /**
     * Lexicographic order for triples.
     */
    private static boolean leq(long a1, long a2, long a3, long b1, long b2, long b3) {
        return a1 < b1 || (a1 == b1 && leq(a2, a3, b2, b3));
    }

    /**
     * Stably sort indexes from src[0..n-1] to dst[0..n-1] with values in 0..K from v. A
     * constant offset of <code>vi</code> is added to indexes from src.
     */
    private static void radixPass(
            LongArray src, LongArray dst, LongArray v, int vi,
            final long n, final int k, long start, LongArray cnt)
    {
        for (long i = 0; i < cnt.length(); i++) {
            cnt.set(i, 0);
        }

        // check counter array's size.
//        assert cnt.length >= k + 1;
//        Arrays.fill(cnt, 0, k + 1, 0L);

        // count occurrences
        for (long i = 0; i < n; i++) {
            long index = v.get(start + vi + src.get(i));
            cnt.set(index, cnt.get(index) + 1);
//            cnt[v[start + vi + src[i]]]++;
        }

        // exclusive prefix sums
        long sum = 0;
        for (long i = 0; i <= k; i++) {
            final long t = cnt.get(i);
            cnt.set(i, sum);
            sum += t;
        }

        // sort
        for (long i = 0; i < n; i++) {
            long index = v.get(start + vi + src.get(i));
            long counterValue = cnt.get(index);
            cnt.set(index, counterValue + 1);
            dst.set(counterValue, src.get(i));
//            dst[cnt[v[start + vi + src[i]]]++] = src[i];
        }
    }


    /**
     * Find the suffix array SA of s[0..n-1] in {1..K}^n. require s[n] = s[n+1] = s[n+2] =
     * 0, n >= 2.
     */
    static LongArray suffixArray(LongArray s, LongArray sa, long n, final int bigK, long start) {

//        cnt = ensureSize(cnt, bigK + 1);

        LongArray cnt1 = new OffheapByteArrayAsFiveByteLongArrayLsb(bigK + 1);
        for (long i = 0; i < cnt1.length(); i++) {
             cnt1.set(i, 0);
        }

        final long n0 = (n + 2) / 3;
        final long n1 = (n + 1) / 3;
        final long n2 = n / 3;
        final long n02 = n0 + n2;

//        final int[] s12 = new int[n02 + 3];
        LongArray s12 = new OffheapByteArrayAsFiveByteLongArrayLsb(n02 + 3);
        s12.set(n02, 0);
        s12.set(n02 + 1, 0);
        s12.set(n02 + 2, 0);
//        s12[n02] = s12[n02 + 1] = s12[n02 + 2] = 0;

        LongArray sa12 = new OffheapByteArrayAsFiveByteLongArrayLsb(n02 + 3);
//        final int[] SA12 = new int[n02 + 3];
//        SA12[n02] = SA12[n02 + 1] = SA12[n02 + 2] = 0;
        sa12.set(n02, 0);
        sa12.set(n02 + 1, 0);
        sa12.set(n02 + 2, 0);


        /*
         * generate positions of mod 1 and mod 2 suffixes the "+(n0-n1)" adds a dummy mod
         * 1 suffix if n%3 == 1
         */
        for (long i = 0, j = 0; i < n + (n0 - n1); i++) {
            if ((i % 3) != 0) {
                s12.set(j++, i);
//                s12[j++] = i;
            }
        }

        // lsb radix sort the mod 1 and mod 2 triples
        radixPass(s12, sa12, s, +2, n02, bigK, start, cnt1);
        radixPass(sa12, s12, s, +1, n02, bigK, start, cnt1);
        radixPass(s12, sa12, s, +0, n02, bigK, start, cnt1);
//        for (int i = 0; i < cnt.length; i++) {
//            System.out.println("[" + i + "]=" + cnt[i]);
//        }

        // find lexicographic names of triples
        int name = 0;
        long c0 = -1;
        long c1 = -1;
        long c2 = -1;
        for (long i = 0; i < n02; i++) {
            if (s.get(start + sa12.get(i)) != c0
                    || s.get(start + sa12.get(i) + 1) != c1
                    || s.get(start + sa12.get(i) + 2) != c2)
            {
//            if (s[start + sa12[i]] != c0 || s[start + sa12[i] + 1] != c1 || s[start + sa12[i] + 2] != c2) {
                name++;
                if (name == Integer.MAX_VALUE) {
                    throw new IllegalStateException("Problem");
                }
                c0 = s.get(start + sa12.get(i));
//                c0 = s[start + sa12[i]];
                c1 = s.get(start + sa12.get(i) + 1);
//                c1 = s[start + sa12[i] + 1];
                c2 = s.get(start + sa12.get(i) + 2);
//                c2 = s[start + sa12[i] + 2];
            }

            if ((sa12.get(i) % 3) == 1) {
//            if ((sa12[i] % 3) == 1) {
                // left half
                s12.set(sa12.get(i) / 3, name);
//                System.out.println("s12[" + (sa12.get(i) / 3) + "]:=" + name);
//                s12[sa12[i] / 3] = name;
            } else {
                // right half
                s12.set(sa12.get(i) / 3 + n0, name);
//                System.out.println("s12[" + (sa12.get(i) / 3 + n0) + "]:=" + name);
//                s12[sa12[i] / 3 + n0] = name;
            }
        }

        // recurse if names are not yet unique
        if (name < n02) {
            /*cnt = */suffixArray(s12, sa12, n02, name, start);
            // store unique names in s12 using the suffix array
            for (long i = 0; i < n02; i++) {
                s12.set(sa12.get(i), i + 1);
//                s12[sa12[i]] = i + 1;
            }
        } else {
            // generate the suffix array of s12 directly
            for (long i = 0; i < n02; i++) {
                sa12.set(s12.get(i) - 1, i);
//                sa12[s12[i] - 1] = i;
//                System.out.println("SA12[" + (s12.get(i) - 1) + "]:=" + i);
            }
        }

        LongArray s0 = new OffheapByteArrayAsFiveByteLongArrayLsb(n0);
//        final int[] s0 = new int[n0];

        // stably sort the mod 0 suffixes from SA12 by their first character
        for (long i = 0, j = 0; i < n02; i++) {
            if (sa12.get(i) < n0) {
//            if (sa12[i] < n0) {
                s0.set(j++, 3 * sa12.get(i));
//                s0[j++] = 3 * sa12[i];
            }
        }

        LongArray sa0 = new OffheapByteArrayAsFiveByteLongArrayLsb(n0);
//        final int[] SA0 = new int[n0];

        radixPass(s0, sa0, s, 0, n0, bigK, start, cnt1);
        s0.close();

        // =============
        // ACHTUNG
        // actual usage of 'sa' starts here (filled successively).
        // Good idea to allocate it right here.
        // =============
        // merge sorted SA0 suffixes and sorted SA12 suffixes
        for (long p = 0, t = n0 - n1, k = 0; k < n; k++) {
            // pos of current offset 12 suffix
            final long i = sa12.get(t) < n0 ? sa12.get(t) * 3 + 1 : (sa12.get(t) - n0) * 3 + 2;
//            final long i = (sa12[t] < n0 ? sa12[t] * 3 + 1 : (sa12[t] - n0) * 3 + 2);
            // pos of current offset 0 suffix
            final long j = sa0.get(p);
//            final long j = sa0[p];

            if (sa12.get(t) < n0
                ? leq(s.get(start + i), s12.get(sa12.get(t) + n0), s.get(start + j), s12.get(j / 3))
                : leq(s.get(start + i), s.get(start + i + 1), s12.get(sa12.get(t) - n0 + 1), s.get(start + j), s.get(start + j + 1), s12.get(j / 3 + n0))
                    )
/*
            if (sa12[t] < n0
                ? leq(s[start + i], s12[sa12[t] + n0], s[start + j], s12[j / 3])
                : leq(s[start + i], s[start + i + 1], s12[sa12[t] - n0 + 1], s[start + j], s[start + j + 1], s12[j / 3 + n0])
                    )
*/
            {
                // suffix from SA12 is smaller
                sa.set(k, i);
//                sa[k] = i;
                t++;
                if (t == n02) {
                    // done --- only SA0 suffixes left
                    for (k++; p < n0; p++, k++) {
                        sa.set(k, sa0.get(p));
//                        sa[k] = sa0[p];
                    }
                }
            } else {
                sa.set(k, j);
//                sa[k] = j;
                p++;
                if (p == n0) {
                    // done --- only SA12 suffixes left
                    for (k++; t < n02; t++, k++) {
                        sa.set(k, sa12.get(t) < n0 ? sa12.get(t) * 3 + 1 : (sa12.get(t) - n0) * 3 + 2);
//                        sa[k] = (sa12[t] < n0 ? sa12[t] * 3 + 1 : (sa12[t] - n0) * 3 + 2);
                    }
                }
            }
        }

        sa12.close();
        s12.close();
        sa0.close();
        cnt1.close();
        return null;
    }

    /**
     * <p>
     * Additional constraints enforced by Karkkainen-Sanders algorithm:
     * <ul>
     * <li>non-negative (>0) symbols in the input (because of radix sort)</li>,
     * <li><code>input.length</code> &gt;= <code>start + length + 3</code> (to simplify
     * border cases)</li>
     * <li>length >= 2</li>
     * </ul>
     * <p>
     */
    public static LongArray buildSuffixArray(LongArray input, long start, long length, LongArray sa) {
        final int alphabetSize = 255;
//        OffheapFiveByteLongArrayLsb sa = new OffheapFiveByteLongArrayLsb(length + 3);
        suffixArray(input, sa, length, alphabetSize, start /*, new long[alphabetSize + 2]*/);
        return sa;
    }
}
