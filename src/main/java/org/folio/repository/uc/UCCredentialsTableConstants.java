package org.folio.repository.uc;

import static org.folio.repository.SqlQueryHelper.limitQuery;
import static org.folio.repository.SqlQueryHelper.selectQuery;

public final class UCCredentialsTableConstants {

  public static final String UC_CREDENTIALS_TABLE_NAME = "usage_consolidation_credentials";

  public static final String CLIENT_ID_COLUMN = "client_id";
  public static final String CLIENT_SECRET_COLUMN = "client_secret";

  public static final String SELECT_UC_CREDENTIALS;

  static {
    SELECT_UC_CREDENTIALS = selectQuery() + " " + limitQuery(1) + ";";
  }

  private UCCredentialsTableConstants() {
  }
}
