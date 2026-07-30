/**
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.actions.server.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.gridsuite.actions.dto.contingency.FilterBasedContingencyList;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * @author Bassel El Cheikh <bassel.el-cheikh at rte-france.com>
 */

@NoArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "filter_based_contingency_list")
public class FilterBasedContingencyListEntity extends AbstractContingencyEntity {

    @Column(name = "filter_ids")
    @ElementCollection(targetClass = UUID.class)
    @CollectionTable(name = "filter_based_contingency_list_filter",
        joinColumns = @JoinColumn(name = "filter_based_contingency_list_id"),
        foreignKey = @ForeignKey(name = "filter_based_contingency_list_id_fk"))
    private List<UUID> filtersIds;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true)
    @JoinColumn(name = "filter_based_contingency_list_id", referencedColumnName = "id",
        foreignKey = @ForeignKey(name = "filter_based_contingency_list_selected_equipment_types_by_filter_fk"))
    private List<EquipmentTypesByFilterEntity> selectedEquipmentTypesByFilter;

    public FilterBasedContingencyListEntity(FilterBasedContingencyList contingencyList) {
        super();
        if (CollectionUtils.isEmpty(contingencyList.getFilters())) {
            return;
        }

        init(contingencyList);
    }

    private void init(FilterBasedContingencyList contingencyList) {
        filtersIds = new ArrayList<>();
        contingencyList.getFilters().forEach(filterAttributes -> filtersIds.add(filterAttributes.id()));
        selectedEquipmentTypesByFilter = new ArrayList<>();
        contingencyList.getSelectedEquipmentTypesByFilter().forEach(equipmentTypesByElement -> selectedEquipmentTypesByFilter.add(EquipmentTypesByFilterEntity.fromDto(equipmentTypesByElement)));
    }

    public FilterBasedContingencyListEntity update(FilterBasedContingencyList contingencyList) {
        filtersIds.clear();
        contingencyList.getFilters().forEach(filterAttributes -> filtersIds.add(filterAttributes.id()));
        selectedEquipmentTypesByFilter.clear();
        contingencyList.getSelectedEquipmentTypesByFilter().forEach(equipmentTypesByElement -> selectedEquipmentTypesByFilter.add(EquipmentTypesByFilterEntity.fromDto(equipmentTypesByElement)));
        return this;
    }
}
