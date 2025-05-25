import pandas as pd
import numpy as np
import matplotlib.pyplot as plt
from scipy.stats import cumfreq

# Wczytanie danych
mse_df = pd.read_csv("mse_per_epoch_RELU.csv", sep=";", decimal=".")
results_df = pd.read_csv("results_RELU.csv", sep=";", decimal=".")
print(mse_df.columns)
# --- 1. Wykres MSE na zbiorze uczącym ---
# Załóżmy, że mamy dane z różnych wariantów w jednym pliku, ale tutaj tylko RELU, więc wykres pojedynczej linii
# Na potrzeby przykładu zróbmy zoom na wartości końcowe (ucięcie od góry osi Y)

epochs = mse_df["Epoch"]
train_mse = mse_df["TrainMSE"]
test_mse = mse_df["TestMSE"]

# Skalowanie osi Y (ucięcie wartości początkowych, zachowując skalę osi X)
# Przytnij maksymalną wartość osi Y do np. 1.2 * wartości minimalnej w ostatnich 10 epokach, by uwypuklić różnice
last_epochs = 10
ymax_train = 1.2 * train_mse[-last_epochs:].min()

plt.figure(figsize=(10,6))
plt.plot(epochs, train_mse, label='Train MSE RELU', color='blue')
plt.ylim(0, ymax_train)
plt.xlabel("Epoka")
plt.ylabel("Błąd MSE (zbiór uczący)")
plt.title("Błąd średniokwadratowy na zbiorze uczącym")
plt.grid(True)
plt.legend()
plt.show()

# --- 2. Wykres MSE na zbiorze testowym + linia odniesienia ---
# Wykres z takim samym skalowaniem osi Y jak na wykresie 1
ymax = max(ymax_train, 1.2 * test_mse[-last_epochs:].min())

plt.figure(figsize=(10,6))
plt.plot(epochs, test_mse, label='Test MSE RELU', color='red')
plt.ylim(0, ymax)
plt.xlabel("Epoka")
plt.ylabel("Błąd MSE (zbiór testowy)")
plt.title("Błąd średniokwadratowy na zbiorze testowym")

# Linia odniesienia - MSE dla zmierzonych wartości (skalowanie oryginalne do skali sieci)
# Obliczamy MSE pomiędzy MeasX,Y a ActualX,Y (oryginalna skala)

meas_x = results_df["MeasX"].values
meas_y = results_df["MeasY"].values
actual_x = results_df["ActualX"].values
actual_y = results_df["ActualY"].values

mse_measured = np.mean((meas_x - actual_x)**2 + (meas_y - actual_y)**2)
plt.axhline(y=mse_measured, color='green', linestyle='--', label=f'MSE pomiarów (referencja) = {mse_measured:.2f}')

plt.grid(True)
plt.legend()
plt.show()

# --- 3. Dystrybuanty błędów (CDF) dla wyników sieci i pomiarów dynamicznych ---
# Błąd: odległość między predykcją a wartością rzeczywistą (oryginalna skala)
# Już mamy kolumnę Distance w results_df

distances = results_df["Distance"].values
measured_errors = np.sqrt((meas_x - actual_x)**2 + (meas_y - actual_y)**2)

# Funkcja pomocnicza do wykresu dystrybuanty
def plot_cdf(data, label, color):
    sorted_data = np.sort(data)
    cdf = np.arange(1, len(sorted_data)+1) / len(sorted_data)
    plt.plot(sorted_data, cdf, label=label, color=color)

plt.figure(figsize=(12,7))

# Dystrybuanta błędów sieci RELU
plot_cdf(distances, "Dystrybuanta błędów - RELU", "blue")

# Dystrybuanta błędów pomiarów dynamicznych
plot_cdf(measured_errors, "Dystrybuanta błędów - pomiary dynamiczne", "green")

plt.xlabel("Błąd (odległość)")
plt.ylabel("Dystrybuanta (CDF)")
plt.title("Dystrybuanty błędów")
plt.grid(True)

# Opcjonalnie skala logarytmiczna na osi X (jeśli błędy rozciągnięte)
plt.xscale("linear")  # zmień na "log" jeśli potrzeba

plt.legend()
plt.show()

# --- 4. Wykres punktowy: wartości skorygowane (predykcje), zmierzone i rzeczywiste ---
# Wybierzemy wariant z najmniejszym średnim błędem (tu tylko RELU)

best_variant = "RELU"  # w przyszłości można wybrać dynamicznie

pred_x = results_df["PredX"].values
pred_y = results_df["PredY"].values

plt.figure(figsize=(12,8))
plt.scatter(meas_x, meas_y, label="Wartości zmierzone", alpha=0.5, s=30, c='orange', zorder=1)
plt.scatter(pred_x, pred_y, label="Wartości skorygowane (predykcje)", alpha=0.7, s=20, c='blue', zorder=2)
plt.scatter(actual_x, actual_y, label="Wartości rzeczywiste", alpha=0.3, s=10, c='green', marker='x', zorder=3)

plt.xlabel("X")
plt.ylabel("Y")
plt.title(f"Wartości pomiarów dynamicznych - {best_variant}")
plt.legend()
plt.grid(True)
plt.show()
