import pandas as pd
import matplotlib.pyplot as plt
import numpy as np

def analyze_and_save(file_name="simulated_data.csv", plot_file="breakouts.png", ema_plot_file="ema_values.png", alert_file="alerts.txt"):
    """
    Analyze the data with one-minute aggregation, detect breakout patterns, 
    save the visualization, and write alerts to a text file.
    
    Args:
    - file_name (str): The CSV file containing generated data.
    - plot_file (str): Name of the output image file for the main plot.
    - ema_plot_file (str): Name of the output image file for EMA values.
    - alert_file (str): Name of the output text file for alerts.
    """
    # Load data
    data = pd.read_csv(file_name)
    data['Timestamp'] = pd.to_datetime(data['Timestamp'], unit='ms')
    
    # Aggregate data to one-minute windows
    aggregated_data = data.resample('1T', on='Timestamp').last().reset_index()
    
    # Initialize EMA columns with zeros
    aggregated_data['ShortEMA'] = 0.0
    aggregated_data['LongEMA'] = 0.0
    
    # Calculate EMAs
    short_alpha = 0.051282051  # Weight for recent values in short EMA
    long_alpha = 0.01980198    # Weight for recent values in long EMA
    
    # Calculate EMAs iteratively
    for i in range(len(aggregated_data)):
        # Skip if the value is NaN
        if pd.isna(aggregated_data.loc[i, 'Value']):
            continue
        
        # First point handling
        if i == 0 or aggregated_data.loc[i-1, 'ShortEMA'] == 0:
            # For the first point or if previous EMA was zero, 
            # set EMA directly to the current value
            aggregated_data.loc[i, 'ShortEMA'] = aggregated_data.loc[i, 'Value']
            aggregated_data.loc[i, 'LongEMA'] = aggregated_data.loc[i, 'Value']
        else:
            # Standard EMA calculation
            # Short EMA calculation
            aggregated_data.loc[i, 'ShortEMA'] = (
                short_alpha * aggregated_data.loc[i, 'Value'] + 
                (1 - short_alpha) * aggregated_data.loc[i-1, 'ShortEMA']
            )
            
            # Long EMA calculation
            aggregated_data.loc[i, 'LongEMA'] = (
                long_alpha * aggregated_data.loc[i, 'Value'] + 
                (1 - long_alpha) * aggregated_data.loc[i-1, 'LongEMA']
            )
    
    # Detect breakout patterns
    aggregated_data['Bullish'] = (
        (aggregated_data['ShortEMA'] > aggregated_data['LongEMA']) & 
        (aggregated_data['ShortEMA'].shift(1) <= aggregated_data['LongEMA'].shift(1))
    )
    aggregated_data['Bearish'] = (
        (aggregated_data['ShortEMA'] < aggregated_data['LongEMA']) & 
        (aggregated_data['ShortEMA'].shift(1) >= aggregated_data['LongEMA'].shift(1))
    )
    
    # Extract timestamps for bullish and bearish breakout patterns
    bullish_points = aggregated_data[aggregated_data['Bullish']]
    bearish_points = aggregated_data[aggregated_data['Bearish']]
    
    # Main Plot with Value and EMAs
    plt.figure(figsize=(15, 8))
    plt.plot(aggregated_data['Timestamp'], aggregated_data['Value'], 
             label='Value', color='blue', alpha=0.5)
    plt.plot(aggregated_data['Timestamp'], aggregated_data['ShortEMA'], 
             label='Short EMA', color='green')
    plt.plot(aggregated_data['Timestamp'], aggregated_data['LongEMA'], 
             label='Long EMA', color='red')
    
    # Highlight breakout points
    plt.scatter(bullish_points['Timestamp'], bullish_points['Value'], 
                color='lime', label='Bullish Breakout', marker='^', s=100)
    plt.scatter(bearish_points['Timestamp'], bearish_points['Value'], 
                color='orange', label='Bearish Breakout', marker='v', s=100)
    
    # Annotate breakout points
    for _, row in bullish_points.iterrows():
        plt.annotate('Buy', 
                     xy=(row['Timestamp'], row['Value']), 
                     xytext=(row['Timestamp'], row['Value'] + 5),
                     arrowprops=dict(facecolor='lime', arrowstyle='->'), 
                     fontsize=10, color='lime')
    
    for _, row in bearish_points.iterrows():
        plt.annotate('Sell', 
                     xy=(row['Timestamp'], row['Value']), 
                     xytext=(row['Timestamp'], row['Value'] - 5),
                     arrowprops=dict(facecolor='orange', arrowstyle='->'), 
                     fontsize=10, color='orange')
    
    # Add labels and legend
    plt.title('One-Minute Aggregated Data Analysis and Breakout Detection')
    plt.xlabel('Timestamp')
    plt.ylabel('Value')
    plt.legend()
    plt.grid(True)
    plt.tight_layout()
    
    # Save the main plot
    plt.savefig(plot_file)
    plt.close()  # Close the first plot
    
    # EMA-only Plot
    plt.figure(figsize=(15, 8))
    plt.plot(aggregated_data['Timestamp'], aggregated_data['ShortEMA'], 
             label='Short EMA', color='green')
    plt.plot(aggregated_data['Timestamp'], aggregated_data['LongEMA'], 
             label='Long EMA', color='red')
    
    # Highlight EMA crossover points
    plt.scatter(bullish_points['Timestamp'], bullish_points['ShortEMA'], 
                color='lime', label='Bullish Crossover', marker='^', s=100)
    plt.scatter(bearish_points['Timestamp'], bearish_points['LongEMA'], 
                color='orange', label='Bearish Crossover', marker='v', s=100)
    
    # Add labels and legend
    plt.title('Exponential Moving Averages (EMA)')
    plt.xlabel('Timestamp')
    plt.ylabel('EMA Value')
    plt.legend()
    plt.grid(True)
    plt.tight_layout()
    
    # Save the EMA-only plot
    plt.savefig(ema_plot_file)
    print(f"Main plot saved as {plot_file}")
    print(f"EMA plot saved as {ema_plot_file}")
    
    # Save alerts to a text file
    with open(alert_file, 'w') as alert_f:
        alert_f.write("--- Buy Alerts (Bullish Breakouts) ---\n")
        alert_f.write(bullish_points[['Timestamp', 'Value']].to_string(index=False))
        alert_f.write("\n\n--- Sell Alerts (Bearish Breakouts) ---\n")
        alert_f.write(bearish_points[['Timestamp', 'Value']].to_string(index=False))
    print(f"Alerts saved as {alert_file}")
    
    return aggregated_data

# Run the analysis and save results
result = analyze_and_save()