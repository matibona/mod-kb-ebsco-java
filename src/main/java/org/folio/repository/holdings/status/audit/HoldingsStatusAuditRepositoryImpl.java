package org.folio.repository.holdings.status.audit;

import static org.folio.common.FunctionUtils.nothing;
import static org.folio.common.LogUtils.logDeleteQueryDebugLevel;
import static org.folio.repository.DbUtil.getHoldingsStatusAuditTableName;
import static org.folio.repository.DbUtil.prepareQuery;
import static org.folio.repository.holdings.status.audit.HoldingsStatusAuditTableConstants.DELETE_BEFORE_TIMESTAMP_FOR_CREDENTIALS;
import static org.folio.util.FutureUtils.mapVertxFuture;

import java.time.OffsetDateTime;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.Tuple;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import org.folio.rest.persist.PostgresClient;

@Component
public class HoldingsStatusAuditRepositoryImpl implements HoldingsStatusAuditRepository {

  private static final Logger LOG = LoggerFactory.getLogger(HoldingsStatusAuditRepositoryImpl.class);

  private final Vertx vertx;

  @Autowired
  public HoldingsStatusAuditRepositoryImpl(Vertx vertx) {
    this.vertx = vertx;
  }

  @Override
  public CompletableFuture<Void> deleteBeforeTimestamp(OffsetDateTime timestamp, UUID credentialsId, String tenantId) {
    String query = prepareQuery(DELETE_BEFORE_TIMESTAMP_FOR_CREDENTIALS, getHoldingsStatusAuditTableName(tenantId));
    Tuple params = Tuple.of(timestamp, credentialsId);
    logDeleteQueryDebugLevel(LOG, query, params);
    Promise<RowSet<Row>> promise = Promise.promise();
    PostgresClient.getInstance(vertx, tenantId).execute(query, params, promise);
    return mapVertxFuture(promise.future()).thenApply(nothing());
  }
}
