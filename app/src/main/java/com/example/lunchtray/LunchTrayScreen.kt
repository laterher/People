/*
 * Copyright (C) 2023 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.lunchtray
import androidx.compose.ui.res.stringResource
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.math.pow

// Перечисление экранов приложения с заголовками (на русском, но ссылки на ресурсы оставлены для совместимости; в UI тексты переведены напрямую)
enum class DepositScreen(@StringRes val title: Int) { // Для заголовков и экранов
    Start(title = R.string.deposit_calculator),
    Initial(title = R.string.initial_deposit_and_rate),
    Additional(title = R.string.monthly_replenishment_and_period),
    Summary(title = R.string.summary_information)
}

// Класс данных для хранения состояния UI (параметры вклада и результаты расчёта)
data class DepositUiState(
    val initial: Double = 0.0,  // Первоначальный взнос
    val rate: Double = 0.0,     // Годовая процентная ставка
    val monthly: Double = 0.0,  // Ежемесячное пополнение
    val months: Int = 0,        // Период в месяцах
    val total: Double = 0.0,    // Итоговая сумма
    val profit: Double = 0.0,   // Прибыль
    val percent: Double = 0.0   // Процент дохода
)

// ViewModel для управления состоянием и расчётами вклада (все вычисления здесь, как указано в задании)
class DepositViewModel : ViewModel() { // ViewModel - класс для хранения данных
    // Приватный поток состояния для внутреннего использования
    private val _uiState = MutableStateFlow(DepositUiState())
    // Публичный поток состояния для подписки в UI
    val uiState: StateFlow<DepositUiState> = _uiState.asStateFlow()

    // Функция обновления первоначального взноса
    fun updateInitial(amount: Double) {
        _uiState.update { it.copy(initial = amount) }
    }

    // Функция обновления процентной ставки
    fun updateRate(rate: Double) {
        _uiState.update { it.copy(rate = rate) }
    }

    // Функция обновления ежемесячного пополнения
    fun updateMonthly(amount: Double) {
        _uiState.update { it.copy(monthly = amount) }
    }

    // Функция обновления периода в месяцах
    fun updateMonths(months: Int) {
        _uiState.update { it.copy(months = months) }
    }

    // Функция расчёта итоговой суммы, прибыли и процента дохода (основная логика приложения)
    fun calculate() {
        val state = uiState.value  // Получаем текущее состояние
        val monthlyRate = state.rate / 12 / 100  // Ежемесячная ставка (годовая / 12 / 100)
        val n = state.months  // Количество месяцев
        val fv: Double  // Будущая стоимость (итоговая сумма)
        if (monthlyRate == 0.0) {
            // Если ставка нулевая, просто суммируем взносы без процентов
            fv = state.initial + state.monthly * n
        } else {
            // Формула сложного процента с ежемесячными пополнениями
            val power = (1 + monthlyRate).pow(n.toDouble())  // (1 + r)^n
            fv = state.initial * power + state.monthly * (power - 1) / monthlyRate
        }
        val invested = state.initial + state.monthly * n  // Общая инвестированная сумма
        val profit = fv - invested  // Прибыль
        // Процент дохода (если инвестировано > 0, иначе 0)
        val percent = if (invested > 0) (profit / invested) * 100 else 0.0
        // Обновляем состояние с результатами
        _uiState.update {
            it.copy(
                total = fv,
                profit = profit,
                percent = percent
            )
        }
    }

    // Функция сброса всех параметров к исходным значениям
    fun reset() {
        _uiState.value = DepositUiState()
    }
}

/**
 * Composable-функция для отображения верхней панели приложения с заголовком и кнопкой "Назад" (если возможно).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DepositAppBar(
    @StringRes currentScreenTitle: Int,
    canNavigateBack: Boolean,
    navigateUp: () -> Unit,
    modifier: Modifier = Modifier
) {
    CenterAlignedTopAppBar( // Верхняя панель
        title = { Text(stringResource(currentScreenTitle)) },  // Заголовок экрана (оставлен с ресурсом, но в UI переведён)
        modifier = modifier,
        navigationIcon = {
            if (canNavigateBack) { // Если true, то показывается кнопка назад
                IconButton(onClick = navigateUp) {
                    Icon(
                        imageVector = Icons.Filled.ArrowBack,
                        contentDescription = "Назад"  // Перевод: Back -> Назад
                    )
                }
            }
        }
    )
}

/**
 * Composable-функция для начального экрана с кнопкой "Рассчитать вклад".
 */
@Composable
fun StartScreen(
    onStartButtonClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column( // Вертикальный контейнер
        modifier = modifier
            .fillMaxSize()
            .padding(dimensionResource(R.dimen.padding_medium)),
        verticalArrangement = Arrangement.Center, // Центрирует
        horizontalAlignment = Alignment.CenterHorizontally // Центрирует
    ) {
        Button(onClick = onStartButtonClicked) {
            Text("Рассчитать вклад")  // Перевод: Calculate Deposit -> Рассчитать вклад
        }
    }
}

/**
 * Composable-функция для экрана ввода первоначального взноса и процентной ставки.
 */
@Composable
fun InitialScreen(
    viewModel: DepositViewModel,
    onCancelButtonClicked: () -> Unit,
    onNextButtonClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()  // Подписываемся на состояние

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(dimensionResource(R.dimen.padding_medium))
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Первоначальный взнос")  // Перевод: Initial Deposit -> Первоначальный взнос
        OutlinedTextField(
            value = if (uiState.initial == 0.0) "" else uiState.initial.toString(),
            onValueChange = {
                val amount = it.toDoubleOrNull() ?: return@OutlinedTextField  // Парсим ввод как double
                viewModel.updateInitial(amount)  // Обновляем в ViewModel
            },
            label = { Text("Первоначальная сумма") },  // Перевод: Initial Amount -> Первоначальная сумма
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )
        Text("Годовая процентная ставка")  // Перевод: Annual Interest Rate -> Годовая процентная ставка
        OutlinedTextField(
            value = if (uiState.rate == 0.0) "" else uiState.rate.toString(),
            onValueChange = { // Парсит ввод
                val rate = it.toDoubleOrNull() ?: return@OutlinedTextField // Обновляет ViewModel
                viewModel.updateRate(rate)
            },
            label = { Text("Годовая ставка (%)") },  // Перевод: Annual Rate (%) -> Годовая ставка (%)
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions( // Числовая клавиатура
                keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp)) // Пустое пространство для отступов
        Row( // Горизонтальный контейнер для кнопок Отмена и Далее
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Button(onClick = onCancelButtonClicked) {
                Text("Отмена")
            }
            Button(onClick = onNextButtonClicked) {
                Text("Далее")
            }
        }
    }
}

/**
 * Composable-функция для экрана ввода ежемесячного пополнения и периода в месяцах.
 */
@Composable
fun AdditionalScreen(
    viewModel: DepositViewModel,
    onCancelButtonClicked: () -> Unit,
    onNextButtonClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(dimensionResource(R.dimen.padding_medium))
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Ежемесячное пополнение")
        OutlinedTextField(
            value = if (uiState.monthly == 0.0) "" else uiState.monthly.toString(),
            onValueChange = {
                val amount = it.toDoubleOrNull() ?: return@OutlinedTextField
                viewModel.updateMonthly(amount)
            },
            label = { Text("Ежемесячная сумма") },
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )
        Text("Период в месяцах")
        OutlinedTextField(
            value = if (uiState.months == 0) "" else uiState.months.toString(),
            onValueChange = {
                val months = it.toIntOrNull() ?: return@OutlinedTextField
                viewModel.updateMonths(months)
            },
            label = { Text("Месяцы") },
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Button(onClick = onCancelButtonClicked) {
                Text("Отмена")
            }
            Button(onClick = {
                viewModel.calculate()  // Выполняем расчёт перед переходом
                onNextButtonClicked()
            }) {
                Text("Далее")
            }
        }
    }
}

/**
 * Composable-функция для экрана суммарной информации с результатами и кнопкой возврата.
 */
@Composable
fun SummaryScreen(
    viewModel: DepositViewModel,
    onBackButtonClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(dimensionResource(R.dimen.padding_medium))
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("Всего получено средств: ${String.format("%.2f", uiState.total)}")  // Перевод: Total Funds Received -> Всего получено средств
        Text("Прибыль: ${String.format("%.2f", uiState.profit)}")  // Перевод: Profit -> Прибыль
        Text("Процент дохода: ${String.format("%.2f", uiState.percent)}%")  // Перевод: Profit Percentage -> Процент дохода
        Spacer(modifier = Modifier.height(16.dp))
        Text("Суммарная информация о параметрах")  // Перевод: Summary Parameters -> Суммарная информация о параметрах
        Text("Первоначальная сумма: ${String.format("%.2f", uiState.initial)}")  // Перевод: Initial Amount -> Первоначальная сумма
        Text("Годовая ставка (%): ${String.format("%.2f", uiState.rate)}%")  // Перевод: Annual Rate (%) -> Годовая ставка (%)
        Text("Ежемесячная сумма: ${String.format("%.2f", uiState.monthly)}")  // Перевод: Monthly Amount -> Ежемесячная сумма
        Text("Месяцы: ${uiState.months}")  // Перевод: Months -> Месяцы
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = onBackButtonClicked,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Text("Вернуться на начальный экран")  // Перевод: Back to Start -> Вернуться на начальный экран
        }
    }
}

/**
 * Основная Composable-функция приложения: настройка навигации и Scaffold.
 */
@Composable
fun DepositCalculatorApp() {
    val navController = rememberNavController()  // Контроллер навигации
    val backStackEntry by navController.currentBackStackEntryAsState()  // Текущий стек навигации
    val currentScreen = DepositScreen.valueOf(
        backStackEntry?.destination?.route ?: DepositScreen.Start.name  // Текущий экран
    )
    val viewModel: DepositViewModel = viewModel()  // ViewModel для всего приложения

    Scaffold(
        topBar = {
            DepositAppBar(
                currentScreenTitle = currentScreen.title,
                canNavigateBack = navController.previousBackStackEntry != null,  // Проверяем, можно ли вернуться назад
                navigateUp = { navController.navigateUp() }
            )
        }
    ) { innerPadding ->
        NavHost( // Контейнер для навигации
            navController = navController,
            startDestination = DepositScreen.Start.name,  // Начальный экран
            modifier = Modifier.padding(innerPadding)
        ) {
            // Навигация на начальный экран
            composable(route = DepositScreen.Start.name) {
                StartScreen(
                    onStartButtonClicked = {
                        navController.navigate(DepositScreen.Initial.name)
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Навигация на экран первоначального взноса
            composable(route = DepositScreen.Initial.name) {
                InitialScreen(
                    viewModel = viewModel,
                    onCancelButtonClicked = {
                        viewModel.reset()  // Сброс состояния
                        navController.popBackStack(DepositScreen.Start.name, inclusive = false)
                    },
                    onNextButtonClicked = {
                        navController.navigate(DepositScreen.Additional.name)
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Навигация на экран дополнительных параметров
            composable(route = DepositScreen.Additional.name) {
                AdditionalScreen(
                    viewModel = viewModel,
                    onCancelButtonClicked = {
                        viewModel.reset()
                        navController.popBackStack(DepositScreen.Start.name, inclusive = false)
                    },
                    onNextButtonClicked = {
                        navController.navigate(DepositScreen.Summary.name)
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Навигация на экран суммарной информации
            composable(route = DepositScreen.Summary.name) {
                SummaryScreen(
                    viewModel  = viewModel,
                    onBackButtonClicked = {
                        viewModel.reset()
                        navController.popBackStack(DepositScreen.Start.name, inclusive = false)
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}