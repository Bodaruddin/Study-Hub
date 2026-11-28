package com.example.data.remote

import com.example.data.local.CachedChapterEntity
import com.example.data.local.CachedClassEntity
import com.example.data.local.CachedExamPaperEntity
import com.example.data.local.CachedMaterialEntity
import com.example.data.local.CachedQuestionEntity
import com.example.data.local.CachedQuizEntity
import com.example.data.local.CachedSubjectEntity
import com.example.data.model.AnnouncementItem
import com.example.data.model.AppSettings
import com.example.data.model.MaterialType
import com.example.data.model.PaperCategory
import com.example.data.model.QuestionType

object SeedCurriculumData {

    val classes: List<CachedClassEntity> = (6L..12L).map { grade ->
        CachedClassEntity(
            id = "class_$grade",
            grade = grade,
            name = "Class $grade",
            description = when (grade) {
                6L -> "Middle school foundation across English, Science, Maths & Social Studies."
                7L -> "Core conceptual building blocks for Classes 7 CBSE & ICSE learners."
                8L -> "Pre-high school mastery with algebra, forces, and literature."
                9L -> "High school readiness with geometry, physics, and structured writing."
                10L -> "Board exam preparation with previous-year papers & revision guides."
                11L -> "Senior secondary specialization in STEM, Humanities & Languages."
                else -> "Final board mastery, full-length mock tests & formula sheets."
            },
            studentCount = 0L,
            published = true
        )
    }

    val subjects: List<CachedSubjectEntity> = buildList {
        val core = listOf(
            Triple("English", "Chapters • Notes • Q&A", "book" to "blue"),
            Triple("Science", "Notes • Summary • MCQs", "science" to "green"),
            Triple("Mathematics", "Formulas • Examples • Practice", "calculate" to "orange"),
            Triple("Social Science", "Notes • Questions • Maps", "globe" to "purple"),
            Triple("Hindi", "Notes • Grammar • Questions", "hindi" to "pink")
        )
        for (grade in 6L..12L) {
            core.forEach { (name, subtitle, style) ->
                val slug = name.lowercase().replace(" ", "_")
                add(
                    CachedSubjectEntity(
                        id = "sub_${slug}_$grade",
                        classGrade = grade,
                        name = name,
                        subtitle = subtitle,
                        iconKey = style.first,
                        colorKey = style.second,
                        chapterCount = if (grade == 7L) 5L else 3L,
                        published = true
                    )
                )
            }
        }
    }

    val chapters: List<CachedChapterEntity> = buildList {
        // Rich Class 7 Chapters matching screenshots + multi-class chapters
        add(CachedChapterEntity("chap_eng_7_1", 7L, "sub_english_7", "English", 1L, "Three Questions", "A king seeks wisdom about the right time, right people, and most important action.", true))
        add(CachedChapterEntity("chap_eng_7_2", 7L, "sub_english_7", "English", 2L, "A Gift of Chappals", "Compassion, humor, and childhood empathy told through Ravi and Mridu.", true))
        add(CachedChapterEntity("chap_eng_7_3", 7L, "sub_english_7", "English", 3L, "Chapter 3: The Selfish Giant", "Oscar Wilde's timeless allegory of generosity, spring, and transformation.", true))
        add(CachedChapterEntity("chap_eng_7_4", 7L, "sub_english_7", "English", 4L, "Past & Perfect Tenses", "Mastering Past Indefinite, Past Perfect, and Present Perfect tense usage.", true))

        add(CachedChapterEntity("chap_sci_7_1", 7L, "sub_science_7", "Science", 1L, "Nutrition in Plants", "Autotrophic photosynthesis, chlorophyll, and symbiotic relationships.", true))
        add(CachedChapterEntity("chap_sci_7_5", 7L, "sub_science_7", "Science", 5L, "Chapter 5: Acids, Bases and Salts", "Natural indicators, neutralization reactions, and everyday chemistry.", true))

        add(CachedChapterEntity("chap_math_7_1", 7L, "sub_mathematics_7", "Mathematics", 1L, "Integers & Number Line", "Properties of addition, subtraction, multiplication and division of integers.", true))
        add(CachedChapterEntity("chap_math_7_6", 7L, "sub_mathematics_7", "Mathematics", 6L, "The Triangle and its Properties", "Medians, altitudes, exterior angle theorem, and Pythagoras property.", true))

        add(CachedChapterEntity("chap_sst_7_2", 7L, "sub_social_science_7", "Social Science", 2L, "Inside Our Earth", "Crust, mantle, core, igneous, sedimentary, and metamorphic rocks.", true))
        add(CachedChapterEntity("chap_hin_7_1", 7L, "sub_hindi_7", "Hindi", 1L, "Hum Panchhi Unmukt Gagan Ke", "Freedom, nature imagery, and poetic appreciation.", true))

        // Representative chapters for Classes 6, 8, 9, 10, 11, 12
        for (grade in listOf(6L, 8L, 9L, 10L, 11L, 12L)) {
            add(CachedChapterEntity("chap_eng_${grade}_1", grade, "sub_english_$grade", "English", 1L, "Reading & Grammar Mastery", "Comprehension, active/passive voice, and essay structure for Class $grade.", true))
            add(CachedChapterEntity("chap_sci_${grade}_1", grade, "sub_science_$grade", "Science", 1L, "Core Science Concepts", "Foundational laws, experiments, and real-world applications for Class $grade.", true))
            add(CachedChapterEntity("chap_math_${grade}_1", grade, "sub_mathematics_$grade", "Mathematics", 1L, "Algebra & Formula Sheet", "Step-by-step solved examples and key identities for Class $grade.", true))
            add(CachedChapterEntity("chap_sst_${grade}_1", grade, "sub_social_science_$grade", "Social Science", 1L, "History & Civics Essentials", "Timelines, constitutional principles, and map work for Class $grade.", true))
            add(CachedChapterEntity("chap_hin_${grade}_1", grade, "sub_hindi_$grade", "Hindi", 1L, "Vyakaran & Sahitya", "Sandhi, Samas, and prose comprehension for Class $grade.", true))
        }
    }

    val materials: List<CachedMaterialEntity> = buildList {
        add(
            CachedMaterialEntity(
                id = "mat_eng_7_short",
                classGrade = 7L,
                subjectId = "sub_english_7",
                subjectName = "English",
                chapterId = "chap_eng_7_3",
                chapterTitle = "Chapter 3: The Selfish Giant",
                title = "English Chapter 3 Short Notes",
                description = "Concise revision summary, themes, and moral takeaways of The Selfish Giant.",
                materialType = MaterialType.SHORT_NOTES.key,
                contentMarkdown = """
                    # The Selfish Giant — Short Notes

                    **Author:** Oscar Wilde

                    ## Central Theme
                    Selfishness isolates us and brings perpetual winter, while kindness and sharing invite warmth, joy, and spring back into life.

                    ## Key Plot Points
                    - **The Garden:** A lush, blooming garden where children love to play every afternoon after school.
                    - **The Wall:** Returning after seven years, the Giant builds a high wall and puts up a *"Trespassers will be prosecuted"* notice.
                    - **Perpetual Winter:** Spring refuses to enter the Giant's garden; only Snow, Frost, North Wind, and Hail remain.
                    - **The Turning Point:** Children sneak in through a small hole in the wall. A tiny boy cannot climb a frost-covered tree until the Giant gently lifts him up.
                    - **Transformation:** The Giant knocks down the wall forever. Years later, the same child returns with marks of love to welcome him to Paradise.

                    ## Key Vocabulary
                    - **Trespassers:** People entering someone's land without permission.
                    - **Linnet:** A small brown songbird.
                    - **Hail:** Pellets of frozen rain falling from clouds.
                    - **Feeble:** Physically weak, especially from age.
                """.trimIndent(),
                fileSizeLabel = "1.1 MB",
                tagsCsv = "literature|short-notes|class-7",
                important = true,
                premiumOnly = false,
                published = true,
                downloadedOffline = true,
                downloadedAtMillis = System.currentTimeMillis() - 86_400_000L
            )
        )
        add(
            CachedMaterialEntity(
                id = "mat_eng_7_detailed",
                classGrade = 7L,
                subjectId = "sub_english_7",
                subjectName = "English",
                chapterId = "chap_eng_7_3",
                chapterTitle = "Chapter 3: The Selfish Giant",
                title = "Detailed Analysis & Character Study",
                description = "Line-by-line literary analysis, character arc of the Giant, and symbolism.",
                materialType = MaterialType.DETAILED_NOTES.key,
                contentMarkdown = """
                    # Detailed Literary Analysis

                    ## Character Evolution of the Giant
                    1. **Possessive & Gruff:** Initially claims *"My own garden is my own garden"* and isolates himself behind stone walls.
                    2. **Reflective:** Realizes why spring skipped his garden—*"How selfish I have been!"*—upon seeing a single corner bloom where children sit.
                    3. **Compassionate & Humble:** Knocks down the wall with a great axe and becomes the children's dearest playmate.

                    ## Symbolism
                    - **Winter & Frost:** Emotional coldness and consequences of greed.
                    - **Blossoms & Birdsong:** Joy, innocence, and community warmth.
                    - **The Little Boy:** Unconditional grace and redemption.
                """.trimIndent(),
                fileSizeLabel = "1.8 MB",
                tagsCsv = "analysis|character-study",
                important = false,
                premiumOnly = false,
                published = true
            )
        )
        add(
            CachedMaterialEntity(
                id = "mat_eng_7_qa",
                classGrade = 7L,
                subjectId = "sub_english_7",
                subjectName = "English",
                chapterId = "chap_eng_7_3",
                chapterTitle = "Chapter 3: The Selfish Giant",
                title = "The Selfish Giant — Q&A Set",
                description = "Exam-ready short and long question-answer solutions.",
                materialType = MaterialType.QA_SET.key,
                contentMarkdown = """
                    # Important Questions & Answers

                    **Q1. Why is the Giant called selfish?**
                    Ans: He forbade children from playing in his lovely garden and built a high wall around it so no one else could enjoy it.

                    **Q2. When did spring finally return to the Giant's garden?**
                    Ans: Spring returned the moment children crept back into the garden through a little hole in the wall and climbed the trees.

                    **Q3. Why was the little boy crying under the tree?**
                    Ans: He was so tiny that he could not reach the branches of the tree, which remained covered in frost and snow until the Giant lifted him up.
                """.trimIndent(),
                fileSizeLabel = "1.4 MB",
                tagsCsv = "qa|exam-prep",
                important = true,
                premiumOnly = false,
                published = true
            )
        )
        add(
            CachedMaterialEntity(
                id = "mat_eng_7_tenses",
                classGrade = 7L,
                subjectId = "sub_english_7",
                subjectName = "English",
                chapterId = "chap_eng_7_4",
                chapterTitle = "Past & Perfect Tenses",
                title = "Grammar Study Guide: Past Tenses",
                description = "Complete rules & examples for Past Indefinite vs Past Perfect Tense.",
                materialType = MaterialType.STUDY_GUIDE.key,
                contentMarkdown = """
                    # Mastering Past Tenses

                    ## 1. Past Perfect Tense (`had + V3`)
                    Used for an action **completed before another past action**.
                    - *Example:* The Giant **had built** the wall before spring arrived.
                    - *Example:* The train **had left** before we reached the station.

                    ## 2. Past Indefinite / Simple Past (`V2`)
                    Used for a single completed action at a specific time in the past.
                    - *Example:* The children **played** in the garden yesterday.

                    ## 3. Present Perfect Tense (`has/have + V3`)
                    Connects a past action to the present moment.
                    - *Example:* Spring **has arrived** in the garden now.
                """.trimIndent(),
                fileSizeLabel = "950 KB",
                tagsCsv = "grammar|tenses|study-guide",
                important = true,
                premiumOnly = false,
                published = true
            )
        )
        add(
            CachedMaterialEntity(
                id = "mat_sci_7_imp",
                classGrade = 7L,
                subjectId = "sub_science_7",
                subjectName = "Science",
                chapterId = "chap_sci_7_5",
                chapterTitle = "Chapter 5: Acids, Bases and Salts",
                title = "Science Chapter 5 Important Questions",
                description = "High-yield board-style questions on Acids, Bases, Litmus, and Neutralization.",
                materialType = MaterialType.IMPORTANT_QUESTIONS.key,
                contentMarkdown = """
                    # Acids, Bases & Salts — Important Questions

                    **Q1. What is the difference between an acid and a base?**
                    - **Acids** taste sour and turn **blue litmus red** (e.g., lemon juice, vinegar, HCl).
                    - **Bases** taste bitter, feel soapy, and turn **red litmus blue** (e.g., baking soda, soap solution, NaOH).

                    **Q2. Define Neutralization with an everyday example.**
                    When an acid and a base react in equal strength, they form **salt + water + heat**.
                    - *Everyday example:* Taking Milk of Magnesia (a mild base) to relieve stomach acidity!
                """.trimIndent(),
                fileSizeLabel = "1.6 MB",
                tagsCsv = "chemistry|important|class-7",
                important = true,
                premiumOnly = false,
                published = true,
                downloadedOffline = true,
                downloadedAtMillis = System.currentTimeMillis() - 172_800_000L
            )
        )
        add(
            CachedMaterialEntity(
                id = "mat_sci_7_mindmap",
                classGrade = 7L,
                subjectId = "sub_science_7",
                subjectName = "Science",
                chapterId = "chap_sci_7_5",
                chapterTitle = "Chapter 5: Acids, Bases and Salts",
                title = "Indicators & pH Mind Map",
                description = "Visual color-change cheat sheet for Litmus, Turmeric, and China Rose.",
                materialType = MaterialType.MIND_MAP.key,
                contentMarkdown = """
                    # Natural Indicators Cheat Sheet

                    - **Litmus (Lichen):** Acid -> Red | Base -> Blue
                    - **Turmeric:** Acid -> Remains Yellow | Base -> Turns Deep Red
                    - **China Rose (Hibiscus):** Acid -> Dark Pink (Magenta) | Base -> Green
                    - **Phenolphthalein:** Acid -> Colorless | Base -> Pink
                """.trimIndent(),
                fileSizeLabel = "820 KB",
                tagsCsv = "mind-map|indicators",
                important = false,
                premiumOnly = false,
                published = true
            )
        )
        add(
            CachedMaterialEntity(
                id = "mat_math_7_formulas",
                classGrade = 7L,
                subjectId = "sub_mathematics_7",
                subjectName = "Mathematics",
                chapterId = "chap_math_7_6",
                chapterTitle = "The Triangle and its Properties",
                title = "Mathematics Formulas & Revision Notes",
                description = "Angle sum property, exterior angle rule, and Pythagoras theorem formulas.",
                materialType = MaterialType.REVISION_NOTES.key,
                contentMarkdown = """
                    # Triangle Properties — Formula Sheet

                    1. **Angle Sum Property:** `∠A + ∠B + ∠C = 180°`
                    2. **Exterior Angle Theorem:** An exterior angle equals the sum of its two interior opposite angles.
                    3. **Triangle Inequality:** Sum of lengths of any two sides is always greater than the third side (`a + b > c`).
                    4. **Pythagoras Property (Right Triangle):** `Hypotenuse² = Base² + Perpendicular²`
                    5. **Equilateral Triangle:** All 3 sides equal; every angle = `60°`.
                """.trimIndent(),
                fileSizeLabel = "1.3 MB",
                tagsCsv = "formulas|geometry|revision",
                important = true,
                premiumOnly = false,
                published = true,
                downloadedOffline = true,
                downloadedAtMillis = System.currentTimeMillis() - 259_200_000L
            )
        )
        add(
            CachedMaterialEntity(
                id = "mat_sst_7_summary",
                classGrade = 7L,
                subjectId = "sub_social_science_7",
                subjectName = "Social Science",
                chapterId = "chap_sst_7_2",
                chapterTitle = "Inside Our Earth",
                title = "SST Chapter 2 Summary",
                description = "Complete summary of Earth's layers and the rock cycle.",
                materialType = MaterialType.CHAPTER_SUMMARY.key,
                contentMarkdown = """
                    # Inside Our Earth — Chapter Summary

                    ## Layers of the Earth
                    - **Crust:** Thinnest outermost solid layer (~35 km continental, ~5 km oceanic; SIAL & SIMA).
                    - **Mantle:** Extends down to 2,900 km below the crust.
                    - **Core:** Innermost high-temperature, high-pressure layer made of Nickel and Iron (NIFE).

                    ## Three Major Rock Types
                    1. **Igneous Rocks:** Formed from cooling molten magma (Granite, Basalt).
                    2. **Sedimentary Rocks:** Compressed layers of rock fragments & fossils (Sandstone, Limestone).
                    3. **Metamorphic Rocks:** Changed under heat & pressure (Marble from Limestone, Slate from Clay).
                """.trimIndent(),
                fileSizeLabel = "2.0 MB",
                tagsCsv = "geography|summary|sst",
                important = false,
                premiumOnly = false,
                published = true
            )
        )
        add(
            CachedMaterialEntity(
                id = "mat_math_7_premium",
                classGrade = 7L,
                subjectId = "sub_mathematics_7",
                subjectName = "Mathematics",
                chapterId = "chap_math_7_6",
                chapterTitle = "The Triangle and its Properties",
                title = "Olympiad Problem Workbook (PDF Guide)",
                description = "Advanced solved Olympiad geometry problems with step-by-step proofs.",
                materialType = MaterialType.PDF_GUIDE.key,
                contentMarkdown = """
                    # Olympiad Geometry Master Workbook

                    ## Strategy 1: Auxiliary Lines
                    Constructing a parallel line through the vertex opposite the base unlocks alternate interior angles immediately.

                    ## Strategy 2: Isosceles Symmetry
                    Angles opposite equal sides are equal; dropping an altitude bisects both the base and vertex angle.
                """.trimIndent(),
                fileSizeLabel = "3.4 MB",
                tagsCsv = "olympiad|pdf|premium",
                important = true,
                premiumOnly = true,
                published = true
            )
        )

        // Add materials for Classes 6, 8, 9, 10, 11, 12 so every class filter is rich!
        for (grade in listOf(6L, 8L, 9L, 10L, 11L, 12L)) {
            add(
                CachedMaterialEntity(
                    id = "mat_eng_${grade}_notes",
                    classGrade = grade,
                    subjectId = "sub_english_$grade",
                    subjectName = "English",
                    chapterId = "chap_eng_${grade}_1",
                    chapterTitle = "Reading & Grammar Mastery",
                    title = "Class $grade English Short Notes",
                    description = "Grammar rules, comprehension strategies, and formal writing format.",
                    materialType = MaterialType.SHORT_NOTES.key,
                    contentMarkdown = "# Class $grade English Short Notes\n\nFocus on active/passive voice transitions, clause connectors, and structured 3-paragraph essay planning.",
                    fileSizeLabel = "1.1 MB",
                    tagsCsv = "english|class-$grade",
                    important = true,
                    premiumOnly = false,
                    published = true
                )
            )
            add(
                CachedMaterialEntity(
                    id = "mat_sci_${grade}_notes",
                    classGrade = grade,
                    subjectId = "sub_science_$grade",
                    subjectName = "Science",
                    chapterId = "chap_sci_${grade}_1",
                    chapterTitle = "Core Science Concepts",
                    title = "Class $grade Science Revision Guide",
                    description = "Essential laws, definitions, and experiment summaries.",
                    materialType = MaterialType.REVISION_NOTES.key,
                    contentMarkdown = "# Class $grade Science Revision\n\nReview key definitions, balanced chemical equations, SI units, and diagram labels before every chapter test.",
                    fileSizeLabel = "1.7 MB",
                    tagsCsv = "science|class-$grade",
                    important = true,
                    premiumOnly = false,
                    published = true
                )
            )
            add(
                CachedMaterialEntity(
                    id = "mat_math_${grade}_notes",
                    classGrade = grade,
                    subjectId = "sub_mathematics_$grade",
                    subjectName = "Mathematics",
                    chapterId = "chap_math_${grade}_1",
                    chapterTitle = "Algebra & Formula Sheet",
                    title = "Class $grade Mathematics Formula Bank",
                    description = "Must-know identities, equations, and quick problem-solving shortcuts.",
                    materialType = MaterialType.STUDY_GUIDE.key,
                    contentMarkdown = "# Class $grade Mathematics Formula Bank\n\n1. `(a + b)^2 = a^2 + 2ab + b^2`\n2. `(a - b)^2 = a^2 - 2ab + b^2`\n3. `a^2 - b^2 = (a + b)(a - b)`",
                    fileSizeLabel = "1.5 MB",
                    tagsCsv = "math|formulas|class-$grade",
                    important = true,
                    premiumOnly = false,
                    published = true
                )
            )
        }
    }

    val quizzes: List<CachedQuizEntity> = listOf(
        CachedQuizEntity(
            id = "quiz_eng_7_tenses",
            classGrade = 7L,
            subjectId = "sub_english_7",
            subjectName = "English",
            chapterId = "chap_eng_7_4",
            chapterTitle = "Past & Perfect Tenses",
            title = "Daily Challenge: Grammar & Tenses",
            description = "Test your mastery of Past, Present, and Perfect Tenses.",
            durationMinutes = 8L,
            questionCount = 5L,
            premiumOnly = false,
            published = true
        ),
        CachedQuizEntity(
            id = "quiz_sci_7_acids",
            classGrade = 7L,
            subjectId = "sub_science_7",
            subjectName = "Science",
            chapterId = "chap_sci_7_5",
            chapterTitle = "Chapter 5: Acids, Bases and Salts",
            title = "Acids, Bases & Indicators Quiz",
            description = "MCQs, True/False, Multi-select & Fill-in-the-blank practice.",
            durationMinutes = 10L,
            questionCount = 4L,
            premiumOnly = false,
            published = true
        ),
        CachedQuizEntity(
            id = "quiz_math_7_triangles",
            classGrade = 7L,
            subjectId = "sub_mathematics_7",
            subjectName = "Mathematics",
            chapterId = "chap_math_7_6",
            chapterTitle = "The Triangle and its Properties",
            title = "Triangle Properties Sprint",
            description = "Angles, Pythagoras property, and triangle inequalities.",
            durationMinutes = 12L,
            questionCount = 4L,
            premiumOnly = false,
            published = true
        ),
        CachedQuizEntity(
            id = "quiz_class10_stem",
            classGrade = 10L,
            subjectId = "sub_science_10",
            subjectName = "Science",
            chapterId = "chap_sci_10_1",
            chapterTitle = "Core Science Concepts",
            title = "Class 10 Board Prep Mock Quiz",
            description = "Mixed high-yield conceptual questions for Class 10.",
            durationMinutes = 15L,
            questionCount = 3L,
            premiumOnly = false,
            published = true
        )
    )

    val questions: List<CachedQuestionEntity> = listOf(
        // English Tenses Quiz (Matches Screenshot 2 Quiz screen!)
        CachedQuestionEntity(
            id = "q_eng_1",
            quizId = "quiz_eng_7_tenses",
            questionType = QuestionType.MCQ.key,
            prompt = "Which sentence correctly uses the Simple Past Tense?",
            optionsJoined = "She walks to school yesterday.||She walked to school yesterday.||She had walk to school yesterday.||She is walking to school yesterday.",
            correctIndicesJoined = "1",
            correctText = "",
            explanation = "Simple Past uses the V2 verb form ('walked') for a completed past event.",
            points = 20L,
            published = true
        ),
        CachedQuestionEntity(
            id = "q_eng_2",
            quizId = "quiz_eng_7_tenses",
            questionType = QuestionType.MCQ.key,
            prompt = "Which tense is used for completed action in the past?",
            optionsJoined = "Present Indefinite Tense||Past Perfect Tense||Present Perfect Tense||Future Indefinite Tense",
            correctIndicesJoined = "1",
            correctText = "",
            explanation = "Past Perfect Tense is used for an action completed before another past action.",
            points = 20L,
            published = true
        ),
        CachedQuestionEntity(
            id = "q_eng_3",
            quizId = "quiz_eng_7_tenses",
            questionType = QuestionType.TRUE_FALSE.key,
            prompt = "True or False: 'Had + past participle (V3)' is the structure of Past Perfect Tense.",
            optionsJoined = "True||False",
            correctIndicesJoined = "0",
            correctText = "",
            explanation = "Past Perfect is always formed with 'had' followed by the third form (past participle) of the verb.",
            points = 20L,
            published = true
        ),
        CachedQuestionEntity(
            id = "q_eng_4",
            quizId = "quiz_eng_7_tenses",
            questionType = QuestionType.FILL_BLANK.key,
            prompt = "Fill in the blank: The children _____ already entered the garden before the Giant returned. (had / have)",
            optionsJoined = "",
            correctIndicesJoined = "",
            correctText = "had",
            explanation = "Because entering happened before the Giant returned in the past, use 'had'.",
            points = 20L,
            published = true
        ),
        CachedQuestionEntity(
            id = "q_eng_5",
            quizId = "quiz_eng_7_tenses",
            questionType = QuestionType.MULTI_SELECT.key,
            prompt = "Select ALL sentences that are in a Past Tense:",
            optionsJoined = "Spring arrived late.||The birds are singing now.||He had built a high wall.||Tomorrow will be sunny.",
            correctIndicesJoined = "0,2",
            correctText = "",
            explanation = "'Spring arrived late' (Simple Past) and 'He had built a high wall' (Past Perfect) are both in past tenses.",
            points = 20L,
            published = true
        ),

        // Science Quiz
        CachedQuestionEntity(
            id = "q_sci_1",
            quizId = "quiz_sci_7_acids",
            questionType = QuestionType.MCQ.key,
            prompt = "Which color does blue litmus paper turn when dipped in lemon juice?",
            optionsJoined = "Green||Red||Yellow||Remains Blue",
            correctIndicesJoined = "1",
            correctText = "",
            explanation = "Lemon juice contains citric acid, and acids turn blue litmus paper red.",
            points = 25L,
            published = true
        ),
        CachedQuestionEntity(
            id = "q_sci_2",
            quizId = "quiz_sci_7_acids",
            questionType = QuestionType.TRUE_FALSE.key,
            prompt = "True or False: Turmeric turns bright red when exposed to a basic (alkaline) solution like soap water.",
            optionsJoined = "True||False",
            correctIndicesJoined = "0",
            correctText = "",
            explanation = "Turmeric is a natural indicator that remains yellow in acids/neutral solutions and turns reddish-brown in bases.",
            points = 25L,
            published = true
        ),
        CachedQuestionEntity(
            id = "q_sci_3",
            quizId = "quiz_sci_7_acids",
            questionType = QuestionType.FILL_BLANK.key,
            prompt = "Fill in the blank: The reaction between an acid and a base is called _____.",
            optionsJoined = "",
            correctIndicesJoined = "",
            correctText = "neutralization",
            explanation = "Neutralization produces salt, water, and heat when an acid reacts with a base.",
            points = 25L,
            published = true
        ),
        CachedQuestionEntity(
            id = "q_sci_4",
            quizId = "quiz_sci_7_acids",
            questionType = QuestionType.MULTI_SELECT.key,
            prompt = "Select ALL substances that are natural acids:",
            optionsJoined = "Vinegar (Acetic Acid)||Soap Solution||Curd (Lactic Acid)||Baking Soda",
            correctIndicesJoined = "0,2",
            correctText = "",
            explanation = "Vinegar contains acetic acid and curd contains lactic acid; soap and baking soda are basic.",
            points = 25L,
            published = true
        ),

        // Math Quiz
        CachedQuestionEntity(
            id = "q_math_1",
            quizId = "quiz_math_7_triangles",
            questionType = QuestionType.MCQ.key,
            prompt = "What is the sum of all three interior angles of any triangle?",
            optionsJoined = "90°||180°||270°||360°",
            correctIndicesJoined = "1",
            correctText = "",
            explanation = "By the Angle Sum Property, the three interior angles of a triangle always add up to 180°.",
            points = 25L,
            published = true
        ),
        CachedQuestionEntity(
            id = "q_math_2",
            quizId = "quiz_math_7_triangles",
            questionType = QuestionType.MCQ.key,
            prompt = "If two angles of a triangle are 50° and 60°, what is the third angle?",
            optionsJoined = "60°||70°||80°||90°",
            correctIndicesJoined = "1",
            correctText = "",
            explanation = "180° - (50° + 60°) = 70°.",
            points = 25L,
            published = true
        ),
        CachedQuestionEntity(
            id = "q_math_3",
            quizId = "quiz_math_7_triangles",
            questionType = QuestionType.TRUE_FALSE.key,
            prompt = "True or False: A triangle can have two obtuse angles.",
            optionsJoined = "True||False",
            correctIndicesJoined = "1",
            correctText = "",
            explanation = "Two obtuse angles (>90° each) would already exceed 180°, which is impossible in a triangle.",
            points = 25L,
            published = true
        ),
        CachedQuestionEntity(
            id = "q_math_4",
            quizId = "quiz_math_7_triangles",
            questionType = QuestionType.FILL_BLANK.key,
            prompt = "Fill in the blank: Each angle of an equilateral triangle measures _____ degrees.",
            optionsJoined = "",
            correctIndicesJoined = "",
            correctText = "60",
            explanation = "All three angles are equal: 180° / 3 = 60°.",
            points = 25L,
            published = true
        ),

        // Class 10 Quiz
        CachedQuestionEntity(
            id = "q_c10_1",
            quizId = "quiz_class10_stem",
            questionType = QuestionType.MCQ.key,
            prompt = "What is the SI unit of electrical resistance?",
            optionsJoined = "Volt||Ampere||Ohm||Watt",
            correctIndicesJoined = "2",
            correctText = "",
            explanation = "Resistance R = V / I is measured in Ohms (Ω).",
            points = 30L,
            published = true
        ),
        CachedQuestionEntity(
            id = "q_c10_2",
            quizId = "quiz_class10_stem",
            questionType = QuestionType.TRUE_FALSE.key,
            prompt = "True or False: A concave mirror can form a real and inverted image.",
            optionsJoined = "True||False",
            correctIndicesJoined = "0",
            correctText = "",
            explanation = "When the object is placed beyond the focus, a concave mirror forms a real, inverted image.",
            points = 30L,
            published = true
        ),
        CachedQuestionEntity(
            id = "q_c10_3",
            quizId = "quiz_class10_stem",
            questionType = QuestionType.FILL_BLANK.key,
            prompt = "Fill in the blank: The pH value of pure neutral water at 25°C is _____.",
            optionsJoined = "",
            correctIndicesJoined = "",
            correctText = "7",
            explanation = "Pure water is neutral with a pH of 7.",
            points = 40L,
            published = true
        )
    )

    val examPapers: List<CachedExamPaperEntity> = listOf(
        CachedExamPaperEntity(
            id = "paper_7_eng_pyq",
            classGrade = 7L,
            subjectId = "sub_english_7",
            subjectName = "English",
            title = "Class 7 - Previous Year (English)",
            paperCategory = PaperCategory.PREVIOUS_YEAR.key,
            year = 2025L,
            fileSizeLabel = "PDF • 2.4 MB",
            contentText = "SECTION A: Reading Comprehension (20 Marks)\nSECTION B: Writing & Grammar — Past Perfect & Active/Passive Voice (30 Marks)\nSECTION C: Literature — The Selfish Giant & Three Questions (30 Marks)",
            published = true
        ),
        CachedExamPaperEntity(
            id = "paper_7_math_pyq",
            classGrade = 7L,
            subjectId = "sub_mathematics_7",
            subjectName = "Mathematics",
            title = "Class 7 - Previous Year (Mathematics)",
            paperCategory = PaperCategory.PREVIOUS_YEAR.key,
            year = 2025L,
            fileSizeLabel = "PDF • 2.1 MB",
            contentText = "Part 1: Integers & Fractions (15 Marks)\nPart 2: Simple Equations & Lines and Angles (25 Marks)\nPart 3: Triangle Properties & Congruence (40 Marks)",
            published = true
        ),
        CachedExamPaperEntity(
            id = "paper_7_sci_pyq",
            classGrade = 7L,
            subjectId = "sub_science_7",
            subjectName = "Science",
            title = "Class 7 - Previous Year (Science)",
            paperCategory = PaperCategory.PREVIOUS_YEAR.key,
            year = 2025L,
            fileSizeLabel = "PDF • 2.3 MB",
            contentText = "Q1-Q10: Objective MCQs on Acids, Bases & Nutrition\nQ11-Q18: Short Answer Reactions & Heat Transfer\nQ19-Q22: Long Answer Experiments with Diagrams",
            published = true
        ),
        CachedExamPaperEntity(
            id = "paper_7_sst_pyq",
            classGrade = 7L,
            subjectId = "sub_social_science_7",
            subjectName = "Social Science",
            title = "Class 7 - Previous Year (SST)",
            paperCategory = PaperCategory.PREVIOUS_YEAR.key,
            year = 2025L,
            fileSizeLabel = "PDF • 2.0 MB",
            contentText = "History: New Kings & Kingdoms\nGeography: Inside Our Earth & Air\nCivics: State Government & Media",
            published = true
        ),
        CachedExamPaperEntity(
            id = "paper_7_sci_model",
            classGrade = 7L,
            subjectId = "sub_science_7",
            subjectName = "Science",
            title = "Class 7 - Model Paper (Science Term 2)",
            paperCategory = PaperCategory.MODEL_PAPER.key,
            year = 2026L,
            fileSizeLabel = "PDF • 1.9 MB",
            contentText = "Model Paper based on latest NCERT/CBSE blueprint: Acids & Bases, Physical & Chemical Changes, Electric Current.",
            published = true
        ),
        CachedExamPaperEntity(
            id = "paper_7_math_sample",
            classGrade = 7L,
            subjectId = "sub_mathematics_7",
            subjectName = "Mathematics",
            title = "Class 7 - Sample Paper (Mathematics)",
            paperCategory = PaperCategory.SAMPLE_PAPER.key,
            year = 2026L,
            fileSizeLabel = "PDF • 2.2 MB",
            contentText = "Full 80-mark timed practice sample paper with case-study questions and step-marking scheme.",
            published = true
        ),
        CachedExamPaperEntity(
            id = "paper_10_math_pyq",
            classGrade = 10L,
            subjectId = "sub_mathematics_10",
            subjectName = "Mathematics",
            title = "Class 10 - Board Previous Year (Math)",
            paperCategory = PaperCategory.PREVIOUS_YEAR.key,
            year = 2025L,
            fileSizeLabel = "PDF • 3.1 MB",
            contentText = "Board Examination Standard Mathematics Paper: Quadratic Equations, Trigonometry, Triangles, Statistics.",
            published = true
        )
    )

    val defaultAnnouncements: List<AnnouncementItem> = listOf(
        AnnouncementItem(
            id = "ann_1",
            title = "Mid-Term Revision Notes Live!",
            message = "Updated chapter summaries and formula sheets for Classes 6–12 are now available for offline download.",
            targetClass = 0L,
            priority = "high",
            published = true
        ),
        AnnouncementItem(
            id = "ann_2",
            title = "Class 7 Daily Grammar Challenge",
            message = "Complete today's 5-question Past Tenses challenge to maintain your study streak and earn +100 points!",
            targetClass = 7L,
            priority = "normal",
            published = true
        )
    )

    val defaultAppSettings = AppSettings(
        id = "global",
        premiumMonthlyPrice = "$4.99/mo",
        premiumYearlyPrice = "$39.99/yr",
        adsEnabled = true,
        adFamilySafeMode = true,
        adPlacementHome = true,
        adPlacementMaterials = true,
        adPlacementQuizzes = false,
        adPlacementPapers = true,
        adBannerTitle = "StudyHub Plus • Family-Safe Learning",
        adBannerSubtitle = "Unlock unlimited offline packs, advanced analytics & ad-free mode.",
        adCtaLabel = "Upgrade",
        adNetworkProvider = "StudyHub Direct (Family-Safe)",
        maintenanceMode = false,
        supportEmail = "support@studyhub.edu",
        published = true
    )
}
