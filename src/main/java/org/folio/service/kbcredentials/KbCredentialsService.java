package org.folio.service.kbcredentials;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.folio.rest.jaxrs.model.KbCredentials;
import org.folio.rest.jaxrs.model.KbCredentialsCollection;
import org.folio.rest.jaxrs.model.KbCredentialsPostRequest;

public interface KbCredentialsService {

  CompletableFuture<KbCredentialsCollection> findAll(Map<String, String> okapiHeaders);

  CompletableFuture<KbCredentials> save(KbCredentialsPostRequest entity, Map<String, String> okapiHeaders);
}
