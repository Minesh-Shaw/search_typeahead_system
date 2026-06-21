import csv
import sys

try:
    import nltk
    from nltk.corpus import stopwords
except ImportError:
    print("❌ Error: The 'nltk' library is required to dynamically load stop words.")
    print("Please install it by running: pip install nltk")
    sys.exit(1)

# Ensure the stopwords corpus is downloaded locally
try:
    nltk.data.find('corpora/stopwords')
except LookupError:
    print("Downloading NLTK stopwords database...")
    nltk.download('stopwords', quiet=True)

# Ensure your original file is named 'dataset.csv' and is in the same folder as this script
input_file = './dataset.csv'
output_file = './cleaned_dataset.csv'

# Dynamically load NLTK's comprehensive set of English stop words
stop_words = set(stopwords.words('english'))

print(f"Reading from '{input_file}' and filtering stop words...")

try:
    with open(input_file, mode='r', encoding='utf-8') as infile, \
         open(output_file, mode='w', encoding='utf-8', newline='') as outfile:
         
        reader = csv.reader(infile)
        writer = csv.writer(outfile)
        
        # Read and write the header row
        header = next(reader, None)
        if header:
            writer.writerow(header)
            
        kept_count = 0
        removed_count = 0
        
        for row in reader:
            if len(row) >= 2:
                query = row[0].strip().lower()
                
                # Filter criteria: 
                # 1. Length >= 3 (removes "a", "an", "is", "of", "it", etc.)
                # 2. Not in our stop words list
                # 3. Contains only alphabetic characters (removes weird punctuation)
                if len(query) >= 3 and query not in stop_words and query.isalpha():
                    writer.writerow(row)
                    kept_count += 1
                else:
                    removed_count += 1

    print("✅ Filtering Complete!")
    print(f"📈 Kept {kept_count} meaningful words.")
    print(f"🗑️  Removed {removed_count} stop words and short words.")
    print(f"\nNext Steps:")
    print(f"1. Rename '{output_file}' to 'dataset.csv'")
    print(f"2. Move it into your Spring Boot 'src/main/resources/' folder.")

except FileNotFoundError:
    print(f"❌ Error: Could not find '{input_file}'. Please make sure it is in the same folder as this script.")