package com.example.Tech.dto.response.chat;

import com.example.Tech.dto.response.product.ProductResponse;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public record ChatReplyResponse(

        @Schema(description = "Send it with the next message to continue the conversation")
        String sessionToken,

        String reply,

        @Schema(description = "Products named in the reply, shown as cards under it")
        List<ProductResponse> products,

        @Schema(description = "Proposed actions, shown as confirm buttons")
        List<ChatAction> actions
) {
}
