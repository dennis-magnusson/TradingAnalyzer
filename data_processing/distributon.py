import pandas as pd

# Define the file path
file_path = "debs2022-gc-trading-day-08-11-21.csv"

# Initialize an empty dictionary to hold ID counts
id_counts = {}

# Define chunk size for reading the file
chunk_size = 10**6  # 1 million rows per chunk

# Process the CSV file in chunks
for chunk in pd.read_csv(file_path, chunksize=chunk_size, usecols=[0]):

    # Rename the first column to 'ID' for easier processing
    chunk.columns = ["ID"]
    # Count occurrences of each ID in the chunk
    chunk_counts = chunk["ID"].value_counts()
    
    # Update the global counts
    for id_, count in chunk_counts.items():
        if id_ in id_counts:
            id_counts[id_] += count
        else:
            id_counts[id_] = count

# Convert the dictionary to a DataFrame for analysis
id_distribution = pd.DataFrame(list(id_counts.items()), columns=["ID", "Count"])

# Compute total counts and percentages
total_count = id_distribution["Count"].sum()
id_distribution["Percentage"] = (id_distribution["Count"] / total_count) * 100

# Sort by count (descending order) for better readability
id_distribution = id_distribution.sort_values(by="Count", ascending=False)

# Save to a CSV file
output_file = "id_distribution.csv"
id_distribution.to_csv(output_file, index=False)

# Display a summary
print(id_distribution.head(10))  # Display top 10 IDs
print(f"\nDistribution saved to {output_file}")

