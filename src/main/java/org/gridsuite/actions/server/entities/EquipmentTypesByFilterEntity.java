/**
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package org.gridsuite.actions.server.entities;

import com.powsybl.iidm.network.IdentifiableType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.gridsuite.actions.dto.EquipmentTypesByFilter;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import java.util.Set;
import java.util.UUID;

/**
 * Store a list of equipment types for a given filter ID
 *
 * @author Florent MILLOT <florent.millot at rte-france.com>
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "equipment_types_by_filter")
public class EquipmentTypesByFilterEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id")
    private UUID id;

    @Column(name = "filter_id")
    @Schema(description = "ID of the filter")
    private UUID filterId;

    @Column(name = "equipment_type")
    @ElementCollection
    @Enumerated(EnumType.STRING)
    @CollectionTable(name = "equipment_types_by_filter_equipment_types",
        joinColumns = @JoinColumn(name = "equipment_types_by_filter_id"),
        foreignKey = @ForeignKey(name = "equipment_types_by_filter_equipment_types_fk"))
    @Fetch(FetchMode.JOIN)
    @Schema(description = "List of associated equipment types")
    Set<IdentifiableType> equipmentTypes;

    public EquipmentTypesByFilter toDto() {
        return new EquipmentTypesByFilter(filterId, equipmentTypes);
    }

    public static EquipmentTypesByFilterEntity fromDto(EquipmentTypesByFilter equipmentTypesByFilter) {
        return new EquipmentTypesByFilterEntity(null, equipmentTypesByFilter.filterId(), Set.copyOf(equipmentTypesByFilter.equipmentTypes()));
    }
}
