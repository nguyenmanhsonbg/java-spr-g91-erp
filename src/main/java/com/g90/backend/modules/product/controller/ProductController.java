package com.g90.backend.modules.product.controller;

import com.g90.backend.config.OpenApiConfig;
import com.g90.backend.dto.ApiResponse;
import com.g90.backend.modules.product.dto.ProductCreateRequest;
import com.g90.backend.modules.product.dto.ProductListQuery;
import com.g90.backend.modules.product.dto.ProductListResponseData;
import com.g90.backend.modules.product.dto.ProductResponse;
import com.g90.backend.modules.product.dto.ProductStatusResponse;
import com.g90.backend.modules.product.dto.ProductStatusUpdateRequest;
import com.g90.backend.modules.product.dto.ProductUpdateRequest;
import com.g90.backend.modules.product.service.ProductService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/products")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH_SCHEME)
public class ProductController {

    private final ProductService productService;

    @GetMapping
    public ApiResponse<ProductListResponseData> getProducts(@Valid @ModelAttribute ProductListQuery query) {
        return ApiResponse.success("Product list fetched successfully", productService.getProducts(query));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ProductResponse>> createProduct(@Valid @RequestBody ProductCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Product created successfully", productService.createProduct(request)));
    }

    @GetMapping("/{id}")
    public ApiResponse<ProductResponse> getProductById(@PathVariable String id) {
        return ApiResponse.success("Product detail fetched successfully", productService.getProductById(id));
    }

    @PutMapping("/{id}")
    public ApiResponse<ProductResponse> updateProduct(
            @PathVariable String id,
            @Valid @RequestBody ProductUpdateRequest request
    ) {
        return ApiResponse.success("Product updated successfully", productService.updateProduct(id, request));
    }

    @PatchMapping("/{id}/status")
    public ApiResponse<ProductStatusResponse> updateProductStatus(
            @PathVariable String id,
            @Valid @RequestBody ProductStatusUpdateRequest request
    ) {
        return ApiResponse.success("Product status updated successfully", productService.updateProductStatus(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<ProductStatusResponse> deleteProduct(@PathVariable String id) {
        return ApiResponse.success("Product archived successfully", productService.deleteProduct(id));
    }
}
