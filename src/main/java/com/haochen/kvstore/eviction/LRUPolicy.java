package com.haochen.kvstore.eviction;

import java.util.HashMap;
import java.util.Map;

/**
 * LRU (Least Recently Used) eviction policy
 *
 * Implements this policy by using the combination of a HashMap
 * And a doubly linked list to achieve update and eviction with O(1) time complexity
 */
public class LRUPolicy<K> implements EvictionPolicy<K> {
    /**
     * Doubly linked list node that stored the key, the previous and the next node
     */
    private class Node{
        K key;
        Node prev;
        Node next;

        Node(K key){
            this.key = key;
        }
    }

    /**
     * Mapping the key to their corresponding node
     */
    private final Map<K, Node> nodeMap;

    /**
     * The dummy node sentinel
     */
    private final Node sentinel;

    /**
     * The constructor of this class;
     * Initializing the sentinel node and the nodeMap
     */
    public LRUPolicy(){
        sentinel = new Node(null);
        sentinel.next = sentinel;
        sentinel.prev = sentinel;

        nodeMap = new HashMap<>();
    }

    @Override
    public void onPut(K key) {
        if(nodeMap.containsKey(key)){
            moveForward(nodeMap.get(key));
        } else {
            Node node = new Node(key);
            nodeMap.put(key, node);
            insertFront(node);
        }
    }

    @Override
    public void onGet(K key) {
        Node node = nodeMap.getOrDefault(key, null);
        if(node != null){
            moveForward(node);
        }
    }

    @Override
    public boolean shouldEvict() {
        return !nodeMap.isEmpty();
    }


    @Override
    public K evictKey() {
        Node lruNode = sentinel.prev;
        if(lruNode == sentinel){
            return null; //There's nothing ti be evicted
        }

        removeNode(lruNode);
        nodeMap.remove(lruNode);

        return lruNode.key;
    }

    /* -------- Helper Methods -------- */

    /**
     * Moving a node to the front of the list
     * @param node
     */
    private void moveForward(Node node){
        //We first remove the node from the list
        removeNode(node);
        //And then we move the node directly to the front
        insertFront(node);
    }

    /**
     * Inserts a node to the front of the list
     * @param node
     */
    private void insertFront(Node node){
        Node temp = sentinel.next;
        node.next = temp;
        node.prev = sentinel;
        temp.prev = node;
        sentinel.next = node;
    }

    /**
     * Remove a node from the list
     * @param node
     */
    private void removeNode(Node node){
        node.prev.next = node.next;
        node.next.prev = node.prev;
    }
}
