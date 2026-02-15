package com.bookcrossing.model;

public enum BookGenre {
    FICTION("Художественная литература"),
    SCIENCE("Научная литература"),
    FANTASY("Фантастика"),
    HISTORY("История"),
    BIOGRAPHY("Биографии"),
    BUSINESS("Бизнес"),
    KIDS("Детские книги"),
    OTHER("Другое");

    private final String displayValue;

    BookGenre(String displayValue) {
        this.displayValue = displayValue;
    }

    public String getDisplayValue() {
        return displayValue;
    }
}