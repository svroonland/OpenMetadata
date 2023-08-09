package org.openmetadata.service.jdbi3;

import static org.openmetadata.common.utils.CommonUtil.listOrEmpty;
import static org.openmetadata.common.utils.CommonUtil.nullOrEmpty;
import static org.openmetadata.schema.type.Include.ALL;
import static org.openmetadata.service.Entity.CONTAINER;
import static org.openmetadata.service.Entity.FIELD_TAGS;
import static org.openmetadata.service.Entity.STORAGE_SERVICE;

import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.json.JsonPatch;
import org.openmetadata.schema.EntityInterface;
import org.openmetadata.schema.entity.data.Container;
import org.openmetadata.schema.entity.services.StorageService;
import org.openmetadata.schema.type.Column;
import org.openmetadata.schema.type.ContainerFileFormat;
import org.openmetadata.schema.type.EntityReference;
import org.openmetadata.schema.type.Include;
import org.openmetadata.schema.type.Relationship;
import org.openmetadata.schema.type.TagLabel;
import org.openmetadata.schema.type.TaskDetails;
import org.openmetadata.service.Entity;
import org.openmetadata.service.exception.CatalogExceptionMessage;
import org.openmetadata.service.resources.feeds.MessageParser;
import org.openmetadata.service.resources.storages.ContainerResource;
import org.openmetadata.service.util.EntityUtil;
import org.openmetadata.service.util.FullyQualifiedName;
import org.openmetadata.service.util.JsonUtils;

public class ContainerRepository extends EntityRepository<Container> {
  private static final String CONTAINER_UPDATE_FIELDS = "dataModel";
  private static final String CONTAINER_PATCH_FIELDS = "dataModel";

  public ContainerRepository(CollectionDAO dao) {
    super(
        ContainerResource.COLLECTION_PATH,
        Entity.CONTAINER,
        Container.class,
        dao.containerDAO(),
        dao,
        CONTAINER_PATCH_FIELDS,
        CONTAINER_UPDATE_FIELDS);
  }

  @Override
  public Container setFields(Container container, EntityUtil.Fields fields) {
    setDefaultFields(container);
    container.setChildren(fields.contains("children") ? getChildrenContainers(container) : container.getChildren());
    container.setParent(fields.contains("parent") ? getParentContainer(container) : container.getParent());
    if (container.getDataModel() != null) {
      populateDataModelColumnTags(fields.contains(FIELD_TAGS), container.getDataModel().getColumns());
    }
    return container;
  }

  @Override
  public Container clearFields(Container container, EntityUtil.Fields fields) {
    container.setParent(fields.contains("parent") ? container.getParent() : null);
    return container.withDataModel(fields.contains("dataModel") ? container.getDataModel() : null);
  }

  private void populateDataModelColumnTags(boolean setTags, List<Column> columns) {
    for (Column c : listOrEmpty(columns)) {
      c.setTags(setTags ? getTags(c.getFullyQualifiedName()) : null);
      populateDataModelColumnTags(setTags, c.getChildren());
    }
  }

  private EntityReference getParentContainer(Container container) {
    if (container == null) return null;
    return container.getParent() != null
        ? container.getParent()
        : getFromEntityRef(container.getId(), Relationship.CONTAINS, CONTAINER, false);
  }

  private void setDefaultFields(Container container) {
    if (container.getService() != null) {
      {
        return;
      }
    }
    EntityReference parentServiceRef =
        getFromEntityRef(container.getId(), Relationship.CONTAINS, STORAGE_SERVICE, true);
    container.withService(parentServiceRef);
  }

  private List<EntityReference> getChildrenContainers(Container container) {
    if (container == null) {
      return Collections.emptyList();
    }
    return !nullOrEmpty(container.getChildren())
        ? container.getChildren()
        : findTo(container.getId(), CONTAINER, Relationship.CONTAINS, CONTAINER);
  }

  @Override
  public void setFullyQualifiedName(Container container) {
    if (container.getParent() != null) {
      container.setFullyQualifiedName(
          FullyQualifiedName.add(container.getParent().getFullyQualifiedName(), container.getName()));
    } else {
      container.setFullyQualifiedName(
          FullyQualifiedName.add(container.getService().getFullyQualifiedName(), container.getName()));
    }
    if (container.getDataModel() != null) {
      setColumnFQN(container.getFullyQualifiedName(), container.getDataModel().getColumns());
    }
  }

  private void setColumnFQN(String parentFQN, List<Column> columns) {
    columns.forEach(
        c -> {
          String columnFqn = FullyQualifiedName.add(parentFQN, c.getName());
          c.setFullyQualifiedName(columnFqn);
          if (c.getChildren() != null) {
            setColumnFQN(columnFqn, c.getChildren());
          }
        });
  }

  @Override
  public void prepare(Container container) {
    // the storage service is not fully filled in terms of props - go to the db and get it in full and re-set it
    StorageService storageService = Entity.getEntity(container.getService(), "", Include.NON_DELETED);
    container.setService(storageService.getEntityReference());
    container.setServiceType(storageService.getServiceType());

    if (container.getParent() != null) {
      Container parent = Entity.getEntity(container.getParent(), "owner", ALL);
      container.withParent(parent.getEntityReference());
    }
    // Validate field tags
    if (container.getDataModel() != null) {
      addDerivedColumnTags(container.getDataModel().getColumns());
      validateColumnTags(container.getDataModel().getColumns());
    }
  }

  @Override
  public void storeEntity(Container container, boolean update) {
    EntityReference storageService = container.getService();
    EntityReference parent = container.getParent();
    List<EntityReference> children = container.getChildren();

    container.withService(null).withParent(null).withChildren(null);

    // Don't store datamodel column tags as JSON but build it on the fly based on relationships
    List<Column> columnWithTags = Lists.newArrayList();
    if (container.getDataModel() != null) {
      columnWithTags.addAll(container.getDataModel().getColumns());
      container.getDataModel().setColumns(ColumnUtil.cloneWithoutTags(columnWithTags));
      container.getDataModel().getColumns().forEach(column -> column.setTags(null));
    }

    store(container, update);

    // Restore the relationships
    container.withService(storageService).withParent(parent).withChildren(children);
    if (container.getDataModel() != null) {
      container.getDataModel().setColumns(columnWithTags);
    }
  }

  @Override
  public void restorePatchAttributes(Container original, Container updated) {
    // Patch can't make changes to following fields. Ignore the changes
    updated
        .withFullyQualifiedName(original.getFullyQualifiedName())
        .withService(original.getService())
        .withParent(original.getParent())
        .withName(original.getName())
        .withId(original.getId());
  }

  @Override
  public void storeRelationships(Container container) {
    // store each relationship separately in the entity_relationship table
    EntityReference service = container.getService();
    addRelationship(service.getId(), container.getId(), service.getType(), CONTAINER, Relationship.CONTAINS);

    // parent container if exists
    EntityReference parentReference = container.getParent();
    if (parentReference != null) {
      addRelationship(parentReference.getId(), container.getId(), CONTAINER, CONTAINER, Relationship.CONTAINS);
    }
  }

  @Override
  public EntityUpdater getUpdater(Container original, Container updated, Operation operation) {
    return new ContainerUpdater(original, updated, operation);
  }

  @Override
  public void applyTags(Container container) {
    // Add container level tags by adding tag to container relationship
    super.applyTags(container);
    if (container.getDataModel() != null) {
      applyTags(container.getDataModel().getColumns());
    }
  }

  private void applyTags(List<Column> columns) {
    // Add column level tags by adding tag to column relationship
    for (Column column : columns) {
      applyTags(column.getTags(), column.getFullyQualifiedName());
      if (column.getChildren() != null) {
        applyTags(column.getChildren());
      }
    }
  }

  @Override
  public List<TagLabel> getAllTags(EntityInterface entity) {
    List<TagLabel> allTags = new ArrayList<>();
    Container container = (Container) entity;
    EntityUtil.mergeTags(allTags, container.getTags());
    if (container.getDataModel() != null) {
      for (Column column : listOrEmpty(container.getDataModel().getColumns())) {
        EntityUtil.mergeTags(allTags, column.getTags());
      }
    }
    return allTags;
  }

  @Override
  public void update(TaskDetails task, MessageParser.EntityLink entityLink, String newValue, String user) {
    // TODO move this as the first check
    if (entityLink.getFieldName().equals("dataModel")) {
      Container container = getByName(null, entityLink.getEntityFQN(), getFields("dataModel,tags"), Include.ALL, false);
      Column column =
          container.getDataModel().getColumns().stream()
              .filter(c -> c.getName().equals(entityLink.getArrayFieldName()))
              .findFirst()
              .orElseThrow(
                  () ->
                      new IllegalArgumentException(
                          CatalogExceptionMessage.invalidFieldName("column", entityLink.getArrayFieldName())));

      String origJson = JsonUtils.pojoToJson(container);
      if (EntityUtil.isDescriptionTask(task.getType())) {
        column.setDescription(newValue);
      } else if (EntityUtil.isTagTask(task.getType())) {
        List<TagLabel> tags = JsonUtils.readObjects(newValue, TagLabel.class);
        column.setTags(tags);
      }
      String updatedEntityJson = JsonUtils.pojoToJson(container);
      JsonPatch patch = JsonUtils.getJsonPatch(origJson, updatedEntityJson);
      patch(null, container.getId(), user, patch);
      return;
    }
    super.update(task, entityLink, newValue, user);
  }

  private void addDerivedColumnTags(List<Column> columns) {
    if (nullOrEmpty(columns)) {
      return;
    }

    for (Column column : columns) {
      column.setTags(addDerivedTags(column.getTags()));
      if (column.getChildren() != null) {
        addDerivedColumnTags(column.getChildren());
      }
    }
  }

  private void validateColumnTags(List<Column> columns) {
    // Add column level tags by adding tag to column relationship
    for (Column column : columns) {
      checkMutuallyExclusive(column.getTags());
      if (column.getChildren() != null) {
        validateColumnTags(column.getChildren());
      }
    }
  }

  /** Handles entity updated from PUT and POST operations */
  public class ContainerUpdater extends ColumnEntityUpdater {
    public ContainerUpdater(Container original, Container updated, Operation operation) {
      super(original, updated, operation);
    }

    @Override
    public void entitySpecificUpdate() {
      updateDataModel(original, updated);
      recordChange("prefix", original.getPrefix(), updated.getPrefix());
      List<ContainerFileFormat> addedItems = new ArrayList<>();
      List<ContainerFileFormat> deletedItems = new ArrayList<>();
      recordListChange(
          "fileFormats",
          original.getFileFormats(),
          updated.getFileFormats(),
          addedItems,
          deletedItems,
          EntityUtil.containerFileFormatMatch);

      // record the changes for size and numOfObjects change without version update.
      recordChange(
          "numberOfObjects",
          original.getNumberOfObjects(),
          updated.getNumberOfObjects(),
          false,
          EntityUtil.objectMatch,
          false);
      recordChange("size", original.getSize(), updated.getSize(), false, EntityUtil.objectMatch, false);
    }

    private void updateDataModel(Container original, Container updated) {

      if (original.getDataModel() == null || updated.getDataModel() == null) {
        recordChange("dataModel", original.getDataModel(), updated.getDataModel(), true);
      }

      if (original.getDataModel() != null && updated.getDataModel() != null) {
        updateColumns(
            "dataModel.columns",
            original.getDataModel().getColumns(),
            updated.getDataModel().getColumns(),
            EntityUtil.columnMatch);
        recordChange(
            "dataModel.partition",
            original.getDataModel().getIsPartitioned(),
            updated.getDataModel().getIsPartitioned());
      }
    }
  }
}
