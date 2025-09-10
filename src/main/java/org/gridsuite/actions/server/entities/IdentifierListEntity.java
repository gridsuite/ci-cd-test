/**
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.actions.server.entities;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;
import java.util.Set;
import java.util.UUID;

/**
 * @author Etienne Homer <etienne.homer@rte-france.com>
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "identifier_list")
public class IdentifierListEntity {
    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "name")
    private String name;

    @Column(name = "equipmentIds")
    @ElementCollection
    @CollectionTable(foreignKey = @ForeignKey(name = "identifierListEntity_equipmentIds_fk1"), indexes = {@Index(name = "identifierListEntity_equipmentIds_idx1", columnList = "identifier_list_entity_id")})
    Set<String> equipmentIds;
}
