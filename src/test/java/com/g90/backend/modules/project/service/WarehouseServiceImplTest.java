package com.g90.backend.modules.project.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.g90.backend.modules.project.entity.WarehouseEntity;
import com.g90.backend.modules.project.repository.WarehouseRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;

@ExtendWith(MockitoExtension.class)
class WarehouseServiceImplTest {

    @Mock
    private WarehouseRepository warehouseRepository;

    private WarehouseServiceImpl warehouseService;

    @BeforeEach
    void setUp() {
        warehouseService = new WarehouseServiceImpl(warehouseRepository);
    }

    @Test
    void getWarehousesReturnsSortedMappedResponses() {
        when(warehouseRepository.findAll(Sort.by(Sort.Direction.ASC, "name").and(Sort.by(Sort.Direction.ASC, "id"))))
                .thenReturn(List.of(
                        warehouse("warehouse-2", "Backup Warehouse", "Ha Noi"),
                        warehouse("warehouse-1", "Main Warehouse", "HCMC")
                ));

        var response = warehouseService.getWarehouses();

        assertThat(response).hasSize(2);
        assertThat(response.get(0).id()).isEqualTo("warehouse-2");
        assertThat(response.get(1).location()).isEqualTo("HCMC");
    }

    private WarehouseEntity warehouse(String id, String name, String location) {
        WarehouseEntity warehouse = new WarehouseEntity();
        warehouse.setId(id);
        warehouse.setName(name);
        warehouse.setLocation(location);
        return warehouse;
    }
}
