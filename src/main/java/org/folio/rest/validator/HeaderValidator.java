package org.folio.rest.validator;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import javax.validation.ValidationException;

import org.springframework.stereotype.Component;

import org.folio.rest.util.RestConstants;

/**
 * Verifies that headers are valid
 */
@Component
public class HeaderValidator {

  private final Collection<String> expectedHeaders = Collections.singletonList(
    RestConstants.OKAPI_URL_HEADER
  );

  /**
   * @param okapiHeaders request headers
   * @throws ValidationException if validation failed
   */
  public void validate(Map<String, String> okapiHeaders) {
    for (String header : expectedHeaders) {
      if (!okapiHeaders.containsKey(header)) {
        throw new ValidationException("Missing header " + header);
      }
    }
  }
}
