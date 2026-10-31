package com.sol.gymtracker.data.seed

import androidx.room.withTransaction
import com.sol.gymtracker.data.db.AppSettingsEntity
import com.sol.gymtracker.data.db.ExerciseEntity
import com.sol.gymtracker.data.db.PlanEntity
import com.sol.gymtracker.data.db.PlanExerciseEntity
import com.sol.gymtracker.data.db.WorkoutDatabase

private data class ExerciseSeed(
    val id: String,
    val name: String,
    val group: String,
    val equipment: String,
    val sets: Int = 3,
    val min: Int = 8,
    val max: Int = 12,
    val rir: Int = 2,
    val rest: Int = 120,
    val note: String = "Работай чисто, без рывков",
    val secondary: String = "",
    val loadType: String = "WEIGHT",
    val bilateral: Boolean = true,
    val step: Double = 0.5,
)

private fun ExerciseSeed.entity() = ExerciseEntity(
    id = id,
    name = name,
    muscleGroup = group,
    secondaryMuscles = secondary,
    equipment = equipment,
    targetSets = sets,
    repMin = min,
    repMax = max,
    targetRir = rir,
    restSeconds = rest,
    techniqueNote = note,
    illustrationKey = group.lowercase().replace(" ", "_") + "_" + id,
    loadType = loadType,
    bilateral = bilateral,
    defaultStepKg = step,
)

object SeedCatalog {
    private val exercises = listOf(
        // Full Body A — exact rotation requested by the user.
        ExerciseSeed("leg_press", "Жим платформы ногами", "Ноги", "Тренажёр", 3, 10, 12, 3, 150, "Таз прижат к спинке", "Квадрицепс, ягодицы", step = 2.5),
        ExerciseSeed("rdl_straps", "Румынская тяга", "Ноги", "Штанга", 3, 8, 10, 3, 150, "Тяни таз назад, поясница ровная", "Ягодицы, бицепс бедра", step = 2.5),
        ExerciseSeed("flat_press", "Жим лёжа", "Грудь", "Штанга / гантели", 3, 8, 10, 3, 120, "Кисти над локтями", "Трицепс, передняя дельта", step = 2.5),
        ExerciseSeed("lat_pulldown", "Тяга верхнего блока", "Спина", "Блок", 3, 10, 12, 3, 120, "Тяни к верху груди, сводя лопатки", "Бицепс", step = 2.5),
        ExerciseSeed("lateral_raise", "Махи гантелями в стороны", "Плечи", "Кроссовер / гантели", 3, 12, 15, 2, 90, "Без рывков корпусом", "Средняя дельта", step = 0.5),
        ExerciseSeed("rope_pushdown", "Разгибание рук с канатом", "Трицепс", "Блок", 3, 12, 15, 2, 90, "Локти прижаты к корпусу", "", step = 0.5),
        // Full Body B.
        ExerciseSeed("hack_squat", "Гакк-приседания", "Ноги", "Тренажёр", 3, 8, 10, 3, 150, "Спина прижата к платформе", "Квадрицепс, ягодицы", step = 2.5),
        ExerciseSeed("leg_curl", "Сгибание ног в тренажёре", "Ноги", "Тренажёр", 3, 10, 12, 2, 120, "Опускай медленно, 2–3 с", "Бицепс бедра", step = 1.0),
        ExerciseSeed("hammer_shoulder_press", "Жим над головой в Хаммере", "Плечи", "Тренажёр", 3, 8, 10, 3, 120, "Не заводи плечи в боль", "Трицепс", step = 2.5),
        ExerciseSeed("seated_cable_row", "Горизонтальная тяга", "Спина", "Блок", 3, 10, 12, 3, 120, "Задержи на пике на 1 с", "Бицепс", step = 2.5),
        ExerciseSeed("pec_deck", "Сведение рук в тренажёре", "Грудь", "Тренажёр", 3, 12, 15, 2, 90, "Своди руки, кисти расслаблены", "Передняя дельта", step = 1.0),
        ExerciseSeed("preacher_ez_curl", "Бицепс на скамье Скотта", "Бицепс", "Скамья Скотта / EZ-гриф", 3, 10, 12, 2, 90, "Держи кисти нейтрально", "", step = 0.5),
        // Full Body C.
        ExerciseSeed("leg_extension", "Разгибание ног сидя", "Ноги", "Тренажёр", 3, 12, 15, 2, 120, "Пауза вверху на 1 с", "Квадрицепс", step = 1.0),
        ExerciseSeed("leg_curl_c", "Сгибание ног сидя", "Ноги", "Тренажёр", 3, 10, 12, 2, 120, "Полная амплитуда, таз на месте", "Бицепс бедра", step = 1.0),
        ExerciseSeed("incline_db_press", "Жим гантелей в наклоне", "Грудь", "Гантели", 3, 8, 10, 3, 120, "Угол скамьи 30°", "Передняя дельта, трицепс", step = 1.0),
        ExerciseSeed("assisted_pullups", "Подтягивания в гравитроне", "Спина", "Гравитрон", 3, 8, 10, 3, 120, "Опускайся медленно, тянись внизу", "Бицепс", "ASSISTED", step = 2.5),
        ExerciseSeed("cable_lateral_raise", "Отведение рук в кроссовере", "Плечи", "Кроссовер", 3, 12, 15, 2, 90, "Держи трос в натяжении", "Средняя дельта", step = 0.5),
        ExerciseSeed("hammer_curl", "Молотки с гантелями", "Бицепс", "Гантели", 2, 10, 12, 2, 90, "Хват нейтральный, кисти прямые", "Предплечья", step = 0.5),
        ExerciseSeed("rope_pushdown_c", "Разгибание рук, добивка", "Трицепс", "Блок", 2, 10, 12, 2, 90, "Добей трицепс, вес лёгкий", "", step = 0.5),
        // Expanded catalogue: common, practical, and a few distinctive choices.
        ExerciseSeed("barbell_squat", "Приседания со штангой", "Ноги", "Штанга", 3, 6, 10, 3, 180, "Стойка устойчивая, колени за носками", "Квадрицепс, ягодицы", step = 2.5),
        ExerciseSeed("front_squat", "Фронтальные приседания", "Ноги", "Штанга", 3, 6, 10, 3, 180, "Локти высоко, корпус собран", "Квадрицепс, корпус", step = 2.5),
        ExerciseSeed("bulgarian_split_squat", "Болгарский сплит-присед", "Ноги", "Гантели", 3, 8, 12, 2, 120, "Опускайся вертикально, колено стабильно", "Ягодицы", bilateral = false, step = 1.0),
        ExerciseSeed("lying_leg_curl", "Сгибание ног лёжа", "Ноги", "Тренажёр", 3, 10, 15, 2, 90, "Таз на подушке", "Бицепс бедра", step = 1.0),
        ExerciseSeed("jefferson_deadlift", "Тяга Джефферсона", "Ноги", "Штанга", 3, 8, 12, 3, 150, "Начни с лёгкого веса, стопы стабильны", "Ягодицы, приводящие", step = 1.0),
        ExerciseSeed("hip_thrust", "Ягодичный мост со штангой", "Ноги", "Штанга", 3, 8, 12, 2, 120, "Подбородок к груди, пауза вверху", "Ягодицы", step = 2.5),
        ExerciseSeed("standing_calf_raise", "Подъёмы на носки стоя", "Ноги", "Тренажёр", 3, 10, 15, 2, 90, "Полная растяжка внизу, пауза вверху", "Икроножные", step = 2.5),
        ExerciseSeed("barbell_bench", "Жим штанги лёжа", "Грудь", "Штанга", 3, 6, 10, 3, 150, "Лопатки собраны, стопы устойчивы", "Трицепс, передняя дельта", step = 2.5),
        ExerciseSeed("machine_chest_press", "Жим груди в тренажёре", "Грудь", "Тренажёр", 3, 8, 12, 2, 120, "Не отрывай плечи от спинки", "Трицепс", step = 2.5),
        ExerciseSeed("cable_fly", "Сведение рук в кроссовере", "Грудь", "Кроссовер", 3, 12, 15, 2, 90, "Своди руки плавно, не бросай вес", "", step = 0.5),
        ExerciseSeed("dips", "Отжимания на брусьях", "Грудь", "Брусья", 3, 6, 12, 2, 120, "Не опускайся ниже комфортной глубины", "Трицепс, передняя дельта", loadType = "BODYWEIGHT", step = 1.0),
        ExerciseSeed("pullups", "Подтягивания", "Спина", "Турник", 3, 5, 10, 2, 150, "Начни повтор с активных лопаток", "Бицепс", loadType = "BODYWEIGHT", step = 1.0),
        ExerciseSeed("chest_supported_row", "Тяга с упором грудью", "Спина", "Тренажёр / гантели", 3, 8, 12, 2, 120, "Грудь на упоре, локти без рывков", "Задняя дельта, бицепс", step = 1.0),
        ExerciseSeed("one_arm_db_row", "Тяга гантели одной рукой", "Спина", "Гантель", 3, 8, 12, 2, 120, "Таз стабилен, локоть к бедру", "Бицепс", bilateral = false, step = 1.0),
        ExerciseSeed("t_bar_row", "Тяга Т-грифа", "Спина", "Т-гриф", 3, 8, 12, 2, 150, "Держи поясницу нейтральной", "Бицепс", step = 2.5),
        ExerciseSeed("straight_arm_pulldown", "Пуловер на верхнем блоке", "Спина", "Блок", 3, 12, 15, 2, 90, "Веди плечом, а не локтем", "Большая круглая", step = 0.5),
        ExerciseSeed("seated_db_press", "Жим гантелей сидя", "Плечи", "Гантели", 3, 8, 12, 2, 120, "Не прогибай поясницу", "Трицепс", step = 1.0),
        ExerciseSeed("face_pull", "Тяга каната к лицу", "Плечи", "Блок", 3, 12, 15, 2, 90, "Разводи канат к вискам, локти высоко", "Задняя дельта", step = 0.5),
        ExerciseSeed("reverse_pec_deck", "Обратная бабочка", "Плечи", "Тренажёр", 3, 12, 15, 2, 90, "Плечи опущены", "Задняя дельта", step = 1.0),
        ExerciseSeed("ez_curl", "Подъём EZ-штанги на бицепс", "Бицепс", "EZ-гриф", 3, 8, 12, 2, 90, "Не раскачивай корпус", "Предплечья", step = 0.5),
        ExerciseSeed("incline_db_curl", "Сгибания гантелей на наклонной", "Бицепс", "Гантели", 3, 10, 15, 2, 90, "Плечо позади корпуса", "", step = 0.5),
        ExerciseSeed("cable_curl", "Сгибания на нижнем блоке", "Бицепс", "Блок", 3, 10, 15, 2, 90, "Держи натяжение внизу", "", step = 0.5),
        ExerciseSeed("overhead_rope_extension", "Разгибание рук над головой", "Трицепс", "Блок", 3, 10, 15, 2, 90, "Локти вперёд и неподвижно", "", step = 0.5),
        ExerciseSeed("close_grip_bench", "Жим узким хватом", "Трицепс", "Штанга", 3, 6, 10, 3, 150, "Не разводи локти насильно", "Грудь", step = 2.5),
        ExerciseSeed("single_arm_pushdown", "Разгибание одной руки", "Трицепс", "Блок", 3, 12, 15, 2, 90, "Корпус неподвижен", "", bilateral = false, step = 0.5),
        ExerciseSeed("reverse_lunge", "Выпады назад с гантелями", "Ноги", "Гантели", 3, 8, 12, 2, 120, "Шагай назад мягко, корпус ровно", "Квадрицепс, ягодицы", bilateral = false, step = 1.0),
        ExerciseSeed("seated_calf_raise", "Подъёмы на носки сидя", "Ноги", "Тренажёр", 3, 12, 20, 2, 90, "Пауза внизу и вверху", "Икроножные", step = 2.5),
        ExerciseSeed("back_extension", "Гиперэкстензия", "Спина", "Скамья", 3, 10, 15, 2, 90, "Движение от таза, не переразгибай", "Ягодицы, бицепс бедра", step = 1.0),
        ExerciseSeed("neutral_lat_pulldown", "Тяга блока нейтральным хватом", "Спина", "Блок", 3, 8, 12, 2, 120, "Веди локти вниз, не раскачивайся", "Бицепс", step = 2.5),
        ExerciseSeed("machine_row", "Тяга с упором грудью (тренажёр)", "Спина", "Тренажёр", 3, 8, 12, 2, 120, "Грудь на упоре, пауза сзади", "Задняя дельта, бицепс", step = 2.5),
        // Additional catalogue: legs, chest, back, shoulders, arms, calves, and abs.
        ExerciseSeed("step_up", "Зашагивания на тумбу", "Ноги", "Гантели", 3, 8, 12, 2, 120, "Толкайся всей стопой", "Ягодицы", bilateral = false, step = 1.0),
        ExerciseSeed("walking_lunge", "Выпады с гантелями", "Ноги", "Гантели", 3, 10, 14, 2, 120, "Держи корпус ровно", "Квадрицепс, ягодицы", bilateral = false, step = 1.0),
        ExerciseSeed("leg_abduction", "Отведение ног в тренажёре", "Ноги", "Тренажёр", 3, 12, 20, 2, 90, "Не разворачивай таз", "Ягодицы", step = 1.0),
        ExerciseSeed("leg_adduction", "Приведение ног в тренажёре", "Ноги", "Тренажёр", 3, 12, 20, 2, 90, "Двигайся плавно", "Приводящие", step = 1.0),
        ExerciseSeed("glute_kickback", "Отведение ноги назад", "Ноги", "Кроссовер", 3, 12, 15, 2, 90, "Не прогибай поясницу", "Ягодицы", bilateral = false, step = 0.5),
        ExerciseSeed("good_morning", "Наклоны со штангой", "Ноги", "Штанга", 3, 8, 12, 3, 120, "Тяни таз назад, спина ровная", "Бицепс бедра", step = 2.5),
        ExerciseSeed("incline_machine_press", "Жим в тренажёре под наклоном", "Грудь", "Тренажёр", 3, 8, 12, 2, 120, "Не отрывай плечи от спинки", "Передняя дельта, трицепс", step = 2.5),
        ExerciseSeed("pushups", "Отжимания", "Грудь", "Собственный вес", 3, 8, 20, 2, 90, "Корпус держи одной линией", "Трицепс", loadType = "BODYWEIGHT", step = 1.0),
        ExerciseSeed("cable_crossover", "Кроссовер на грудь", "Грудь", "Кроссовер", 3, 12, 15, 2, 90, "Своди руки перед собой", "", step = 0.5),
        ExerciseSeed("machine_pullover", "Пуловер в тренажёре", "Спина", "Тренажёр", 3, 10, 15, 2, 90, "Веди локти вниз", "Большая круглая", step = 1.0),
        ExerciseSeed("close_grip_lat_pulldown", "Тяга блока узким хватом", "Спина", "Блок", 3, 8, 12, 2, 120, "Тяни локти к корпусу", "Бицепс", step = 2.5),
        ExerciseSeed("dumbbell_shrug", "Шраги с гантелями", "Плечи", "Гантели", 3, 10, 15, 2, 90, "Поднимай плечи строго вверх", "Трапеция", step = 1.0),
        ExerciseSeed("front_raise", "Подъём гантелей перед собой", "Плечи", "Гантели", 3, 10, 15, 2, 90, "Не раскачивай корпус", "Передняя дельта", step = 0.5),
        ExerciseSeed("cable_upright_row", "Тяга к подбородку", "Плечи", "Кроссовер", 3, 10, 15, 2, 90, "Локти веди выше кистей", "Трапеция", step = 0.5),
        ExerciseSeed("concentration_curl", "Концентрированный подъём", "Бицепс", "Гантель", 3, 10, 15, 2, 90, "Не отрывай локоть от бедра", "", bilateral = false, step = 0.5),
        ExerciseSeed("cable_hammer_curl", "Молотки на канате", "Бицепс", "Блок", 3, 10, 15, 2, 90, "Держи локти неподвижно", "Предплечья", step = 0.5),
        ExerciseSeed("skull_crusher", "Французский жим лёжа", "Трицепс", "EZ-гриф", 3, 8, 12, 2, 120, "Локти направлены вверх", "", step = 1.0),
        ExerciseSeed("bench_dip", "Отжимания от скамьи", "Трицепс", "Собственный вес", 3, 8, 15, 2, 90, "Плечи держи ниже ушей", "Грудь", loadType = "BODYWEIGHT", step = 1.0),
        ExerciseSeed("donkey_calf_raise", "Подъёмы на носки в наклоне", "Икры", "Тренажёр", 3, 12, 20, 2, 90, "Пауза в верхней точке", "", step = 2.5),
        ExerciseSeed("smith_calf_raise", "Подъёмы на носки в Смите", "Икры", "Машина Смита", 3, 10, 20, 2, 90, "Опускай пятки медленно", "", step = 2.5),
        ExerciseSeed("cable_crunch", "Скручивания на блоке", "Пресс", "Блок", 3, 10, 15, 2, 90, "Скручивай корпус, не тяни руками", "", step = 0.5),
        ExerciseSeed("hanging_knee_raise", "Подъём коленей в висе", "Пресс", "Турник", 3, 8, 15, 2, 90, "Подкручивай таз вверху", "Сгибатели бедра", loadType = "BODYWEIGHT", step = 1.0),
        ExerciseSeed("ab_wheel", "Ролик для пресса", "Пресс", "Ролик", 3, 6, 15, 2, 120, "Не прогибай поясницу", "Плечи", loadType = "BODYWEIGHT", step = 1.0),
        ExerciseSeed("reverse_crunch", "Обратные скручивания", "Пресс", "Собственный вес", 3, 10, 20, 2, 90, "Поднимай таз без рывка", "", loadType = "BODYWEIGHT", step = 1.0),
    )

    private val presetDefinitions = listOf(
        "Full Body A" to listOf("leg_press", "rdl_straps", "flat_press", "lat_pulldown", "lateral_raise", "rope_pushdown"),
        "Full Body B" to listOf("hack_squat", "leg_curl", "hammer_shoulder_press", "seated_cable_row", "pec_deck", "preacher_ez_curl"),
        "Full Body C" to listOf("leg_extension", "leg_curl_c", "incline_db_press", "assisted_pullups", "cable_lateral_raise", "hammer_curl", "rope_pushdown_c"),
    )

    suspend fun seedIfNeeded(database: WorkoutDatabase) {
        database.withTransaction {
            val isEmpty = database.exercises().count() == 0
            if (isEmpty) {
                database.exercises().insertAll(exercises.map { it.entity() })
                for ((name, ids) in presetDefinitions) {
                    val planId = "preset_${name.last()}"
                    database.plans().insert(PlanEntity(planId, name, "", isPreset = true))
                    database.plans().insertExercises(ids.mapIndexed { order, exerciseId ->
                        val exercise = exercises.first { it.id == exerciseId }
                        PlanExerciseEntity(planId, exerciseId, order, exercise.sets, exercise.min, exercise.max, exercise.rir, exercise.rest)
                    })
                }
                database.settings().save(AppSettingsEntity())
            } else {
                val existingIds = database.exercises().getIds().toSet()
                database.exercises().insertAll(exercises.filter { it.id !in existingIds }.map { it.entity() })
            }
            syncSeedCopy(database)
        }
    }

    /**
     * Держит названия и подсказки встроенных упражнений в соответствии с этим файлом,
     * в том числе на уже установленной базе. Свои упражнения не трогает.
     */
    private suspend fun syncSeedCopy(database: WorkoutDatabase) {
        val existing = database.exercises().getAll().associateBy { it.id }
        exercises.forEach { seed ->
            val current = existing[seed.id] ?: return@forEach
            val updated = current.copy(name = seed.name, techniqueNote = seed.note)
            if (updated != current) database.exercises().update(updated)
        }
    }
}