package com.g90.backend.modules.promotion.integration;

import com.g90.backend.modules.promotion.entity.PromotionEntity;

public interface PromotionNotificationGateway {

    void notifyPromotionCreated(PromotionEntity promotion);
}
