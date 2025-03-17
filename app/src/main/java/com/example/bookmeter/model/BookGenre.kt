package com.example.bookmeter.model

enum class BookGenre(val displayName: String) {
    FICTION("Fiction"),
    NON_FICTION("Non-fiction"),
    MYSTERY("Mystery"),
    THRILLER("Thriller"),
    SCIENCE_FICTION("Science Fiction"),
    FANTASY("Fantasy"),
    ROMANCE("Romance"),
    HORROR("Horror"),
    BIOGRAPHY("Biography"),
    HISTORY("History"),
    POETRY("Poetry"),
    SELF_HELP("Self-help"),
    BUSINESS("Business"),
    CHILDREN("Children's"),
    YOUNG_ADULT("Young Adult"),
    COMICS("Comics & Graphic Novels");
    
    companion object {
        fun getDisplayNames(): List<String> {
            return values().map { it.displayName }
        }
        
        fun fromDisplayName(name: String): BookGenre? {
            return values().find { it.displayName == name }
        }
        
        fun toDisplayNames(genres: List<String>): List<String> {
            return genres.mapNotNull { genreName ->
                values().find { it.name == genreName }?.displayName
            }
        }
        
        fun fromDisplayNames(displayNames: List<String>): List<String> {
            return displayNames.mapNotNull { displayName ->
                values().find { it.displayName == displayName }?.name
            }
        }
    }
}
