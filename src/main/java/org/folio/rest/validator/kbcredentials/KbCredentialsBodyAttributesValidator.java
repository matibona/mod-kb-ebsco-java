package org.folio.rest.validator.kbcredentials;

import org.folio.rest.jaxrs.model.KbCredentialsDataAttributes;
import org.folio.rest.validator.ValidatorUtil;

class KbCredentialsBodyAttributesValidator {

  private static final String URL_PARAMETER = "url";
  private static final String NAME_PARAMETER = "name";
  private static final String API_KEY_PARAMETER = "apiKey";
  private static final String CUSTOMER_ID_PARAMETER = "customerId";

  private final int nameLengthMax;

  protected KbCredentialsBodyAttributesValidator(int nameLengthMax) {
    this.nameLengthMax = nameLengthMax;
  }

  protected void validateAttributes(KbCredentialsDataAttributes attributes) {
    validateName(attributes);
    validateApiKey(attributes);
    validateCustomerId(attributes);
    validateUrl(attributes);
  }

  protected void validateCustomerId(KbCredentialsDataAttributes attributes) {
    ValidatorUtil.checkIsNotBlank(CUSTOMER_ID_PARAMETER, attributes.getCustomerId());
  }

  protected void validateApiKey(KbCredentialsDataAttributes attributes) {
    ValidatorUtil.checkIsNotBlank(API_KEY_PARAMETER, attributes.getApiKey());
  }

  protected void validateName(KbCredentialsDataAttributes attributes) {
    ValidatorUtil.checkIsNotBlank(NAME_PARAMETER, attributes.getName());
    ValidatorUtil.checkMaxLength(NAME_PARAMETER, attributes.getName(), nameLengthMax);
  }

  protected void validateUrl(KbCredentialsDataAttributes attributes) {
    ValidatorUtil.checkIsNotBlank(URL_PARAMETER, attributes.getUrl());
    ValidatorUtil.checkUrlFormat(URL_PARAMETER, attributes.getUrl());
  }
}
