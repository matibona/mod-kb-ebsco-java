package org.folio.util;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;

import static org.folio.test.util.TestUtil.STUB_TENANT;
import static org.folio.test.util.TestUtil.getFile;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.matching.EqualToPattern;
import com.github.tomakehurst.wiremock.matching.UrlPathPattern;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.DeliveryContext;
import io.vertx.core.eventbus.Message;

import org.folio.rest.jaxrs.model.Configs;
import org.folio.rest.persist.PostgresClient;
import org.folio.service.holdings.message.LoadHoldingsMessage;

/**
 * Contains common methods that are used in mod-kb-ebsco-java
 */
public final class KBTestUtil {

  private KBTestUtil() {
  }

  /**
   * Mocks wiremock server to return RM API configuration from specified file,
   * RM API url will be changed to wiremockUrl so that following requests to RM API will be sent to wiremock instead
   *
   * @param configurationsFile configuration file, first config object must contain url config
   * @param wiremockUrl        wiremock url with port
   */
  public static void mockConfiguration(String configurationsFile, String wiremockUrl) throws IOException, URISyntaxException {
    ObjectMapper mapper = new ObjectMapper();
    Configs configurations = mapper.readValue(getFile(configurationsFile), Configs.class);
    if (!configurations.getConfigs().isEmpty()) {
      configurations.getConfigs().get(0).setValue(wiremockUrl);
    }

    stubFor(get(new UrlPathPattern(new EqualToPattern("/configurations/entries"), false))
      .willReturn(new ResponseDefinitionBuilder()
        .withBody(mapper.writeValueAsString(configurations))));
  }

  /**
   * Mocks wiremock server to return empty test RM API configuration from specified file,
   * RM API url will be changed to wiremockUrl so that following requests to RM API will be sent to wiremock instead
   *
   * @param wiremockUrl wiremock url with port
   */
  public static void mockEmptyConfiguration(String wiremockUrl) throws IOException, URISyntaxException {
    String emptyConfiguration = "responses/kb-ebsco/configuration/get-configuration-empty.json";
    mockConfiguration(emptyConfiguration, wiremockUrl);
  }

  /**
   * Mocks wiremock server to return default test RM API configuration from specified file,
   * RM API url will be changed to wiremockUrl so that following requests to RM API will be sent to wiremock instead
   *
   * @param wiremockUrl wiremock url with port
   */
  public static void mockDefaultConfiguration(String wiremockUrl) throws IOException, URISyntaxException {
    String configurationsFile = "responses/kb-ebsco/configuration/get-configuration.json";
    mockConfiguration(configurationsFile, wiremockUrl);
  }

  public static void clearDataFromTable(Vertx vertx, String tableName) {
    CompletableFuture<Void> future = new CompletableFuture<>();
    PostgresClient.getInstance(vertx).execute(
      "DELETE FROM " + (PostgresClient.convertToPsqlStandard(STUB_TENANT) + "." + tableName),
      event -> future.complete(null));
    future.join();
  }

  public static Handler<DeliveryContext<LoadHoldingsMessage>> interceptAndContinue(String serviceAddress, String serviceMethodName,
                                                                                   Consumer<Message> messageConsumer) {
    return messageContext -> {
      Message message = messageContext.message();
      if (messageMatches(serviceAddress, serviceMethodName, message)) {
        messageConsumer.accept(message);
        messageContext.next();
      } else {
        messageContext.next();
      }
    };
  }

  public static Handler<DeliveryContext<LoadHoldingsMessage>> interceptAndStop(String serviceAddress, String serviceMethodName,
                                                      Consumer<Message> messageConsumer) {
    return messageContext -> {
      Message message = messageContext.message();
      if (messageMatches(serviceAddress, serviceMethodName, message)) {
        messageConsumer.accept(message);
      } else {
        messageContext.next();
      }
    };
  }

  private static boolean messageMatches(String serviceAddress, String serviceMethodName, Message message) {
    return serviceAddress.equals(message.address())
      && serviceMethodName.equals(message.headers().get("action"));
  }
}
