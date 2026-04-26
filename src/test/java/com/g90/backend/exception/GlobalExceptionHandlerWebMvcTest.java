package com.g90.backend.exception;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.g90.backend.dto.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

class GlobalExceptionHandlerWebMvcTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new TestController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void missingRequestParamUsesCommonErrorResponse() throws Exception {
        mockMvc.perform(get("/test-errors/required-param"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("Invalid request data"))
                .andExpect(jsonPath("$.errors[0].field").value("token"))
                .andExpect(jsonPath("$.errors[0].message").value("token is required"));
    }

    @Test
    void typeMismatchUsesCommonErrorResponse() throws Exception {
        mockMvc.perform(get("/test-errors/typed-param").param("page", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("Invalid request data"))
                .andExpect(jsonPath("$.errors[0].field").value("page"))
                .andExpect(jsonPath("$.errors[0].message").value("page has invalid value"));
    }

    @Test
    void bodyValidationUsesCommonErrorResponse() throws Exception {
        mockMvc.perform(post("/test-errors/body")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("Invalid request data"))
                .andExpect(jsonPath("$.errors[0].field").value("name"))
                .andExpect(jsonPath("$.errors[0].message").value("name is required"));
    }

    @Test
    void malformedBodyUsesCommonErrorResponse() throws Exception {
        mockMvc.perform(post("/test-errors/body")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("Invalid request data"))
                .andExpect(jsonPath("$.errors[0].field").value("request"))
                .andExpect(jsonPath("$.errors[0].message").value("Request body is invalid"));
    }

    @Test
    void methodNotAllowedUsesCommonErrorResponse() throws Exception {
        mockMvc.perform(post("/test-errors/required-param"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.code").value("METHOD_NOT_ALLOWED"))
                .andExpect(jsonPath("$.message").value("HTTP method is not supported for this endpoint"))
                .andExpect(jsonPath("$.errors[0].field").value("method"))
                .andExpect(jsonPath("$.errors[0].message").value("POST is not supported for this endpoint"));
    }

    @Test
    void apiExceptionUsesCommonErrorResponse() throws Exception {
        mockMvc.perform(get("/test-errors/api-exception"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.message").value("Permission denied for test"));
    }

    @RestController
    @RequestMapping("/test-errors")
    private static class TestController {

        @GetMapping("/required-param")
        ApiResponse<Void> requiredParam(@RequestParam("token") String token) {
            return ApiResponse.success("OK", null);
        }

        @GetMapping("/typed-param")
        ApiResponse<Void> typedParam(@RequestParam("page") Integer page) {
            return ApiResponse.success("OK", null);
        }

        @PostMapping("/body")
        ApiResponse<Void> body(@Valid @RequestBody TestRequest request) {
            return ApiResponse.success("OK", null);
        }

        @GetMapping("/api-exception")
        ApiResponse<Void> apiException() {
            throw new ForbiddenOperationException("Permission denied for test");
        }
    }

    private static class TestRequest {

        @NotBlank(message = "name is required")
        private String name;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }
}
