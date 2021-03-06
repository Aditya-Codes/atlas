/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.atlas.repository.audit;

import org.apache.atlas.RequestContextV1;
import org.apache.atlas.model.audit.EntityAuditEventV2;
import org.apache.atlas.model.audit.EntityAuditEventV2.EntityAuditAction;
import org.apache.atlas.exception.AtlasBaseException;
import org.apache.atlas.listener.EntityChangeListenerV2;
import org.apache.atlas.model.instance.AtlasClassification;
import org.apache.atlas.model.instance.AtlasEntity;
import org.apache.atlas.type.AtlasEntityType;
import org.apache.atlas.type.AtlasStructType.AtlasAttribute;
import org.apache.atlas.type.AtlasType;
import org.apache.atlas.type.AtlasTypeRegistry;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.apache.atlas.model.audit.EntityAuditEventV2.EntityAuditAction.CLASSIFICATION_ADD;
import static org.apache.atlas.model.audit.EntityAuditEventV2.EntityAuditAction.CLASSIFICATION_DELETE;
import static org.apache.atlas.model.audit.EntityAuditEventV2.EntityAuditAction.CLASSIFICATION_UPDATE;
import static org.apache.atlas.model.audit.EntityAuditEventV2.EntityAuditAction.ENTITY_CREATE;
import static org.apache.atlas.model.audit.EntityAuditEventV2.EntityAuditAction.ENTITY_DELETE;
import static org.apache.atlas.model.audit.EntityAuditEventV2.EntityAuditAction.ENTITY_IMPORT_CREATE;
import static org.apache.atlas.model.audit.EntityAuditEventV2.EntityAuditAction.ENTITY_IMPORT_DELETE;
import static org.apache.atlas.model.audit.EntityAuditEventV2.EntityAuditAction.ENTITY_IMPORT_UPDATE;
import static org.apache.atlas.model.audit.EntityAuditEventV2.EntityAuditAction.ENTITY_UPDATE;

@Component
public class EntityAuditListenerV2 implements EntityChangeListenerV2 {
    private static final Logger LOG = LoggerFactory.getLogger(EntityAuditListenerV2.class);

    private final EntityAuditRepository auditRepository;
    private final AtlasTypeRegistry     typeRegistry;

    @Inject
    public EntityAuditListenerV2(EntityAuditRepository auditRepository, AtlasTypeRegistry typeRegistry) {
        this.auditRepository = auditRepository;
        this.typeRegistry    = typeRegistry;
    }

    @Override
    public void onEntitiesAdded(List<AtlasEntity> entities, boolean isImport) throws AtlasBaseException {
        List<EntityAuditEventV2> events = new ArrayList<>();

        for (AtlasEntity entity : entities) {
            EntityAuditEventV2 event = createEvent(entity, isImport ? ENTITY_IMPORT_CREATE : ENTITY_CREATE);

            events.add(event);
        }

        auditRepository.putEventsV2(events);
    }

    @Override
    public void onEntitiesUpdated(List<AtlasEntity> entities, boolean isImport) throws AtlasBaseException {
        List<EntityAuditEventV2> events = new ArrayList<>();

        for (AtlasEntity entity : entities) {
            EntityAuditEventV2 event = createEvent(entity, isImport ? ENTITY_IMPORT_UPDATE : ENTITY_UPDATE);

            events.add(event);
        }

        auditRepository.putEventsV2(events);
    }

    @Override
    public void onEntitiesDeleted(List<AtlasEntity> entities, boolean isImport) throws AtlasBaseException {
        List<EntityAuditEventV2> events = new ArrayList<>();

        for (AtlasEntity entity : entities) {
            EntityAuditEventV2 event = createEvent(entity, isImport ? ENTITY_IMPORT_DELETE : ENTITY_DELETE, "Deleted entity");

            events.add(event);
        }

        auditRepository.putEventsV2(events);
    }

    @Override
    public void onClassificationsAdded(AtlasEntity entity, List<AtlasClassification> classifications) throws AtlasBaseException {
        if (CollectionUtils.isNotEmpty(classifications)) {
            List<EntityAuditEventV2> events = new ArrayList<>();

            for (AtlasClassification classification : classifications) {
                events.add(createEvent(entity, CLASSIFICATION_ADD, "Added classification: " + AtlasType.toJson(classification)));
            }

            auditRepository.putEventsV2(events);
        }
    }

    @Override
    public void onClassificationsUpdated(AtlasEntity entity, List<AtlasClassification> classifications) throws AtlasBaseException {
        if (CollectionUtils.isNotEmpty(classifications)) {
            List<EntityAuditEventV2> events = new ArrayList<>();

            for (AtlasClassification classification : classifications) {
                events.add(createEvent(entity, CLASSIFICATION_UPDATE, "Updated classification: " + AtlasType.toJson(classification)));
            }

            auditRepository.putEventsV2(events);
        }
    }

    @Override
    public void onClassificationsDeleted(AtlasEntity entity, List<String> classificationNames) throws AtlasBaseException {
        if (CollectionUtils.isNotEmpty(classificationNames)) {
            List<EntityAuditEventV2> events = new ArrayList<>();

            for (String classificationName : classificationNames) {
                events.add(createEvent(entity, CLASSIFICATION_DELETE, "Deleted classification: " + classificationName));
            }

            auditRepository.putEventsV2(events);
        }
    }

    private EntityAuditEventV2 createEvent(AtlasEntity entity, EntityAuditAction action, String details) {
        return new EntityAuditEventV2(entity.getGuid(), RequestContextV1.get().getRequestTime(),
                                      RequestContextV1.get().getUser(), action, details, entity);
    }

    private EntityAuditEventV2 createEvent(AtlasEntity entity, EntityAuditAction action) {
        String detail = getAuditEventDetail(entity, action);

        return createEvent(entity, action, detail);
    }

    private String getAuditEventDetail(AtlasEntity entity, EntityAuditAction action) {
        Map<String, Object> prunedAttributes = pruneEntityAttributesForAudit(entity);

        String auditPrefix  = getAuditPrefix(action);
        String auditString  = auditPrefix + AtlasType.toJson(entity);
        byte[] auditBytes   = auditString.getBytes(StandardCharsets.UTF_8);
        long   auditSize    = auditBytes != null ? auditBytes.length : 0;
        long   auditMaxSize = auditRepository.repositoryMaxSize();

        if (auditMaxSize >= 0 && auditSize > auditMaxSize) { // don't store attributes in audit
            LOG.warn("audit record too long: entityType={}, guid={}, size={}; maxSize={}. entity attribute values not stored in audit",
                    entity.getTypeName(), entity.getGuid(), auditSize, auditMaxSize);

            Map<String, Object> attrValues = entity.getAttributes();

            entity.setAttributes(null);

            auditString = auditPrefix + AtlasType.toJson(entity);

            entity.setAttributes(attrValues);
        }

        restoreEntityAttributes(entity, prunedAttributes);

        return auditString;
    }

    private Map<String, Object> pruneEntityAttributesForAudit(AtlasEntity entity) {
        Map<String, Object> ret               = null;
        Map<String, Object> entityAttributes  = entity.getAttributes();
        List<String>        excludeAttributes = auditRepository.getAuditExcludeAttributes(entity.getTypeName());
        AtlasEntityType     entityType        = typeRegistry.getEntityTypeByName(entity.getTypeName());

        if (CollectionUtils.isNotEmpty(excludeAttributes) && MapUtils.isNotEmpty(entityAttributes) && entityType != null) {
            for (AtlasAttribute attribute : entityType.getAllAttributes().values()) {
                String attrName  = attribute.getName();
                Object attrValue = entityAttributes.get(attrName);

                if (excludeAttributes.contains(attrName)) {
                    if (ret == null) {
                        ret = new HashMap<>();
                    }

                    ret.put(attrName, attrValue);
                    entityAttributes.remove(attrName);
                }
            }
        }

        return ret;
    }

    private void restoreEntityAttributes(AtlasEntity entity, Map<String, Object> prunedAttributes) {
        if (MapUtils.isEmpty(prunedAttributes)) {
            return;
        }

        AtlasEntityType entityType = typeRegistry.getEntityTypeByName(entity.getTypeName());

        if (entityType != null && MapUtils.isNotEmpty(entityType.getAllAttributes())) {
            for (AtlasAttribute attribute : entityType.getAllAttributes().values()) {
                String attrName = attribute.getName();

                if (prunedAttributes.containsKey(attrName)) {
                    entity.setAttribute(attrName, prunedAttributes.get(attrName));
                }
            }
        }
    }

    private String getAuditPrefix(EntityAuditAction action) {
        final String ret;

        switch (action) {
            case ENTITY_CREATE:
                ret = "Created: ";
                break;
            case ENTITY_UPDATE:
                ret = "Updated: ";
                break;
            case ENTITY_DELETE:
                ret = "Deleted: ";
                break;
            case CLASSIFICATION_ADD:
                ret = "Added classification: ";
                break;
            case CLASSIFICATION_DELETE:
                ret = "Deleted classification: ";
                break;
            case CLASSIFICATION_UPDATE:
                ret = "Updated classification: ";
                break;
            case ENTITY_IMPORT_CREATE:
                ret = "Created by import: ";
                break;
            case ENTITY_IMPORT_UPDATE:
                ret = "Updated by import: ";
                break;
            case ENTITY_IMPORT_DELETE:
                ret = "Deleted by import: ";
                break;
            default:
                ret = "Unknown: ";
        }

        return ret;
    }
}