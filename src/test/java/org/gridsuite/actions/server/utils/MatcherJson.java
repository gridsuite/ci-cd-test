/*
  Copyright (c) 2023, RTE (http://www.rte-france.com)
  This Source Code Form is subject to the terms of the Mozilla Public
  License, v. 2.0. If a copy of the MPL was not distributed with this
  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.actions.server.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;

/**
 * @author Etienne Homer <etienne.homer at rte-france.com>
 */
@AllArgsConstructor
public class MatcherJson<T> extends TypeSafeMatcher<T> {
    private final ObjectMapper mapper;
    private final T reference;

    @SneakyThrows
    @Override
    public boolean matchesSafely(T s) {
        return mapper.writeValueAsString(reference).equals(mapper.writeValueAsString(s));
    }

    @Override
    public void describeTo(Description description) {
        description.appendValue(reference);
    }
}
