package com.g90.backend.modules.product.storage;

import java.util.List;
import org.springframework.web.multipart.MultipartFile;

public interface ProductImageStorageService {

    List<String> store(List<MultipartFile> files);
}
