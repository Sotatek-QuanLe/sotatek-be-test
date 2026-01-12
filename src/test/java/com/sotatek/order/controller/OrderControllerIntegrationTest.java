package com.sotatek.order.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sotatek.order.model.dto.request.CreateOrderRequest;
import com.sotatek.order.model.dto.request.OrderItemRequest;
import com.sotatek.order.model.dto.request.UpdateOrderRequest;
import com.sotatek.order.model.enums.OrderStatus;
import com.sotatek.order.model.enums.PaymentMethod;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@SuppressWarnings("null")
class OrderControllerIntegrationTest {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private ObjectMapper objectMapper;

        @Test
        void createOrder_Success_Returns201() throws Exception {
                CreateOrderRequest request = createValidRequest();

                mockMvc.perform(post("/api/orders")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.id").exists())
                                .andExpect(jsonPath("$.memberId").value("M001"))
                                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                                .andExpect(jsonPath("$.totalAmount").isNumber());
        }

        @Test
        void createOrder_InvalidMember_Returns404() throws Exception {
                CreateOrderRequest request = createValidRequest();
                request.setMemberId("not-found");

                mockMvc.perform(post("/api/orders")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isNotFound())
                                .andExpect(jsonPath("$.error").value("MEMBER_NOT_FOUND"));
        }

        @Test
        void createOrder_InactiveMember_Returns400() throws Exception {
                CreateOrderRequest request = createValidRequest();
                request.setMemberId("inactive-member");

                mockMvc.perform(post("/api/orders")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.error").value("MEMBER_INACTIVE"));
        }

        @Test
        void createOrder_ValidationError_Returns400() throws Exception {
                CreateOrderRequest request = new CreateOrderRequest();
                request.setMemberId(""); // Invalid: blank
                request.setItems(List.of());
                request.setPaymentMethod(PaymentMethod.CREDIT_CARD);

                mockMvc.perform(post("/api/orders")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                                .andExpect(jsonPath("$.fieldErrors").exists());
        }

        @Test
        void createOrder_IdempotencyKey_ReturnsSameResponse() throws Exception {
                CreateOrderRequest request = createValidRequest();
                String idempotencyKey = UUID.randomUUID().toString();

                // First request
                MvcResult first = mockMvc.perform(post("/api/orders")
                                .header("Idempotency-Key", idempotencyKey)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isCreated())
                                .andReturn();

                // Second request with same key
                MvcResult second = mockMvc.perform(post("/api/orders")
                                .header("Idempotency-Key", idempotencyKey)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isCreated())
                                .andReturn();

                // Should return same response
                assertEquals(first.getResponse().getContentAsString(),
                                second.getResponse().getContentAsString());
        }

        @Test
        void getOrder_Exists_Returns200() throws Exception {
                // First create an order
                CreateOrderRequest request = createValidRequest();
                MvcResult createResult = mockMvc.perform(post("/api/orders")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isCreated())
                                .andReturn();

                // Extract ID from response
                String response = createResult.getResponse().getContentAsString();
                Long orderId = objectMapper.readTree(response).get("id").asLong();

                // Get order
                mockMvc.perform(get("/api/orders/{id}", orderId))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.id").value(orderId))
                                .andExpect(jsonPath("$.memberId").value("M001"));
        }

        @Test
        void getOrder_NotFound_Returns404() throws Exception {
                mockMvc.perform(get("/api/orders/{id}", 99999L))
                                .andExpect(status().isNotFound())
                                .andExpect(jsonPath("$.error").value("ORDER_NOT_FOUND"));
        }

        @Test
        void listOrders_WithPagination_Returns200() throws Exception {
                mockMvc.perform(get("/api/orders")
                                .param("page", "0")
                                .param("size", "10")
                                .param("sortBy", "createdAt")
                                .param("sortDir", "desc"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.content").isArray())
                                .andExpect(jsonPath("$.pageable").exists())
                                .andExpect(jsonPath("$.totalElements").isNumber());
        }

        @Test
        void cancelOrder_Success_Returns200() throws Exception {
                // First create an order
                CreateOrderRequest createReq = createValidRequest();
                MvcResult createResult = mockMvc.perform(post("/api/orders")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(createReq)))
                                .andExpect(status().isCreated())
                                .andReturn();

                Long orderId = objectMapper.readTree(
                                createResult.getResponse().getContentAsString()).get("id").asLong();

                // Cancel order
                UpdateOrderRequest cancelReq = new UpdateOrderRequest();
                cancelReq.setStatus(OrderStatus.CANCELLED);

                mockMvc.perform(put("/api/orders/{id}", orderId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(cancelReq)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.status").value("CANCELLED"));
        }

        @Test
        void cancelOrder_AlreadyCancelled_Returns400() throws Exception {
                // Create and cancel an order first
                CreateOrderRequest createReq = createValidRequest();
                MvcResult createResult = mockMvc.perform(post("/api/orders")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(createReq)))
                                .andReturn();

                Long orderId = objectMapper.readTree(
                                createResult.getResponse().getContentAsString()).get("id").asLong();

                UpdateOrderRequest cancelReq = new UpdateOrderRequest();
                cancelReq.setStatus(OrderStatus.CANCELLED);

                // First cancel
                mockMvc.perform(put("/api/orders/{id}", orderId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(cancelReq)));

                // Second cancel - should fail
                mockMvc.perform(put("/api/orders/{id}", orderId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(cancelReq)))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.error").value("INVALID_ORDER_STATUS"));
        }

        private CreateOrderRequest createValidRequest() {
                OrderItemRequest item = new OrderItemRequest();
                item.setProductId("P001");
                item.setQuantity(2);

                CreateOrderRequest request = new CreateOrderRequest();
                request.setMemberId("M001");
                request.setItems(List.of(item));
                request.setPaymentMethod(PaymentMethod.CREDIT_CARD);
                return request;
        }
}
