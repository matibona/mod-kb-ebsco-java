package org.folio.rest.converter.tags;

import java.util.EnumMap;
import java.util.Map;

import org.springframework.core.convert.converter.Converter;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import org.folio.rest.jaxrs.model.HasOneRelationship;
import org.folio.rest.jaxrs.model.RelationshipData;
import org.folio.rest.jaxrs.model.TagCollectionItem;
import org.folio.rest.jaxrs.model.TagDataAttributes;
import org.folio.rest.jaxrs.model.TagRelationship;
import org.folio.rest.util.RestConstants;
import org.folio.tag.RecordType;
import org.folio.tag.Tag;

@Component
public class TagConverter implements Converter<Tag, TagCollectionItem> {

  private static final Map<RecordType, String> RECORD_TYPES = new EnumMap<>(RecordType.class);

  static {
    RECORD_TYPES.put(RecordType.PACKAGE, RestConstants.PACKAGES_TYPE);
    RECORD_TYPES.put(RecordType.PROVIDER, RestConstants.PROVIDERS_TYPE);
    RECORD_TYPES.put(RecordType.TITLE, RestConstants.TITLES_TYPE);
    RECORD_TYPES.put(RecordType.RESOURCE, RestConstants.RESOURCES_TYPE);
  }

  @Override
  public TagCollectionItem convert(@NonNull Tag source) {
    return new TagCollectionItem()
      .withType(RestConstants.TAGS_TYPE)
      .withId(source.getId())
      .withAttributes(createAttributes(source))
      .withRelationships(createRelationships(source));
  }

  private TagDataAttributes createAttributes(Tag source) {
    return new TagDataAttributes().withValue(source.getValue());
  }

  private TagRelationship createRelationships(Tag source) {
    return new TagRelationship().withRecord(
            new HasOneRelationship().withData(
                new RelationshipData()
                  .withId(source.getRecordId())
                  .withType(RECORD_TYPES.get(source.getRecordType()))));
  }

}