import java.util.*;
import java.util.concurrent.locks.*;

// LRU cache - hashmap + doubly linked list - O(1) get put
// reference from leetcode 146 but i made it thread safe myself
// lakshmi bhavani - took 2 days, deadlock issue fixed with fair lock
// my laptop i5 8gb

public class ConcurrentLRUCache<K,V> {
    int capacity;
    long defaultTTL;
    Map<K, Node<K,V>> map; // using HashMap not ConcurrentHashMap coz we have own lock
    Node<K,V> head, tail;
    ReentrantReadWriteLock lock = new ReentrantReadWriteLock(true); // fair=true imp - prevents writer starvation
    long hits=0;
    long misses=0;

    public ConcurrentLRUCache(int cap, long ttl){
        capacity=cap;
        defaultTTL=ttl;
        map=new HashMap<>();
        head=new Node<>(null,null,0);
        tail=new Node<>(null,null,0);
        head.next=tail;
        tail.prev=head;

        // background cleaner thread - runs every 2 sec
        Thread cleaner = new Thread(()->{
            while(true){
                try{
                    Thread.sleep(2000);
                    lock.writeLock().lock();
                    try{
                        cleanup();
                    }finally{
                        lock.writeLock().unlock();
                    }
                }catch(Exception e){
                    // e.printStackTrace();
                }
            }
        });
        cleaner.setDaemon(true); // imp else program wont exit
        cleaner.start();
    }

    public ConcurrentLRUCache(int cap){
        this(cap,0);
    }

    public V get(K key){
        lock.writeLock().lock(); // need write lock because we move to head - read not enough
        try{
            Node<K,V> n = map.get(key);
            if(n==null){
                misses++;
                return null;
            }
            if(n.isExpired()){
                // lazy eviction - remove if expired on get
                remove(n);
                map.remove(key);
                misses++;
                return null;
            }
            // move to head - most recent
            remove(n);
            addFirst(n);
            hits++;
            return n.value;
        }finally{
            lock.writeLock().unlock();
        }
    }

    public void put(K key, V val){
        put(key,val,defaultTTL);
    }

    public void put(K key, V val, long ttl){
        lock.writeLock().lock();
        try{
            long actualTTL = ttl>0 ? ttl : defaultTTL;
            Node<K,V> existing = map.get(key);
            if(existing!=null){
                existing.value=val;
                existing.expiry = actualTTL>0 ? System.currentTimeMillis()+actualTTL : Long.MAX_VALUE;
                remove(existing);
                addFirst(existing);
            }else{
                Node<K,V> nn = new Node<>(key,val,actualTTL);
                map.put(key,nn);
                addFirst(nn);
                if(map.size()>capacity){
                    Node<K,V> lru = tail.prev; // last node
                    remove(lru);
                    map.remove(lru.key);
                }
            }
            // also cleanup expired ones sometimes
            if(map.size()>capacity/2) cleanup(); // small optimization by me
        }finally{
            lock.writeLock().unlock();
        }
    }

    void addFirst(Node<K,V> node){
        node.next=head.next;
        node.prev=head;
        head.next.prev=node;
        head.next=node;
    }

    void remove(Node<K,V> node){
        node.prev.next=node.next;
        node.next.prev=node.prev;
    }

    void cleanup(){
        // scan from tail and remove expired - O(n) but okay coz ttl rare
        Node<K,V> curr = tail.prev;
        while(curr!=head){
            Node<K,V> prev = curr.prev;
            if(curr.isExpired()){
                remove(curr);
                map.remove(curr.key);
            }
            curr=prev;
        }
    }

    public int size(){
        lock.readLock().lock();
        try{ return map.size(); } finally{ lock.readLock().unlock(); }
    }
}
