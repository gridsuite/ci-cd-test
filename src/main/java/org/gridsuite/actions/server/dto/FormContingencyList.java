/**
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.actions.server.dto;

import com.powsybl.commons.PowsyblException;
import com.powsybl.contingency.contingency.list.*;
import com.powsybl.iidm.criteria.*;
import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Network;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.gridsuite.actions.server.utils.ContingencyListType;
import org.gridsuite.actions.server.utils.EquipmentType;

import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * @author Chamseddine benhamed <chamseddine.benhamed at rte-france.com>
 * @author Etienne Homer <etienne.homer@rte-france.com>
 */
@Getter
@NoArgsConstructor
@Schema(description = "Form contingency list")
public class FormContingencyList extends AbstractContingencyList {

    @Schema(description = "Equipment type")
    private String equipmentType;

    @Schema(description = "Nominal voltage")
    private NumericalFilter nominalVoltage;

    @Schema(description = "Nominal voltage 1")
    private NumericalFilter nominalVoltage1;

    @Schema(description = "Nominal voltage 2")
    private NumericalFilter nominalVoltage2;

    @Schema(description = "Countries")
    private Set<String> countries;

    @Schema(description = "Countries 1")
    private Set<String> countries1;

    @Schema(description = "Countries 2")
    private Set<String> countries2;

    public FormContingencyList(UUID uuid,
                               Instant date,
                               String equipmentType,
                               NumericalFilter nominalVoltage,
                               NumericalFilter nominalVoltage1,
                               NumericalFilter nominalVoltage2,
                               Set<String> countries,
                               Set<String> countries1,
                               Set<String> countries2) {
        super(new ContingencyListMetadataImpl(uuid, ContingencyListType.FORM, date));
        this.equipmentType = equipmentType;
        this.nominalVoltage = nominalVoltage;
        this.nominalVoltage1 = nominalVoltage1;
        this.nominalVoltage2 = nominalVoltage2;
        this.countries = countries;
        this.countries1 = countries1;
        this.countries2 = countries2;
    }

    public FormContingencyList(String equipmentType,
                               NumericalFilter nominalVoltage,
                               NumericalFilter nominalVoltage1,
                               NumericalFilter nominalVoltage2,
                               Set<String> countries,
                               Set<String> countries1,
                               Set<String> countries2) {
        this(null, null, equipmentType, nominalVoltage, nominalVoltage1, nominalVoltage2, countries, countries1, countries2);
    }

    private static VoltageInterval getVoltageInterval(SingleNominalVoltageCriterion singleNominalVoltageCriterion) {
        return singleNominalVoltageCriterion != null ?
                singleNominalVoltageCriterion.getVoltageInterval()
                : null;
    }

    @Override
    public ContingencyList toPowsyblContingencyList(Network network) {
        AbstractEquipmentCriterionContingencyList contingencyList;
        switch (EquipmentType.valueOf(this.getEquipmentType())) {
            case GENERATOR:
            case STATIC_VAR_COMPENSATOR:
            case SHUNT_COMPENSATOR:
            case BUSBAR_SECTION:
            case DANGLING_LINE:
                contingencyList = new InjectionCriterionContingencyList(
                        this.getId().toString(),
                        this.getEquipmentType(),
                        new SingleCountryCriterion(this.getCountries().stream().map(c -> Country.valueOf(c)).collect(Collectors.toList())),
                        NumericalFilter.toNominalVoltageCriterion(this.getNominalVoltage()),
                        Collections.emptyList(),
                        null
                );
                break;
            case HVDC_LINE:
                contingencyList = new HvdcLineCriterionContingencyList(
                        this.getId().toString(),
                        new TwoCountriesCriterion(
                                this.getCountries1().stream().map(c -> Country.valueOf(c)).collect(Collectors.toList()),
                                this.getCountries2().stream().map(c -> Country.valueOf(c)).collect(Collectors.toList())
                        ),
                        // TODO ?????
                        new TwoNominalVoltageCriterion(
                                getVoltageInterval(NumericalFilter.toNominalVoltageCriterion(this.getNominalVoltage())),
                                null
                        ),
                        Collections.emptyList(),
                        null
                );
                break;
            case LINE:
                contingencyList = new LineCriterionContingencyList(
                        this.getId().toString(),
                        new TwoCountriesCriterion(
                                this.getCountries1().stream().map(c -> Country.valueOf(c)).collect(Collectors.toList()),
                                this.getCountries2().stream().map(c -> Country.valueOf(c)).collect(Collectors.toList())
                        ),
                        new TwoNominalVoltageCriterion(
                                getVoltageInterval(NumericalFilter.toNominalVoltageCriterion(this.getNominalVoltage1())),
                                getVoltageInterval(NumericalFilter.toNominalVoltageCriterion(this.getNominalVoltage2()))
                        ),
                        Collections.emptyList(),
                        null
                );
                break;
            case TWO_WINDINGS_TRANSFORMER:
                contingencyList = new TwoWindingsTransformerCriterionContingencyList(
                        this.getId().toString(),
                        new SingleCountryCriterion(this.getCountries().stream().map(c -> Country.valueOf(c)).collect(Collectors.toList())),
                        new TwoNominalVoltageCriterion(
                                getVoltageInterval(NumericalFilter.toNominalVoltageCriterion(this.getNominalVoltage1())),
                                getVoltageInterval(NumericalFilter.toNominalVoltageCriterion(this.getNominalVoltage2()))
                        ),
                        Collections.emptyList(),
                        null
                );
                break;
            default:
                throw new PowsyblException("Unknown equipment type");
        }
        return contingencyList;
    }

    //TODO this a temporary workaround to get elements not found in the network
    // this should be deleted when a fix is added to powsybl
    @Override
    public Map<String, Set<String>> getNotFoundElements(Network network) {
        return Map.of();
    }
}
