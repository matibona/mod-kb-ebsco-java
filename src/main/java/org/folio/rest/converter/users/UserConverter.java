package org.folio.rest.converter.users;

import static org.folio.db.RowSetUtils.fromUUID;
import static org.folio.db.RowSetUtils.toUUID;

import org.jetbrains.annotations.NotNull;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import org.folio.repository.users.DbUser;
import org.folio.rest.jaxrs.model.AssignedUser;
import org.folio.service.users.User;

public class UserConverter {

  private UserConverter() {

  }

  @Component
  public static class FromDb implements Converter<DbUser, User> {

    @Override
    public User convert(@NotNull DbUser source) {
      return User.builder()
        .id(fromUUID(source.getId()))
        .lastName(source.getLastName())
        .middleName(source.getMiddleName())
        .firstName(source.getFirstName())
        .patronGroup(source.getPatronGroup())
        .build();
    }
  }

  @Component
  public static class ToDb implements Converter<User, DbUser> {

    @Override
    public DbUser convert(@NotNull User source) {
      return DbUser.builder()
        .id(toUUID(source.getId()))
        .username(source.getUserName())
        .lastName(source.getLastName())
        .middleName(source.getMiddleName())
        .firstName(source.getFirstName())
        .patronGroup(source.getPatronGroup())
        .build();
    }
  }

  @Component
  public static class FromAssignedUser implements Converter<AssignedUser, User> {

    @Override
    public User convert(@NotNull AssignedUser source) {
      return User.builder()
        .id(source.getId())
        .userName(source.getAttributes().getUserName())
        .lastName(source.getAttributes().getLastName())
        .middleName(source.getAttributes().getMiddleName())
        .firstName(source.getAttributes().getFirstName())
        .patronGroup(source.getAttributes().getPatronGroup())
        .build();
    }
  }
}
