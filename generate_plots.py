import os
import csv
import matplotlib.pyplot as plt

# 1. Автоматически создаем папку plots
os.makedirs("plots", exist_ok=True)

csv_path = os.path.join("results", "results.csv")

data_time = {}   # {algorithm: ([sizes], [times])}
data_depth = {}  # {algorithm: ([sizes], [depths])}

# 2. Читаем данные из results.csv
with open(csv_path, mode='r', encoding='utf-8') as f:
    reader = csv.DictReader(f)
    for row in reader:
        if row['InputType'].strip().lower() == 'random':
            algo = row['Algorithm'].strip()
            size = int(row['InputSize'])
            time_ms = float(row['ExecutionTimeMs'])
            depth = int(row['MaxRecursionDepth'])

            if algo not in data_time:
                data_time[algo] = ([], [])
                data_depth[algo] = ([], [])

            data_time[algo][0].append(size)
            data_time[algo][1].append(time_ms)

            data_depth[algo][0].append(size)
            data_depth[algo][1].append(depth)

styles = {
    'MergeSort': ('o-', '#1f77b4'),
    'QuickSort': ('s-', '#ff7f0e'),
    'DeterministicSelect': ('^-', '#2ca02c'),
    'ClosestPair': ('d-', '#d62728')
}

# 3. Строим График 1: Время от N
plt.figure(figsize=(8, 5))
for algo, (sizes, times) in data_time.items():
    style, color = styles.get(algo, ('o-', 'blue'))
    plt.plot(sizes, times, style, label=algo, color=color, linewidth=2, markersize=6)

plt.xscale('log')
plt.yscale('log')
plt.xlabel('Input Size N (log scale)', fontsize=11)
plt.ylabel('Execution Time (ms, log scale)', fontsize=11)
plt.title('Execution Time vs Input Size N (Random Input)', fontsize=13, fontweight='bold')
plt.grid(True, which="both", ls="--", alpha=0.5)
plt.legend(fontsize=10)
plt.tight_layout()
plt.savefig(os.path.join("plots", "time_vs_n.png"), dpi=300)
plt.close()

# 4. Строим График 2: Глубина рекурсии от N
plt.figure(figsize=(8, 5))
for algo, (sizes, depths) in data_depth.items():
    style, color = styles.get(algo, ('o-', 'blue'))
    plt.plot(sizes, depths, style, label=algo, color=color, linewidth=2, markersize=6)

plt.xscale('log')
plt.xlabel('Input Size N (log scale)', fontsize=11)
plt.ylabel('Max Recursion Depth', fontsize=11)
plt.title('Max Recursion Depth vs Input Size N (Random Input)', fontsize=13, fontweight='bold')
plt.grid(True, which="both", ls="--", alpha=0.5)
plt.legend(fontsize=10)
plt.tight_layout()
plt.savefig(os.path.join("plots", "recursion_depth_vs_n.png"), dpi=300)
plt.close()

print(" Успешно! Файлы time_vs_n.png и recursion_depth_vs_n.png созданы в папке plots/")