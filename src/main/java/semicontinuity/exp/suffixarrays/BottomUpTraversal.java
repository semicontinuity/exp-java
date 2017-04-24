package semicontinuity.exp.suffixarrays;

import java.util.ArrayDeque;
import java.util.function.Consumer;

import semicontinuity.exp.offheap.LongArray;

/**
 * Implements bottom-up traversal of SA.
 * See https://pdfs.semanticscholar.org/4ca9/ea95a0a9846965e86619e646d9ca36930c18.pdf for details.
 */
public class BottomUpTraversal {

    private final LongArray sa;
    private final LongArray lcp;

    public BottomUpTraversal(LongArray sa, LongArray lcp) {
        this.sa = sa;
        this.lcp = lcp;
    }

    public void run(Consumer<LcpInterval> callback) {
        ArrayDeque<LcpInterval> intervals = new ArrayDeque<>();
        intervals.push(new LcpInterval(0, 0, -1));

        for (long i = 1; i < sa.length(); i++) {
            long left = i - 1;
            while (fixedLcp(i) < intervals.peek().value) {
                LcpInterval interval = intervals.pop();
                interval.to = i - 1;
                callback.accept(interval);
                left = interval.from;
            }

            if (fixedLcp(i) > intervals.peek().value) {
                intervals.push(new LcpInterval(fixedLcp(i), left, -1));
            }
        }

        while (!intervals.isEmpty()) {
            LcpInterval interval = intervals.pop();
            interval.to = sa.length() - 1;
            callback.accept(interval);
        }
    }

    private long fixedLcp(long i) {
        // at [0] there is -1 in generated LCP table, we need 0
        return i == 0 ? 0 : lcp.get(i);
    }
}
