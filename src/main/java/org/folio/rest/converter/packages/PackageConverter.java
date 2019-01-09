package org.folio.rest.converter.packages;

import static org.folio.rest.converter.packages.PackageConverterUtils.createEmptyPackageRelationship;
import static org.folio.rest.util.RestConstants.PACKAGES_TYPE;
import static org.folio.rest.util.RestConstants.PROVIDERS_TYPE;
import static org.folio.rest.util.RestConstants.RESOURCES_TYPE;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.converter.Converter;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import org.folio.rest.jaxrs.model.HasManyRelationship;
import org.folio.rest.jaxrs.model.HasOneRelationship;
import org.folio.rest.jaxrs.model.MetaDataIncluded;
import org.folio.rest.jaxrs.model.Package;
import org.folio.rest.jaxrs.model.PackageCollectionItem;
import org.folio.rest.jaxrs.model.PackageRelationship;
import org.folio.rest.jaxrs.model.Provider;
import org.folio.rest.jaxrs.model.Proxy;
import org.folio.rest.jaxrs.model.RelationshipData;
import org.folio.rest.jaxrs.model.ResourceCollection;
import org.folio.rest.jaxrs.model.Token;
import org.folio.rest.util.RestConstants;
import org.folio.rmapi.model.PackageByIdData;
import org.folio.rmapi.model.PackageData;
import org.folio.rmapi.model.Titles;
import org.folio.rmapi.model.TokenInfo;
import org.folio.rmapi.model.VendorById;
import org.folio.rmapi.result.PackageResult;

@Component
public class PackageConverter implements Converter<PackageResult, Package> {

  @Autowired
  private Converter<PackageData, PackageCollectionItem> packageCollectionItemConverter;

  @Autowired
  private Converter<VendorById, Provider> vendorConverter;
  @Autowired
  private Converter<Titles, ResourceCollection> resourcesConverter;
  @Autowired
  private Converter<TokenInfo, Token> tokenInfoConverter;


  @Override
  public Package convert(@NonNull PackageResult result) {
    PackageByIdData packageByIdData = result.getPackageData();
    Titles titles = result.getTitles();
    VendorById vendor = result.getVendor();

    Package packageData = new Package()
      .withData(packageCollectionItemConverter.convert(packageByIdData))
      .withJsonapi(RestConstants.JSONAPI);

    packageData.getData()
      .withRelationships(createEmptyPackageRelationship())
      .withType(PACKAGES_TYPE)
      .getAttributes()
      .withProxy(convertToProxy(packageByIdData.getProxy()))
      .withPackageToken(tokenInfoConverter.convert(packageByIdData.getPackageToken()));

    if (titles != null) {
      packageData.getData()
        .withRelationships(new PackageRelationship()
          .withResources(new HasManyRelationship()
            .withMeta(new MetaDataIncluded()
              .withIncluded(true))
            .withData(convertResourcesRelationship(packageByIdData, titles))));

      packageData
        .getIncluded()
        .addAll(resourcesConverter.convert(titles).getData());
    }

    if (vendor != null) {
      packageData.getIncluded().add(vendorConverter.convert(vendor).getData());
      packageData.getData()
        .getRelationships()
        .withProvider(new HasOneRelationship()
          .withData(new RelationshipData()
            .withId(String.valueOf(vendor.getVendorId()))
            .withType(PROVIDERS_TYPE)));
    }

    return packageData;
  }

  private Proxy convertToProxy(org.folio.rmapi.model.Proxy proxy) {
    return proxy != null ? new Proxy().withId(proxy.getId())
      .withInherited(proxy.getInherited()) : null;
  }

  private List<RelationshipData> convertResourcesRelationship(PackageByIdData packageByIdData, Titles titles) {
    return titles.getTitleList().stream()
      .map(title ->
        new RelationshipData()
          .withId(packageByIdData.getVendorId() + "-" + packageByIdData.getPackageId() + "-" + title.getTitleId())
          .withType(RESOURCES_TYPE))
      .collect(Collectors.toList());
  }
}