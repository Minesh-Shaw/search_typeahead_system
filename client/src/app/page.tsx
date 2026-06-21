"use client";

import { useState, useEffect, KeyboardEvent } from "react";

export default function Home() {
  const [query, setQuery] = useState("");
  const [debouncedQuery, setDebouncedQuery] = useState("");
  const [suggestions, setSuggestions] = useState<string[]>([]);
  const [trending, setTrending] = useState<string[]>([]);
  const [searchResponse, setSearchResponse] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(false);
  const [selectedIndex, setSelectedIndex] = useState(-1);

  // Fetch Trending Searches on initial load
  useEffect(() => {
    const fetchTrending = async () => {
      try {
        const response = await fetch("http://localhost:8080/trending");
        if (response.ok) {
          const data = await response.json();
          setTrending(data);
        }
      } catch (error) {
        console.error("Failed to fetch trending searches", error);
      }
    };
    fetchTrending();
  }, []);

  // Debounce Logic
  useEffect(() => {
    const timer = setTimeout(() => {
      setDebouncedQuery(query);
    }, 300);
    return () => clearTimeout(timer);
  }, [query]);

  // Fetch Suggestions
  useEffect(() => {
    if (!debouncedQuery.trim()) {
      // eslint-disable-next-line react-hooks/set-state-in-effect
      setSuggestions([]);
      setSelectedIndex(-1);
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

  // Handle Search Submission
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
        setSearchResponse(`${data.message}: "${searchStr}"`);
        setSuggestions([]); 
        setQuery(""); 
        setSelectedIndex(-1);
      }
    } catch (error) {
      console.error("Failed to submit search", error);
    }
  };

  // Keyboard Navigation Support
  const handleKeyDown = (e: KeyboardEvent<HTMLInputElement>) => {
    if (suggestions.length === 0) {
      if (e.key === "Enter") handleSearch(query);
      return;
    }

    if (e.key === "ArrowDown") {
      e.preventDefault();
      setSelectedIndex((prev) => (prev < suggestions.length - 1 ? prev + 1 : prev));
    } else if (e.key === "ArrowUp") {
      e.preventDefault();
      setSelectedIndex((prev) => (prev > 0 ? prev - 1 : -1));
    } else if (e.key === "Enter") {
      e.preventDefault();
      if (selectedIndex >= 0 && selectedIndex < suggestions.length) {
        handleSearch(suggestions[selectedIndex]);
      } else {
        handleSearch(query);
      }
    }
  };

  return (
    <main className="min-h-screen bg-white flex flex-col items-center pt-32 px-4 font-sans text-gray-900">
      <h1 className="text-5xl font-bold mb-8 tracking-tight">
        <span className="text-blue-500">T</span>
        <span className="text-red-500">y</span>
        <span className="text-yellow-500">p</span>
        <span className="text-blue-500">e</span>
        <span className="text-green-500">a</span>
        <span className="text-red-500">h</span>
        <span className="text-blue-500">e</span>
        <span className="text-yellow-500">a</span>
        <span className="text-green-500">d</span>
      </h1>

      <div className="w-full max-w-2xl relative">
        <div className="relative flex items-center w-full h-14 rounded-full border border-gray-200 bg-white hover:shadow-md focus-within:shadow-md transition-shadow px-4">
          <svg className="w-5 h-5 text-gray-400 mr-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
          </svg>
          
          <input
            type="text"
            className="flex-1 h-full outline-none text-lg bg-transparent"
            placeholder="Search the web..."
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            onKeyDown={handleKeyDown}
          />
          
          {isLoading && (
            <div className="w-5 h-5 border-2 border-blue-500 border-t-transparent rounded-full animate-spin"></div>
          )}
        </div>

        {/* Suggestions Dropdown */}
        {suggestions.length > 0 && query.trim() !== "" && (
          <div className="absolute top-16 left-0 w-full bg-white border border-gray-100 rounded-2xl shadow-xl overflow-hidden z-20 pb-2 pt-2">
            <ul>
              {suggestions.map((suggestion, index) => (
                <li
                  key={index}
                  className={`px-5 py-2 cursor-pointer flex items-center ${
                    selectedIndex === index ? "bg-gray-100" : "hover:bg-gray-100"
                  }`}
                  onClick={() => handleSearch(suggestion)}
                  onMouseEnter={() => setSelectedIndex(index)}
                >
                  <svg className="w-4 h-4 text-gray-400 mr-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
                  </svg>
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

      {/* Trending Searches Section */}
      {query.trim() === "" && trending.length > 0 && (
        <div className="w-full max-w-2xl mt-10 p-6 bg-gray-50 rounded-2xl border border-gray-100">
          <div className="flex items-center mb-4 text-gray-700">
            <svg className="w-5 h-5 mr-2 text-red-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13 7h8m0 0v8m0-8l-8 8-4-4-6 6" />
            </svg>
            <h2 className="text-lg font-semibold">Trending Searches</h2>
          </div>
          <div className="flex flex-wrap gap-2">
            {trending.map((trend, index) => (
              <button
                key={index}
                onClick={() => handleSearch(trend)}
                className="px-4 py-2 bg-white border border-gray-200 rounded-full text-sm hover:bg-gray-100 transition-colors shadow-sm text-gray-700"
              >
                {trend}
              </button>
            ))}
          </div>
        </div>
      )}

      {/* Dummy Response Display */}
      {searchResponse && (
        <div className="fixed bottom-8 bg-gray-800 text-white px-6 py-3 rounded-full shadow-lg animate-fade-in flex items-center">
          <span className="mr-2">✅</span> {searchResponse}
        </div>
      )}
    </main>
  );
}