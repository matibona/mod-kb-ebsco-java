package org.folio.repository.kbcredentials;

import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface KbCredentialsRepository {

  CompletableFuture<Collection<DbKbCredentials>> findAll(String tenant);

  CompletableFuture<Optional<DbKbCredentials>> findById(String id, String tenant);

  CompletableFuture<DbKbCredentials> save(DbKbCredentials credentials, String tenant);
}
