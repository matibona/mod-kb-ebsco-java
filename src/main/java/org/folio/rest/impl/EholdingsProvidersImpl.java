package org.folio.rest.impl;

import static org.folio.http.HttpConsts.CONTENT_TYPE_HEADER;
import static org.folio.http.HttpConsts.JSON_API_TYPE;
import static org.folio.rest.validator.ValidatorUtil.*;

import javax.validation.ValidationException;
import javax.ws.rs.core.Response;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.apache.http.HttpStatus;
import org.folio.config.RMAPIConfigurationServiceCache;
import org.folio.config.RMAPIConfigurationServiceImpl;
import org.folio.config.api.RMAPIConfigurationService;
import org.folio.http.ConfigurationClientProvider;
import org.folio.rest.annotations.Validate;
import org.folio.rest.aspect.HandleValidationErrors;
import org.folio.rest.converter.PackagesConverter;
import org.folio.rest.converter.VendorConverter;
import org.folio.rest.jaxrs.model.ProviderPutRequest;
import org.folio.rest.jaxrs.resource.EholdingsProviders;
import org.folio.rest.model.OkapiData;
import org.folio.rest.model.Sort;
import org.folio.rest.util.ErrorHandler;
import org.folio.rest.util.ErrorUtil;
import org.folio.rest.validator.HeaderValidator;
import org.folio.rest.validator.PackageParametersValidator;
import org.folio.rest.validator.ProviderPutBodyValidator;
import org.folio.rmapi.RMAPIService;
import org.folio.rmapi.exception.RMAPIResourceNotFoundException;
import org.folio.rmapi.exception.RMAPIServiceException;
import org.folio.rmapi.model.VendorPut;

public class EholdingsProvidersImpl implements EholdingsProviders {

  private static final String GET_PROVIDER_NOT_FOUND_MESSAGE = "Provider not found";
  private static final String PUT_PROVIDER_ERROR_MESSAGE = "Failed to update provider";
  private static final String GET_PROVIDER_PACKAGES_ERROR_MESSAGE = "Failed to retrieve provider packages";
  private static final String INVALID_PROVIDER_ID_ERROR = "Provider id is invalid - ";

  private final Logger logger = LoggerFactory.getLogger(EholdingsConfigurationImpl.class);

  private RMAPIConfigurationService configurationService;
  private HeaderValidator headerValidator;
  private VendorConverter converter;
  private PackagesConverter packagesConverter;
  private ProviderPutBodyValidator bodyValidator;
  private PackageParametersValidator parametersValidator;

  public EholdingsProvidersImpl() {
    this(
      new RMAPIConfigurationServiceCache(
        new RMAPIConfigurationServiceImpl(new ConfigurationClientProvider())),
      new HeaderValidator(),
      new VendorConverter(),
      new ProviderPutBodyValidator(),
      new PackagesConverter(),
      new PackageParametersValidator());
  }

  public EholdingsProvidersImpl(RMAPIConfigurationService configurationService,
                                HeaderValidator headerValidator,
                                VendorConverter converter,
                                ProviderPutBodyValidator bodyValidator,
                                PackagesConverter packageConverter,
                                PackageParametersValidator parametersValidator) {
    this.configurationService = configurationService;
    this.headerValidator = headerValidator;
    this.converter = converter;
    this.bodyValidator = bodyValidator;
    this.packagesConverter = packageConverter;
    this.parametersValidator = parametersValidator;
  }

  @Override
  @Validate
  @HandleValidationErrors
  public void getEholdingsProviders(String q, String sort, int page, int count, Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    headerValidator.validate(okapiHeaders);
    validateSort(sort);
    validateQuery(q);

    CompletableFuture.completedFuture(null)
    .thenCompose(o -> configurationService.retrieveConfiguration(new OkapiData(okapiHeaders)))
    .thenCompose(rmapiConfiguration -> {
      RMAPIService rmapiService = new RMAPIService(rmapiConfiguration.getCustomerId(), rmapiConfiguration.getAPIKey(),
        rmapiConfiguration.getUrl(), vertxContext.owner());
      return rmapiService.retrieveProviders(q, page, count, Sort.valueOf(sort.toUpperCase()));
    })
    .thenAccept(vendors ->
      asyncResultHandler.handle(Future.succeededFuture(GetEholdingsProvidersResponse
       .respond200WithApplicationVndApiJson(converter.convert(vendors)))))
    .exceptionally(e -> {
      if(e.getCause() instanceof RMAPIServiceException){
        RMAPIServiceException rmApiException = (RMAPIServiceException)e.getCause();
        asyncResultHandler.handle(Future.succeededFuture(GetEholdingsProvidersResponse
          .status(rmApiException.getRMAPICode())
          .header(CONTENT_TYPE_HEADER, JSON_API_TYPE)
          .entity(ErrorUtil.createErrorFromRMAPIResponse(rmApiException))
          .build()));
      }
      else {
        asyncResultHandler.handle(Future.succeededFuture(GetEholdingsProvidersResponse
          .status(HttpStatus.SC_INTERNAL_SERVER_ERROR)
          .header(CONTENT_TYPE_HEADER, JSON_API_TYPE)
          .entity(ErrorUtil.createError(e.getCause().getMessage()))
          .build()));
      }
      return null;
    });
  }

  @Override
  @HandleValidationErrors
  public void getEholdingsProvidersByProviderId(String providerId, String include, Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {

    long providerIdLong = getProviderId(providerId);

    headerValidator.validate(okapiHeaders);

    CompletableFuture.completedFuture(null)
      .thenCompose(o -> configurationService.retrieveConfiguration(new OkapiData(okapiHeaders)))
      .thenCompose(rmapiConfiguration -> {
        RMAPIService rmapiService = new RMAPIService(rmapiConfiguration.getCustomerId(), rmapiConfiguration.getAPIKey(),
          rmapiConfiguration.getUrl(), vertxContext.owner());
        return rmapiService.retrieveProvider(providerIdLong, include);
      })
      .thenAccept(
        vendorResult ->
          asyncResultHandler.handle(Future.succeededFuture(GetEholdingsProvidersByProviderIdResponse
            .respond200WithApplicationVndApiJson(converter.convertToProvider(vendorResult.getVendor(), vendorResult.getPackages()))))
      )
      .exceptionally(e -> {
        new ErrorHandler()
          .add(RMAPIResourceNotFoundException.class, exception ->
            GetEholdingsProvidersByProviderIdResponse.respond404WithApplicationVndApiJson(
              ErrorUtil.createError(GET_PROVIDER_NOT_FOUND_MESSAGE)))
        .addDefaultMapper()
        .handle(asyncResultHandler, e);
        return null;
      });
  }

  @Override
  @HandleValidationErrors
  public void putEholdingsProvidersByProviderId(String providerId, String contentType, ProviderPutRequest entity,
      Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {

    long providerIdLong = getProviderId(providerId);

    headerValidator.validate(okapiHeaders);
    bodyValidator.validate(entity);

    VendorPut rmapiVendor = converter.convertToVendor(entity);

    CompletableFuture.completedFuture(null)
        .thenCompose(o -> configurationService.retrieveConfiguration(new OkapiData(okapiHeaders)))
        .thenCompose(rmapiConfiguration -> {
          RMAPIService rmapiService = new RMAPIService(rmapiConfiguration.getCustomerId(),
              rmapiConfiguration.getAPIKey(), rmapiConfiguration.getUrl(), vertxContext.owner());
          return rmapiService.updateProvider(providerIdLong, rmapiVendor);
        })
        .thenAccept(vendor -> asyncResultHandler.handle(Future.succeededFuture(PutEholdingsProvidersByProviderIdResponse
            .respond200WithApplicationVndApiJson(converter.convertToProvider(vendor)))))
        .exceptionally(e -> {
          logger.error(PUT_PROVIDER_ERROR_MESSAGE, e);
          new ErrorHandler()
            .addRmApiMapper()
            .addDefaultMapper()
            .handle(asyncResultHandler, e);
          return null;
        });
  }

  @Override
  @Validate
  @HandleValidationErrors
  public void getEholdingsProvidersPackagesByProviderId(String providerId, String q, String filterSelected,
                                                        String filterType, String sort, int page, int count,
                                                        Map<String, String> okapiHeaders,
                                                        Handler<AsyncResult<Response>> asyncResultHandler,
                                                        Context vertxContext) {
    long providerIdLong = getProviderId(providerId);

    headerValidator.validate(okapiHeaders);
    parametersValidator.validate("true", filterSelected, filterType, sort, q);

    Sort nameSort = Sort.valueOf(sort.toUpperCase());
    CompletableFuture.completedFuture(null)
      .thenCompose(o -> configurationService.retrieveConfiguration(new OkapiData(okapiHeaders)))
      .thenCompose(rmapiConfiguration -> {
        RMAPIService rmapiService = new RMAPIService(rmapiConfiguration.getCustomerId(), rmapiConfiguration.getAPIKey(),
          rmapiConfiguration.getUrl(), vertxContext.owner());
        return rmapiService.retrievePackages(filterSelected, filterType, providerIdLong, q, page, count, nameSort);
      })
      .thenAccept(packages ->
        asyncResultHandler.handle(Future.succeededFuture(GetEholdingsProvidersPackagesByProviderIdResponse
          .respond200WithApplicationVndApiJson(packagesConverter.convert(packages)))))
      .exceptionally(e -> {
        logger.error(GET_PROVIDER_PACKAGES_ERROR_MESSAGE, e);
        new ErrorHandler()
          .add(RMAPIResourceNotFoundException.class, exception ->
            GetEholdingsProvidersPackagesByProviderIdResponse.respond404WithApplicationVndApiJson(
              ErrorUtil.createError(GET_PROVIDER_NOT_FOUND_MESSAGE)
            ))
          .addRmApiMapper()
          .addDefaultMapper()
          .handle(asyncResultHandler, e);
        return null;
      });
  }

  private long getProviderId(String providerId) {
    long providerIdLong;
    try {
      providerIdLong = Long.parseLong(providerId);
    } catch (NumberFormatException e) {
      throw new ValidationException(INVALID_PROVIDER_ID_ERROR + providerId, e);
    }
    return providerIdLong;
  }

  private void validateSort(String sort) {
    if (!Sort.contains(sort.toUpperCase())) {
      throw new ValidationException("Invalid sort parameter");
    }
  }

  private void validateQuery(String query) {
    if ("".equals(query)) {
      throw new ValidationException("Search parameter cannot be empty");
    }
  }
}