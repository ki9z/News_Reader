package com.data.model

data class LocalCity(
    val title: String,
    val query: String,
    val countryCode: String,
    val countryName: String,
    val continent: String,
    val aliases: Set<String> = emptySet(),
    val countryHints: Set<String> = emptySet(),
    val isDefault: Boolean = false
)

object LocalCityCatalog {
    val majorCities: List<LocalCity> = listOf(
        // Asia
        LocalCity("Ha Noi", "Ha Noi", "vn", "Vietnam", "Asia", setOf("Hanoi", "Ha Noi", "Hà Nội"), setOf("Vietnam", "Viet Nam", "VN"), true),
        LocalCity("Ho Chi Minh City", "Ho Chi Minh City", "vn", "Vietnam", "Asia", setOf("HCMC", "Sai Gon", "Saigon", "TPHCM"), setOf("Vietnam", "Viet Nam", "VN"), true),
        LocalCity("Tokyo", "Tokyo", "jp", "Japan", "Asia", setOf("Tokyo"), setOf("Japan"), true),
        LocalCity("Seoul", "Seoul", "kr", "South Korea", "Asia", setOf("Seoul"), setOf("South Korea", "Korea"), true),
        LocalCity("Singapore", "Singapore", "sg", "Singapore", "Asia", setOf("Singapore"), setOf("Singapore"), true),
        LocalCity("Bangkok", "Bangkok", "th", "Thailand", "Asia", setOf("Bangkok"), setOf("Thailand")),
        LocalCity("Jakarta", "Jakarta", "id", "Indonesia", "Asia", setOf("Jakarta"), setOf("Indonesia")),
        LocalCity("Manila", "Manila", "ph", "Philippines", "Asia", setOf("Manila"), setOf("Philippines")),
        LocalCity("Kuala Lumpur", "Kuala Lumpur", "my", "Malaysia", "Asia", setOf("Kuala Lumpur", "KL"), setOf("Malaysia")),
        LocalCity("Mumbai", "Mumbai", "in", "India", "Asia", setOf("Mumbai", "Bombay"), setOf("India")),
        LocalCity("Dubai", "Dubai", "ae", "United Arab Emirates", "Asia", setOf("Dubai"), setOf("United Arab Emirates", "UAE"), true),
        LocalCity("Hong Kong", "Hong Kong", "hk", "Hong Kong", "Asia", setOf("Hong Kong"), setOf("Hong Kong")),

        // Europe
        LocalCity("London", "London", "gb", "United Kingdom", "Europe", setOf("London"), setOf("United Kingdom", "UK", "Britain"), true),
        LocalCity("Paris", "Paris", "fr", "France", "Europe", setOf("Paris"), setOf("France"), true),
        LocalCity("Berlin", "Berlin", "de", "Germany", "Europe", setOf("Berlin"), setOf("Germany")),
        LocalCity("Madrid", "Madrid", "es", "Spain", "Europe", setOf("Madrid"), setOf("Spain")),
        LocalCity("Rome", "Rome", "it", "Italy", "Europe", setOf("Rome"), setOf("Italy")),
        LocalCity("Amsterdam", "Amsterdam", "nl", "Netherlands", "Europe", setOf("Amsterdam"), setOf("Netherlands")),
        LocalCity("Istanbul", "Istanbul", "tr", "Turkey", "Europe", setOf("Istanbul"), setOf("Turkey")),
        LocalCity("Moscow", "Moscow", "ru", "Russia", "Europe", setOf("Moscow"), setOf("Russia")),

        // Africa
        LocalCity("Cairo", "Cairo", "eg", "Egypt", "Africa", setOf("Cairo"), setOf("Egypt"), true),
        LocalCity("Johannesburg", "Johannesburg", "za", "South Africa", "Africa", setOf("Johannesburg", "Joburg"), setOf("South Africa"), true),
        LocalCity("Lagos", "Lagos", "ng", "Nigeria", "Africa", setOf("Lagos"), setOf("Nigeria")),
        LocalCity("Nairobi", "Nairobi", "ke", "Kenya", "Africa", setOf("Nairobi"), setOf("Kenya")),
        LocalCity("Casablanca", "Casablanca", "ma", "Morocco", "Africa", setOf("Casablanca"), setOf("Morocco")),
        LocalCity("Accra", "Accra", "gh", "Ghana", "Africa", setOf("Accra"), setOf("Ghana")),

        // Americas
        LocalCity("New York", "New York", "us", "United States", "Americas", setOf("New York", "NYC"), setOf("United States", "USA", "US"), true),
        LocalCity("Los Angeles", "Los Angeles", "us", "United States", "Americas", setOf("Los Angeles", "LA"), setOf("United States", "USA", "US")),
        LocalCity("Washington", "Washington", "us", "United States", "Americas", setOf("Washington", "Washington DC", "D.C."), setOf("United States", "USA", "US")),
        LocalCity("Toronto", "Toronto", "ca", "Canada", "Americas", setOf("Toronto"), setOf("Canada")),
        LocalCity("Mexico City", "Mexico City", "mx", "Mexico", "Americas", setOf("Mexico City", "CDMX"), setOf("Mexico")),
        LocalCity("Sao Paulo", "Sao Paulo", "br", "Brazil", "Americas", setOf("Sao Paulo", "Sao Paulo City"), setOf("Brazil"), true),
        LocalCity("Buenos Aires", "Buenos Aires", "ar", "Argentina", "Americas", setOf("Buenos Aires"), setOf("Argentina")),
        LocalCity("Santiago", "Santiago", "cl", "Chile", "Americas", setOf("Santiago"), setOf("Chile")),
        LocalCity("Bogota", "Bogota", "co", "Colombia", "Americas", setOf("Bogota"), setOf("Colombia")),
        LocalCity("Lima", "Lima", "pe", "Peru", "Americas", setOf("Lima"), setOf("Peru")),

        // Oceania
        LocalCity("Sydney", "Sydney", "au", "Australia", "Oceania", setOf("Sydney"), setOf("Australia"), true),
        LocalCity("Melbourne", "Melbourne", "au", "Australia", "Oceania", setOf("Melbourne"), setOf("Australia")),
        LocalCity("Brisbane", "Brisbane", "au", "Australia", "Oceania", setOf("Brisbane"), setOf("Australia")),
        LocalCity("Perth", "Perth", "au", "Australia", "Oceania", setOf("Perth"), setOf("Australia")),
        LocalCity("Auckland", "Auckland", "nz", "New Zealand", "Oceania", setOf("Auckland"), setOf("New Zealand")),
        LocalCity("Wellington", "Wellington", "nz", "New Zealand", "Oceania", setOf("Wellington"), setOf("New Zealand"))
    )

    fun defaultCities(): List<LocalCity> = majorCities.filter { it.isDefault }

    fun additionalCities(): List<LocalCity> = majorCities.filterNot { it.isDefault }
}

