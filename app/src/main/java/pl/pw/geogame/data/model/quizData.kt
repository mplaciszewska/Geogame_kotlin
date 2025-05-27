package pl.pw.geogame.data.model

data class QuizData(
    val id: Int,
    val latitude: Double,
    val longitude: Double,
    val floor: FloorLevel,
    val question: String,
    val answers: List<String>,         // lista możliwych odpowiedzi
    val correctAnswerIndex: Int,       // indeks poprawnej odpowiedzi
    val difficulty: DifficultyLevel,
    var isVisited: Boolean = false
)

enum class DifficultyLevel(val points: Int) {
    EASY(10),
    MEDIUM(20),
    HARD(30)
}
enum class FloorLevel(val level: Int) {
    FLOOR_0(0),
    FLOOR_1(1),
    FLOOR_2(2),
    FLOOR_3(3),
    FLOOR_4(4)
}

val quizQuestions = mutableListOf(
    QuizData(
        id = 1,
        latitude = 52.220730290226065,
        longitude = 21.010318215414,
        floor = FloorLevel.FLOOR_0,
        question = "W którym roku założono Politechnikę Warszawską?",
        answers = listOf("1826", "1898", "1915", "1945"),
        correctAnswerIndex = 2,
        difficulty = DifficultyLevel.MEDIUM
    ),
    QuizData(
        id = 2,
        latitude = 52.22091569442403,
        longitude = 21.010269669646306,
        floor = FloorLevel.FLOOR_0,
        question = "Ile wydziałów ma Politechnika Warszawska?",
        answers = listOf("18", "21", "20", "19"),
        correctAnswerIndex = 3,
        difficulty = DifficultyLevel.MEDIUM
    ),
    QuizData(
        id = 3,
        latitude = 52.22072614916491,
        longitude = 21.009928371520378,
        floor = FloorLevel.FLOOR_1,
        question = "W którym roku Politechnika Warszawska otrzymała status uczelni badawczej?",
        answers = listOf("2019", "2020", "2016", "2023"),
        correctAnswerIndex = 0,
        difficulty = DifficultyLevel.HARD
    ),
    QuizData(
        id = 4,
        latitude = 52.220348696143816,
        longitude = 21.010110096524247,
        floor = FloorLevel.FLOOR_1,
        question = "Kto jest obecnym rektorem Politechniki Warszawskiej?",
        answers = listOf("prof. dr hab. inż. Jan Szmidt", "prof. dr hab. inż. Krzysztof Zaremba", "prof. dr hab. inż. Krzysztof Bakuła"),
        correctAnswerIndex = 1,
        difficulty = DifficultyLevel.EASY
    ),
    QuizData(
        id = 5,
        latitude = 52.220593531076695,
        longitude = 21.0093885823082,
        floor = FloorLevel.FLOOR_2,
        question = "W jakich 4 głównych kolorach jest sztandar Politechniki Warszawskiej?",
        answers = listOf("biały, czerwony, żółty, granatowy", "biały, czerwony, czarny, żółty", "biały, czarny, żółty, granatowy"),
        correctAnswerIndex = 0,
        difficulty = DifficultyLevel.MEDIUM
    ),
    QuizData(
        id = 6,
        latitude = 52.221119346506754,
        longitude = 21.00990356642956,
        floor = FloorLevel.FLOOR_3,
        question = "W którym roku Politechnika Warszawska otrzymała status uczelni badawczej?",
        answers = listOf("2019", "2020", "2016", "2023"),
        correctAnswerIndex = 0,
        difficulty = DifficultyLevel.MEDIUM
    ),
    QuizData(
        id = 7,
        latitude = 52.221013390347714,
        longitude = 21.010025609820982,
        floor = FloorLevel.FLOOR_3,
        question = "W którym roku powołano Muzeum Politechniki Warszawskiej?",
        answers = listOf("1995", "1978", "2003", "1983"),
        correctAnswerIndex = 1,
        difficulty = DifficultyLevel.MEDIUM
    )

)