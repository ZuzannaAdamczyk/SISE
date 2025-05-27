import pandas as pd
import numpy as np
import matplotlib.pyplot as plt

activation_functions = ["RELU", "TANH", "SIGMOID"]

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
    ymax_test = max(ymax_test, 1.2 * ymin_test)

# --- 1. Wykres MSE na zbiorze uczącym dla wszystkich aktywacji ---
plt.figure(figsize=(10,6))

for act in activation_functions:
    epochs = mse_data[act]["Epoch"]
    train_mse = mse_data[act]["TrainMSE"]
    plt.plot(epochs, train_mse, label=f'Train MSE {act}')

plt.ylim(0, 600000)
plt.xlabel("Epoka")
plt.ylabel("Błąd MSE (zbiór uczący)")
plt.title("Błąd średniokwadratowy na zbiorze uczącym")
plt.grid(True)
plt.legend()
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

mse_measured = np.mean((meas_x - actual_x)**2 + (meas_y - actual_y)**2)
plt.axhline(y=mse_measured, color='green', linestyle='--', label=f'MSE pomiarów (referencja) = {mse_measured:.2f}')

plt.ylim(0, ymax_test)
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

# --- 4. Wykres punktowy: wartości skorygowane (predykcje), zmierzone i rzeczywiste ---

plt.figure(figsize=(12,8))

markers = {"RELU":"o", "TANH":"^", "SIGMOID":"s"}
sizes = {"RELU":4, "TANH":4, "SIGMOID":4}
alphas = {"RELU":0.3, "TANH":0.3, "SIGMOID":0.3}
colors_pred = {"RELU":"blue", "TANH":"orange", "SIGMOID":"purple"}

# Wartości zmierzone i rzeczywiste tylko raz (z RELU)
plt.scatter(meas_x, meas_y, label="Wartości zmierzone", alpha=0.7, s=5, c='black', zorder=1)
plt.scatter(actual_x, actual_y, label="Wartości rzeczywiste", alpha=0.7, s=3, c='green', marker='x', zorder=3)

# Wartości skorygowane (predykcje) dla każdej aktywacji
for act in activation_functions:
    pred_x = results_data[act]["PredX"].values
    pred_y = results_data[act]["PredY"].values
    plt.scatter(pred_x, pred_y, label=f"Wartości skorygowane ({act})",
                alpha=alphas[act], s=sizes[act], c=colors_pred[act], marker=markers[act], zorder=2)

plt.xlabel("X")
plt.ylabel("Y")
plt.title("Wartości pomiarów dynamicznych - porównanie wariantów aktywacji")
plt.legend()
plt.grid(True)
plt.show()
