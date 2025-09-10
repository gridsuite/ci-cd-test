/**
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.actions.server.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.gridsuite.actions.server.dto.FormContingencyList;
import org.gridsuite.actions.server.utils.EquipmentType;

import java.util.HashSet;
import java.util.Set;

import static org.apache.commons.collections4.SetUtils.emptyIfNull;

/**
 * @author Chamseddine benhamed <chamseddine.benhamed at rte-france.com>
 * @author Franck Lecuyer <franck.lecuyer at rte-france.com>
 */
@NoArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "form_contingency_list")
public class FormContingencyListEntity extends AbstractContingencyEntity {

    @Column(name = "equipmentType")
    private String equipmentType;

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JoinColumn(name = "numericFilterId_id",
            referencedColumnName = "id",
            foreignKey = @ForeignKey(
                    name = "numericFilterId_id_fk"
            ), nullable = true)
    NumericalFilterEntity nominalVoltage;

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JoinColumn(name = "numericFilterId1_id",
            referencedColumnName = "id",
            foreignKey = @ForeignKey(
                    name = "numericFilterId_id_fk1"
            ), nullable = true)
    NumericalFilterEntity nominalVoltage1;

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JoinColumn(name = "numericFilterId2_id",
            referencedColumnName = "id",
            foreignKey = @ForeignKey(
                    name = "numericFilterId_id_fk2"
            ), nullable = true)
    NumericalFilterEntity nominalVoltage2;

    @Column(name = "country")
    @ElementCollection
    @CollectionTable(foreignKey = @ForeignKey(name = "formContingencyListEntity_countries_fk"), indexes = {@Index(name = "formContingencyListEntity_countries_idx", columnList = "form_contingency_list_entity_id")})
    private Set<String> countries;

    @Column(name = "country1")
    @ElementCollection
    @CollectionTable(foreignKey = @ForeignKey(name = "formContingencyListEntity_countries1_fk"), indexes = {@Index(name = "formContingencyListEntity_countries1_idx", columnList = "form_contingency_list_entity_id")})
    private Set<String> countries1;

    @Column(name = "country2")
    @ElementCollection
    @CollectionTable(foreignKey = @ForeignKey(name = "formContingencyListEntity_countries2_fk"), indexes = {@Index(name = "formContingencyListEntity_countries2_idx", columnList = "form_contingency_list_entity_id")})
    private Set<String> countries2;

    public FormContingencyListEntity(FormContingencyList formContingencyList) {
        super();
        init(formContingencyList);
    }

    /* called in constructor so it is final */
    final void init(FormContingencyList formContingencyList) {
        this.equipmentType = formContingencyList.getEquipmentType();
        EquipmentType type = EquipmentType.valueOf(this.equipmentType);
        // protection against unrelevant input data
        if (type == EquipmentType.TWO_WINDINGS_TRANSFORMER || type == EquipmentType.LINE) {
            this.nominalVoltage1 = NumericalFilterEntity.convert(formContingencyList.getNominalVoltage1());
            this.nominalVoltage2 = NumericalFilterEntity.convert(formContingencyList.getNominalVoltage2());
        } else {
            this.nominalVoltage = NumericalFilterEntity.convert(formContingencyList.getNominalVoltage());
        }
        if (type == EquipmentType.LINE || type == EquipmentType.HVDC_LINE) {
            this.countries1 = new HashSet<>(emptyIfNull(formContingencyList.getCountries1()));
            this.countries2 = new HashSet<>(emptyIfNull(formContingencyList.getCountries2()));
        } else {
            this.countries = new HashSet<>(emptyIfNull(formContingencyList.getCountries()));
        }
    }

    public FormContingencyListEntity update(FormContingencyList formContingencyList) {
        init(formContingencyList);
        return this;
    }
}
