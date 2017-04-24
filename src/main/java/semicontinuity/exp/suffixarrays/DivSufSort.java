package semicontinuity.exp.suffixarrays;

import semicontinuity.exp.offheap.LongArray;

/**
 * Adopted from jsuffixarrays to support long datasets.
 * https://github.com/carrotsearch/jsuffixarrays
 * <p>
 * Straightforward reimplementation of the divsufsort algorithm given in: <pre><code>
 * Yuta Mori, Short description of improved two-stage suffix sorting
 * algorithm, 2005.
 * http://homepage3.nifty.com/wpage/software/itssort.txt
 * </code></pre>
 * <p>
 * This implementation is basically a translation of the C version given by Yuta Mori:
 * <tt>libdivsufsort-2.0.0, http://code.google.com/p/libdivsufsort/</tt>
 * <p>
 * The implementation of this algorithm makes some assumptions about the input. See
 * {@link #buildSuffixArray(LongArray, long, long, LongArray)} for details.
 */
public final class DivSufSort {

    /* constants */
    private static final int DEFAULT_ALPHABET_SIZE = 256;
    private static final int SS_INSERTIONSORT_THRESHOLD = 8;
    private static final int SS_BLOCKSIZE = 1024;
    private static final int SS_MISORT_STACKSIZE = 16;
    private static final int SS_SMERGE_STACKSIZE = 32;
    private static final int TR_STACKSIZE = 64;
    private static final int TR_INSERTIONSORT_THRESHOLD = 8;

    private static final int[] sqqTable = {
            0, 16, 22, 27, 32, 35, 39, 42, 45, 48, 50, 53, 55, 57, 59, 61, 64, 65, 67, 69,
            71, 73, 75, 76, 78, 80, 81, 83, 84, 86, 87, 89, 90, 91, 93, 94, 96, 97, 98, 99,
            101, 102, 103, 104, 106, 107, 108, 109, 110, 112, 113, 114, 115, 116, 117, 118,
            119, 120, 121, 122, 123, 124, 125, 126, 128, 128, 129, 130, 131, 132, 133, 134,
            135, 136, 137, 138, 139, 140, 141, 142, 143, 144, 144, 145, 146, 147, 148, 149,
            150, 150, 151, 152, 153, 154, 155, 155, 156, 157, 158, 159, 160, 160, 161, 162,
            163, 163, 164, 165, 166, 167, 167, 168, 169, 170, 170, 171, 172, 173, 173, 174,
            175, 176, 176, 177, 178, 178, 179, 180, 181, 181, 182, 183, 183, 184, 185, 185,
            186, 187, 187, 188, 189, 189, 190, 191, 192, 192, 193, 193, 194, 195, 195, 196,
            197, 197, 198, 199, 199, 200, 201, 201, 202, 203, 203, 204, 204, 205, 206, 206,
            207, 208, 208, 209, 209, 210, 211, 211, 212, 212, 213, 214, 214, 215, 215, 216,
            217, 217, 218, 218, 219, 219, 220, 221, 221, 222, 222, 223, 224, 224, 225, 225,
            226, 226, 227, 227, 228, 229, 229, 230, 230, 231, 231, 232, 232, 233, 234, 234,
            235, 235, 236, 236, 237, 237, 238, 238, 239, 240, 240, 241, 241, 242, 242, 243,
            243, 244, 244, 245, 245, 246, 246, 247, 247, 248, 248, 249, 249, 250, 250, 251,
            251, 252, 252, 253, 253, 254, 254, 255
    };

    private static final int[] lgTable = {
            -1, 0, 1, 1, 2, 2, 2, 2, 3, 3, 3, 3, 3, 3, 3, 3, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4,
            4, 4, 4, 4, 4, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5,
            5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6,
            6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6,
            6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 7, 7, 7, 7, 7, 7, 7,
            7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7,
            7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7,
            7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7,
            7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7,
            7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7
    };

    /* fields */
    private final int alphabetSize;
    private final int bucketASize;
    private final int bucketBSize;
    private LongArray sa;
    private LongArray t;
    private long start;


    private static final class StackElement {
        final long a;
        final long b;
        final long c;
        final int e;
        int d;

        StackElement(long a, long b, long c, int d, int e) {
            this.a = a;
            this.b = b;
            this.c = c;
            this.d = d;
            this.e = e;
        }

        StackElement(long a, long b, long c, int d) {
            this(a, b, c, d, 0);
        }
    }


    private static final class TRBudget {
        long chance;
        long remain;
        long incval;
        long count;

        private TRBudget(long chance, long incval) {
            this.chance = chance;
            this.remain = incval;
            this.incval = incval;
        }

        private long check(long size) {
            if (size <= this.remain) {
                this.remain -= size;
                return 1;
            }
            if (this.chance == 0) {
                this.count += size;
                return 0;
            }
            this.remain += this.incval - size;
            this.chance -= 1;
            return 1;
        }
    }

    private static final class TRPartitionResult {
        final long a;
        final long b;

        TRPartitionResult(long a, long b) {
            this.a = a;
            this.b = b;
        }
    }

    public DivSufSort() {
        alphabetSize = DEFAULT_ALPHABET_SIZE;
        bucketASize = alphabetSize;
        bucketBSize = alphabetSize * alphabetSize;
    }

    public DivSufSort(int alphabetSize) {
        this.alphabetSize = alphabetSize;
        bucketASize = this.alphabetSize;
        bucketBSize = this.alphabetSize * this.alphabetSize;
    }


    /**
     * {@inheritDoc}
     * <p>
     * Additional constraints enforced by DivSufSort algorithm:
     * <ul>
     * <li>non-negative (&ge;0) symbols in the input</li>
     * <li>symbols limited by alphabet size passed in the constructor.</li>
     * <li>length >= 2</li>
     * </ul>
     * <p>
     */
    public LongArray buildSuffixArray(LongArray input, long start, long length, LongArray sa) {
//        assertAlways(input != null, "input must not be null");
//        assertAlways(length >= 2, "input length must be >= 2");
//        MinMax mm = Tools.minmax(input, start, length);
//        assertAlways(mm.min >= 0, "input must not be negative");
//        assertAlways(mm.max < alphabetSize, "max alphabet size is " + alphabetSize);


        this.sa = sa;
        this.t = input;
        long[] bucketA = new long[bucketASize];
        long[] bucketB = new long[bucketBSize];
        this.start = start;
        /* Suffixsort. */
        long m = sortTypeBstar(bucketA, bucketB, length);
        constructSuffixArray(bucketA, bucketB, length, m);
        return sa;
    }

    /**
     * Constructs the suffix array by using the sorted order of type B* suffixes.
     */
    private void constructSuffixArray(long[] bucketA, long[] bucketB, long n, long m) {
        long i; // ptr
        long j;
        long k;
        long s;
        int c0;
        int c1;
        int c2;
        // (_c1)])
        if (0 < m) {
            /*
             * Construct the sorted order of type B suffixes by using the sorted order of
             * type B suffixes.
             */
            for (c1 = alphabetSize - 2; 0 <= c1; --c1) {
                /* Scan the suffix array from right to left. */
                for (i = bucketB[c1 * alphabetSize + (c1 + 1)], j = bucketA[c1 + 1] - 1, k = 0, c2 = -1; i <= j; --j) {
                    if (0 < (s = sa.get(j))) {
                        // Tools.assertAlways(t[s] == c1, "");
                        // Tools.assertAlways(((s + 1) < n) && (t[s] <= t[s +
                        // 1]),
                        // "");
                        // Tools.assertAlways(t[s - 1] <= t[s], "");
                        sa.set(j, ~s);
                        c0 = (int) t.get(start + --s);
                        if ((0 < s) && (t.get(start + s - 1) > c0)) {
                            s = ~s;
                        }
                        if (c0 != c2) {
                            if (0 <= c2) {
                                bucketB[c1 * alphabetSize + c2] = k;
                            }
                            k = bucketB[c1 * alphabetSize + (c2 = c0)];
                        }
                        // Tools.assertAlways(k < j, "");
                        sa.set(k--, s);
                    } else {
                        // Tools.assertAlways(((s == 0) && (t[s] == c1))
                        // || (s < 0), "");
                        sa.set(j, ~s);
                    }
                }
            }
        }

        /*
         * Construct the suffix array by using the sorted order of type B suffixes.
         */
        k = bucketA[c2 = (int) t.get(start + n - 1)];
        sa.set(k++, ((int) t.get(start + n - 2) < c2) ? ~(n - 1) : (n - 1));
        /* Scan the suffix array from left to right. */
        for (i = 0, j = n; i < j; ++i) {
            if (0 < (s = sa.get(i))) {
                // Tools.assertAlways(t[s - 1] >= t[s], "");
                c0 = (int) t.get(start + --s);
                if ((s == 0) || (t.get(start + s - 1) < c0)) {
                    s = ~s;
                }
                if (c0 != c2) {
                    bucketA[c2] = k;
                    k = bucketA[c2 = c0];
                }
                // Tools.assertAlways(i < k, "");
                sa.set(k++, s);
            }
            else {
                // Tools.assertAlways(s < 0, "");
                sa.set(i, ~s);
            }
        }
    }

    private long sortTypeBstar(long[] bucketA, long[] bucketB, long n) {
        long paB;
        long isaB;
        long buf;

        long i;
        long j;
        long k;
        long t;
        long m;
        long bufsize;

        int c0;
        int c1;

        /*
         * Count the number of occurrences of the first one or two characters of each type
         * A, B and B suffix. Moreover, store the beginning position of all type B
         * suffixes into the array sa.
         */
        for (i = n - 1, m = n, c0 = (int) this.t.get(start + n - 1); 0 <= i;) {
            /* type A suffix. */
            do {
                ++bucketA[c1 = c0];
            }
            while ((0 <= --i) && ((c0 = (int) this.t.get(start + i)) >= c1));
            if (0 <= i) {
                /* type B suffix. */
                ++bucketB[c0 * alphabetSize + c1];
                --m;
                sa.set(m, i);
                /* type B suffix. */
                for (--i, c1 = c0; (0 <= i) && ((c0 = (int) this.t.get(start + i)) <= c1); --i, c1 = c0) {
                    ++bucketB[c1 * alphabetSize + c0];
                }
            }
        }
        m = n - m;

        // note:
        // A type B* suffix is lexicographically smaller than a type B suffix
        // that
        // begins with the same first two characters.

        // Calculate the index of start/end point of each bucket.
        for (c0 = 0, i = 0, j = 0; c0 < alphabetSize; ++c0) {
            t = i + bucketA[c0];
            bucketA[c0] = i + j; /* start point */
            i = t + bucketB[c0 * alphabetSize + c0];
            for (c1 = c0 + 1; c1 < alphabetSize; ++c1) {
                j += bucketB[c0 * alphabetSize + c1];
                bucketB[c0 * alphabetSize + c1] = j; // end point
                i += bucketB[c1 * alphabetSize + c0];
            }
        }

        if (0 < m) {
            // Sort the type B* suffixes by their first two characters.
            paB = n - m;    // sa
            isaB = m;   // sa
            for (i = m - 2; 0 <= i; --i) {
                t = sa.get(paB + i);
                c0 = (int) this.t.get(start + t);
                c1 = (int) this.t.get(start + t + 1);
                sa.set(--bucketB[c0 * alphabetSize + c1], i);
            }
            t = sa.get(paB + m - 1);
            c0 = (int) this.t.get(start + t);
            c1 = (int) this.t.get(start + t + 1);
            sa.set(--bucketB[c0 * alphabetSize + c1], m - 1);

            // Sort the type B* substrings using sssort.

            buf = m; // sa
            bufsize = n - (2 * m);

            for (c0 = alphabetSize - 2, j = m; 0 < j; --c0) {
                for (c1 = alphabetSize - 1; c0 < c1; j = i, --c1) {
                    i = bucketB[c0 * alphabetSize + c1];
                    if (1 < (j - i)) {
                        ssSort(paB, i, j, buf, bufsize, 2, n, sa.get(i) == (m - 1));
                    }
                }
            }

            // Compute ranks of type B* substrings.
            for (i = m - 1; 0 <= i; --i) {
                if (0 <= sa.get(i)) {
                    j = i;
                    do {
                        sa.set(isaB + sa.get(i), i);
                    }
                    while ((0 <= --i) && (0 <= sa.get(i)));
                    sa.set(i + 1, i - j);
                    if (i <= 0) {
                        break;
                    }
                }
                j = i;
                do {
                    long saI = ~sa.get(i);
                    sa.set(i, saI);
                    sa.set(isaB + saI, j);
                }
                while (sa.get(--i) < 0);
                sa.set(isaB + sa.get(i), j);
            }
            // Construct the inverse suffix array of type B* suffixes using
            // trsort.
            trSort(isaB, m, 1);
            // Set the sorted order of type B* suffixes.
            for (i = n - 1, j = m, c0 = (int) this.t.get(start + n - 1); 0 <= i; ) {
                for (--i, c1 = c0; (0 <= i) && ((c0 = (int) this.t.get(start + i)) >= c1); --i, c1 = c0) {
                }
                if (0 <= i) {
                    t = i;
                    for (--i, c1 = c0; (0 <= i) && ((c0 = (int) this.t.get(start + i)) <= c1); --i, c1 = c0) {
                    }
                    sa.set(sa.get(isaB + --j), ((t == 0) || (1 < (t - i))) ? t : ~t);
                }
            }

            // Calculate the index of start/end point of each bucket.
            bucketB[(alphabetSize - 1) * alphabetSize + (alphabetSize - 1)] = n; // end
            // point
            for (c0 = alphabetSize - 2, k = m - 1; 0 <= c0; --c0) {
                i = bucketA[c0 + 1] - 1;
                for (c1 = alphabetSize - 1; c0 < c1; --c1) {
                    t = i - bucketB[c1 * alphabetSize + c0];
                    bucketB[c1 * alphabetSize + c0] = i; // end point

                    // Move all type B* suffixes to the correct position.
                    for (i = t, j = bucketB[c0 * alphabetSize + c1]; j <= k; --i, --k) {
                        sa.set(i, sa.get(k));
                    }
                }
                bucketB[c0 * alphabetSize + (c0 + 1)] = i
                        - bucketB[c0 * alphabetSize + c0] + 1; //
                bucketB[c0 * alphabetSize + c0] = i; // end point
            }
        }

        return m;
    }

    private void ssSort(
            long pa, long first, long last, long buf, long bufsize, long depth, long n, boolean lastsuffix)
    {
        long a; // sa pointer
        long b;
        long middle;
        long curbuf;
        long j;
        long k;
        long curbufsize;
        long limit;

        long i;

        if (lastsuffix) {
            ++first;
        }

        if ((bufsize < SS_BLOCKSIZE) && (bufsize < (last - first))
                && (bufsize < (limit = ssIsqrt(last - first)))) {
            if (SS_BLOCKSIZE < limit) {
                limit = SS_BLOCKSIZE;
            }
            buf = middle = last - limit;
            bufsize = limit;
        } else {
            middle = last;
            limit = 0;
        }
        for (a = first, i = 0; SS_BLOCKSIZE < (middle - a); a += SS_BLOCKSIZE, ++i) {
            ssMintroSort(pa, a, a + SS_BLOCKSIZE, depth);
            curbufsize = last - (a + SS_BLOCKSIZE);
            curbuf = a + SS_BLOCKSIZE;
            if (curbufsize <= bufsize) {
                curbufsize = bufsize;
                curbuf = buf;
            }
            for (b = a, k = SS_BLOCKSIZE, j = i; (j & 1) != 0; b -= k, k <<= 1, j >>= 1) {
                ssSwapMerge(pa, b - k, b, b + k, curbuf, curbufsize, depth);
            }
        }
        ssMintroSort(pa, a, middle, depth);
        for (k = SS_BLOCKSIZE; i != 0; k <<= 1, i >>= 1) {
            if ((i & 1) != 0) {
                ssSwapMerge(pa, a - k, a, middle, buf, bufsize, depth);
                a -= k;
            }
        }
        if (limit != 0) {
            ssMintroSort(pa, middle, last, depth);
            ssInplaceMerge(pa, first, middle, last, depth);
        }

        if (lastsuffix) {
            long p1 = sa.get(pa + sa.get(first - 1));
            long p11 = n - 2;
            for (a = first, i = sa.get(first - 1); (a < last)
                    && ((sa.get(a) < 0) || (0 < ssCompare(p1, p11, pa + sa.get(a), depth))); ++a) {
                sa.set(a - 1, sa.get(a));
            }
            sa.set(a - 1, i);
        }
    }

    /**
     * special version of ss_compare for handling
     * <code>ss_compare(t, &(PAi[0]), PA + *a, depth)</code> situation.
     */
    private long ssCompare(long pa, long pb, long p2, long depth) {
        // pointers to t
        long u1;
        long u2;
        long u1n;
        long u2n;

        for (u1 = depth + pa, u2 = depth + sa.get(p2), u1n = pb + 2, u2n = sa.get(p2 + 1) + 2; (u1 < u1n)
                && (u2 < u2n) && (t.get(start + u1) == t.get(start + u2)); ++u1, ++u2)
        {
        }

        return u1 < u1n ? (u2 < u2n ? t.get(start + u1) - t.get(start + u2) : 1) : (u2 < u2n ? -1 : 0);
    }

    private long ssCompare(long p1, long p2, long depth) {
        // pointers to t
        long u1;
        long u2;
        long u1n;
        long u2n;

        for (u1 = depth + sa.get(p1),
                     u2 = depth + sa.get(p2),
                     u1n = sa.get(p1 + 1) + 2,
                     u2n = sa.get(p2 + 1) + 2;
             (u1 < u1n) && (u2 < u2n) && (t.get(start + u1) == t.get(start + u2)); ++u1, ++u2)
        {
        }

        return u1 < u1n ? (u2 < u2n ? t.get(start + u1) - t.get(start + u2) : 1) : (u2 < u2n ? -1 : 0);
    }

    private void ssInplaceMerge(long pa, long first, long middle, long last, long depth) {
        // PA, middle, first, last are pointers to sa

        // pointer to sa
        long p;
        long a;
        long b;
        long len;
        long half;
        long q;
        long r;
        long x;

        for (; ; ) {
            if (sa.get(last - 1) < 0) {
                x = 1;
                p = pa + ~sa.get(last - 1);
            } else {
                x = 0;
                p = pa + sa.get(last - 1);
            }
            for (a = first, len = middle - first, half = len >> 1, r = -1; 0 < len; len = half, half >>= 1) {
                b = a + half;
                q = ssCompare(pa + ((0 <= sa.get(b)) ? sa.get(b) : ~sa.get(b)), p, depth);
                if (q < 0) {
                    a = b + 1;
                    half -= (len & 1) ^ 1;
                } else {
                    r = q;
                }
            }
            if (a < middle) {
                if (r == 0) {
                    sa.set(a, ~sa.get(a));
                }
                ssRotate(a, middle, last);
                last -= middle - a;
                middle = a;
                if (first == middle) {
                    break;
                }
            }
            --last;
            if (x != 0) {
                while (sa.get(--last) < 0) {
                    // nop
                }
            }
            if (middle == last) {
                break;
            }
        }
    }

    private void ssRotate(long first, long middle, long last) {
        // first, middle, last are pointers in sa
        // pointers in sa
        long a;
        long b;
        long t;
        long l;
        long r;
        l = middle - first;
        r = last - middle;
        for (; (0 < l) && (0 < r); ) {
            if (l == r) {
                ssBlockSwap(first, middle, l);
                break;
            }
            if (l < r) {
                a = last - 1;
                b = middle - 1;
                t = sa.get(a);
                do {
                    sa.set(a--, sa.get(b));
                    sa.set(b--, sa.get(a));
                    if (b < first) {
                        sa.set(a, t);
                        last = a;
                        if ((r -= l + 1) <= l) {
                            break;
                        }
                        a -= 1;
                        b = middle - 1;
                        t = sa.get(a);
                    }
                }
                while (true);
            } else {
                a = first;
                b = middle;
                t = sa.get(a);
                do {
                    sa.set(a++, sa.get(b));
                    sa.set(b++, sa.get(a));
                    if (last <= b) {
                        sa.set(a, t);
                        first = a + 1;
                        if ((l -= r + 1) <= r) {
                            break;
                        }
                        a += 1;
                        b = middle;
                        t = sa.get(a);
                    }
                }
                while (true);
            }
        }
    }

    private void ssBlockSwap(long a, long b, long n) {
        // a, b -- pointer to sa
        long t;
        for (; 0 < n; --n, ++a, ++b) {
            t = (int) sa.get(a);
            sa.set(a, sa.get(b));
            sa.set(b, t);
        }
    }

    private static long getIDX(long a) {
        return (0 <= a) ? a : ~a;
    }

    private static long min(long a, long b) {
        return a < b ? a : b;
    }

    /**
     * D&C based merge.
     */
    private void ssSwapMerge(long pa, long first, long middle, long last, long buf, long bufsize, long depth) {
        // Pa, first, middle, last and buf - pointers in sa array

        StackElement[] stack = new StackElement[SS_SMERGE_STACKSIZE];
        long l; // pointers in sa
        long r;
        long lm;
        long rm;
        long m;
        long len;
        long half;
        int ssize;
        long check;
        long next;

        for (check = 0, ssize = 0; ; ) {
            if ((last - middle) <= bufsize) {
                if ((first < middle) && (middle < last)) {
                    ssMergeBackward(pa, first, middle, last, buf, depth);
                }
                if (((check & 1) != 0)
                        || (
                        ((check & 2) != 0) && (
                                ssCompare(pa + getIDX(sa.get(first - 1)), pa + sa.get(first), depth) == 0
                        )
                )) {
                    sa.set(first, ~sa.get(first));
                }
                if (((check & 4) != 0)
                        && ((ssCompare(pa + getIDX(sa.get(last - 1)), pa + sa.get(last), depth) == 0))) {
                    sa.set(last, ~sa.get(last));
                }

                if (ssize > 0) {
                    StackElement se = stack[--ssize];
                    first = se.a;
                    middle = se.b;
                    last = se.c;
                    check = se.d;
                } else {
                    return;
                }
                continue;
            }

            if ((middle - first) <= bufsize) {
                if (first < middle) {
                    ssMergeForward(pa, first, middle, last, buf, depth);
                }
                if (((check & 1) != 0)
                        || (
                        ((check & 2) != 0) && (
                                ssCompare(pa + getIDX(sa.get(first - 1)), pa + sa.get(first), depth) == 0
                        )
                )) {
                    sa.set(first, ~sa.get(first));
                }
                if (((check & 4) != 0)
                        && ((ssCompare(pa + getIDX(sa.get(last - 1)), pa + sa.get(last), depth) == 0))) {
                    sa.set(last, ~sa.get(last));
                }

                if (ssize > 0) {
                    StackElement se = stack[--ssize];
                    first = se.a;
                    middle = se.b;
                    last = se.c;
                    check = se.d;
                } else {
                    return;
                }

                continue;
            }

            for (m = 0, len = min(middle - first, last - middle), half = len >> 1; 0 < len; len = half, half >>= 1) {
                if (ssCompare(
                        pa + getIDX(sa.get(middle + m + half)),
                        pa + getIDX(sa.get(middle - m - half - 1)),
                        depth) < 0)
                {
                    m += half + 1;
                    half -= (len & 1) ^ 1;
                }
            }

            if (0 < m) {
                lm = middle - m;
                rm = middle + m;
                ssBlockSwap(lm, middle, m);
                l = r = middle;
                next = 0;
                if (rm < last) {
                    if (sa.get(rm) < 0) {
                        sa.set(rm, ~sa.get(rm));
                        if (first < lm) {
                            for (; sa.get(--l) < 0; ) {
                            }
                            next |= 4;
                        }
                        next |= 1;
                    } else if (first < lm) {
                        for (; sa.get(r) < 0; ++r) {
                        }
                        next |= 2;
                    }
                }

                if ((l - first) <= (last - r)) {
                    stack[ssize++] = new StackElement(r, rm, last, (int) ((next & 3) | (check & 4)));

                    middle = lm;
                    last = l;
                    check = (check & 3) | (next & 4);
                } else {
                    if (((next & 2) != 0) && (r == middle)) {
                        next ^= 6;
                    }
                    stack[ssize++] = new StackElement(first, lm, l, (int) ((check & 3) | (next & 4)));

                    first = r;
                    middle = rm;
                    check = (next & 3) | (check & 4);
                }
            } else {
                if (ssCompare(pa + getIDX(sa.get(middle - 1)), pa + sa.get(middle), depth) == 0) {
                    sa.set(middle, ~sa.get(middle));
                }

                if (((check & 1) != 0)
                        || (
                        ((check & 2) != 0) && (
                                ssCompare(pa + getIDX(sa.get(first - 1)), pa + sa.get(first), depth) == 0
                        )
                ))
                {
                    sa.set(first, ~sa.get(first));
                }
                if (((check & 4) != 0)
                        && ((ssCompare(pa + getIDX(sa.get(last - 1)), pa + sa.get(last), depth) == 0))) {
                    sa.set(last, ~sa.get(last));
                }

                if (ssize > 0) {
                    StackElement se = stack[--ssize];
                    first = se.a;
                    middle = se.b;
                    last = se.c;
                    check = se.d;
                } else {
                    return;
                }
            }
        }
    }

    
    /**
     * Merge-forward with internal buffer.
     */
    private void ssMergeForward(long pa, long first, long middle, long last, long buf, long depth) {
        // PA, first, middle, last, buf are pointers to sa
        // pointers to sa
        long a;
        long b;
        long c;
        long bufend;
        long t;
        long r;

        bufend = buf + (middle - first) - 1;
        ssBlockSwap(buf, first, middle - first);

        for (t = sa.get(a = first), b = buf, c = middle; ; ) {
            r = ssCompare(pa + sa.get(b), pa + sa.get(c), depth);
            if (r < 0) {
                do {
                    sa.set(a++, sa.get(b));
                    if (bufend <= b) {
                        sa.set(bufend, t);
                        return;
                    }
                    sa.set(b++, sa.get(a));
                }
                while (sa.get(b) < 0);
            } else if (r > 0) {
                do {
                    sa.set(a++, sa.get(c));
                    sa.set(c++, sa.get(a));
                    if (last <= c) {
                        while (b < bufend) {
                            sa.set(a++, sa.get(b));
                            sa.set(b++, sa.get(a));
                        }
                        sa.set(a, sa.get(b));
                        sa.set(b, t);
                        return;
                    }
                }
                while (sa.get(c) < 0);
            } else {
                sa.set(c, ~sa.get(c));
                do {
                    sa.set(a++, sa.get(b));
                    if (bufend <= b) {
                        sa.set(bufend, t);
                        return;
                    }
                    sa.set(b++, sa.get(a));
                }
                while (sa.get(b) < 0);

                do {
                    sa.set(a++, sa.get(c));
                    sa.set(c++, sa.get(a));
                    if (last <= c) {
                        while (b < bufend) {
                            sa.set(a++, sa.get(b));
                            sa.set(b++, sa.get(a));
                        }
                        sa.set(a, sa.get(b));
                        sa.set(b, t);
                        return;
                    }
                }
                while (sa.get(c) < 0);
            }
        }

    }

    /**
     * Merge-backward with internal buffer.
     */
    private void ssMergeBackward(long pa, long first, long middle, long last, long buf, long depth) {
        // PA, first, middle, last, buf are pointers in sa
        // pointers in sa
        long p1;
        long p2;
        // pointers in sa
        long a;
        long b;
        long c;
        long bufend;

        long t;
        long r;
        long x;

        bufend = buf + (last - middle) - 1;
        ssBlockSwap(buf, middle, last - middle);

        x = 0;
        if (sa.get(bufend) < 0) {
            p1 = pa + ~sa.get(bufend);
            x |= 1;
        } else {
            p1 = pa + sa.get(bufend);
        }
        if (sa.get(middle - 1) < 0) {
            p2 = pa + ~sa.get(middle - 1);
            x |= 2;
        } else {
            p2 = pa + sa.get(middle - 1);
        }
        for (t = sa.get(a = last - 1), b = bufend, c = middle - 1; ; ) {
            r = ssCompare(p1, p2, depth);
            if (0 < r) {
                if ((x & 1) != 0) {
                    do {
                        sa.set(a--, sa.get(b));
                        sa.set(b--, sa.get(a));
                    }
                    while (sa.get(b) < 0);
                    x ^= 1;
                }
                sa.set(a--, sa.get(b));
                if (b <= buf) {
                    sa.set(buf, t);
                    break;
                }
                sa.set(b--, sa.get(a));
                if (sa.get(b) < 0) {
                    p1 = pa + ~sa.get(b);
                    x |= 1;
                } else {
                    p1 = pa + sa.get(b);
                }
            } else if (r < 0) {
                if ((x & 2) != 0) {
                    do {
                        sa.set(a--, sa.get(c));
                        sa.set(c--, sa.get(a));
                    }
                    while (sa.get(c) < 0);
                    x ^= 2;
                }
                sa.set(a--, sa.get(c));
                sa.set(c--, sa.get(a));
                if (c < first) {
                    while (buf < b) {
                        sa.set(a--, sa.get(b));
                        sa.set(b--, sa.get(a));
                    }
                    sa.set(a, sa.get(b));
                    sa.set(b, t);
                    break;
                }
                if (sa.get(c) < 0) {
                    p2 = pa + ~sa.get(c);
                    x |= 2;
                } else {
                    p2 = pa + sa.get(c);
                }
            } else {
                if ((x & 1) != 0) {
                    do {
                        sa.set(a--, sa.get(b));
                        sa.set(b--, sa.get(a));
                    }
                    while (sa.get(b) < 0);
                    x ^= 1;
                }
                sa.set(a--, ~sa.get(b));
                if (b <= buf) {
                    sa.set(buf, t);
                    break;
                }
                sa.set(b--, sa.get(a));
                if ((x & 2) != 0) {
                    do {
                        sa.set(a--, sa.get(c));
                        sa.set(c--, sa.get(a));
                    }
                    while (sa.get(c) < 0);
                    x ^= 2;
                }
                sa.set(a--, sa.get(c));
                sa.set(c--, sa.get(a));
                if (c < first) {
                    while (buf < b) {
                        sa.set(a--, sa.get(b));
                        sa.set(b--, sa.get(a));
                    }
                    sa.set(a, sa.get(b));
                    sa.set(b, t);
                    break;
                }
                if (sa.get(b) < 0) {
                    p1 = pa + ~sa.get(b);
                    x |= 1;
                } else {
                    p1 = pa + sa.get(b);
                }
                if (sa.get(c) < 0) {
                    p2 = pa + ~sa.get(c);
                    x |= 2;
                } else {
                    p2 = pa + sa.get(c);
                }
            }
        }
    }

    /**
     * Insertionsort for small size groups
     */
    private void ssInsertionSort(long pa, long first, long last, long depth) {
        // PA, first, last are pointers in sa
        // pointers in sa
        long i;
        long j;
        long t;
        long r;

        for (i = last - 2; first <= i; --i) {
            for (t = sa.get(i), j = i + 1; 0 < (r = ssCompare(pa + t, pa + sa.get(j), depth)); ) {
                do {
                    sa.set(j - 1, sa.get(j));
                }
                while ((++j < last) && (sa.get(j) < 0));
                if (last <= j) {
                    break;
                }
            }
            if (r == 0) {
                sa.set(j, ~sa.get(j));
            }
            sa.set(j - 1, t);
        }

    }

    private static long ssIsqrt(long x) {
        long y;


        if (x >= (SS_BLOCKSIZE * SS_BLOCKSIZE)) {
            return SS_BLOCKSIZE;
        }
        int e = e(x);

        if (e >= 16) {
            y = sqqTable[(int) (x >> ((e - 6) - (e & 1)))] << ((e >> 1) - 7);
            if (e >= 24) {
                y = (y + 1 + x / y) >> 1;
            }
            y = (y + 1 + x / y) >> 1;
        } else if (e >= 8) {
            y = (sqqTable[(int) (x >> ((e - 6) - (e & 1)))] >> (7 - (e >> 1))) + 1;
        } else {
            return sqqTable[(int) x] >> 4;
        }

        return (x < (y * y)) ? y - 1 : y;
    }


    private static int e(long x) {
        if ((x & 0xffff0000) != 0) {
            return ((x & 0xff000000) != 0) ? 24 + lgTable[(int) ((x >> 24) & 0xff)]
                                           : 16 + lgTable[(int) ((x >> 16) & 0xff)];
        } else {
            return ((x & 0x0000ff00) != 0) ? 8 + lgTable[(int) ((x >> 8) & 0xff)]
                                           : 0 + lgTable[(int) ((x >> 0) & 0xff)];
        }
    }

    /**
     * Multikey introsort for medium size groups.
     * last - first range?
     */
    private void ssMintroSort(long pa, long first, long last, long depth) {
        final int STACK_SIZE = SS_MISORT_STACKSIZE;
        StackElement[] stack = new StackElement[STACK_SIZE];
        long tD; // t ptr
        long a; // sa ptr
        long b;
        long c;
        long d;
        long e;
        long f;
        long s;
        long t;
        int ssize;
        int limit;
        long v;
        long x = 0;
        for (ssize = 0, limit = ssIlg(last - first); ; ) {

            if ((last - first) <= SS_INSERTIONSORT_THRESHOLD) {
                if (1 < (last - first)) {
                    ssInsertionSort(pa, first, last, depth);
                }
                if (ssize > 0) {
                    StackElement se = stack[--ssize];
                    first = se.a;
                    last = se.b;
                    depth = se.c;
                    limit = se.d;
                } else {
                    return;
                }

                continue;
            }

            tD = depth;
            if (limit-- == 0) {
                ssHeapSort(tD, pa, first, last - first);
            }
            if (limit < 0) {
                for (a = first + 1, v = this.t.get(start + tD + sa.get(pa + sa.get(first))); a < last; ++a) {
                    if ((x = this.t.get(start + tD + sa.get(pa + sa.get(a)))) != v) {
                        if (1 < (a - first)) {
                            break;
                        }
                        v = x;
                        first = a;
                    }
                }

                if (this.t.get(start + tD + sa.get(pa + sa.get(first)) - 1) < v) {
                    first = ssPartition(pa, first, a, depth);
                }
                if ((a - first) <= (last - a)) {
                    if (1 < (a - first)) {
                        stack[ssize++] = new StackElement(a, last, depth, -1);
                        last = a;
                        depth += 1;
                        limit = ssIlg(a - first);
                    } else {
                        first = a;
                        limit = -1;
                    }
                } else {
                    if (1 < (last - a)) {
                        stack[ssize++] = new StackElement(first, a, depth + 1, ssIlg(a
                                - first));
                        first = a;
                        limit = -1;
                    } else {
                        last = a;
                        depth += 1;
                        limit = ssIlg(a - first);
                    }
                }
                continue;
            }

            // choose pivot
            a = ssPivot(tD, pa, first, last);
            v = this.t.get(start + tD + sa.get(pa + sa.get(a)));
            swapInSA(first, a);

            // partition
            for (b = first; (++b < last) && ((x = this.t.get(start + tD + sa.get(pa + sa.get(b)))) == v); ) {
            }
            if (((a = b) < last) && (x < v)) {
                for (; (++b < last) && ((x = this.t.get(start + tD + sa.get(pa + sa.get(b)))) <= v); ) {
                    if (x == v) {
                        swapInSA(b, a);
                        ++a;
                    }
                }
            }

            for (c = last; (b < --c) && ((x = this.t.get(start + tD + sa.get(pa + sa.get(c)))) == v); ) {
            }
            if ((b < (d = c)) && (x > v)) {
                for (; (b < --c) && ((x = this.t.get(start + tD + sa.get(pa + sa.get(c)))) >= v); ) {
                    if (x == v) {
                        swapInSA(c, d);
                        --d;
                    }
                }
            }

            for (; b < c; ) {
                swapInSA(b, c);
                for (; (++b < c) && ((x = this.t.get(start + tD + sa.get(pa + sa.get(b)))) <= v); ) {
                    if (x == v) {
                        swapInSA(b, a);
                        ++a;
                    }
                }
                for (; (b < --c) && ((x = this.t.get(start + tD + sa.get(pa + sa.get(c)))) >= v); ) {
                    if (x == v) {
                        swapInSA(c, d);
                        --d;
                    }
                }
            }

            if (a <= d) {
                c = b - 1;

                if ((s = a - first) > (t = b - a)) {
                    s = t;
                }
                for (e = first, f = b - s; 0 < s; --s, ++e, ++f) {
                    swapInSA(e, f);
                }
                if ((s = d - c) > (t = last - d - 1)) {
                    s = t;
                }
                for (e = b, f = last - s; 0 < s; --s, ++e, ++f) {
                    swapInSA(e, f);
                }

                a = first + (b - a);
                c = last - (d - c);
                b = (v <= this.t.get(start + tD + sa.get(pa + sa.get(a)) - 1)) ? a : ssPartition(pa, a, c,
                        depth
                );

                if ((a - first) <= (last - c)) {
                    if ((last - c) <= (c - b)) {
                        stack[ssize++] = new StackElement(b, c, depth + 1, ssIlg(c - b));
                        stack[ssize++] = new StackElement(c, last, depth, limit);
                        last = a;
                    } else if ((a - first) <= (c - b)) {
                        stack[ssize++] = new StackElement(c, last, depth, limit);
                        stack[ssize++] = new StackElement(b, c, depth + 1, ssIlg(c - b));
                        last = a;
                    } else {
                        stack[ssize++] = new StackElement(c, last, depth, limit);
                        stack[ssize++] = new StackElement(first, a, depth, limit);
                        first = b;
                        last = c;
                        depth += 1;
                        limit = ssIlg(c - b);
                    }
                } else {
                    if ((a - first) <= (c - b)) {
                        stack[ssize++] = new StackElement(b, c, depth + 1, ssIlg(c - b));
                        stack[ssize++] = new StackElement(first, a, depth, limit);
                        first = c;
                    } else if ((last - c) <= (c - b)) {
                        stack[ssize++] = new StackElement(first, a, depth, limit);
                        stack[ssize++] = new StackElement(b, c, depth + 1, ssIlg(c - b));
                        first = c;
                    } else {
                        stack[ssize++] = new StackElement(first, a, depth, limit);
                        stack[ssize++] = new StackElement(c, last, depth, limit);
                        first = b;
                        last = c;
                        depth += 1;
                        limit = ssIlg(c - b);
                    }
                }

            } else {
                limit += 1;
                if (this.t.get(start + tD + sa.get(pa + sa.get(first)) - 1) < v) {
                    first = ssPartition(pa, first, last, depth);
                    limit = ssIlg(last - first);
                }
                depth += 1;
            }

        }

    }

    /**
     * Returns the pivot element.
     */
    private long ssPivot(long tD, long pa, long first, long last) {
        // sa pointer
        long middle;
        long t = last - first;
        middle = first + t / 2;

        if (t <= 512) {
            if (t <= 32) {
                return ssMedian3(tD, pa, first, middle, last - 1);
            } else {
                t >>= 2;
                return ssMedian5(tD, pa, first, first + t, middle, last - 1 - t, last - 1);
            }
        }
        t >>= 3;
        first = ssMedian3(tD, pa, first, first + t, first + (t << 1));
        middle = ssMedian3(tD, pa, middle - t, middle, middle + t);
        last = ssMedian3(tD, pa, last - 1 - (t << 1), last - 1 - t, last - 1);
        return ssMedian3(tD, pa, first, middle, last);
    }

    /**
     * Returns the median of five elements
     */
    private long ssMedian5(long tD, long pa, long v1, long v2, long v3, long v4, long v5) {
        long t;
        if (this.t.get(start + tD + sa.get(pa + sa.get(v2))) > this.t.get(start + tD + sa.get(pa + sa.get(v3)))) {
            t = v2;
            v2 = v3;
            v3 = t;

        }
        if (this.t.get(start + tD + sa.get(pa + sa.get(v4))) > this.t.get(start + tD + sa.get(pa + sa.get(v5)))) {
            t = v4;
            v4 = v5;
            v5 = t;
        }
        if (this.t.get(start + tD + sa.get(pa + sa.get(v2))) > this.t.get(start + tD + sa.get(pa + sa.get(v4)))) {
            t = v2;
            v2 = v4;
            v4 = t;
            t = v3;
            v3 = v5;
            v5 = t;
        }
        if (this.t.get(start + tD + sa.get(pa + sa.get(v1))) > this.t.get(start + tD + sa.get(pa + sa.get(v3)))) {
            t = v1;
            v1 = v3;
            v3 = t;
        }
        if (this.t.get(start + tD + sa.get(pa + sa.get(v1))) > this.t.get(start + tD + sa.get(pa + sa.get(v4)))) {
            t = v1;
            v1 = v4;
            v4 = t;
            t = v3;
            v3 = v5;
            v5 = t;
        }
        if (this.t.get(start + tD + sa.get(pa + sa.get(v3))) > this.t.get(start + tD + sa.get(pa + sa.get(v4)))) {
            return v4;
        }
        return v3;
    }

    /**
     * Returns the median of three elements.
     */
    private long ssMedian3(long tD, long pa, long v1, long v2, long v3) {
        if (t.get(start + tD + sa.get(pa + sa.get(v1))) > t.get(start + tD + sa.get(pa + sa.get(v2)))) {
            long t = v1;
            v1 = v2;
            v2 = t;
        }
        if (t.get(start + tD + sa.get(pa + sa.get(v2))) > t.get(start + tD + sa.get(pa + sa.get(v3)))) {
            if (t.get(start + tD + sa.get(pa + sa.get(v1))) > t.get(start + tD + sa.get(pa + sa.get(v3)))) {
                return v1;
            } else {
                return v3;
            }
        }
        return v2;
    }

    /**
     * Binary partition for substrings.
     */
    private long ssPartition(long pa, long first, long last, long depth) {
        // sa pointer
        long a;
        long b;
        long t;
        for (a = first - 1, b = last; ; ) {
            for (; (++a < b) && ((sa.get(pa + sa.get(a)) + depth) >= (sa.get(pa + sa.get(a) + 1) + 1)); ) {
                sa.set(a, ~sa.get(a));
            }
            for (; (a < --b) && ((sa.get(pa + sa.get(b)) + depth) < (sa.get(pa + sa.get(b) + 1) + 1)); ) {
            }
            if (b <= a) {
                break;
            }
            t = ~sa.get(b);
            sa.set(b, sa.get(a));
            sa.set(a, t);
        }
        if (first < a) {
            sa.set(first, ~sa.get(first));
        }
        return a;
    }

    /**
     * Simple top-down heapsort.
     */
    private void ssHeapSort(long tD, long pa, long sa, long size) {
        long i;
        long m;
        long t;

        m = size;
        if ((size % 2) == 0) {
            m--;
            if (this.t.get(start + tD + this.sa.get(pa + this.sa.get(sa + (m / 2))))
                    < this.t.get(start + tD + this.sa.get(pa + this.sa.get(sa + m))))
            {
                swapInSA(sa + m, sa + (m / 2));
            }
        }

        for (i = m / 2 - 1; 0 <= i; --i) {
            ssFixDown(tD, pa, sa, i, m);
        }
        if ((size % 2) == 0) {
            swapInSA(sa, sa + m);
            ssFixDown(tD, pa, sa, 0, m);
        }
        for (i = m - 1; 0 < i; --i) {
            t = this.sa.get(sa);
            this.sa.set(sa, this.sa.get(sa + i));
            ssFixDown(tD, pa, sa, 0, i);
            this.sa.set(sa + i, t);
        }
    }

    private void ssFixDown(long tD, long pa, long sa, long i, long size) {
        long j;
        long k;
        long v;
        long c;
        long d;
        long e;

        for (v = this.sa.get(sa + i), c = t.get(start + tD + this.sa.get(pa + v)); (j = 2 * i + 1) < size;
             this.sa.set(sa + i, this.sa.get(sa + k)), i = k)
        {
            d = t.get(start + tD + this.sa.get(pa + this.sa.get(sa + (k = j++))));
            if (d < (e = t.get(start + tD + this.sa.get(pa + this.sa.get(sa + j))))) {
                k = j;
                d = e;
            }
            if (d <= c) {
                break;
            }
        }
        this.sa.set(i + sa, v);
    }

    private static int ssIlg(long n) {

        return ((n & 0xff00) != 0) ? 8 + lgTable[(int) ((n >> 8) & 0xff)]
                                   : 0 + lgTable[(int) ((n >> 0) & 0xff)];
    }

    private void swapInSA(long a, long b) {
        long tmp = sa.get(a);
        sa.set(a, sa.get(b));
        sa.set(b, tmp);
    }

    /**
     * Tandem repeat sort
     */
    private void trSort(long isa, long n, long depth) {
        TRBudget budget = new TRBudget(trIlg(n) * 2 / 3, n);
        long isaD;
        // sa pointers
        long first;
        long last;
        long t;
        long skip;
        long unsorted;
        for (isaD = isa + depth; -n < sa.get(0); isaD += isaD - isa) {
            first = 0;
            skip = 0;
            unsorted = 0;
            do {
                if ((t = sa.get(first)) < 0) {
                    first -= t;
                    skip += t;
                } else {
                    if (skip != 0) {
                        sa.set(first + skip, skip);
                        skip = 0;
                    }
                    last = sa.get(isa + t) + 1;
                    if (1 < (last - first)) {
                        budget.count = 0;
                        trIntroSort(isa, isaD, first, last, budget);
                        if (budget.count != 0) {
                            unsorted += budget.count;
                        } else {
                            skip = first - last;
                        }
                    } else if ((last - first) == 1) {
                        skip = -1;
                    }
                    first = last;
                }
            }
            while (first < n);
            if (skip != 0) {
                sa.set(first + skip, skip);
            }
            if (unsorted == 0) {
                break;
            }
        }
    }

    private TRPartitionResult trPartition(long isaD, long first, long middle, long last, long pa, long pb, long v) {
        // ptr
        long a;
        long b;
        long c;
        long d;
        long e;
        long f;
        long t;
        long s;
        long x = 0;

        for (b = middle - 1; (++b < last) && ((x = sa.get(isaD + sa.get(b))) == v); ) {
        }
        if (((a = b) < last) && (x < v)) {
            for (; (++b < last) && ((x = sa.get(isaD + sa.get(b))) <= v); ) {
                if (x == v) {
                    swapInSA(a, b);
                    ++a;
                }
            }
        }
        for (c = last; (b < --c) && ((x = sa.get(isaD + sa.get(c))) == v); ) {
        }
        if ((b < (d = c)) && (x > v)) {
            for (; (b < --c) && ((x = sa.get(isaD + sa.get(c))) >= v); ) {
                if (x == v) {
                    swapInSA(c, d);
                    --d;
                }
            }
        }
        for (; b < c; ) {
            swapInSA(c, b);
            for (; (++b < c) && ((x = sa.get(isaD + sa.get(b))) <= v); ) {
                if (x == v) {
                    swapInSA(a, b);
                    ++a;
                }
            }
            for (; (b < --c) && ((x = sa.get(isaD + sa.get(c))) >= v); ) {
                if (x == v) {
                    swapInSA(c, d);
                    --d;
                }
            }
        }

        if (a <= d) {
            c = b - 1;
            if ((s = a - first) > (t = b - a)) {
                s = t;
            }
            for (e = first, f = b - s; 0 < s; --s, ++e, ++f) {
                swapInSA(e, f);
            }
            if ((s = d - c) > (t = last - d - 1)) {
                s = t;
            }
            for (e = b, f = last - s; 0 < s; --s, ++e, ++f) {
                swapInSA(e, f);
            }
            first += b - a;
            last -= d - c;
        }
        return new TRPartitionResult(first, last);
    }

    private void trIntroSort(long isa, long isaD, long first, long last, TRBudget budget) {
        final int STACK_SIZE = TR_STACKSIZE;
        StackElement[] stack = new StackElement[STACK_SIZE];
        // pointers
        long a = 0;
        long b = 0;
        long c;
        long v;
        long x = 0;
        long incr = isaD - isa;


        int trlink = -1;
        for (int ssize = 0, limit = trIlg(last - first); ; ) {
            if (limit < 0) {
                if (limit == -1) {
                    /* tandem repeat partition */
                    TRPartitionResult res = trPartition(isaD - incr, first, first, last,
                            a, b, last - 1
                    );
                    a = res.a;
                    b = res.b;
                    /* update ranks */
                    if (a < last) {
                        for (c = first, v = a - 1; c < a; ++c) {
                            sa.set(isa + sa.get(c), v);
                        }
                    }
                    if (b < last) {
                        for (c = a, v = b - 1; c < b; ++c) {
                            sa.set(isa + sa.get(c), v);
                        }
                    }

                    /* push */
                    if (1 < (b - a)) {
                        stack[ssize++] = new StackElement(0, a, b, 0, 0);
                        stack[ssize++] = new StackElement(isaD - incr, first, last, -2, trlink);
                        trlink = ssize - 2;
                    }
                    if ((a - first) <= (last - b)) {
                        if (1 < (a - first)) {
                            stack[ssize++] = new StackElement(isaD, b, last, trIlg(last - b), trlink);
                            last = a;
                            limit = trIlg(a - first);
                        } else if (1 < (last - b)) {
                            first = b;
                            limit = trIlg(last - b);
                        } else {
                            if (ssize > 0) {
                                StackElement se = stack[--ssize];
                                isaD = se.a;
                                first = se.b;
                                last = se.c;
                                limit = se.d;
                                trlink = se.e;
                            } else {
                                return;
                            }

                        }
                    } else {
                        if (1 < (last - b)) {
                            stack[ssize++] = new StackElement(isaD, first, a, trIlg(a
                                    - first), trlink);
                            first = b;
                            limit = trIlg(last - b);
                        } else if (1 < (a - first)) {
                            last = a;
                            limit = trIlg(a - first);
                        } else {
                            if (ssize > 0) {
                                StackElement se = stack[--ssize];
                                isaD = se.a;
                                first = se.b;
                                last = se.c;
                                limit = se.d;
                                trlink = se.e;
                            } else {
                                return;
                            }
                        }
                    }
                } else if (limit == -2) {
                    /* tandem repeat copy */
                    StackElement se = stack[--ssize];
                    a = se.b;
                    b = se.c;
                    if (stack[ssize].d == 0) {
                        trCopy(isa, first, a, b, last, isaD - isa);
                    } else {
                        if (0 <= trlink) {
                            stack[trlink].d = -1;
                        }
                        trPartialCopy(isa, first, a, b, last, isaD - isa);
                    }
                    if (ssize > 0) {
                        se = stack[--ssize];
                        isaD = se.a;
                        first = se.b;
                        last = se.c;
                        limit = se.d;
                        trlink = se.e;
                    } else {
                        return;
                    }
                } else {
                    /* sorted partition */
                    if (0 <= sa.get(first)) {
                        a = first;
                        do {
                            sa.set(isa + sa.get(a), a);
                        }
                        while ((++a < last) && (0 <= sa.get(a)));
                        first = a;
                    }
                    if (first < last) {
                        a = first;
                        do {
                            sa.set(a, ~sa.get(a));
                        }
                        while (sa.get(++a) < 0);
                        int next = (sa.get(isa + sa.get(a)) != sa.get(isaD + sa.get(a)))
                               ? trIlg(a - first + 1) : -1;
                        if (++a < last) {
                            for (b = first, v = a - 1; b < a; ++b) {
                                sa.set(isa + sa.get(b), v);
                            }
                        }

                        /* push */
                        if (budget.check(a - first) != 0) {
                            if ((a - first) <= (last - a)) {
                                stack[ssize++] = new StackElement(isaD, a, last, -3, trlink);
                                isaD += incr;
                                last = a;
                                limit = next;
                            } else {
                                if (1 < (last - a)) {
                                    stack[ssize++] = new StackElement(isaD + incr, first, a, next, trlink);
                                    first = a;
                                    limit = -3;
                                } else {
                                    isaD += incr;
                                    last = a;
                                    limit = next;
                                }
                            }
                        } else {
                            if (0 <= trlink) {
                                stack[trlink].d = -1;
                            }
                            if (1 < (last - a)) {
                                first = a;
                                limit = -3;
                            } else {
                                if (ssize > 0) {
                                    StackElement se = stack[--ssize];
                                    isaD = se.a;
                                    first = se.b;
                                    last = se.c;
                                    limit = se.d;
                                    trlink = se.e;
                                } else {
                                    return;
                                }
                            }
                        }
                    } else {
                        if (ssize > 0) {
                            StackElement se = stack[--ssize];
                            isaD = se.a;
                            first = se.b;
                            last = se.c;
                            limit = se.d;
                            trlink = se.e;
                        } else {
                            return;
                        }
                    }
                }
                continue;
            }

            if ((last - first) <= TR_INSERTIONSORT_THRESHOLD) {
                trInsertionSort(isaD, first, last);
                limit = -3;
                continue;
            }

            if (limit-- == 0) {
                trHeapSort(isaD, first, last - first);
                for (a = last - 1; first < a; a = b) {
                    for (x = sa.get(isaD + sa.get(a)), b = a - 1;
                         (first <= b) && (sa.get(isaD + sa.get(b)) == x); --b)
                    {
                        sa.set(b, ~sa.get(b));
                    }
                }
                limit = -3;
                continue;
            }
            // choose pivot
            a = trPivot(isaD, first, last);
            swapInSA(first, a);
            v = sa.get(isaD + sa.get(first));

            // partition
            TRPartitionResult res = trPartition(isaD, first, first + 1, last, a, b, v);
            a = res.a;
            b = res.b;

            if ((last - first) != (b - a)) {
                int
                next = (sa.get(isa + sa.get(a)) != v) ? trIlg(b - a) : -1;

                /* update ranks */
                for (c = first, v = a - 1; c < a; ++c) {
                    sa.set(isa + sa.get(c), v);
                }
                if (b < last) {
                    for (c = a, v = b - 1; c < b; ++c) {
                        sa.set(isa + sa.get(c), v);
                    }
                }

                /* push */
                if ((1 < (b - a)) && ((budget.check(b - a) != 0))) {
                    if ((a - first) <= (last - b)) {
                        if ((last - b) <= (b - a)) {
                            if (1 < (a - first)) {
                                stack[ssize++] = new StackElement(isaD + incr, a, b,
                                        next, trlink
                                );
                                stack[ssize++] = new StackElement(isaD, b, last, limit,
                                        trlink
                                );
                                last = a;
                            } else if (1 < (last - b)) {
                                stack[ssize++] = new StackElement(isaD + incr, a, b,
                                        next, trlink
                                );
                                first = b;
                            } else {
                                isaD += incr;
                                first = a;
                                last = b;
                                limit = next;
                            }
                        } else if ((a - first) <= (b - a)) {
                            if (1 < (a - first)) {
                                stack[ssize++] = new StackElement(isaD, b, last, limit,
                                        trlink
                                );
                                stack[ssize++] = new StackElement(isaD + incr, a, b,
                                        next, trlink
                                );
                                last = a;
                            } else {
                                stack[ssize++] = new StackElement(isaD, b, last, limit,
                                        trlink
                                );
                                isaD += incr;
                                first = a;
                                last = b;
                                limit = next;
                            }
                        } else {
                            stack[ssize++] = new StackElement(isaD, b, last, limit,
                                    trlink
                            );
                            stack[ssize++] = new StackElement(isaD, first, a, limit,
                                    trlink
                            );
                            isaD += incr;
                            first = a;
                            last = b;
                            limit = next;
                        }
                    } else {
                        if ((a - first) <= (b - a)) {
                            if (1 < (last - b)) {
                                stack[ssize++] = new StackElement(isaD + incr, a, b,
                                        next, trlink
                                );
                                stack[ssize++] = new StackElement(isaD, first, a, limit,
                                        trlink
                                );
                                first = b;
                            } else if (1 < (a - first)) {
                                stack[ssize++] = new StackElement(isaD + incr, a, b,
                                        next, trlink
                                );
                                last = a;
                            } else {
                                isaD += incr;
                                first = a;
                                last = b;
                                limit = next;
                            }
                        } else if ((last - b) <= (b - a)) {
                            if (1 < (last - b)) {
                                stack[ssize++] = new StackElement(isaD, first, a, limit,
                                        trlink
                                );
                                stack[ssize++] = new StackElement(isaD + incr, a, b,
                                        next, trlink
                                );
                                first = b;
                            } else {
                                stack[ssize++] = new StackElement(isaD, first, a, limit,
                                        trlink
                                );
                                isaD += incr;
                                first = a;
                                last = b;
                                limit = next;
                            }
                        } else {
                            stack[ssize++] = new StackElement(isaD, first, a, limit,
                                    trlink
                            );
                            stack[ssize++] = new StackElement(isaD, b, last, limit,
                                    trlink
                            );
                            isaD += incr;
                            first = a;
                            last = b;
                            limit = next;
                        }
                    }
                } else {
                    if ((1 < (b - a)) && (0 <= trlink)) {
                        stack[trlink].d = -1;
                    }
                    if ((a - first) <= (last - b)) {
                        if (1 < (a - first)) {
                            stack[ssize++] = new StackElement(isaD, b, last, limit,
                                    trlink
                            );
                            last = a;
                        } else if (1 < (last - b)) {
                            first = b;
                        } else {
                            if (ssize > 0) {
                                StackElement se = stack[--ssize];
                                isaD = se.a;
                                first = se.b;
                                last = se.c;
                                limit = se.d;
                                trlink = se.e;
                            } else {
                                return;
                            }
                        }
                    } else {
                        if (1 < (last - b)) {
                            stack[ssize++] = new StackElement(isaD, first, a, limit,
                                    trlink
                            );
                            first = b;
                        } else if (1 < (a - first)) {
                            last = a;
                        } else {
                            if (ssize > 0) {
                                StackElement se = stack[--ssize];
                                isaD = se.a;
                                first = se.b;
                                last = se.c;
                                limit = se.d;
                                trlink = se.e;
                            } else {
                                return;
                            }
                        }
                    }
                }
            } else {
                if (budget.check(last - first) != 0) {
                    limit = trIlg(last - first);
                    isaD += incr;
                } else {
                    if (0 <= trlink) {
                        stack[trlink].d = -1;
                    }
                    if (ssize > 0) {
                        StackElement se = stack[--ssize];
                        isaD = se.a;
                        first = se.b;
                        last = se.c;
                        limit = se.d;
                        trlink = se.e;
                    } else {
                        return;
                    }
                }
            }

        }

    }

    /**
     * Returns the pivot element.
     */
    private long trPivot(long isaD, long first, long last) {
        long middle;
        long t;

        t = last - first;
        middle = first + t / 2;

        if (t <= 512) {
            if (t <= 32) {
                return trMedian3(isaD, first, middle, last - 1);
            } else {
                t >>= 2;
                return trMedian5(isaD, first, first + t, middle, last - 1 - t, last - 1);
            }
        }
        t >>= 3;
        first = trMedian3(isaD, first, first + t, first + (t << 1));
        middle = trMedian3(isaD, middle - t, middle, middle + t);
        last = trMedian3(isaD, last - 1 - (t << 1), last - 1 - t, last - 1);
        return trMedian3(isaD, first, middle, last);
    }

    /**
     * Returns the median of five elements.
     */
    private long trMedian5(long isaD, long v1, long v2, long v3, long v4, long v5) {
        long t;
        if (sa.get(isaD + sa.get(v2)) > sa.get(isaD + sa.get(v3))) {
            t = v2;
            v2 = v3;
            v3 = t;
        }
        if (sa.get(isaD + sa.get(v4)) > sa.get(isaD + sa.get(v5))) {
            t = v4;
            v4 = v5;
            v5 = t;
        }
        if (sa.get(isaD + sa.get(v2)) > sa.get(isaD + sa.get(v4))) {
            t = v2;
            v2 = v4;
            v4 = t;
            t = v3;
            v3 = v5;
            v5 = t;
        }
        if (sa.get(isaD + sa.get(v1)) > sa.get(isaD + sa.get(v3))) {
            t = v1;
            v1 = v3;
            v3 = t;
        }
        if (sa.get(isaD + sa.get(v1)) > sa.get(isaD + sa.get(v4))) {
            t = v1;
            v1 = v4;
            v4 = t;
            t = v3;
            v3 = v5;
            v5 = t;
        }
        if (sa.get(isaD + sa.get(v3)) > sa.get(isaD + sa.get(v4))) {
            return v4;
        }
        return v3;
    }

    /**
     * Returns the median of three elements.
     * Only for last - first <= 32?
     */
    private long trMedian3(long isaD, long v1, long v2, long v3) {
        if (sa.get(isaD + sa.get(v1)) > sa.get(isaD + sa.get(v2))) {
            long t = v1;
            v1 = v2;
            v2 = t;
        }
        if (sa.get(isaD + sa.get(v2)) > sa.get(isaD + sa.get(v3))) {
            if (sa.get(isaD + sa.get(v1)) > sa.get(isaD + sa.get(v3))) {
                return v1;
            } else {
                return v3;
            }
        }
        return v2;
    }

    private void trHeapSort(long isaD, long sa, long size) {
        long i;
        long m;
        long t;

        m = size;
        if ((size % 2) == 0) {
            m--;
            if (this.sa.get(isaD + this.sa.get(sa + m / 2)) < this.sa.get(isaD + this.sa.get(sa + m))) {
                swapInSA(sa + m, sa + m / 2);
            }
        }

        for (i = m / 2 - 1; 0 <= i; --i) {
            trFixDown(isaD, sa, i, m);
        }
        if ((size % 2) == 0) {
            swapInSA(sa, sa + m);
            trFixDown(isaD, sa, 0, m);
        }
        for (i = m - 1; 0 < i; --i) {
            t = this.sa.get(sa);
            this.sa.set(sa, this.sa.get(sa + i));
            trFixDown(isaD, sa, 0, i);
            this.sa.set(sa + i, t);
        }

    }

    private void trFixDown(long isaD, long sa, long i, long size) {
        long j;
        long k;
        long v;
        long c;
        long d;
        long e;

        for (v = this.sa.get(sa + i), c = this.sa.get(isaD + v);
             (j = 2 * i + 1) < size; this.sa.set(sa + i, this.sa.get(sa + k)), i = k)
        {
            d = this.sa.get(isaD + this.sa.get(sa + (k = j++)));
            if (d < (e = this.sa.get(isaD + this.sa.get(sa + j)))) {
                k = j;
                d = e;
            }
            if (d <= c) {
                break;
            }
        }
        this.sa.set(sa + i, v);
    }

    private void trInsertionSort(long isaD, long first, long last) {
        // sa ptr
        long b;
        long t;
        long r;

        for (long a = first + 1; a < last; ++a) {
            for (t = sa.get(a), b = a - 1; 0 > (r = sa.get(isaD + t) - sa.get(isaD + sa.get(b))); ) {
                do {
                    sa.set(b + 1, sa.get(b));
                }
                while ((first <= --b) && (sa.get(b) < 0));
                if (b < first) {
                    break;
                }
            }
            if (r == 0) {
                sa.set(b, ~sa.get(b));
            }
            sa.set(b + 1, t);
        }
    }

    private void trPartialCopy(long isa, long first, long a, long b, long last, long depth) {
        long c; // ptr
        long d;
        long e;
        long s;
        long v;
        long rank;
        long lastrank;
        long newrank = -1;

        v = b - 1;
        lastrank = -1;
        for (c = first, d = a - 1; c <= d; ++c) {
            if ((0 <= (s = sa.get(c) - depth)) && (sa.get(isa + s) == v)) {
                sa.set(++d, s);
                rank = sa.get(isa + s + depth);
                if (lastrank != rank) {
                    lastrank = rank;
                    newrank = d;
                }
                sa.set(isa + s, newrank);
            }
        }

        lastrank = -1;
        for (e = d; first <= e; --e) {
            rank = sa.get(isa + sa.get(e));
            if (lastrank != rank) {
                lastrank = rank;
                newrank = e;
            }
            if (newrank != rank) {
                sa.set(isa + sa.get(e), newrank);
            }
        }

        lastrank = -1;
        for (c = last - 1, e = d + 1, d = b; e < d; --c) {
            if ((0 <= (s = sa.get(c) - depth)) && (sa.get(isa + s) == v)) {
                sa.set(--d, s);
                rank = sa.get(isa + s + depth);
                if (lastrank != rank) {
                    lastrank = rank;
                    newrank = d;
                }
                sa.set(isa + s, newrank);
            }
        }
    }

    /**
     * sort suffixes of middle partition by using sorted order of suffixes of left and
     * right partition.
     */
    private void trCopy(long isa, long first, long a, long b, long last, long depth) {
        long c; // ptr
        long d;
        long e;
        long s;
        long v;

        v = b - 1;
        for (c = first, d = a - 1; c <= d; ++c) {
            s = sa.get(c) - depth;
            if ((0 <= s) && (sa.get(isa + s) == v)) {
                sa.set(++d, s);
                sa.set(isa + s, d);
            }
        }
        for (c = last - 1, e = d + 1, d = b; e < d; --c) {
            s = sa.get(c) - depth;
            if ((0 <= s) && (sa.get(isa + s) == v)) {
                sa.set(--d, s);
                sa.set(isa + s, d);
            }
        }
    }

    static int trIlg(long n) {
        if ((n & 0xffff0000) != 0) {
            if ((n & 0xff000000) != 0) {
                return 24 + lgTable[(int) ((n >> 24) & 0xff)];
            } else {
                return 16 + lgTable[(int) ((n >> 16) & 0xff)];
            }
        } else {
            if ((n & 0x0000ff00) != 0) {
                return 8 + lgTable[(int) ((n >> 8) & 0xff)];
            } else {
                return 0 + lgTable[(int) ((n >> 0) & 0xff)];
            }
        }
    }

}
