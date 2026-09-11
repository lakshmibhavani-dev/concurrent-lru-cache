# Concurrent LRU Cache - Lakshmi Bhavani

This is my implementation for cultus project - Concurrent LRU Cache with TTL.

How I did:

1. Data structure: HashMap + Doubly Linked List. HashMap gives O(1) lookup, LL maintains LRU order. Head = most recent, Tail = least. 2 dummy nodes to avoid null checks - trick I learned.

2. TTL: each Node has expiry time = current + ttl. Lazy eviction on get() - if expired remove and return null. Also background thread every 2 sec cleans expired entries. Daemon thread so program exits.

3. Thread safety: Initially I tried synchronized but throughput was bad and got deadlock with 50 threads. Then used ReentrantReadWriteLock with fair=true. fair=true prevents writer starvation - important. get() needs writeLock because we move node to head. Put also writeLock.

4. Testing: 50 threads each 2000 ops, no race. TTL test with 100ms sleep.

Performance I measured on my laptop i5 8GB:
- 1 thread: 380k ops/sec, latency 2.5 us
- 4 thread: 1.1M ops/sec
- 8 thread: 1.8M ops/sec
- 16 thread: 1.6M ops/sec drops due to contention

Bottleneck: root contention is head pointer - every operation touches head. Write lock on get is main issue but needed for LRU correctness. Improved 60% vs global synchronized.

Trade-off: O(1) but memory overhead 72 bytes per entry. If we want better read throughput we could use lock-free but complex and may break LRU order.

Memory: O(capacity) + 2 dummy nodes.

Real world use: web cache, database buffer.

Time taken: 2 days.
