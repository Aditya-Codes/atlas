/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.atlas.omrs.metadatacollection.properties.instances;

import java.util.ArrayList;

/**
 * EntityUniverse extends EntityDetail to add the relationships that this entity has.  These are available
 * in an iterator to make them easy to process.
 */
public class EntityUniverse extends EntityDetail
{
    private ArrayList<Relationship>  entityRelationships = null;


    /**
     * Default constructor - initializes entity's universe as empty.
     */
    public EntityUniverse()
    {
        super();
    }


    /**
     * Copy/clone constructor.
     *
     * @param templateElement - template to copy.  If null passed, the EntityUniverse is initialized as empty.
     */
    public EntityUniverse(EntityUniverse   templateElement)
    {
        super(templateElement);

        if (templateElement != null)
        {
            entityRelationships = templateElement.getEntityRelationships();
        }
    }


    /**
     * Copy/clone constructor from an EntityDetail.
     *
     * @param templateElement - template to copy.  If null passed, the EntityUniverse is initialized as empty.
     */
    public EntityUniverse(EntityDetail   templateElement)
    {
        super(templateElement);
    }


    /**
     * Return a copy of the relationships for this entity in an iterator.
     *
     * @return Relationships list.
     */
    public ArrayList<Relationship> getEntityRelationships()
    {
        return entityRelationships;
    }


    /**
     * Set up the list of relationships for this entity.
     *
     * @param entityRelationships - Relationships list
     */
    public void setEntityRelationships(ArrayList<Relationship> entityRelationships)
    {
        this.entityRelationships = entityRelationships;
    }


    /**
     * Standard toString method.
     *
     * @return JSON style description of variables.
     */
    @Override
    public String toString()
    {
        return "EntityUniverse{" +
                "entityRelationships=" + entityRelationships +
                ", properties=" + getProperties() +
                ", classifications=" + getClassifications() +
                ", type=" + getType() +
                ", instanceProvenanceType=" + getInstanceProvenanceType() +
                ", metadataCollectionId='" + getMetadataCollectionId() + '\'' +
                ", instanceURL='" + getInstanceURL() + '\'' +
                ", GUID='" + getGUID() + '\'' +
                ", status=" + getStatus() +
                ", createdBy='" + getCreatedBy() + '\'' +
                ", updatedBy='" + getUpdatedBy() + '\'' +
                ", createTime=" + getCreateTime() +
                ", updateTime=" + getUpdateTime() +
                ", version=" + getVersion() +
                ", statusOnDelete=" + getStatusOnDelete() +
                '}';
    }
}
