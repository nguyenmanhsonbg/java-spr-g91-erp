package com.g90.backend.modules.product.controller;

import com.g90.backend.config.OpenApiConfig;
import com.g90.backend.dto.ApiResponse;
import com.g90.backend.modules.product.dto.ProductCreateRequest;
import com.g90.backend.modules.product.dto.ProductImageUploadResponse;
import com.g90.backend.modules.product.dto.ProductListQuery;
import com.g90.backend.modules.product.dto.ProductListResponseData;
import com.g90.backend.modules.product.dto.ProductResponse;
import com.g90.backend.modules.product.dto.ProductStatusResponse;
import com.g90.backend.modules.product.dto.ProductStatusUpdateRequest;
import com.g90.backend.modules.product.dto.ProductUpdateRequest;
import com.g90.backend.modules.product.service.ProductService;
import com.g90.backend.modules.product.storage.ProductImageStorageService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import java.util.ArrayList;
import java.util.List;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/products")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH_SCHEME)
public class ProductController {

    private final ProductService productService;
    private final ProductImageStorageService productImageStorageService;

    @GetMapping
    public ApiResponse<ProductListResponseData> getProducts(@Valid @ModelAttribute ProductListQuery query) {
        return ApiResponse.success("Product list fetched successfully", productService.getProducts(query));
    }

    @PostMapping(value = "/upload-images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload product images to local storage")
    public ApiResponse<ProductImageUploadResponse> uploadProductImages(@RequestPart("files") List<MultipartFile> files) {
        List<String> uploadedUrls = productImageStorageService.store(files);
        return ApiResponse.success("Product images uploaded successfully", new ProductImageUploadResponse(uploadedUrls));
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<ProductResponse>> createProduct(@Valid @RequestBody ProductCreateRequest request) {
        return createProductResponse(request);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<ProductResponse>> createProductWithImages(
            @Valid @ModelAttribute ProductCreateRequest request,
            @RequestPart(value = "files", required = false) List<MultipartFile> files
    ) {
        mergeUploadedImageUrls(request, files);
        return createProductResponse(request);
    }

    private ResponseEntity<ApiResponse<ProductResponse>> createProductResponse(ProductCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Product created successfully", productService.createProduct(request)));
    }

    private void mergeUploadedImageUrls(ProductCreateRequest request, List<MultipartFile> files) {
        request.setImageUrls(mergeImageUrls(request.getImageUrls(), files));
    }

    private void mergeUploadedImageUrls(ProductUpdateRequest request, List<MultipartFile> files) {
        request.setImageUrls(mergeImageUrls(request.getImageUrls(), files));
    }

    private List<String> mergeImageUrls(List<String> existingImageUrls, List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            return existingImageUrls;
        }

        List<String> mergedImageUrls = new ArrayList<>();
        if (existingImageUrls != null) {
            mergedImageUrls.addAll(existingImageUrls);
        }
        mergedImageUrls.addAll(productImageStorageService.store(files));
        return mergedImageUrls;
    }

    @GetMapping("/{id}")
    public ApiResponse<ProductResponse> getProductById(@PathVariable String id) {
        return ApiResponse.success("Product detail fetched successfully", productService.getProductById(id));
    }

    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ApiResponse<ProductResponse> updateProduct(
            @PathVariable String id,
            @Valid @RequestBody ProductUpdateRequest request
    ) {
        return updateProductResponse(id, request);
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<ProductResponse> updateProductWithImages(
            @PathVariable String id,
            @Valid @ModelAttribute ProductUpdateRequest request,
            @RequestPart(value = "files", required = false) List<MultipartFile> files
    ) {
        mergeUploadedImageUrls(request, files);
        return updateProductResponse(id, request);
    }

    private ApiResponse<ProductResponse> updateProductResponse(String id, ProductUpdateRequest request) {
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
