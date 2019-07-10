package org.folio.repository.resources;


import static org.hamcrest.Matchers.empty;
import static org.junit.Assert.assertThat;

import static org.folio.rest.impl.PackagesTestData.STUB_PACKAGE_ID;
import static org.folio.util.TestUtil.STUB_TENANT;

import java.util.Collections;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import org.folio.spring.config.TestConfig;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = TestConfig.class)
public class ResourcesRepositoryImplTest {
  @Autowired
  ResourceRepository repository;

  @Test
  public void shouldReturnEmptyListWhenTagListIsEmpty() {
    List<ResourceInfoInDB> resources = repository.findByTagNameAndPackageId(Collections.emptyList(), STUB_PACKAGE_ID, 1, 25, STUB_TENANT).join();
    assertThat(resources, empty());
  }
}