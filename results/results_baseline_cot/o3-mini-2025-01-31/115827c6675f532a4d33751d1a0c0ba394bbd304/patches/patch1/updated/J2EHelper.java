package org.pac4j.dropwizard;

import java.io.IOException;
import java.util.EnumSet;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.FilterRegistration;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;

import org.pac4j.core.config.Config;
import org.pac4j.dropwizard.Pac4jFactory.ServletCallbackFilterConfiguration;
import org.pac4j.dropwizard.Pac4jFactory.ServletLogoutFilterConfiguration;
import org.pac4j.dropwizard.Pac4jFactory.ServletSecurityFilterConfiguration;

import io.dropwizard.setup.Environment;
import org.pac4j.jee.filter.AbstractConfigFilter;
import org.pac4j.jee.filter.CallbackFilter;
import org.pac4j.jee.filter.LogoutFilter;
import org.pac4j.jee.filter.SecurityFilter;

/**
 *
 * @author Evan Meagher
 * @author Victor Noel - Linagora
 * @since 1.1.0
 *
 */
public final class J2EHelper {

    private J2EHelper() {
        // utility class
    }

    public static void registerSecurityFilter(Environment environment,
            Config config, ServletSecurityFilterConfiguration fConf) {

        final SecurityFilter filter = new SecurityFilter();

        filter.setClients(fConf.getClients());
        filter.setAuthorizers(fConf.getAuthorizers());
        filter.setMatchers(fConf.getMatchers());
        filter.setMultiProfile(fConf.getMultiProfile());

        registerFilter(environment, config, filter, fConf.getMapping());
    }

    public static void registerCallbackFilter(Environment environment,
            Config config, ServletCallbackFilterConfiguration fConf) {

        final CallbackFilter filter = new CallbackFilter();

        filter.setDefaultUrl(fConf.getDefaultUrl());
        filter.setMultiProfile(fConf.getMultiProfile());
        filter.setRenewSession(fConf.getRenewSession());

        registerFilter(environment, config, filter, fConf.getMapping());
    }

    public static void registerLogoutFilter(Environment environment,
            Config config, ServletLogoutFilterConfiguration fConf) {

        final LogoutFilter filter = new LogoutFilter();

        filter.setDefaultUrl(fConf.getDefaultUrl());
        filter.setLogoutUrlPattern(fConf.getLogoutUrlPattern());
        filter.setLocalLogout(fConf.getLocalLogout());
        filter.setDestroySession(fConf.getDestroySession());
        filter.setCentralLogout(fConf.getCentralLogout());

        registerFilter(environment, config, filter, fConf.getMapping());
    }

    private static void registerFilter(Environment environment, Config config,
                                       AbstractConfigFilter filter, String mapping) {

        filter.setConfigOnly(config);

        final FilterRegistration.Dynamic filterRegistration = environment
                .servlets().addFilter(filter.getClass().getName(), new JakartaFilterAdapter(filter));

        filterRegistration.addMappingForUrlPatterns(
                EnumSet.of(DispatcherType.REQUEST), true, mapping);
    }

    private static class JakartaFilterAdapter implements Filter {

        private final AbstractConfigFilter delegate;

        JakartaFilterAdapter(AbstractConfigFilter delegate) {
            this.delegate = delegate;
        }

        @Override
        public void init(FilterConfig filterConfig) throws ServletException {
            delegate.init(filterConfig);
        }

        @Override
        public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
                throws IOException, ServletException {
            delegate.doFilter(request, response, chain);
        }

        @Override
        public void destroy() {
            delegate.destroy();
        }
    }
}