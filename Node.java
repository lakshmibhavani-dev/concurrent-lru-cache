import java.util.*;

// Node for doubly LL - lakshmi - 10 sept
// first I forgot to add expiry, then added later
public class Node<K,V> {
    K key;
    V value;
    long expiry; // ttl time
    Node<K,V> prev;
    Node<K,V> next;

    public Node(K k, V v, long ttl){
        key=k;
        value=v;
        if(ttl>0) expiry=System.currentTimeMillis()+ttl;
        else expiry=Long.MAX_VALUE; // no expiry
    }

    boolean isExpired(){
        return System.currentTimeMillis() > expiry;
    }
}
