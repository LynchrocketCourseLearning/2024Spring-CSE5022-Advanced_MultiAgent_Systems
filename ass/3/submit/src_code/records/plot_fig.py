import pandas as pd
import matplotlib.pyplot as plt

filenames = ['regular_Vickrey.txt', 'random_Vickrey.txt', 'regular_Dutch.txt', 'random_Dutch.txt']
for filename in filenames:
    data = pd.read_csv(filename)
    total_utility = data.groupby('tick').sum()
    
    plt.figure(figsize=(10,5))
    plt.plot(total_utility['utility'], label='utility')
    plt.xlabel('tick')
    plt.ylabel('total utility')
    plt.title(f'Total utility of cameras for every tick in {filename}')
    plt.savefig(f'{filename.split(".")[0]}.png')

