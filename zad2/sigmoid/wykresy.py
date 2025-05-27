import pandas as pd
import numpy as np
import matplotlib.pyplot as plt

activation_functions = ["RELU", "TANH", "SIGMOID"]

colors_pred = {"RELU": "blue", "TANH": "orange", "SIGMOID": "purple"}
markers = {"RELU": "o", "TANH": "^", "SIGMOID": "s"}

mse_data = {}
results_data = {}

# Wczytanie danych dla każdej aktywacji
for act in activation_functions:
    mse_data[act] = pd.read_csv(f"mse_per_epoch_{act}.csv", sep=";", decimal=".")
    results_data[act] = pd.read_csv(f"results_{act}.csv", sep=";", decimal=".")
# Oblicz górny limit osi Y na podstawie najmniejszych wartości z ostatnich epok
last_epochs = 50
ymax_train = 0
ymax_test = 0

for act in activation_functions:
    train_mse = mse_data[act]["TrainMSE"]
    test_mse = mse_data[act]["TestMSE"]
    ymin_train = train_mse[-last_epochs:].min()
    ymin_test = test_mse[-last_epochs:].min()
    ymax_train = max(ymax_train, 1.2 * ymin_train)
    #ymax_test = max(ymax_test, 1.2 * ymin_test)

# --- 1. Wykres MSE na zbiorze uczącym dla wszystkich aktywacji ---
plt.figure(figsize=(10,6))

for act in activation_functions:
    epochs = mse_data[act]["Epoch"]
    train_mse = mse_data[act]["TrainMSE"]
    plt.plot(epochs, train_mse, label=f'Train MSE {act}')

plt.ylim(0, 0.1)
plt.xlabel("Epoka")
plt.ylabel("Błąd MSE (zbiór uczący)")
plt.title("Błąd średniokwadratowy na zbiorze uczącym")
plt.grid(True)
plt.legend()
plt.ylim(0, 0.1)
plt.show()

# --- 2. Wykres MSE na zbiorze testowym z linią odniesienia dla pomiarów ---
plt.figure(figsize=(10,6))

for act in activation_functions:
    epochs = mse_data[act]["Epoch"]
    test_mse = mse_data[act]["TestMSE"]
    plt.plot(epochs, test_mse, label=f'Test MSE {act}')



# Linia odniesienia na podstawie pomiarów (weźmy z RELU, bo każda powinna mieć ten sam MeasX/Y i ActualX/Y)
meas_x = results_data["RELU"]["MeasX"].values
meas_y = results_data["RELU"]["MeasY"].values
actual_x = results_data["RELU"]["ActualX"].values
actual_y = results_data["RELU"]["ActualY"].values

mse_measured = 0.04396403
plt.axhline(y=mse_measured, color='green', linestyle='--', label=f'MSE pomiarów')

plt.ylim(0, 0.1)
plt.xlabel("Epoka")
plt.ylabel("Błąd MSE (zbiór testowy)")
plt.title("Błąd średniokwadratowy na zbiorze testowym")
plt.grid(True)
plt.legend()
plt.show()

# --- 3. Dystrybuanty błędów (CDF) dla wyników sieci i pomiarów dynamicznych ---

def plot_cdf(data, label, color):
    sorted_data = np.sort(data)
    cdf = np.arange(1, len(sorted_data)+1) / len(sorted_data)
    plt.plot(sorted_data, cdf, label=label, color=color)

plt.figure(figsize=(12,7))

colors = {"RELU":"blue", "TANH":"orange", "SIGMOID":"purple"}

# Błędy pomiarów dynamicznych - tylko raz (z RELU)
measured_errors = np.sqrt((meas_x - actual_x)**2 + (meas_y - actual_y)**2)
plot_cdf(measured_errors, "Dystrybuanta błędów - pomiary dynamiczne", "green")

# Dystrybuanta błędów modeli dla każdej aktywacji
for act in activation_functions:
    distances = results_data[act]["Distance"].values
    plot_cdf(distances, f"Dystrybuanta błędów - {act}", colors[act])

plt.xlabel("Błąd (odległość)")
plt.ylabel("Dystrybuanta (CDF)")
plt.title("Dystrybuanty błędów")
plt.grid(True)
plt.xscale("linear")  # Możesz zmienić na "log" jeśli trzeba
plt.legend()
plt.show()
# --- 4. Wykres punktowy tylko dla aktywacji z najmniejszym końcowym MSE testowym ---

# Znajdź aktywację z najmniejszym końcowym MSE testowym
min_mse = float('inf')
best_act = None

for act in activation_functions:
    final_test_mse = mse_data[act]["TestMSE"].iloc[-1]
    if final_test_mse < min_mse:
        min_mse = final_test_mse
        best_act = act

print(f"Najlepsza funkcja aktywacji wg MSE: {best_act} (MSE = {min_mse:.6f})")

# Dane do wykresu
pred_x = results_data[best_act]["PredX"].values
pred_y = results_data[best_act]["PredY"].values

plt.figure(figsize=(12,8))

plt.scatter(actual_x, actual_y, label="Wartości rzeczywiste", alpha=0.7, s=3, c='green', marker='x', zorder=3)
plt.scatter(meas_x, meas_y, label="Wartości zmierzone", alpha=0.3, s=5, c='gray', zorder=1)

# Predykcje tylko dla najlepszej aktywacji
plt.scatter(pred_x, pred_y, label=f"Wartości skorygowane ({best_act})",
            alpha=1.0, s=4, c=colors_pred[best_act], marker=markers[best_act], zorder=2)

plt.xlabel("X")
plt.ylabel("Y")
plt.title(f"Najlepszy wariant aktywacji: {best_act} (najniższy MSE testowy {min_mse:.4f})")
plt.legend()
plt.grid(True)
plt.show()
