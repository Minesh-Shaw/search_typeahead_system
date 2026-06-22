import csv
import sys

try:
    from datasets import load_dataset
except ImportError:
    print("❌ Error: The 'datasets' library is required.")
    print("Please install it by running: pip install datasets")
    sys.exit(1)

# Configuration
HF_DATASET_NAME = "amazon/AmazonQAC" 
OUTPUT_FILE = "dataset.csv"
MAX_ROWS_TO_PROCESS = 1000000  # Cap the processing to 1 Million rows

print(f"Downloading dataset '{HF_DATASET_NAME}' from Hugging Face...")

try:
    dataset = load_dataset(HF_DATASET_NAME, split="train", streaming=True)
    
    # Dictionary to aggregate the actual popularity for each unique query
    queries = {}
    print(f"Extracting queries from the first {MAX_ROWS_TO_PROCESS:,} rows of the dataset. This will be very fast...")

    for i, row in enumerate(dataset):
        # Stop once we hit our 1 Million row limit
        if i >= MAX_ROWS_TO_PROCESS:
            break

        # Use the exact column names revealed by our debug output
        query_text = row.get('final_search_term')
        popularity = row.get('popularity', 1)
        
        if query_text and isinstance(query_text, str):
            clean_query = query_text.strip().lower()
            if len(clean_query) > 3:
                # Add the actual Amazon popularity score to our total count for this word
                queries[clean_query] = queries.get(clean_query, 0) + popularity
                
        # PROGRESS TRACKER
        if i % 100000 == 0 and i > 0:
            print(f"⏳ Processed {i:,} / {MAX_ROWS_TO_PROCESS:,} raw rows... Found {len(queries):,} unique queries so far.")

    print(f"\n✅ Successfully extracted {len(queries):,} unique queries from {MAX_ROWS_TO_PROCESS:,} rows.")
    print(f"Sorting by true popularity and writing to {OUTPUT_FILE}...")

    # Sort queries by their total aggregated popularity in descending order
    sorted_queries = sorted(queries.items(), key=lambda x: x[1], reverse=True)
    
    with open(OUTPUT_FILE, mode='w', encoding='utf-8', newline='') as outfile:
        writer = csv.writer(outfile)
        writer.writerow(['query', 'count'])
        
        for query, count in sorted_queries:
            writer.writerow([query, count])

    print("🎉 Complete!")
    print(f"File saved as {OUTPUT_FILE}. Check the file size before committing to GitHub.")

except Exception as e:
    print(f"❌ An error occurred: {e}")