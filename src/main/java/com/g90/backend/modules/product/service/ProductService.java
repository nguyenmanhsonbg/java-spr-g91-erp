package com.g90.backend.modules.product.service;

import com.g90.backend.modules.product.dto.ProductCreateRequest;
import com.g90.backend.modules.product.dto.ProductListQuery;
import com.g90.backend.modules.product.dto.ProductListResponseData;
import com.g90.backend.modules.product.dto.ProductResponse;
import com.g90.backend.modules.product.dto.ProductStatusResponse;
import com.g90.backend.modules.product.dto.ProductStatusUpdateRequest;
import com.g90.backend.modules.product.dto.ProductUpdateRequest;

public interface ProductService {

    ProductListResponseData getProducts(ProductListQuery query);

    ProductResponse createProduct(ProductCreateRequest request);

    ProductResponse getProductById(String id);

    ProductResponse updateProduct(String id, ProductUpdateRequest request);

    ProductStatusResponse updateProductStatus(String id, ProductStatusUpdateRequest request);
}
