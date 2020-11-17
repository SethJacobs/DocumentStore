package edu.yu.cs.com1320.project.impl;

import edu.yu.cs.com1320.project.Stack;

public class StackImpl<T> implements Stack<T> {

    Node root;

    /**
     * @param element object to add to the Stack
     */
    @Override
    public void push(T element) {
        Node node= new Node(element);
        if(root==null)
        {
            root=node;
        }

        else
        {
            Node temp=root;
            root=node;
            node.next=temp;
        }
    }



    /**
     * removes and returns element at the top of the stack
     *
     * @return element at the top of the stack, null if the stack is empty
     */
    @Override
    public T pop() {
        if(root==null)
        {
            return null;
        }
        else
        {
            T popped=(T)root.element;
            root=root.next;
            return popped;
        }
    }

    /**
     * @return the element at the top of the stack without removing it
     */
    @Override
    public T peek() {
        if(root==null)
        {
            return null;
        }
        else{
            return (T)root.element;
        }
    }

    /**
     * @return how many elements are currently in the stack
     */
    @Override
    public int size() {
        int size=0;
        while(root!=null)
        {
            size++;
            root=root.next;
        }
        return size;
    }
}


class Node<T> {
    T element;
    Node next;

    Node(T element)
    {
        this.element=element;
    }


}