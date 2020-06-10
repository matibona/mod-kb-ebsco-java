package org.folio.repository.assigneduser;

import static org.folio.repository.SqlQueryHelper.count;
import static org.folio.repository.SqlQueryHelper.deleteQuery;
import static org.folio.repository.SqlQueryHelper.insertQuery;
import static org.folio.repository.SqlQueryHelper.selectQuery;
import static org.folio.repository.SqlQueryHelper.updateOnConflictedIdQuery;
import static org.folio.repository.SqlQueryHelper.updateQuery;
import static org.folio.repository.SqlQueryHelper.whereQuery;

public class AssignedUsersConstants {

  public static final String ASSIGNED_USERS_TABLE_NAME = "assigned_users";

  public static final String ID_COLUMN = "id";
  public static final String CREDENTIALS_ID = "credentials_id";
  public static final String USER_NAME = "user_name";
  public static final String FIRST_NAME = "first_name";
  public static final String MIDDLE_NAME = "middle_name";
  public static final String LAST_NAME = "last_name";
  public static final String PATRON_GROUP = "patron_group";

  public static final String SELECT_ASSIGNED_USERS_BY_CREDENTIALS_ID_QUERY;
  public static final String SELECT_ASSIGNED_USERS_BY_CREDENTIALS_ID_AND_USERS_ID_QUERY;
  public static final String SELECT_COUNT_BY_CREDENTIALS_ID_QUERY;
  public static final String UPSERT_ASSIGNED_USERS_QUERY;
  public static final String INSERT_ASSIGNED_USER_QUERY;
  public static final String UPDATE_ASSIGNED_USER_QUERY;
  public static final String DELETE_ASSIGNED_USER_QUERY;

  static {
    String[] allColumns = new String[] {
      ID_COLUMN, CREDENTIALS_ID, USER_NAME, FIRST_NAME, MIDDLE_NAME, LAST_NAME, PATRON_GROUP
    };
    String[] updateColumns = new String[] {
      USER_NAME, FIRST_NAME, MIDDLE_NAME, LAST_NAME, PATRON_GROUP
    };

    SELECT_ASSIGNED_USERS_BY_CREDENTIALS_ID_QUERY = selectQuery() + " " + whereQuery(CREDENTIALS_ID) + ";";
    SELECT_ASSIGNED_USERS_BY_CREDENTIALS_ID_AND_USERS_ID_QUERY = selectQuery() + " " +
      whereQuery(ID_COLUMN, CREDENTIALS_ID) + ";";
    SELECT_COUNT_BY_CREDENTIALS_ID_QUERY = selectQuery(count()) + " " + whereQuery(CREDENTIALS_ID);

    UPSERT_ASSIGNED_USERS_QUERY = insertQuery(allColumns) + " " + updateOnConflictedIdQuery(ID_COLUMN, allColumns) + ";";
    INSERT_ASSIGNED_USER_QUERY = insertQuery(allColumns) + ";";
    UPDATE_ASSIGNED_USER_QUERY = updateQuery(updateColumns) + " " + whereQuery(ID_COLUMN, CREDENTIALS_ID) + ";";
    DELETE_ASSIGNED_USER_QUERY = deleteQuery() + " " + whereQuery(CREDENTIALS_ID, ID_COLUMN) + ";";
  }

  private AssignedUsersConstants() {
  }
}