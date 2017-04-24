package semicontinuity.exp.suffixarrays;

import semicontinuity.exp.offheap.LongArray;
import semicontinuity.exp.offheap.LongArrayView;
import semicontinuity.exp.offheap.OffheapByteArrayAsFiveByteLongArrayLsb;
import semicontinuity.exp.offheap.OffheapByteArrayAsLongArrayLsb;

/**
 * Adopted from jsuffixarrays to support long datasets.
 * https://github.com/carrotsearch/jsuffixarrays
 *
 * Original copyright notice:
 *
 * Copyright (c) 2008-2009 Yuta Mori All Rights Reserved.
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 *
 * SA-IS algorithm, implemented by Yuta Mori. SAIS is a very simple and small library that
 * provides an implementation of the induced sorting-based suffix array construction
 * algorithm. The algorithm runs in O(n) worst-case time, and MAX(2n, 4k) worst-case extra
 * working space, where n and k are the length of the input string and the number of
 * alphabets.
 * <p>
 * Ge Nong, Sen Zhang and Wai Hong Chan, Two Efficient Algorithms for Linear Suffix Array
 * Construction, 2008.
 *
 * @see "http://yuta.256.googlepages.com/sais"
 */
public final class Sais {
    // find the start or end of each bucket
    private static void getCounts(LongArray t, LongArray bigC, long n, long k) {
        for (long i = 0; i < k; ++i) {
            bigC.set(i, 0);
        }
        for (long i = 0; i < n; ++i) {
            bigC.update(t.get(i), 1);
        }
    }

    private static void getBuckets(LongArray bigC, LongArray bigB, long k, boolean end) {
        long i;
        long sum = 0;
        if (end) {
            for (i = 0; i < k; ++i) {
                sum += bigC.get(i);
                bigB.set(i, sum);
            }
        } else {
            for (i = 0; i < k; ++i) {
                sum += bigC.get(i);
                bigB.set(i, sum - bigC.get(i));
            }
        }
    }

    // compute SA and BWT
    private static void induceSA(LongArray T, LongArray sa, LongArray bigC, LongArray bigB, long n, long k) {
        long b;
        long i;
        long j;
        long c0;
        long c1;
        // compute SAl
        if (bigC == bigB) {
            getCounts(T, bigC, n, k);
        }
        getBuckets(bigC, bigB, k, false);
    // find starts of buckets
        j = n - 1;
        b = bigB.get(c1 = T.get(j));
        sa.set(b++, ((0 < j) && (T.get(j - 1) < c1)) ? ~j : j);
        for (i = 0; i < n; ++i) {
            j = sa.get(i);
            sa.set(i, ~j);
            if (0 < j) {
                if ((c0 = T.get(--j)) != c1) {
                    bigB.set(c1, b);
                    b = bigB.get(c1 = c0);
                }
                sa.set(b++, ((0 < j) && (T.get(j - 1) < c1)) ? ~j : j);
            }
        }

    // compute SAs

        if (bigC == bigB) {
            getCounts(T, bigC, n, k);
        }
        getBuckets(bigC, bigB, k, true);
        /* find ends of buckets */

        for (i = n - 1, b = bigB.get(c1 = 0); 0 <= i; --i) {
            if (0 < (j = sa.get(i))) {
                if ((c0 = T.get(--j)) != c1) {
                    bigB.set(c1, b);
                    b = bigB.get(c1 = c0);
                }
                sa.set(--b, ((j == 0) || (T.get(j - 1) > c1)) ? ~j : j);
            } else {
                sa.set(i, ~j);
            }
        }
    }

    private static long computeBWT(LongArray t, LongArray sa, LongArray bigC, LongArray bigB, long n, long k) {
        long b;
        long i;
        long j;
        long pidx = -1;
        long c0;
        long c1;
        // compute SAl
        if (bigC == bigB) {
            getCounts(t, bigC, n, k);
        }
        getBuckets(bigC, bigB, k, false); // find starts of buckets
        j = n - 1;
        b = bigB.get(c1 = t.get(j));
        sa.set(b++, ((0 < j) && (t.get(j - 1) < c1)) ? ~j : j);
        for (i = 0; i < n; ++i) {
            if (0 < (j = sa.get(i))) {
                sa.set(i, ~(c0 = t.get(--j)));
                if (c0 != c1) {
                    bigB.set(c1, b);
                    b = bigB.get(c1 = c0);
                }
                sa.set(b++, ((0 < j) && (t.get(j - 1) < c1)) ? ~j : j);
            } else if (j != 0) {
                sa.set(i, ~j);
            }
        }
    // compute SAs
        if (bigC == bigB) {
            getCounts(t, bigC, n, k);
        }
        getBuckets(bigC, bigB, k, true); // find ends of buckets
        for (i = n - 1, b = bigB.get(c1 = 0); 0 <= i; --i) {
            if (0 < (j = sa.get(i))) {
                sa.set(i, c0 = t.get(--j));
                if (c0 != c1) {
                    bigB.set(c1, b);
                    b = bigB.get(c1 = c0);
                }
                sa.set(--b, ((0 < j) && (t.get(j - 1) > c1)) ? ~(t.get(j - 1)) : j);
            } else if (j != 0) {
                sa.set(i, ~j);
            } else {
                pidx = i;
            }
        }
        return pidx;
    }


    // find the suffix array SA of T[0..n-1] in {0..k-1}^n
    //   use a working space (excluding T and SA) of at most 2n+O(1) for a constant alphabet
    private static long saIs(LongArray t, LongArray sa, long fs, long n, long k, boolean isbwt) {
        LongArray bigC;
        LongArray bigB;
        LongArray ra;
        long i;
        long j;
        long c;
        long m;
        long p;
        long q;
        long plen;
        long qlen;
        long name;
        long pidx = 0;
        long c0;
        long c1;
        boolean diff;

        // stage 1: reduce the problem by at least 1/2 sort all the S-substrings
        if (k <= fs) {
            bigC = new LongArrayView(sa, n);
            bigB = (k <= (fs - k)) ? new LongArrayView(sa, n + k) : bigC;
        } else {
            bigB = bigC = new OffheapByteArrayAsLongArrayLsb(k);
        }
        getCounts(t, bigC, n, k);
        getBuckets(bigC, bigB, k, true);
        /* find ends of buckets */

        for (i = 0; i < n; ++i) {
            sa.set(i, 0);
        }
        for (i = n - 2, c = 0, c1 = t.get(n - 1); 0 <= i; --i, c1 = c0) {
            if ((c0 = t.get(i)) < (c1 + c)) {
                c = 1;
            } else if (c != 0) {
                sa.set(bigB.update(c1, -1), i + 1);
                c = 0;
            }
        }
        induceSA(t, sa, bigC, bigB, n, k);

        bigB.close();
        bigC.close();

        // compact all the sorted substrings into the first m items of SA 2*m must be not larger than n (proveable)
        for (i = 0, m = 0; i < n; ++i) {
            p = sa.get(i);
            if ((0 < p) && (t.get(p - 1) > (c0 = t.get(p)))) {
                for (j = p + 1; (j < n) && (c0 == (c1 = t.get(j))); ++j) {
                }
                if ((j < n) && (c0 < c1)) {
                    sa.set(m++, p);
                }
            }
        }
        j = m + (n >> 1);
        for (i = m; i < j; ++i) {
            sa.set(i, 0);
        } // init the name array buffer
        // store the length of all substrings
        for (i = n - 2, j = n, c = 0, c1 = t.get(n - 1); 0 <= i; --i, c1 = c0) {
            if ((c0 = t.get(i)) < (c1 + c)) {
                c = 1;
            } else if (c != 0) {
                sa.set(m + ((i + 1) >> 1), j - i - 1);
                j = i + 1;
                c = 0;
            }
        }
        // find the lexicographic names of all substrings
        for (i = 0, name = 0, q = n, qlen = 0; i < m; ++i) {
            p = sa.get(i);
            plen = sa.get(m + (p >> 1));
            diff = true;
            if (plen == qlen) {
                for (j = 0; (j < plen) && (t.get(p + j) == t.get(q + j)); ++j) {
                }
                if (j == plen) {
                    diff = false;
                }
            }
            if (diff) {
                ++name;
                q = p;
                qlen = plen;
            }
            sa.set(m + (p >> 1), name);
        }

        // stage 2: solve the reduced problem recurse if names are not yet unique
        if (name < m) {
            ra = new LongArrayView(sa, n + fs - m);
            for (i = m + (n >> 1) - 1, j = n + fs - 1; m <= i; --i) {
                if (sa.get(i) != 0) {
                    sa.set(j--, sa.get(i) - 1);
                }
            }
            saIs(ra, sa, fs + n - m * 2, m, name, false);
            ra = null;
            for (i = n - 2, j = m * 2 - 1, c = 0, c1 = t.get(n - 1); 0 <= i; --i, c1 = c0) {
                if ((c0 = t.get(i)) < (c1 + c)) {
                    c = 1;
                } else if (c != 0) {
                    sa.set(j--, i + 1);
                    c = 0;
                } // get p1
            }
            for (i = 0; i < m; ++i) {
                sa.set(i, sa.get(sa.get(i) + m));
            } // get index
        }

        // stage 3: induce the result for the original problem
        if (k <= fs) {
            bigC = new LongArrayView(sa, n);
            bigB = (k <= (fs - k)) ? new LongArrayView(sa, n + k) : bigC;
        } else {
            bigB = bigC = new OffheapByteArrayAsLongArrayLsb(k);
        }
        // put all left-most S characters into their buckets
        getCounts(t, bigC, n, k);
        getBuckets(bigC, bigB, k, true); // find ends of buckets
        for (i = m; i < n; ++i) {
            sa.set(i, 0);
        } // init SA[m..n-1]
        for (i = m - 1; 0 <= i; --i) {
            j = sa.get(i);
            sa.set(i, 0);
            long character = t.get(j);
            long updated = bigB.update(character, -1);
            sa.set(updated, j);
        }
        if (!isbwt) {
            induceSA(t, sa, bigC, bigB, n, k);
        } else {
            pidx = computeBWT(t, sa, bigC, bigB, n, k);
        }
        bigC.close();
        bigB.close();
        return pidx;
    }


     // Suffixsorting
    public static long suffixsort(LongArray t, LongArray sa, long n, long k) {
        return saIs(t, sa, 0, n, k, false);
    }




    // Burrows-Wheeler Transform
    public static long bwtransform(LongArray t, LongArray u, LongArray bigA, long n) {
        long i;
        long pidx;
        if ((t == null) || (u == null) || (bigA == null) || (t.length() < n) || (u.length() < n) || (bigA.length() < n)) {
            return -1;
        }
        if (n <= 1) {
            if (n == 1) {
                u.set(0, t.get(0));
            }
            return n;
        }
        pidx = saIs(new LongArrayView(t, 0), bigA, 0, n, 256, true);
        u.set(0, t.get(n - 1));
        for (i = 0; i < pidx; ++i) {
            u.set(i + 1, bigA.get(i));
        }
        for (i += 1; i < n; ++i) {
            u.set(i, bigA.get(i));
        }
        return pidx + 1;
    }

    public OffheapByteArrayAsFiveByteLongArrayLsb buildSuffixArray(LongArray input, long length) {
        // TODO: [dw] add constraints here.
        OffheapByteArrayAsFiveByteLongArrayLsb sa = new OffheapByteArrayAsFiveByteLongArrayLsb(length);
        suffixsort(input, sa, length, 256);
        return sa;
    }
}
