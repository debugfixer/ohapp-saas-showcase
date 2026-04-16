package ru.soat.ohapp.saas.support;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Стандартный формат ответа при ошибке.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {

    /** Временная метка ошибки */
    private LocalDateTime timestamp;

    /** HTTP статус код */
    private int status;

    /** Тип ошибки (для программной обработки) */
    private String error;

    /** Человекочитаемое сообщение */
    private String message;

    /** Ошибки по полям (только для validation) */
    private Map<String, String> fieldErrors;

    /** Путь к endpoint где произошла ошибка */
    private String path;
}
