package org.folio.config;

import io.vertx.core.Context;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.folio.config.api.RMAPIConfigurationService;
import org.folio.config.cache.RMAPIConfigurationCache;
import org.folio.config.model.ConfigurationError;
import org.folio.rest.model.OkapiData;

public class RMAPIConfigurationServiceCache implements RMAPIConfigurationService {

  private RMAPIConfigurationService rmapiConfigurationService;

  public RMAPIConfigurationServiceCache(RMAPIConfigurationService rmapiConfigurationService) {
    this.rmapiConfigurationService = rmapiConfigurationService;
  }

  @Override
  public CompletableFuture<RMAPIConfiguration> retrieveConfiguration(OkapiData okapiData) {
    RMAPIConfiguration cachedConfiguration = RMAPIConfigurationCache.getInstance()
      .getValue(okapiData.getTenant());
    if(cachedConfiguration != null){
      return CompletableFuture.completedFuture(cachedConfiguration);
    }

    return rmapiConfigurationService.retrieveConfiguration(okapiData)
    .thenCompose(rmapiConfiguration -> {
      RMAPIConfigurationCache.getInstance()
        .putValue(okapiData.getTenant(), rmapiConfiguration);
      return CompletableFuture.completedFuture(rmapiConfiguration);
    });
  }

  @Override
  public CompletableFuture<RMAPIConfiguration> updateConfiguration(RMAPIConfiguration rmapiConfiguration, OkapiData okapiData) {
    return rmapiConfigurationService.updateConfiguration(rmapiConfiguration, okapiData)
    .thenCompose(configuration ->  {
      RMAPIConfigurationCache.getInstance()
        .putValue(okapiData.getTenant(), configuration);
      return CompletableFuture.completedFuture(configuration);
    });
  }

  @Override
  public CompletableFuture<List<ConfigurationError>> verifyCredentials(RMAPIConfiguration rmapiConfiguration, Context vertxContext) {
    if(rmapiConfiguration.getConfigValid() != null && rmapiConfiguration.getConfigValid()){
      return CompletableFuture.completedFuture(Collections.emptyList());
    }
    return rmapiConfigurationService.verifyCredentials(rmapiConfiguration, vertxContext)
      .thenCompose(errors -> {
        if(errors.isEmpty()){
          rmapiConfiguration.setConfigValid(true);
        }
        return CompletableFuture.completedFuture(errors);
      });
  }

}