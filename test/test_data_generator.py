import csv
from datetime import datetime, timedelta

def generate_simulated_data(file_name="simulated_data.csv", duration_minutes=240, interval_seconds=1):
    """
    Simulate data generation for one hour, writing to a CSV file, without taking real-time.

    Args:
    - file_name (str): Name of the output CSV file.
    - duration_minutes (int): Duration of simulated data in minutes.
    - interval_seconds (int): Interval in seconds between data points.
    """
    # Initialize parameters
    value = 1.0
    short_ema = 0.0
    long_ema = 0.0
    increasing_factor = 1.0
    smoothing_short = 0.051282051
    smoothing_long = 0.01980198
    start_time = datetime.now()
    simulated_start_time = datetime.now()  # Base simulated timestamps

    # Open file for writing
    with open(file_name, mode="w", newline="") as file:
        writer = csv.writer(file)
        # Write header
        writer.writerow(["Key", "Value", "Timestamp", "ShortEMA", "LongEMA"])

        current_time = simulated_start_time
        end_time = simulated_start_time + timedelta(minutes=duration_minutes)

        while current_time < end_time:
            # Generate data
            key = "testKey"
            timestamp = int(current_time.timestamp() * 1000)  # Simulated timestamp in milliseconds
            record_value = [key, value, timestamp]

            # Calculate EMAs
            short_ema = smoothing_short * value + (1 - smoothing_short) * short_ema
            long_ema = smoothing_long * value + (1 - smoothing_long) * long_ema

            # Write row to file
            writer.writerow(record_value + [short_ema, long_ema])
            print(f"Generated: {record_value}, ShortEMA: {short_ema}, LongEMA: {long_ema}")

            # Update value
            value += increasing_factor
            if value > 100:
                increasing_factor = -1.0
            elif value < 20:
                increasing_factor = 1.0

            # Move to the next simulated timestamp
            current_time += timedelta(seconds=interval_seconds)

    print(f"Simulated data generation completed. File saved as {file_name}.")

# Run the simulated data generator
generate_simulated_data()
