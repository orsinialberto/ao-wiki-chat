package com.example.ao_wiki_chat.model.dto.aochat;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Generic API response wrapper matching ao-chat frontend expectations.
 * Format: { success: boolean, data?: T, error?: string, errorType?: string, message?: string, chatId?: string }
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record AoChatApiResponse<T>(
        boolean success,
        T data,
        String error,
        String errorType,
        String message,
        String chatId
) {
    public static <T> AoChatApiResponse<T> success(T data) {
        return new AoChatApiResponse<>(true, data, null, null, null, null);
    }

    public static <T> AoChatApiResponse<T> error(String error, String message) {
        return new AoChatApiResponse<>(false, null, error, null, message, null);
    }

    public static <T> AoChatApiResponse<T> error(String error, String message, String errorType) {
        return new AoChatApiResponse<>(false, null, error, errorType, message, null);
    }

    public static <T> AoChatApiResponse<T> errorWithChatId(String error, String message, String errorType, String chatId) {
        return new AoChatApiResponse<>(false, null, error, errorType, message, chatId);
    }
}
