/*
 *  Copyright 2021 Collate
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *  http://www.apache.org/licenses/LICENSE-2.0
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.openmetadata.service.jdbi3;

import static org.openmetadata.common.utils.CommonUtil.listOrEmpty;
import static org.openmetadata.common.utils.CommonUtil.nullOrEmpty;

import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.openmetadata.schema.entity.domains.DataProduct;
import org.openmetadata.schema.type.EntityReference;
import org.openmetadata.schema.type.Relationship;
import org.openmetadata.service.Entity;
import org.openmetadata.service.resources.domains.DataProductResource;
import org.openmetadata.service.util.EntityUtil.Fields;
import org.openmetadata.service.util.FullyQualifiedName;

@Slf4j
public class DataProductRepository extends EntityRepository<DataProduct> {
  private static final String UPDATE_FIELDS = "experts"; // Domain field can't be updated

  public DataProductRepository(CollectionDAO dao) {
    super(
        DataProductResource.COLLECTION_PATH,
        Entity.DATA_PRODUCT,
        DataProduct.class,
        dao.dataProductDAO(),
        dao,
        UPDATE_FIELDS,
        UPDATE_FIELDS);
  }

  @Override
  public DataProduct setFields(DataProduct entity, Fields fields) {
    return entity.withExperts(fields.contains("experts") ? getExperts(entity) : entity.getExperts());
  }

  @Override
  public DataProduct clearFields(DataProduct entity, Fields fields) {
    return entity.withExperts(fields.contains("experts") ? entity.getExperts() : null);
  }

  // TODO to to inheritance for experts
  private List<EntityReference> getExperts(DataProduct entity) {
    return !nullOrEmpty(entity.getExperts())
        ? entity.getExperts()
        : findTo(entity.getId(), Entity.DATA_PRODUCT, Relationship.EXPERT, Entity.USER);
  }

  @Override
  public void prepare(DataProduct entity) {
    // Parent, Experts, Owner are already validated
  }

  @Override
  public void storeEntity(DataProduct entity, boolean update) {
    List<EntityReference> experts = entity.getExperts();
    entity.withExperts(null);
    store(entity, update);
    entity.withExperts(experts);
  }

  @Override
  public void storeRelationships(DataProduct entity) {
    addRelationship(
        entity.getDomain().getId(), entity.getId(), Entity.DOMAIN, Entity.DATA_PRODUCT, Relationship.CONTAINS);
    for (EntityReference expert : listOrEmpty(entity.getExperts())) {
      addRelationship(entity.getId(), expert.getId(), Entity.DATA_PRODUCT, Entity.USER, Relationship.EXPERT);
    }
  }

  @Override
  public EntityUpdater getUpdater(DataProduct original, DataProduct updated, Operation operation) {
    return new DataProductUpdater(original, updated, operation);
  }

  @Override
  public void restorePatchAttributes(DataProduct original, DataProduct updated) {
    updated.withDomain(original.getDomain()); // Domain can't be changed
  }

  @Override
  public void setFullyQualifiedName(DataProduct entity) {
    EntityReference domain = entity.getDomain();
    entity.setFullyQualifiedName(FullyQualifiedName.add(domain.getFullyQualifiedName(), entity.getName()));
  }

  public class DataProductUpdater extends EntityUpdater {
    public DataProductUpdater(DataProduct original, DataProduct updated, Operation operation) {
      super(original, updated, operation);
    }

    @Override
    public void entitySpecificUpdate() {
      updateExperts();
    }

    private void updateExperts() {
      List<EntityReference> origExperts = listOrEmpty(original.getExperts());
      List<EntityReference> updatedExperts = listOrEmpty(updated.getExperts());
      updateToRelationships(
          "experts",
          Entity.DATA_PRODUCT,
          original.getId(),
          Relationship.EXPERT,
          Entity.USER,
          origExperts,
          updatedExperts,
          false);
    }
  }
}
