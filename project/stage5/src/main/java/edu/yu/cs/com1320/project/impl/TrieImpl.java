package edu.yu.cs.com1320.project.impl;

import edu.yu.cs.com1320.project.Trie;

import java.util.*;

public class TrieImpl<Value> implements Trie<Value> {

    private static int alphabetSize=256;
    private Node<Value> root=new Node<>();

    private Set<Set<Value>> deleteAllSet = new HashSet<>();


    private class Node<Value>
    {
        Set<Value> value= new HashSet<>();
        Node[] children = new Node[TrieImpl.alphabetSize];

    }



    /**
     * add the given value at the given key
     *
     * @param key
     * @param val
     */
    public void put(String key, Value val) {

        if(val==null || key==null)
        {
            return;
        }
        else
        {
            this.root=put(this.root,key.toUpperCase(),val,0);
        }

    }


    private Node put(Node x, String key, Value val, int d)
    {


        if (x == null)
        {
            x = new Node();
        }

        if (d == key.length()) {

            x.value.add(val);
            return x;

        }

        char c = key.charAt(d);
        x.children[c] = this.put(x.children[c], key.toUpperCase(), val, d + 1);
        return x;

    }











    private Node get(Node node, String key, int d) {

        //link was null - return null, indicating a miss
        if (node == null)
        {
            return null;
        }
        //we've reached the last node in the key,
        //return the node
        if (d == key.length())
        {
            return node;
        }
        //proceed to the next node in the chain of nodes that
        //forms the desired key
        char c = key.charAt(d);
        return this.get(node.children[c], key, d + 1);
    }



    /**
     * get all exact matches for the given key, sorted in descending order.
     * Search is CASE INSENSITIVE.
     * @param key
     * @param comparator used to sort  values
     * @return a List of matching Values, in descending order
     */

    public List<Value> getAllSorted(String key, Comparator<Value> comparator) {
        if(key==null || comparator==null)
        {
            return Collections.emptyList();
        }

        Node x = this.get(this.root, key.toUpperCase(), 0);
        if (x == null)
        {
            return Collections.emptyList();
        }

        List list= Arrays.asList(x.value);
        Collections.sort(list, comparator);
        return list;
    }
    /**
     * get all matches which contain a String with the given prefix, sorted in descending order.
     * For example, if the key is "Too", you would return any value that contains "Tool", "Too", "Tooth", "Toodle", etc.
     * Search is CASE INSENSITIVE.
     * @param prefix
     * @param comparator used to sort values
     * @return a List of all matching Values containing the given prefix, in descending order
     */
    public List<Value> getAllWithPrefixSorted(String prefix, Comparator<Value> comparator) {
        if(prefix==null || comparator==null)
        {
            return Collections.emptyList();
        }

        Node current =findNode(prefix.toUpperCase());
        List<Set<Value>>prefixes = new ArrayList<>();
        prefixCollect(current,prefixes);
        List<Value> valueList = new ArrayList<>();

        for(Set<Value> set: prefixes)
        {
            for(Value value: set)
            {
                if(!valueList.contains(value))
                {
                    valueList.add(value);
                }

            }

        }

        Collections.sort(valueList, comparator);



        return valueList;
    }


    private Node findNode(String string )
    {
        Node node =this.root;
        for(int i=0; i<string.length(); i++)
        {
            Node child=node.children[string.charAt(i)];
            if(child==null)
            {
                return null;
            }
            node=child;
        }
        return node;
    }

    private void prefixCollect(Node root, List<Set<Value>> list)
    {
        if(root!=null)
        {
            if(root.value!=null && !root.value.isEmpty())
            {
                list.add(root.value);
            }
            for(int i=0; i<alphabetSize; i++)
            {
                if(root.children[i]!=null)
                {
                    prefixCollect(root.children[i],list);
                }
            }
        }
    }






    /**
     * Delete the subtree rooted at the last character of the prefix.
     * Search is CASE INSENSITIVE.
     * @param prefix
     * @return a Set of all Values that were deleted.
     */
    public Set<Value> deleteAllWithPrefix(String prefix) {
        if(prefix==null)
        {
            return Collections.emptySet();
        }


        Node current =deleteNode(prefix.toUpperCase());
        Set<Set<Value>> prefixes = new HashSet<>();
        deletePrefixCollect(current,prefixes);
        Set<Value> valueSet = new HashSet<>();

        for(Set<Value> set: prefixes)
        {
            for(Value value: set)
            {
                if(!valueSet.contains(value))
                {
                    valueSet.add(value);
                }

            }

        }


        return valueSet;
    }

    private void deletePrefixCollect(Node root, Set<Set<Value>> set)
    {
        if(root!=null)
        {
            if(root.value!=null && !root.value.isEmpty())
            {

                set.add(root.value);
                root.value=null;
            }
            for(int i=0; i<alphabetSize; i++)
            {
                if(root.children[i]!=null)
                {
                    deletePrefixCollect(root.children[i],set);
                }
            }
        }
    }
    private Node deleteNode(String string )
    {
        Node node =this.root;
        for(int i=0; i<string.length(); i++)
        {
            Node child=node.children[string.charAt(i)];
            if(child==null)
            {
                return null;
            }
            node=child;
        }
        return node;
    }


    /**
     * Delete all values from the node of the given key (do not remove the values from other nodes in the Trie)
     * @param key
     * @return a Set of all Values that were deleted.
     */
    public Set<Value> deleteAll(String key)
    {
        if(key==null)
        {
            return Collections.emptySet();
        }

        this.root = deleteAll(this.root, key.toUpperCase(), 0);
        // List list=(List<Value>) Arrays.asList(this.root.value);
        // return new HashSet<>(list);
        Set<Value> set= new HashSet<>();
        for(Set<Value> setS:deleteAllSet)
        {
            for(Value value: setS)
            {
                if(!set.contains(value))
                {
                    set.add(value);
                }
            }

        }
        return set;
    }

    private Node deleteAll(Node x, String key, int d)
    {
        if (x == null)
        {
            return null;
        }
        //we're at the node to del - set the val to null
        if (d == key.length())
        {
            deleteAllSet.add( x.value);
            x.value = null;
        }
        //continue down the trie to the target node
        else
        {
            char c = key.charAt(d);
            x.children[c] = this.deleteAll(x.children[c], key.toUpperCase(), d + 1);
        }
        //this node has a val – do nothing, return the node
        if (x.value != null&& !x.value.isEmpty())
        {
            return x;
        }
        //remove subtrie rooted at x if it is completely empty
        for (int c = 0; c <alphabetSize; c++) {
            if (x.value != null) {
                if (x.children[c] != null && !x.value.isEmpty()) {
                    return x; //not empty
                }
            }
        }
        //empty - set this link to null in the parent
        return null;
    }


    /**
     * Remove the given value from the node of the given key (do not remove the value from other nodes in the Trie)
     * @param key
     * @param val
     * @return the value which was deleted. If the key did not contain the given value, return null.
     */
    public Value delete(String key, Value val) {


        if(val==null)
        {
            return null;
        }
        this.delete(val, this.root,key.toUpperCase(),0);

        return val;

    }

    private Node delete(Value value, Node x, String key, int d)
    {

        //link was null - return null, indicating a miss
        if (x == null)
        {

            return null;
        }
        //we've reached the last node in the key,
        //return the node
        Value winner=null;
        if (d == key.length())
        {
            for(Value val:  (Set<Value>) x.value)
            {
                if (val!=value) {
                    //x.value.remove(val);
                    x.value.remove(value);
                    break;

                }
            }

            if(x.value.isEmpty())
            {
                x=null;
            }
            return x;
        }
        //proceed to the next node in the chain of nodes that
        //forms the desired key

        char c = key.charAt(d);
        return this.delete(value, x.children[c], key, d + 1);
    }
}
