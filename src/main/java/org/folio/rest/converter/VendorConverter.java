package org.folio.rest.converter;

import java.util.List;
import java.util.stream.Collectors;

import org.folio.rest.jaxrs.model.MetaDataIncluded;
import org.folio.rest.jaxrs.model.MetaTotalResults;
import org.folio.rest.jaxrs.model.Packages;
import org.folio.rest.jaxrs.model.Provider;
import org.folio.rest.jaxrs.model.ProviderCollection;
import org.folio.rest.jaxrs.model.ProviderData;
import org.folio.rest.jaxrs.model.ProviderDataAttributes;
import org.folio.rest.jaxrs.model.ProviderListDataAttributes;
import org.folio.rest.jaxrs.model.ProviderPutRequest;
import org.folio.rest.jaxrs.model.Providers;
import org.folio.rest.jaxrs.model.Proxy;
import org.folio.rest.jaxrs.model.Relationships;
import org.folio.rest.jaxrs.model.Token;
import org.folio.rest.util.RestConstants;
import org.folio.rmapi.model.TokenInfo;
import org.folio.rmapi.model.Vendor;
import org.folio.rmapi.model.VendorById;
import org.folio.rmapi.model.VendorPut;
import org.folio.rmapi.model.Vendors;

public class VendorConverter {

  private static final Relationships EMPTY_PACKAGE_RELATIONSHIP = new Relationships()
    .withPackages(new Packages()
      .withMeta(new MetaDataIncluded()
        .withIncluded(false))
      .withData(null));
  private static final String PROVIDERS_TYPE = "providers";

  private CommonAttributesConverter commonConverter;

  private PackagesConverter packagesConverter;

  public VendorConverter() {
    this(new CommonAttributesConverter(), new PackagesConverter());
  }

  public VendorConverter(CommonAttributesConverter commonConverter, PackagesConverter packagesConverter) {
    this.commonConverter = commonConverter;
    this.packagesConverter = packagesConverter;
  }

  public ProviderCollection convert(Vendors vendors) {
    List<Providers> providerList = vendors.getVendorList().stream()
      .map(this::convertVendor)
      .collect(Collectors.toList());
    return new ProviderCollection()
      .withJsonapi(RestConstants.JSONAPI)
      .withMeta(new MetaTotalResults().withTotalResults(vendors.getTotalResults()))
      .withData(providerList);
  }

  private Providers convertVendor(Vendor vendor) {
    String token = vendor.getVendorToken();
    return new Providers()
      .withId(String.valueOf(vendor.getVendorId()))
      .withType(PROVIDERS_TYPE)
      .withAttributes(new ProviderListDataAttributes()
        .withName(vendor.getVendorName())
        .withPackagesTotal(vendor.getPackagesTotal())
        .withPackagesSelected(vendor.getPackagesSelected())
        .withSupportsCustomPackages(vendor.isCustomer())
        .withProviderToken(token != null ? new Token().withValue(token) : null))
      .withRelationships(EMPTY_PACKAGE_RELATIONSHIP);
  }

  public Provider convertToProvider(VendorById vendor) {
    return convertToProvider(vendor, null);
  }

  public Provider convertToProvider(VendorById vendor, org.folio.rmapi.model.Packages packages) {
    TokenInfo vendorToken = vendor.getVendorByIdToken();
    return new Provider()
      .withData(new ProviderData()
        .withId(String.valueOf(vendor.getVendorId()))
        .withType(PROVIDERS_TYPE)
        .withAttributes(new ProviderDataAttributes()
          .withName(vendor.getVendorName())
          .withPackagesTotal(vendor.getPackagesTotal())
          .withPackagesSelected(vendor.getPackagesSelected())
          .withSupportsCustomPackages(vendor.isCustomer())
          .withProviderToken(commonConverter.convertToken(vendorToken))
          .withProxy(new Proxy()
            .withId(vendor.getProxy().getId())
            .withInherited(vendor.getProxy().getInherited()))
        )
        .withRelationships(EMPTY_PACKAGE_RELATIONSHIP))
      .withIncluded(packages == null ? null : packagesConverter.convert(packages).getData())
      .withJsonapi(RestConstants.JSONAPI);
  }

  public VendorPut convertToVendor(ProviderPutRequest provider) {

    VendorPut vendor = new VendorPut();

    // RM API gives an error when we pass inherited as true along with updated proxy
    // value
    // Hard code it to false; it should not affect the state of inherited that RM
    // API maintains
    org.folio.rmapi.model.Proxy vendorProxy = new org.folio.rmapi.model.Proxy();
    org.folio.rmapi.model.VendorPutToken vendorToken = new org.folio.rmapi.model.VendorPutToken();

    if (provider.getData().getAttributes().getProxy() != null) {
      vendorProxy.setInherited(false);
      vendorProxy.setId(provider.getData().getAttributes().getProxy().getId());
      vendor.setProxy(vendorProxy);
    }

    if (provider.getData().getAttributes().getProviderToken() != null) {
      vendorToken.setValue(provider.getData().getAttributes().getProviderToken().getValue());
      vendor.setVendorPutToken(vendorToken);
    }

    return vendor;

  }
}
