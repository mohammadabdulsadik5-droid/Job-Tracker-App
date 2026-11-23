package com.example.domain

object GeographyCatalog {

    const val CUSTOM_CITY_LABEL = "অন্য শহর (লিখুন)"

    private val asiaCountries = listOf(
        "UAE", "Saudi Arabia", "Qatar", "Oman", "Kuwait", "Bahrain",
        "Bangladesh", "India", "Malaysia", "Singapore", "Japan", "South Korea",
        "Thailand", "Vietnam", "Indonesia", "Philippines", "China", "Hong Kong",
        "Taiwan", "Sri Lanka", "Nepal", "Pakistan", "Maldives", "Jordan",
        "Lebanon", "Iraq", "Iran", "Israel", "Kazakhstan", "Uzbekistan",
        "Azerbaijan", "Georgia", "Armenia", "Mongolia", "Cambodia", "Laos",
        "Myanmar", "Brunei", "Timor-Leste", "Bhutan", "Yemen", "Syria",
        "Afghanistan", "Kyrgyzstan", "Tajikistan", "Turkmenistan", "Palestine",
    )

    private val europeCountries = listOf(
        "Italy", "Portugal", "Germany", "France", "Spain", "United Kingdom",
        "Netherlands", "Belgium", "Poland", "Romania", "Croatia", "Malta",
        "Greece", "Sweden", "Norway", "Denmark", "Finland", "Switzerland",
        "Austria", "Ireland", "Czechia", "Hungary", "Slovakia", "Slovenia",
        "Bulgaria", "Serbia", "Bosnia and Herzegovina", "Albania",
        "North Macedonia", "Montenegro", "Kosovo", "Moldova", "Ukraine",
        "Belarus", "Lithuania", "Latvia", "Estonia", "Iceland", "Luxembourg",
        "Cyprus", "Andorra", "Monaco", "San Marino", "Liechtenstein", "Vatican City",
    )

    private val africaCountries = listOf(
        "South Africa", "Egypt", "Morocco", "Nigeria", "Kenya", "Ghana",
        "Ethiopia", "Tanzania", "Uganda", "Algeria", "Tunisia", "Senegal",
        "Ivory Coast", "Cameroon", "Angola", "Mozambique", "Zambia", "Zimbabwe",
        "Namibia", "Botswana", "Rwanda", "Mauritius", "Madagascar", "Mali",
        "Burkina Faso", "Niger", "Chad", "Sudan", "South Sudan", "Libya",
        "Eritrea", "Djibouti", "Somalia", "Gabon", "Congo", "DR Congo",
        "Equatorial Guinea", "Central African Republic", "Benin", "Togo",
        "Sierra Leone", "Liberia", "Guinea", "Guinea-Bissau", "Gambia",
        "Cabo Verde", "Sao Tome and Principe", "Seychelles", "Comoros",
        "Burundi", "Malawi", "Lesotho", "Eswatini", "Mauritania",
    )

    private val northAmericaCountries = listOf(
        "United States", "Canada", "Mexico", "Costa Rica", "Panama", "Jamaica",
        "Dominican Republic", "Cuba", "Guatemala", "Honduras", "El Salvador",
        "Nicaragua", "Belize", "Bahamas", "Barbados", "Trinidad and Tobago",
        "Haiti", "Antigua and Barbuda", "Dominica", "Grenada",
        "Saint Kitts and Nevis", "Saint Lucia", "Saint Vincent and the Grenadines",
    )

    private val southAmericaCountries = listOf(
        "Brazil", "Argentina", "Chile", "Colombia", "Peru", "Ecuador",
        "Uruguay", "Paraguay", "Bolivia", "Venezuela", "Guyana", "Suriname",
    )

    private val oceaniaCountries = listOf(
        "Australia", "New Zealand", "Fiji", "Papua New Guinea", "Samoa",
        "Tonga", "Vanuatu", "Solomon Islands", "Micronesia", "Palau",
        "Marshall Islands", "Kiribati", "Nauru", "Tuvalu",
    )

    private val russiaCountries = listOf("Russia")
    private val turkeyCountries = listOf("Turkey")

    private val citiesByCountry: Map<String, List<String>> = mapOf(
        // Asia
        "UAE" to listOf(
            "Dubai", "Abu Dhabi", "Sharjah", "Ajman",
            "Ras Al Khaimah", "Fujairah", "Umm Al Quwain", "Al Ain",
        ),
        "Saudi Arabia" to listOf("Riyadh", "Jeddah", "Dammam", "Khobar", "Mecca", "Medina"),
        "Qatar" to listOf("Doha", "Al Wakrah", "Al Khor", "Lusail"),
        "Oman" to listOf("Muscat", "Salalah", "Sohar", "Nizwa"),
        "Kuwait" to listOf("Kuwait City", "Hawalli", "Salmiya", "Jahra"),
        "Bahrain" to listOf("Manama", "Muharraq", "Riffa"),
        "Bangladesh" to listOf("Dhaka", "Chattogram", "Gazipur", "Narayanganj", "Sylhet", "Rajshahi", "Khulna"),
        "India" to listOf("Mumbai", "Delhi", "Bengaluru", "Hyderabad", "Chennai", "Kolkata", "Pune"),
        "Malaysia" to listOf("Kuala Lumpur", "Penang", "Johor Bahru", "Shah Alam"),
        "Singapore" to listOf("Singapore"),
        "Japan" to listOf("Tokyo", "Osaka", "Yokohama", "Nagoya", "Fukuoka"),
        "South Korea" to listOf("Seoul", "Busan", "Incheon", "Daegu"),
        // Europe
        "Italy" to listOf("Milan", "Rome", "Turin", "Bologna", "Florence", "Naples", "Venice", "Verona", "Biella", "Prato"),
        "Portugal" to listOf("Lisbon", "Porto", "Braga", "Guimaraes", "Coimbra", "Faro"),
        "Germany" to listOf("Berlin", "Munich", "Frankfurt", "Hamburg", "Stuttgart", "Cologne", "Dusseldorf"),
        "France" to listOf("Paris", "Lyon", "Marseille", "Toulouse", "Nice", "Lille"),
        "Spain" to listOf("Madrid", "Barcelona", "Valencia", "Seville", "Bilbao"),
        "United Kingdom" to listOf("London", "Manchester", "Birmingham", "Leeds", "Edinburgh", "Glasgow"),
        "Netherlands" to listOf("Amsterdam", "Rotterdam", "Utrecht", "Eindhoven", "The Hague"),
        "Belgium" to listOf("Brussels", "Antwerp", "Ghent", "Bruges"),
        "Poland" to listOf("Warsaw", "Krakow", "Wroclaw", "Gdansk", "Lodz"),
        "Romania" to listOf("Bucharest", "Cluj-Napoca", "Timisoara", "Iasi"),
        "Croatia" to listOf("Zagreb", "Split", "Rijeka"),
        "Malta" to listOf("Valletta", "Sliema", "Birkirkara"),
        "Greece" to listOf("Athens", "Thessaloniki", "Heraklion"),
        "Sweden" to listOf("Stockholm", "Gothenburg", "Malmo"),
        "Norway" to listOf("Oslo", "Bergen", "Trondheim"),
        "Denmark" to listOf("Copenhagen", "Aarhus", "Odense"),
        "Finland" to listOf("Helsinki", "Tampere", "Espoo"),
        "Switzerland" to listOf("Zurich", "Geneva", "Basel", "Bern"),
        "Austria" to listOf("Vienna", "Graz", "Linz", "Salzburg"),
        "Ireland" to listOf("Dublin", "Cork", "Galway"),
        "Czechia" to listOf("Prague", "Brno", "Ostrava"),
        "Hungary" to listOf("Budapest", "Debrecen"),
        // Africa
        "South Africa" to listOf("Johannesburg", "Cape Town", "Durban", "Pretoria"),
        "Egypt" to listOf("Cairo", "Alexandria", "Giza"),
        "Morocco" to listOf("Casablanca", "Rabat", "Marrakech", "Tangier"),
        "Nigeria" to listOf("Lagos", "Abuja", "Port Harcourt"),
        "Kenya" to listOf("Nairobi", "Mombasa"),
        // North America
        "United States" to listOf("New York", "Los Angeles", "Chicago", "Houston", "Miami", "Dallas"),
        "Canada" to listOf("Toronto", "Vancouver", "Montreal", "Calgary", "Ottawa"),
        "Mexico" to listOf("Mexico City", "Monterrey", "Guadalajara", "Cancun"),
        // South America
        "Brazil" to listOf("Sao Paulo", "Rio de Janeiro", "Brasilia", "Curitiba"),
        "Argentina" to listOf("Buenos Aires", "Cordoba", "Rosario"),
        "Chile" to listOf("Santiago", "Valparaiso"),
        "Colombia" to listOf("Bogota", "Medellin", "Cali"),
        // Oceania
        "Australia" to listOf("Sydney", "Melbourne", "Brisbane", "Perth", "Adelaide"),
        "New Zealand" to listOf("Auckland", "Wellington", "Christchurch"),
        // Russia & Turkey
        "Russia" to listOf("Moscow", "Saint Petersburg", "Novosibirsk", "Yekaterinburg", "Kazan", "Sochi"),
        "Turkey" to listOf("Istanbul", "Ankara", "Izmir", "Bursa", "Antalya", "Gaziantep", "Denizli", "Adana"),
    )

    fun countriesForRegion(region: Region): List<String> = when (region) {
        Region.ALL -> emptyList()
        Region.ASIA -> asiaCountries
        Region.EUROPE -> europeCountries
        Region.AFRICA -> africaCountries
        Region.NAMERICA -> northAmericaCountries
        Region.SAMERICA -> southAmericaCountries
        Region.OCEANIA -> oceaniaCountries
        Region.RUSSIA -> russiaCountries
        Region.TURKEY -> turkeyCountries
    }

    fun citiesForCountry(country: String): List<String> {
        return citiesByCountry[country].orEmpty()
    }
}
