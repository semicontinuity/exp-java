package semicontinuity.exp.suffixarrays;

public class LcpInterval {
    public long value;
    public long from;
    public long to;

    LcpInterval(long value, long from, long to) {
        this.value = value;
        this.from = from;
        this.to = to;
    }
}
