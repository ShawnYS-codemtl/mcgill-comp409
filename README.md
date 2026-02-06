# mcgill-comp409
*COMP409: Concurrent Programming*

WINTER 2025

FINAL GRADE: B+

## Assignment 1
Snowman Drawing: Fill an image with random, non-overlapping snowmen of varying sizes and orientations using multiple threads, ensuring proper synchronization and measuring multithreaded speedup.

Concurrent Snakes & Ladders: Simulate a 10×10 board game with a player thread moving according to dice rolls and concurrent threads adding/removing snakes and ladders, logging all actions with timestamps.

## Assignment 2
Word Search Puzzle: Fill an n×n grid with letters according to English frequencies, then use t threads to search for valid words along random non-repeating sequences, synchronizing each cell to avoid data races and logging contributions.

Professor, TAs, and Grad Students: Simulate professor interactions with k TAs (requiring groups of 3) and 5 grad students arriving at random times, using monitors and condition variables to coordinate questions, interruptions, and research sessions.

## Assignment 3
Concurrent Resizable Array: Implement a thread-safe resizable array with get and set methods, providing both a blocking (q1a.java) and lock-free (q1b.java) version, and compare their performance under mixed read/write workloads.

Parallel Bracket Matching: Use a thread pool to verify properly nested brackets in a string using divide-and-conquer, computing (b,f,m) triples per substring and combining results to achieve parallel speedup.
