package org.folio.repository.holdings.status;

import static org.folio.common.ListUtils.createPlaceholders;
import static org.folio.db.DbUtils.executeInTransaction;
import static org.folio.repository.DbUtil.getHoldingsStatusTableName;
import static org.folio.repository.DbUtil.mapColumn;
import static org.folio.repository.holdings.status.HoldingsStatusTableConstants.DELETE_LOADING_STATUS;
import static org.folio.repository.holdings.status.HoldingsStatusTableConstants.GET_HOLDINGS_STATUS;
import static org.folio.repository.holdings.status.HoldingsStatusTableConstants.INSERT_LOADING_STATUS;
import static org.folio.repository.holdings.status.HoldingsStatusTableConstants.UPDATE_IMPORTED_COUNT;
import static org.folio.repository.holdings.status.HoldingsStatusTableConstants.UPDATE_LOADING_STATUS;
import static org.folio.util.FutureUtils.mapResult;
import static org.folio.util.FutureUtils.mapVertxFuture;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import javax.annotation.Nullable;

import io.vertx.core.AsyncResult;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.sql.ResultSet;
import io.vertx.ext.sql.SQLConnection;
import io.vertx.ext.sql.UpdateResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import org.folio.common.VertxIdProvider;
import org.folio.rest.jaxrs.model.HoldingsLoadingStatus;
import org.folio.rest.persist.PostgresClient;

@Component
public class HoldingsStatusRepositoryImpl implements HoldingsStatusRepository {
  private static final Logger LOG = LoggerFactory.getLogger(HoldingsStatusRepositoryImpl.class);

  private Vertx vertx;
  private VertxIdProvider vertxIdProvider;

  @Autowired
  public HoldingsStatusRepositoryImpl(Vertx vertx, VertxIdProvider vertxIdProvider) {
    this.vertx = vertx;
    this.vertxIdProvider = vertxIdProvider;
  }

  @Override
  public CompletableFuture<HoldingsLoadingStatus> get(String tenantId) {
    return get(tenantId, null);
  }

  @Override
  public CompletableFuture<Void> save(HoldingsLoadingStatus status, String tenantId) {

    final String query = String.format(INSERT_LOADING_STATUS, getHoldingsStatusTableName(tenantId), createPlaceholders(3));
    JsonArray parameters = new JsonArray().add(UUID.randomUUID().toString()).add(Json.encode(status)).add(vertxIdProvider.getVertxId());
    LOG.info("Do insert query = " + query);
    Promise<UpdateResult> promise = Promise.promise();
    PostgresClient postgresClient = PostgresClient.getInstance(vertx, tenantId);
    postgresClient.execute(query, parameters, promise);
    return mapVertxFuture(promise.future()).thenApply(result -> null);
  }

  @Override
  public CompletableFuture<Void> update(HoldingsLoadingStatus status, String tenantId) {
    final String query = String.format(UPDATE_LOADING_STATUS, getHoldingsStatusTableName(tenantId));
    String vertxId = vertxIdProvider.getVertxId();
    JsonArray parameters = new JsonArray().add(Json.encode(status)).add(vertxId);
    LOG.info("Do update query = " + query);
    Promise<UpdateResult> promise = Promise.promise();
    PostgresClient postgresClient = PostgresClient.getInstance(vertx, tenantId);
    postgresClient.execute(query, parameters, promise);
    return mapVertxFuture(promise.future())
      .thenApply(this::assertUpdated);
  }

  @Override
  public CompletableFuture<Void> delete(String tenantId) {
    final String query = String.format(DELETE_LOADING_STATUS, getHoldingsStatusTableName(tenantId));
    LOG.info("Do delete query = " + query);
    Promise<UpdateResult> promise = Promise.promise();
    PostgresClient postgresClient = PostgresClient.getInstance(vertx, tenantId);
    postgresClient.execute(query, promise);
    return mapVertxFuture(promise.future()).thenApply(result -> null);
  }

  @Override
  public CompletableFuture<HoldingsLoadingStatus> increaseImportedCount(int holdingsAmount, int pageAmount, String tenantId) {
    return executeInTransaction(tenantId, vertx, (postgresClient, connection) -> {
      final String query = String.format(UPDATE_IMPORTED_COUNT, getHoldingsStatusTableName(tenantId), holdingsAmount, pageAmount);
      String vertxId = vertxIdProvider.getVertxId();
      JsonArray parameters = new JsonArray().add(vertxId);
      LOG.info("Increment imported count query = " + query);
      Promise<UpdateResult> promise = Promise.promise();
      postgresClient.execute(connection, query, parameters, promise);
      return mapVertxFuture(promise.future())
        .thenApply(this::assertUpdated)
        .thenCompose(o -> get(tenantId, connection));
    });
  }

  private CompletableFuture<HoldingsLoadingStatus> get(String tenantId, @Nullable  AsyncResult<SQLConnection> connection) {
    final String query = String.format(GET_HOLDINGS_STATUS, getHoldingsStatusTableName(tenantId));
    PostgresClient postgresClient = PostgresClient.getInstance(vertx, tenantId);
    LOG.info("Select holdings loading status = " + query);
    Promise<ResultSet> promise = Promise.promise();
    if(connection != null) {
      postgresClient.select(connection, query, promise);
    }
    else{
      postgresClient.select(query, promise);
    }
    return mapResult(promise.future(), this::mapStatus);
  }

  private Void assertUpdated(UpdateResult result) {
    if(result.getUpdated() == 0){
      throw new IllegalArgumentException("Couldn't update holdings status");
    }
    return null;
  }

  private HoldingsLoadingStatus mapStatus(ResultSet resultSet) {
    return mapColumn(resultSet.getRows().get(0), "jsonb", HoldingsLoadingStatus.class).orElse(null);
  }
}
