package semicontinuity.exp.compress.dictionary;

import java.io.BufferedOutputStream;
import java.io.Closeable;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.function.Consumer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import semicontinuity.exp.offheap.LongArray;
import semicontinuity.exp.offheap.OffheapByteArrayAsFiveByteLongArrayLsb;

import semicontinuity.exp.suffixarrays.BottomUpTraversal;
import semicontinuity.exp.suffixarrays.LcpInterval;

/**
 * Builds frequent intervals dataset.
 * Finds all LCP intervals that represent at least 2 suffixes, with a minimum common length 4.
 * Writes LCP intervals to file, together with rating assigned to each interval.
 */
public class BuildFrequentIntervalsMain {
    private static final Logger logger = LogManager.getLogger(BuildFrequentIntervalsMain.class);

    public static void main(String[] args) throws IOException {
        new BuildFrequentIntervalsMain().run(new File(args[0]));
    }

    static class Solver implements Closeable {
        private final LongArray sa;
        private final LongArray rsufa;
        private final LongArray score;
        private final Consumer<LcpInterval> sink;

        Solver(LongArray sa, LongArray rsufa, Consumer<LcpInterval> sink) {
            this.sa = sa;
            this.rsufa = rsufa;
            this.score = new OffheapByteArrayAsFiveByteLongArrayLsb(rsufa.length());
            this.sink = sink;
        }

        // it seems that intervals with width 1 are not processed
        private void computeScores(LcpInterval interval) {
            if (interval.value >= 4) {
                // Scan the LCP interval and get the maximum score from scores array
                long maxScore = 0;
                for (long i = interval.from; i <= interval.to; i++) {
                    if (score.get(i) > maxScore) {
                        maxScore = score.get(i);
                    }
                }

                long intervalScore = interval.value * (interval.to - interval.from + 1);

                // if it is not a top interval
                if (interval.from != 0 && interval.to != score.length()) {
                    long fillScore = Math.max(intervalScore, maxScore);
                    for (long i = interval.from; i <= interval.to; i++) {
                        this.score.set(i, fillScore);
                    }

                    // coloring direct child: indicate that longer string is in dictionary
                    for (long i = interval.from; i <= interval.to; i++) {
                        long index = rsufa.get(sa.get(i) + 1);
                        if (this.score.get(index) < fillScore) {
                            this.score.set(index, fillScore);
                        }
                    }
                }
            }
        }

        private void reportQualifyingIntervals(LcpInterval interval) {
            if (interval.value >= 4) {
                long intervalScore = interval.value * (interval.to - interval.from + 1);
                if (intervalScore >= score.get(interval.to)) {
                    sink.accept(interval);
                }
            }
        }

        public void close() {
            score.close();
        }
    }


    private void run(File folder) throws IOException {
        logger.info("Generating");

        LongArray sa = Helper.openLongs(new File(folder, "sa"));
        LongArray rsa = Helper.openLongs(new File(folder, "rsa"));
        LongArray lcp = Helper.openLongs(new File(folder, "lcp"));
        File outputFile = new File(folder, "frequent-intervals");

        try (BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(new FileOutputStream(outputFile))) {
            try (DataOutputStream dataOutputStream = new DataOutputStream(bufferedOutputStream)) {
                BottomUpTraversal traversal = new BottomUpTraversal(sa, lcp);

                Consumer<LcpInterval> sink = interval -> {
                    // text position:long; length:int; rating:float
                    if (interval.value <= Integer.MAX_VALUE && interval.value >= 4) {
                        long textPosition = sa.get(interval.to);
                        try {
                            dataOutputStream.writeLong(textPosition);
                            dataOutputStream.writeInt((int) interval.value);
                            dataOutputStream.writeFloat(
                                    (float) (interval.to - interval.from + 1)
                                            * (float) (interval.value - 3) / (float) interval.value
                            );
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }
                };

                Solver solver = new Solver(sa, rsa, sink);
                traversal.run(solver::computeScores);
                traversal.run(solver::reportQualifyingIntervals);
                solver.close();
            }
        }

        sa.close();
        rsa.close();
        lcp.close();
    }
}
