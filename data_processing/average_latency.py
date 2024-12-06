import os
import csv
from typing import List, Tuple

def calculate_column_averages(file_path: str) -> Tuple[float, float]:
    """
    Calculate the average of the first two columns in a log file.
    
    :param file_path: Path to the log file
    :return: Tuple of (EMA Calculation Latency average, Arrival Latency average)
    """
    ema_latencies = []
    arrival_latencies = []
    
    try:
        with open(file_path, 'r') as csvfile:
            # Skip the header row
            next(csvfile)
            
            csv_reader = csv.reader(csvfile)
            for row in csv_reader:
                # Convert first two columns to float
                ema_latencies.append(float(row[0]))
                arrival_latencies.append(float(row[1]))
                
        ema_latencies = ema_latencies[1:]
        arrival_latencies = arrival_latencies[1:]
        
        # Calculate averages
        ema_avg = sum(ema_latencies) / len(ema_latencies) if ema_latencies else 0
        arrival_avg = sum(arrival_latencies) / len(arrival_latencies) if arrival_latencies else 0
        
        return ema_avg, arrival_avg
    
    except (FileNotFoundError, IndexError, ValueError) as e:
        print(f"Error processing file {file_path}: {e}")
        return 0, 0

def process_log_files(base_pattern: str, start: int, end: int, step: int) -> List[Tuple[str, float, float]]:
    """
    Process multiple log files with a numeric pattern.
    
    :param base_pattern: Base filename pattern
    :param start: Starting number
    :param end: Ending number
    :param step: Increment between numbers
    :return: List of tuples (filename, EMA Latency Avg, Arrival Latency Avg)
    """
    results = []
    
    for num in range(start, end + 1, step):
        filename = f"{base_pattern}{num}.log"
        
        if os.path.exists(filename):
            ema_avg, arrival_avg = calculate_column_averages(filename)
            results.append((filename, ema_avg, arrival_avg))
        else:
            print(f"File {filename} not found. Skipping.")
    
    return results

def main():
    # Configure your log file pattern here
    base_pattern = "latency_"
    start = 100
    end = 800
    step = 100
    
    # Process log files
    results = process_log_files(base_pattern, start, end, step)
    
    # Print results
    print("Log File Latency Averages:")
    print("-" * 50)
    print(f"{'Filename':<20} {'EMA Latency Avg':<20} {'Arrival Latency Avg':<20}")
    print("-" * 50)
    
    for filename, ema_avg, arrival_avg in results:
        print(f"{filename:<20} {ema_avg:<20.2f} {arrival_avg:<20.2f}")

if __name__ == "__main__":
    main()
