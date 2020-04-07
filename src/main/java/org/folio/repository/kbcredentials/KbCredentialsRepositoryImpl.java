package org.folio.repository.kbcredentials;

import static java.lang.String.format;
import static java.util.Arrays.asList;

import static org.folio.common.FutureUtils.mapResult;
import static org.folio.common.ListUtils.mapItems;
import static org.folio.db.DbUtils.createParams;
import static org.folio.repository.DbUtil.getKbCredentialsTableName;
import static org.folio.repository.kbcredentials.KbCredentialsTableConstants.API_KEY_COLUMN;
import static org.folio.repository.kbcredentials.KbCredentialsTableConstants.CREATED_BY_USER_ID_COLUMN;
import static org.folio.repository.kbcredentials.KbCredentialsTableConstants.CREATED_BY_USER_NAME_COLUMN;
import static org.folio.repository.kbcredentials.KbCredentialsTableConstants.CREATED_DATE_COLUMN;
import static org.folio.repository.kbcredentials.KbCredentialsTableConstants.CUSTOMER_ID_COLUMN;
import static org.folio.repository.kbcredentials.KbCredentialsTableConstants.ID_COLUMN;
import static org.folio.repository.kbcredentials.KbCredentialsTableConstants.INSERT_CREDENTIALS_QUERY;
import static org.folio.repository.kbcredentials.KbCredentialsTableConstants.NAME_COLUMN;
import static org.folio.repository.kbcredentials.KbCredentialsTableConstants.SELECT_CREDENTIALS_BY_ID_QUERY;
import static org.folio.repository.kbcredentials.KbCredentialsTableConstants.SELECT_CREDENTIALS_QUERY;
import static org.folio.repository.kbcredentials.KbCredentialsTableConstants.UPDATED_BY_USER_ID_COLUMN;
import static org.folio.repository.kbcredentials.KbCredentialsTableConstants.UPDATED_BY_USER_NAME_COLUMN;
import static org.folio.repository.kbcredentials.KbCredentialsTableConstants.UPDATED_DATE_COLUMN;
import static org.folio.repository.kbcredentials.KbCredentialsTableConstants.URL_COLUMN;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.sql.ResultSet;
import io.vertx.ext.sql.UpdateResult;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import org.folio.db.exc.Constraint;
import org.folio.db.exc.ConstraintViolationException;
import org.folio.db.exc.translation.DBExceptionTranslator;
import org.folio.rest.exception.InputValidationException;
import org.folio.rest.persist.PostgresClient;

@Component
public class KbCredentialsRepositoryImpl implements KbCredentialsRepository {

  private static final Logger LOG = LoggerFactory.getLogger(KbCredentialsRepositoryImpl.class);

  private static final String SELECT_LOG_MESSAGE = "Do select query = {}";
  private static final String INSERT_LOG_MESSAGE = "Do insert query = {}";

  private static final String CREDENTIALS_NAME_UNIQUENESS_MESSAGE = "Duplicate name";
  private static final String CREDENTIALS_NAME_UNIQUENESS_DETAILS = "Credentials with name '%s' already exist";

  @Autowired
  private Vertx vertx;
  @Autowired
  private DBExceptionTranslator excTranslator;

  @Override
  public CompletableFuture<Collection<DbKbCredentials>> findAll(String tenant) {
    String query = format(SELECT_CREDENTIALS_QUERY, getKbCredentialsTableName(tenant));

    LOG.info(SELECT_LOG_MESSAGE, query);
    Promise<ResultSet> promise = Promise.promise();
    pgClient(tenant).select(query, promise);

    return mapResult(promise.future().recover(excTranslator.translateOrPassBy()), this::mapCredentialsCollection);
  }

  @Override
  public CompletableFuture<Optional<DbKbCredentials>> findById(String id, String tenant) {
    String query = format(SELECT_CREDENTIALS_BY_ID_QUERY, getKbCredentialsTableName(tenant));

    LOG.info(SELECT_LOG_MESSAGE, query);
    Promise<ResultSet> promise = Promise.promise();
    pgClient(tenant).select(query, createParams(Collections.singleton(id)), promise);

    return mapResult(promise.future().recover(excTranslator.translateOrPassBy()), this::mapSingleCredentials);
  }

  @Override
  public CompletableFuture<DbKbCredentials> save(DbKbCredentials credentials, String tenant) {
    String query = format(INSERT_CREDENTIALS_QUERY, getKbCredentialsTableName(tenant));

    String id = credentials.getId();
    if (StringUtils.isBlank(id)) {
      id = UUID.randomUUID().toString();
    }
    JsonArray params = createParams(asList(
      id,
      credentials.getUrl(),
      credentials.getName(),
      credentials.getApiKey(),
      credentials.getCustomerId(),
      credentials.getCreatedDate(),
      credentials.getCreatedByUserId(),
      credentials.getCreatedByUserName()
    ));

    LOG.info(INSERT_LOG_MESSAGE, query);
    Promise<UpdateResult> promise = Promise.promise();
    pgClient(tenant).execute(query, params, promise);

    Future<UpdateResult> resultFuture = promise.future()
      .recover(excTranslator.translateOrPassBy())
      .recover(uniqueNameConstraintViolation(credentials.getName()));
    return mapResult(resultFuture, setId(credentials, id));
  }

  private Collection<DbKbCredentials> mapCredentialsCollection(ResultSet resultSet) {
    return mapItems(resultSet.getRows(), this::mapCredentials);
  }

  private Optional<DbKbCredentials> mapSingleCredentials(ResultSet resultSet) {
    List<JsonObject> rows = resultSet.getRows();
    return rows.isEmpty() ? Optional.empty() : Optional.of(mapCredentials(rows.get(0)));
  }

  private DbKbCredentials mapCredentials(JsonObject row) {
    return DbKbCredentials.builder()
      .id(row.getString(ID_COLUMN))
      .url(row.getString(URL_COLUMN))
      .name(row.getString(NAME_COLUMN))
      .apiKey(row.getString(API_KEY_COLUMN))
      .customerId(row.getString(CUSTOMER_ID_COLUMN))
      .createdDate(row.getInstant(CREATED_DATE_COLUMN))
      .updatedDate(row.getInstant(UPDATED_DATE_COLUMN))
      .createdByUserId(row.getString(CREATED_BY_USER_ID_COLUMN))
      .updatedByUserId(row.getString(UPDATED_BY_USER_ID_COLUMN))
      .createdByUserName(row.getString(CREATED_BY_USER_NAME_COLUMN))
      .updatedByUserName(row.getString(UPDATED_BY_USER_NAME_COLUMN))
      .build();
  }

  private Function<Throwable, Future<UpdateResult>> uniqueNameConstraintViolation(String value) {
    return throwable -> {
      if (throwable instanceof ConstraintViolationException) {
        Constraint constraint = ((ConstraintViolationException) throwable).getConstraint();
        if (constraint.getType() == Constraint.Type.UNIQUE && constraint.getColumns().contains(NAME_COLUMN)) {
          return Future.failedFuture(new InputValidationException(
            CREDENTIALS_NAME_UNIQUENESS_MESSAGE,
            format(CREDENTIALS_NAME_UNIQUENESS_DETAILS, value)));
        }
      }
      return Future.failedFuture(throwable);
    };
  }

  private Function<UpdateResult, DbKbCredentials> setId(DbKbCredentials credentials, String id) {
    return updateResult -> credentials.toBuilder().id(id).build();
  }

  private PostgresClient pgClient(String tenantId) {
    return PostgresClient.getInstance(vertx, tenantId);
  }
}
