package com.rzware.collections.auth;

import javax.enterprise.context.ApplicationScoped;
import java.util.function.Supplier;

import io.quarkus.oidc.OidcRequestContext;
import io.smallrye.mutiny.Uni;
import io.quarkus.oidc.OidcTenantConfig;
import io.quarkus.oidc.TenantConfigResolver;
import io.vertx.ext.web.RoutingContext;

@ApplicationScoped
public class CollectionsConfigResolver implements TenantConfigResolver {
    private Supplier<OidcTenantConfig> createTenantConfig() {
        final OidcTenantConfig config = new OidcTenantConfig();

        config.setTenantId("craig");
        config.setAuthServerUrl("https://keycloak.rzware.com/realms/quarkus");
        config.setClientId("backend-service");
        OidcTenantConfig.Credentials credentials = new OidcTenantConfig.Credentials();
        credentials.setSecret("secret");
        config.setCredentials(credentials);


        // any other setting support by the quarkus-oidc extension

        return () -> config;
    }

    @Override
    public Uni<OidcTenantConfig> resolve(RoutingContext context, OidcRequestContext<OidcTenantConfig> requestContext) {
        String path = context.request().path();
        String[] parts = path.split("/");

        if (parts.length == 0) {
            // resolve to default tenant configuration
            return null;
        }

        if ("craig".equals(parts[1])) {
            // Do 'return requestContext.runBlocking(createTenantConfig());'
            // if a blocking call is required to create a tenant config
            return Uni.createFrom().item(createTenantConfig());
        }

        // resolve to default tenant configuration
        return Uni.createFrom().item(createTenantConfig());
//        return null;
    }
}