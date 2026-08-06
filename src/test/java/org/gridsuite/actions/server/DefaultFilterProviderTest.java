package org.gridsuite.actions.server;

import org.gridsuite.actions.server.service.FilterService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class DefaultFilterProviderTest {
    @InjectMocks
    private DefaultFilterProvider defaultFilterProvider;

    @Mock
    private FilterService filterService;

    @Test
    void getFiltersShouldCallFilterService() {
        List<UUID> filters = List.of(UUID.randomUUID(), UUID.randomUUID());
        defaultFilterProvider.getFilters(filters);
        verify(filterService).getFilters(filters);
    }
}
