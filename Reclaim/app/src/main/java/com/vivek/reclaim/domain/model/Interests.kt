package com.vivek.reclaim.domain.model

/**
 * The interests a user can pick from. Each one needs at least two feeds in assets/feeds.json, which a unit test
 * checks, so an interest can never be offered without articles behind it.
 */
object Interests {
    val all: List<String> = listOf(
        "Technology",
        "Startups",
        "Fitness",
        "Finance",
        "Science",
        "Motivation",
        "Movies",
        "Sports",
        "Gaming",
        "History",
        "Health",
        "Productivity",
        "Space",
        "Psychology",
        "Books",
        "Travel",
        "Food",
        "Music",
        "Design",
        "Environment",
    )
}
