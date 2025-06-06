package com.artipie.security.policy;

import com.amihaiemil.eoyaml.Yaml;
import com.artipie.asto.Storage;
import com.artipie.asto.blocking.BlockingStorage;
import com.artipie.asto.factory.StorageFactory;
import com.artipie.asto.factory.Storages;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Map;

/**
 * Policy factory to create {@link YamlPolicy}. Yaml policy is read from storage, and it's required
 * to describe this storage in the configuration. Configuration format is the following:
 *
 * policy:
 *   type: yaml_policy
 *   storage:
 *     type: fs
 *     path: /some/path
 *
 * The storage itself is expected to have yaml files with permissions in the following structure:
 *
 * ..
 * ├── roles.yaml
 * ├── users
 * │   ├── david.yaml
 * │   ├── jane.yaml
 * │   ├── ...
 *
 * @since 1.2
 */
@ArtipiePolicyFactory("yaml_policy")
public final class YamlPolicyFactory implements PolicyFactory {

    @Override
    public Policy<?> getPolicy(final PolicyConfig config) {
        final PolicyConfig sub = config.config("storage");
        try {
            final Storage storage = new StorageFactory(Map.of()).newStorage(
                sub.string("type"), sub.value()
            );
            return new YamlPolicy(
                new BlockingStorage(storage)
            );
        } catch (final IOException err) {
            throw new UncheckedIOException(err);
        }
    }
}