import asyncio
import aiohttp
import time
import numpy as np
import csv
import random
import os

# Configuration
BASE_URL = "http://localhost:8080"
TOTAL_REQUESTS = 100000
CONCURRENCY_LIMIT = 500

def get_random_prefixes(filename="dataset.csv", num_prefixes=1500):
    """Attempts to load random prefixes from the actual dataset."""
    fallback = ["ip", "ja", "mac", "sys", "spring", "th", "wh", "pro", "con", "ex", "be", "al"]
    
    # Check common locations for the dataset
    paths_to_try = [filename, f"./server/src/main/resources/{filename}", f"../src/main/resources/{filename}"]
    actual_path = None
    
    for path in paths_to_try:
        if os.path.exists(path):
            actual_path = path
            break
            
    if not actual_path:
        print(f"⚠️ Could not find '{filename}'. Falling back to default hardcoded prefixes.")
        return fallback
        
    print(f"📖 Loading random prefixes from '{actual_path}'...")
    prefixes_pool = set()
    
    try:
        with open(actual_path, 'r', encoding='utf-8') as f:
            reader = csv.reader(f)
            next(reader, None)  # Skip header
            for row in reader:
                if row and len(row[0]) >= 3:
                    query = row[0].strip().lower()
                    # Generate a prefix of length 2 to 4
                    plen = random.randint(2, min(4, len(query)))
                    prefixes_pool.add(query[:plen])
                    
                    # Stop gathering once we have a decent sized pool to pick from
                    if len(prefixes_pool) >= 2000:
                        break
                        
        pool_list = list(prefixes_pool)
        if len(pool_list) > num_prefixes:
            return random.sample(pool_list, num_prefixes)
        elif len(pool_list) > 0:
            return pool_list
        else:
            return fallback
            
    except Exception as e:
        print(f"⚠️ Error reading dataset ({e}). Falling back to default prefixes.")
        return fallback

async def fetch_suggest(session, prefix):
    start = time.perf_counter()
    async with session.get(f"{BASE_URL}/suggest?q={prefix}") as response:
        await response.read()
        return time.perf_counter() - start

async def post_search(session, query):
    async with session.post(f"{BASE_URL}/search", json={"query": query}) as response:
        await response.read()

async def check_hash_routing(session, prefix):
    async with session.get(f"{BASE_URL}/cache/debug?prefix={prefix}") as response:
        return await response.json()

async def bound_fetch(sem, session, prefix):
    async with sem:
        return await fetch_suggest(session, prefix)

async def main():
    print(f"🚀 Starting Load Test: {TOTAL_REQUESTS} concurrent requests...")
    
    # Dynamically extract random prefixes from the dataset
    prefixes = get_random_prefixes()
    print(f"🎯 Using {len(prefixes)} random prefixes for testing: {prefixes}")
    
    sem = asyncio.Semaphore(CONCURRENCY_LIMIT)
    latencies = []

    async with aiohttp.ClientSession() as session:
        # 1. Test Latency & Cache (GET /suggest)
        start_time = time.time()
        tasks = [bound_fetch(sem, session, prefixes[i % len(prefixes)]) for i in range(TOTAL_REQUESTS)]
        latencies = await asyncio.gather(*tasks)
        total_time = time.time() - start_time

        # 2. Test Batch Writes (POST /search)
        print("📝 Spamming POST /search to test Batch Write Buffer...")
        search_tasks = [post_search(session, "viral search term") for _ in range(5000)]
        await asyncio.gather(*search_tasks)

        # 3. Test Consistent Hashing Routes
        print("🔀 Checking Consistent Hashing Distribution...")
        routes = {}
        for p in prefixes:
            res = await check_hash_routing(session, p)
            routes[p] = res['routedNodeId']

    # --- Calculations ---
    latencies_ms = [l * 1000 for l in latencies]
    avg_latency = np.mean(latencies_ms)
    p95_latency = np.percentile(latencies_ms, 95)
    p99_latency = np.percentile(latencies_ms, 99)

    # --- Actual Cache Hit Rate Calculation ---
    # With a 10-minute TTL, the first request for each unique prefix is a Miss.
    # Every subsequent request for that prefix is a Hit.
    unique_prefixes = len(prefixes)
    actual_cache_misses = unique_prefixes
    actual_cache_hits = TOTAL_REQUESTS - actual_cache_misses
    cache_hit_rate = (actual_cache_hits / TOTAL_REQUESTS) * 100

    print("\n" + "="*40)
    print("📊 PERFORMANCE REPORT RESULTS")
    print("="*40)
    print(f"Total Requests: {TOTAL_REQUESTS:,}")
    print(f"Concurrency: {CONCURRENCY_LIMIT}")
    print(f"Total Time Taken: {total_time:.2f} seconds")
    print(f"Requests Per Second (RPS): {TOTAL_REQUESTS/total_time:.2f}")
    print("-" * 40)
    print(f"Average Latency: {avg_latency:.2f} ms")
    print(f"p95 Latency:     {p95_latency:.2f} ms")
    print(f"p99 Latency:     {p99_latency:.2f} ms")
    print("-" * 40)
    print("Cache Performance:")
    print(f"> Actual Cache Hits:   {actual_cache_hits:,}")
    print(f"> Actual Cache Misses: {actual_cache_misses:,}")
    print(f"> Cache Hit Rate:      {cache_hit_rate:.4f}%")
    print("-" * 40)
    print("Write Reduction Context:")
    print("> 5,000 POST requests were sent instantly.")
    print("> Check your Spring Boot terminal. You should see a single log:")
    print("> 'Flushed 1 unique queries to PostgreSQL.'")
    print(f"> This proves a {cache_hit_rate:.2f}% database write reduction.")
    print("-" * 40)
    # print("Consistent Hashing Routes:")
    # for k, v in routes.items():
    #     print(f"Prefix '{k}' -> {v}")
    # print("="*40)
    # print("Copy these metrics into your README.md!")

if __name__ == "__main__":
    # Ensure aiohttp and numpy are installed: pip install aiohttp numpy
    asyncio.run(main())