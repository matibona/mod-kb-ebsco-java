package org.folio.rest.util;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import org.apache.http.HttpStatus;
import org.folio.rest.exception.InputValidationException;
import org.folio.rmapi.exception.RMAPIServiceException;

import javax.ws.rs.core.Response;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Utility class for mapping exceptions to response that is passed to io.vertx.core.Handler
 *
 * ErrorHandler instance can be configured with error mappers for each Exception type by using add* methods,
 *
 * When {@link org.folio.rest.util.ErrorHandler#handle(io.vertx.core.Handler, java.lang.Throwable)} method is called,
 * first mapper that matches exception type is used to construct javax.ws.rs.core.Response and pass it to io.vertx.core.Handler
 *
 * Error mappers are checked in the order they were registered
 */
public class ErrorHandler {

  private static final String CONTENT_TYPE_HEADER = "Content-Type";
  private static final String CONTENT_TYPE_VALUE = "application/vnd.api+json";

  private Map<Class<? extends Throwable>, Function<? extends Throwable, Response>> errorMappers = new LinkedHashMap<>();

  /**
   * Register error mapper for exceptionClass
   * @param exceptionClass class of exception that this mapper will handle
   * @param errorMapper function that converts exception to javax.ws.rs.core.Response
   * @return this
   */
  public <T extends Throwable> ErrorHandler add(Class<T> exceptionClass, Function<T, Response> errorMapper) {
    errorMappers.put(exceptionClass, errorMapper);
    return this;
  }

  /**
   * Register error mapper for Throwable
   * @return this
   */
  public ErrorHandler addDefaultMapper() {
    add(Throwable.class, exception -> Response
      .status(HttpStatus.SC_INTERNAL_SERVER_ERROR)
      .header(CONTENT_TYPE_HEADER, CONTENT_TYPE_VALUE)
      .entity(ErrorUtil.createError(exception.getMessage())).build());
    return this;
  }

  /**
   * Register error mapper for InputValidationException
   * @return this
   */
  public ErrorHandler addInputValidationMapper() {
    add(InputValidationException.class, exception ->
      Response.status(HttpStatus.SC_BAD_REQUEST)
        .header(CONTENT_TYPE_HEADER, CONTENT_TYPE_VALUE)
        .entity(ErrorUtil.createError(exception.getMessage(), exception.getMessageDetail()))
        .build());
    return this;
  }

  /**
   * Register error mapper for RMAPIServiceException
   * @return this
   */
  public ErrorHandler addRmApiMapper() {
    add(RMAPIServiceException.class, exception -> Response
      .status(exception.getRMAPICode())
      .header(CONTENT_TYPE_HEADER, CONTENT_TYPE_VALUE)
      .entity(ErrorUtil.createErrorFromRMAPIResponse(exception))
      .build());
    return this;
  }

  /**
   * Use registered error mappers to create response from exception and pass it to asyncResultHandler
   */
  public void handle(Handler<AsyncResult<Response>> asyncResultHandler, Throwable e) {
    Function<Throwable, Response> errorMapper = (Function<Throwable, Response>) errorMappers.entrySet()
      .stream()
      .filter(entry -> entry.getKey().isInstance(e.getCause()))
      .findFirst()
      .orElseGet(null).getValue();

    asyncResultHandler.handle(Future.succeededFuture(errorMapper.apply(e.getCause())));
  }
}


