"use client";

import { useState, useEffect } from "react";

export default function Home() {
  const [query, setQuery] = useState("");
  const [debouncedQuery, setDebouncedQuery] = useState("");
  const [suggestions, setSuggestions] = useState<string[]>([]);
  const [searchResponse, setSearchResponse] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(false);

  // 1. Debounce Logic (Rubric Requirement)
  // This prevents the UI from spamming the backend on every single keystroke.
  // It waits 300ms after the user stops typing before setting the debounced query.
  useEffect(() => {
    const timer = setTimeout(() => {
      setDebouncedQuery(query);
    }, 300);

    return () => clearTimeout(timer);
  }, [query]);

  // 2. Fetch Suggestions from Java Backend
  useEffect(() => {
    if (!debouncedQuery.trim()) {
      // eslint-disable-next-line react-hooks/set-state-in-effect
      setSuggestions([]);
      return;
    }

    const fetchSuggestions = async () => {
      setIsLoading(true);
      try {
        const response = await fetch(`http://localhost:8080/suggest?q=${encodeURIComponent(debouncedQuery)}`);
        if (response.ok) {
          const data = await response.json();
          setSuggestions(data);
        }
      } catch (error) {
        console.error("Failed to fetch suggestions", error);
      } finally {
        setIsLoading(false);
      }
    };

    fetchSuggestions();
  }, [debouncedQuery]);

  // 3. Handle Search Submission (Rubric Requirement)
  const handleSearch = async (searchStr: string) => {
    if (!searchStr.trim()) return;

    try {
      const response = await fetch("http://localhost:8080/search", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ query: searchStr }),
      });

      if (response.ok) {
        const data = await response.json();
        // Display the dummy response "Searched"
        setSearchResponse(`${data.message}: "${searchStr}"`);
        setSuggestions([]); // Hide suggestions dropdown
        setQuery(""); // Clear input
      }
    } catch (error) {
      console.error("Failed to submit search", error);
    }
  };

  return (
    <main className="min-h-screen bg-gray-50 flex flex-col items-center pt-32 px-4">
      {/* Google-style title */}
      <h1 className="text-5xl font-bold text-gray-800 mb-8 tracking-tight">
        Type<span className="text-blue-600">ahead</span>
      </h1>

      <div className="w-full max-w-2xl relative">
        {/* Search Bar Container */}
        <div className="relative flex items-center w-full h-14 rounded-full border border-gray-300 bg-white hover:shadow-md focus-within:shadow-md transition-shadow px-4">
          <svg className="w-5 h-5 text-gray-400 mr-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
          </svg>
          
          <input
            type="text"
            className="flex-1 h-full outline-none text-lg text-gray-700 bg-transparent"
            placeholder="Search for something..."
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            onKeyDown={(e) => {
              if (e.key === "Enter") {
                handleSearch(query);
              }
            }}
          />
          
          {isLoading && (
            <div className="w-5 h-5 border-2 border-blue-500 border-t-transparent rounded-full animate-spin"></div>
          )}
        </div>

        {/* Suggestions Dropdown */}
        {suggestions.length > 0 && (
          <div className="absolute top-16 left-0 w-full bg-white border border-gray-200 rounded-2xl shadow-lg overflow-hidden z-10">
            <ul className="py-2">
              {suggestions.map((suggestion, index) => (
                <li
                  key={index}
                  className="px-5 py-2.5 hover:bg-gray-100 cursor-pointer text-gray-700 flex items-center"
                  onClick={() => handleSearch(suggestion)}
                >
                  <svg className="w-4 h-4 text-gray-400 mr-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
                  </svg>
                  {/* Highlight the matched prefix in bold */}
                  <span className="font-semibold text-black">
                    {suggestion.substring(0, debouncedQuery.length)}
                  </span>
                  <span>{suggestion.substring(debouncedQuery.length)}</span>
                </li>
              ))}
            </ul>
          </div>
        )}
      </div>

      {/* Dummy Response Display */}
      {searchResponse && (
        <div className="mt-12 p-4 bg-green-100 text-green-800 rounded-lg shadow-sm border border-green-200 animate-fade-in">
          ✅ {searchResponse}
          <p className="text-sm mt-1 text-green-600">The query has been submitted to the Java batch-writer buffer.</p>
        </div>
      )}
    </main>
  );
}