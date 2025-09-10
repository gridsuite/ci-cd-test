/*
  Copyright (c) 2023, RTE (http://www.rte-france.com)
  This Source Code Form is subject to the terms of the Mozilla Public
  License, v. 2.0. If a copy of the MPL was not distributed with this
  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.actions.server.dto;

import org.gridsuite.actions.server.utils.ContingencyListType;

import java.time.Instant;
import java.util.UUID;

/**
 * @author Etienne Homer <etienne.homer@rte-france.com>
 */
public interface ContingencyListMetadata {

    UUID getId();

    Instant getModificationDate();

    ContingencyListType getType();
}
