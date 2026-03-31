package com.bookcrossing.config;

import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * Централизованная обработка исключений для всего приложения.
 * Устраняет недочёт #6: отсутствие единого @ControllerAdvice с @ExceptionHandler.
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** Сущность не найдена (книга, пользователь, бронь и т.д.). */
    @ExceptionHandler(EntityNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String handleEntityNotFound(EntityNotFoundException ex, Model model) {
        log.warn("Сущность не найдена: {}", ex.getMessage());
        model.addAttribute("errorCode", "404");
        model.addAttribute("errorTitle", "Не найдено");
        model.addAttribute("errorMessage", ex.getMessage() != null
                ? ex.getMessage() : "Запрашиваемый объект не найден.");
        return "error";
    }

    /** Некорректные данные / нарушение бизнес-правил. */
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String handleIllegalArgument(IllegalArgumentException ex, Model model) {
        log.warn("Некорректный аргумент: {}", ex.getMessage());
        model.addAttribute("errorCode", "400");
        model.addAttribute("errorTitle", "Некорректный запрос");
        model.addAttribute("errorMessage", ex.getMessage() != null
                ? ex.getMessage() : "Запрос содержит недопустимые данные.");
        return "error";
    }

    /** Несуществующий статический ресурс. */
    @ExceptionHandler(NoResourceFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String handleNoResource(NoResourceFoundException ex, Model model) {
        log.debug("Ресурс не найден: {}", ex.getMessage());
        model.addAttribute("errorCode", "404");
        model.addAttribute("errorTitle", "Страница не найдена");
        model.addAttribute("errorMessage", "Такой страницы не существует.");
        return "error";
    }

    /**
     * Все прочие непредвиденные исключения.
     * Логируем полный стек — скрывать нельзя.
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public String handleGeneral(Exception ex, Model model) {
        log.error("Необработанное исключение: {}", ex.getMessage(), ex);
        model.addAttribute("errorCode", "500");
        model.addAttribute("errorTitle", "Внутренняя ошибка");
        model.addAttribute("errorMessage",
                "Что-то пошло не так. Попробуйте позже или обратитесь к администратору.");
        return "error";
    }
}