/**
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package org.gridsuite.actions.server.dto;

import com.powsybl.contingency.Contingency;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Set;

/**
 * @author Seddik Yengui <seddik.yengui@rte-france.com>
 */

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ContingencyInfos {
    private String id;
    private Contingency contingency;
    private Set<String> notFoundElements;
    private Set<String> notConnectedElements;
}
