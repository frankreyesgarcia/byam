package io.zold.api;

import java.util.Comparator;
import org.cactoos.iterable.Filtered;
import org.cactoos.iterable.IterableEnvelope;
import org.cactoos.scalar.LengthOf;
import org.cactoos.scalar.ScalarEnvelope;

/**
 * {@link Remote} nodes that should receive taxes.
 *
 * @since 1.0
 */
public final class TaxBeneficiaries extends IterableEnvelope<Remote> {

    /**
     * Ctor.
     *
     * @param nodes Remote nodes to select from.
     */
    public TaxBeneficiaries(final Iterable<Remote> nodes) {
        super(() -> new Sorted<>(
            Comparator.comparing(Remote::score),
            new Filtered<>(
                // @checkstyle MagicNumberCheck (1 line)
                n -> {
                    final ScalarEnvelope<Integer> length = new LengthOf(n.score().suffixes());
                    return length.value() >= 16;
                },
                nodes
            )
        ));
    }
}