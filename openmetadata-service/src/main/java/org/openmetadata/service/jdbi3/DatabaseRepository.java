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

import static org.openmetadata.common.utils.CommonUtil.nullOrEmpty;
import static org.openmetadata.schema.type.Include.ALL;
import static org.openmetadata.service.Entity.DATABASE_SERVICE;
import static org.openmetadata.service.Entity.FIELD_DOMAIN;

import java.util.List;
import org.openmetadata.schema.entity.data.Database;
import org.openmetadata.schema.entity.services.DatabaseService;
import org.openmetadata.schema.type.EntityReference;
import org.openmetadata.schema.type.Include;
import org.openmetadata.schema.type.Relationship;
import org.openmetadata.service.Entity;
import org.openmetadata.service.resources.databases.DatabaseResource;
import org.openmetadata.service.util.EntityUtil;
import org.openmetadata.service.util.EntityUtil.Fields;
import org.openmetadata.service.util.FullyQualifiedName;

public class DatabaseRepository extends EntityRepository<Database> {
  public DatabaseRepository(CollectionDAO dao) {
    super(DatabaseResource.COLLECTION_PATH, Entity.DATABASE, Database.class, dao.databaseDAO(), dao, "", "");
  }

  @Override
  public void setFullyQualifiedName(Database database) {
    database.setFullyQualifiedName(FullyQualifiedName.build(database.getService().getName(), database.getName()));
  }

  @Override
  public void prepare(Database database) {
    populateService(database);
  }

  @Override
  public void storeEntity(Database database, boolean update) {
    // Relationships and fields such as service are not stored as part of json
    EntityReference service = database.getService();
    database.withService(null);
    store(database, update);
    database.withService(service);
  }

  @Override
  public void storeRelationships(Database database) {
    EntityReference service = database.getService();
    addRelationship(service.getId(), database.getId(), service.getType(), Entity.DATABASE, Relationship.CONTAINS);
  }

  @Override
  public Database setInheritedFields(Database database, Fields fields) {
    // If database does not have domain, then inherit it from parent database service
    if (fields.contains(FIELD_DOMAIN) && database.getDomain() == null) {
      DatabaseService service = Entity.getEntity(DATABASE_SERVICE, database.getService().getId(), "domain", ALL);
      database.withDomain(service.getDomain());
    }
    return database;
  }

  private List<EntityReference> getSchemas(Database database) {
    if (database == null) {
      return null;
    }
    return !nullOrEmpty(database.getDatabaseSchemas())
        ? database.getDatabaseSchemas()
        : findTo(database.getId(), Entity.DATABASE, Relationship.CONTAINS, Entity.DATABASE_SCHEMA);
  }

  public Database setFields(Database database, Fields fields) {
    if (database.getService() == null) {
      database.setService(getContainer(database.getId()));
    }
    database.setDatabaseSchemas(
        fields.contains("databaseSchemas") ? getSchemas(database) : database.getDatabaseSchemas());
    if (database.getUsageSummary() == null) {
      database.setUsageSummary(
          fields.contains("usageSummary")
              ? EntityUtil.getLatestUsage(daoCollection.usageDAO(), database.getId())
              : null);
    }
    return database;
  }

  public Database clearFields(Database database, Fields fields) {
    database.setDatabaseSchemas(fields.contains("databaseSchemas") ? database.getDatabaseSchemas() : null);
    return database.withUsageSummary(fields.contains("usageSummary") ? database.getUsageSummary() : null);
  }

  @Override
  public void restorePatchAttributes(Database original, Database updated) {
    // Patch can't make changes to following fields. Ignore the changes
    updated
        .withFullyQualifiedName(original.getFullyQualifiedName())
        .withName(original.getName())
        .withService(original.getService())
        .withId(original.getId());
  }

  @Override
  public EntityRepository<Database>.EntityUpdater getUpdater(Database original, Database updated, Operation operation) {
    return new DatabaseUpdater(original, updated, operation);
  }

  private void populateService(Database database) {
    DatabaseService service = Entity.getEntity(database.getService(), "", Include.NON_DELETED);
    database.setService(service.getEntityReference());
    database.setServiceType(service.getServiceType());
  }

  public class DatabaseUpdater extends EntityUpdater {
    public DatabaseUpdater(Database original, Database updated, Operation operation) {
      super(original, updated, operation);
    }

    @Override
    public void entitySpecificUpdate() {
      recordChange("retentionPeriod", original.getRetentionPeriod(), updated.getRetentionPeriod());
    }
  }
}
