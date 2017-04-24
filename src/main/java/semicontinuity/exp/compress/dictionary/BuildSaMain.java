package semicontinuity.exp.compress.dictionary;

import java.io.File;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import semicontinuity.exp.offheap.ByteArrayAsLongArrayAdapter;
import semicontinuity.exp.offheap.LongArray;
import semicontinuity.exp.offheap.OffheapByteArrayAsFiveByteLongArrayLsb;

import semicontinuity.exp.suffixarrays.Sais;

/**
 * Builds suffix array for the given binary data file.
 */
public class BuildSaMain {
    private static final Logger logger = LogManager.getLogger(BuildSaMain.class);

    public static void main(String[] args) {
        new BuildSaMain().run(new File(args[0]));
    }

    private void run(File folder) {
        logger.info("Generating");

        ByteArrayAsLongArrayAdapter input = Helper.openBytes(new File(folder, "data"));
        LongArray sa = new OffheapByteArrayAsFiveByteLongArrayLsb(input.length());

        Sais.suffixsort(input, sa, input.length(), 256);

        for (int i = 0; i < 11; i++) {
            System.out.println("sa " + i + '=' + sa.get(i) + " --> "
                    + input.get(sa.get(i))
                    + '.' + input.get(sa.get(i) + 1)
                    + '.' + input.get(sa.get(i) + 2)
                    + '.' + input.get(sa.get(i) + 3)
                    + '.' + input.get(sa.get(i) + 4)
                    + '.' + input.get(sa.get(i) + 5)
            );
        }
        input.close();

        logger.info("Saving");
        LongArray saPersistent = Helper.createLongs(new File(folder, "sa"), input.length());
        Helper.copyLongs(sa, saPersistent);
        sa.close();
        saPersistent.close();
    }
}
