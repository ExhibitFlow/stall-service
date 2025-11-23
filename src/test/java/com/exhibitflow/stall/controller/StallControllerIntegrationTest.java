package com.exhibitflow.stall.controller;

import com.exhibitflow.stall.dto.CreateStallRequest;
import com.exhibitflow.stall.dto.UpdateStallRequest;
import com.exhibitflow.stall.model.Stall;
import com.exhibitflow.stall.model.StallSize;
import com.exhibitflow.stall.model.StallStatus;
import com.exhibitflow.stall.repository.StallRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@EmbeddedKafka(partitions = 1, topics = {"stall.reserved", "stall.released"})
@Transactional
class StallControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private StallRepository stallRepository;

    @BeforeEach
    void setUp() {
        stallRepository.deleteAll();
    }

    @Test
    @WithMockUser(roles = "VIEWER")
    void getStalls_shouldReturnPagedStalls_withViewerRole() throws Exception {
        // Given
        Stall stall = createTestStall("A-001", StallSize.MEDIUM, "Hall A", "500.00", StallStatus.AVAILABLE);
        stallRepository.save(stall);

        // When/Then
        mockMvc.perform(get("/api/stalls")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].code").value("A-001"))
                .andExpect(jsonPath("$.content[0].size").value("MEDIUM"))
                .andExpect(jsonPath("$.content[0].status").value("AVAILABLE"));
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void getStalls_shouldFilterByStatus_withManagerRole() throws Exception {
        // Given
        stallRepository.save(createTestStall("A-001", StallSize.MEDIUM, "Hall A", "500.00", StallStatus.AVAILABLE));
        stallRepository.save(createTestStall("A-002", StallSize.LARGE, "Hall B", "750.00", StallStatus.HELD));

        // When/Then
        mockMvc.perform(get("/api/stalls")
                        .param("status", "AVAILABLE")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].code").value("A-001"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getStallById_shouldReturnStall_withAdminRole() throws Exception {
        // Given
        Stall stall = createTestStall("A-001", StallSize.MEDIUM, "Hall A", "500.00", StallStatus.AVAILABLE);
        Stall saved = stallRepository.save(stall);

        // When/Then
        mockMvc.perform(get("/api/stalls/{id}", saved.getId())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("A-001"))
                .andExpect(jsonPath("$.size").value("MEDIUM"));
    }

    @Test
    @WithMockUser
    void getStallById_shouldReturn404_whenNotFound() throws Exception {
        // When/Then
        mockMvc.perform(get("/api/stalls/{id}", 999L)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Stall not found with id: 999"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createStall_shouldCreateNewStall_withAdminRole() throws Exception {
        // Given
        CreateStallRequest request = CreateStallRequest.builder()
                .code("B-002")
                .size(StallSize.LARGE)
                .location("Hall B")
                .price(new BigDecimal("750.00"))
                .build();

        // When/Then
        mockMvc.perform(post("/api/stalls")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("B-002"))
                .andExpect(jsonPath("$.size").value("LARGE"))
                .andExpect(jsonPath("$.status").value("AVAILABLE"));
    }

    @Test
    @WithMockUser(roles = "VIEWER")
    void createStall_shouldReturn403_withViewerRole() throws Exception {
        // Given
        CreateStallRequest request = CreateStallRequest.builder()
                .code("B-002")
                .size(StallSize.LARGE)
                .location("Hall B")
                .price(new BigDecimal("750.00"))
                .build();

        // When/Then
        mockMvc.perform(post("/api/stalls")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void createStall_shouldReturn403_withManagerRole() throws Exception {
        // Given
        CreateStallRequest request = CreateStallRequest.builder()
                .code("B-002")
                .size(StallSize.LARGE)
                .location("Hall B")
                .price(new BigDecimal("750.00"))
                .build();

        // When/Then
        mockMvc.perform(post("/api/stalls")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser
    void createStall_shouldReturn400_whenValidationFails() throws Exception {
        // Given
        CreateStallRequest request = CreateStallRequest.builder()
                .code("")
                .size(StallSize.LARGE)
                .location("Hall B")
                .price(new BigDecimal("-10.00"))
                .build();

        // When/Then
        mockMvc.perform(post("/api/stalls")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"));
    }

    @Test
    @WithMockUser
    void createStall_shouldReturn409_whenCodeExists() throws Exception {
        // Given
        stallRepository.save(createTestStall("A-001", StallSize.MEDIUM, "Hall A", "500.00", StallStatus.AVAILABLE));

        CreateStallRequest request = CreateStallRequest.builder()
                .code("A-001")
                .size(StallSize.LARGE)
                .location("Hall B")
                .price(new BigDecimal("750.00"))
                .build();

        // When/Then
        mockMvc.perform(post("/api/stalls")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Stall with code A-001 already exists"));
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void updateStall_shouldUpdateStall_withManagerRole() throws Exception {
        // Given
        Stall stall = createTestStall("A-001", StallSize.MEDIUM, "Hall A", "500.00", StallStatus.AVAILABLE);
        Stall saved = stallRepository.save(stall);

        UpdateStallRequest request = UpdateStallRequest.builder()
                .location("Hall C")
                .price(new BigDecimal("600.00"))
                .build();

        // When/Then
        mockMvc.perform(put("/api/stalls/{id}", saved.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.location").value("Hall C"))
                .andExpect(jsonPath("$.price").value(600.00));
    }

    @Test
    @WithMockUser(roles = "VIEWER")
    void updateStall_shouldReturn403_withViewerRole() throws Exception {
        // Given
        Stall stall = createTestStall("A-001", StallSize.MEDIUM, "Hall A", "500.00", StallStatus.AVAILABLE);
        Stall saved = stallRepository.save(stall);

        UpdateStallRequest request = UpdateStallRequest.builder()
                .location("Hall C")
                .price(new BigDecimal("600.00"))
                .build();

        // When/Then
        mockMvc.perform(put("/api/stalls/{id}", saved.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void holdStall_shouldChangeStatusToHeld_withManagerRole() throws Exception {
        // Given
        Stall stall = createTestStall("A-001", StallSize.MEDIUM, "Hall A", "500.00", StallStatus.AVAILABLE);
        Stall saved = stallRepository.save(stall);

        // When/Then
        mockMvc.perform(post("/api/stalls/{id}/hold", saved.getId())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("HELD"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void holdStall_shouldBeIdempotent_withAdminRole() throws Exception {
        // Given
        Stall stall = createTestStall("A-001", StallSize.MEDIUM, "Hall A", "500.00", StallStatus.HELD);
        Stall saved = stallRepository.save(stall);

        // When/Then
        mockMvc.perform(post("/api/stalls/{id}/hold", saved.getId())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("HELD"));
    }

    @Test
    @WithMockUser(roles = "VIEWER")
    void holdStall_shouldReturn403_withViewerRole() throws Exception {
        // Given
        Stall stall = createTestStall("A-001", StallSize.MEDIUM, "Hall A", "500.00", StallStatus.AVAILABLE);
        Stall saved = stallRepository.save(stall);

        // When/Then
        mockMvc.perform(post("/api/stalls/{id}/hold", saved.getId())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void releaseStall_shouldChangeStatusToAvailable_withManagerRole() throws Exception {
        // Given
        Stall stall = createTestStall("A-001", StallSize.MEDIUM, "Hall A", "500.00", StallStatus.HELD);
        Stall saved = stallRepository.save(stall);

        // When/Then
        mockMvc.perform(post("/api/stalls/{id}/release", saved.getId())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("AVAILABLE"));
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void reserveStall_shouldChangeStatusToReserved_withManagerRole() throws Exception {
        // Given
        Stall stall = createTestStall("A-001", StallSize.MEDIUM, "Hall A", "500.00", StallStatus.HELD);
        Stall saved = stallRepository.save(stall);

        // When/Then
        mockMvc.perform(post("/api/stalls/{id}/reserve", saved.getId())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESERVED"));
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void reserveStall_shouldReturn400_whenNotHeld() throws Exception {
        // Given
        Stall stall = createTestStall("A-001", StallSize.MEDIUM, "Hall A", "500.00", StallStatus.AVAILABLE);
        Stall saved = stallRepository.save(stall);

        // When/Then
        mockMvc.perform(post("/api/stalls/{id}/reserve", saved.getId())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Cannot reserve stall with status: AVAILABLE. Only HELD stalls can be reserved."));
    }

    @Test
    void getStalls_shouldReturn401_whenNoAuthentication() throws Exception {
        // When/Then
        mockMvc.perform(get("/api/stalls")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    private Stall createTestStall(String code, StallSize size, String location, String price, StallStatus status) {
        return Stall.builder()
                .code(code)
                .size(size)
                .location(location)
                .price(new BigDecimal(price))
                .status(status)
                .build();
    }
}
