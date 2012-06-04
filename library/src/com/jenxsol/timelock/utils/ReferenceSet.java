package com.jenxsol.timelock.utils;

import java.lang.ref.Reference;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * This class aids in implementing sets of references.
 */
public abstract class ReferenceSet<T extends Object> extends AbstractSet<T>
{

    private final HashSet<Reference<T>> contents;

    /**
     * Get an instance of ReferenceSet.
     */
    public ReferenceSet()
    {
        super();

        contents = new HashSet<Reference<T>>();
    }

    /**
     * Create a ReferenceSet with the given capacity.
     * 
     * @param n
     *            the initial capacity
     */
    public ReferenceSet(int n)
    {
        super();
        contents = new HashSet<Reference<T>>(n);
    }

    /**
     * Get a ReferenceSet with the contents from the given Collection.
     * 
     * @param c
     *            the collection
     */
    public ReferenceSet(Collection<T> c)
    {
        super();
        if (c == null)
        {
            throw new NullPointerException("Null collection provided to ReferenceSet");
        }

        contents = new HashSet<Reference<T>>(c.size() * 2);

        // Copy references into the content map
        for (T o : c)
        {
            add(o);
        }
    }

    /**
     * Add an object to the Set.
     * 
     * @param o
     *            the object
     * @return true if the object did not already exist
     */
    @Override
    public boolean add(T o)
    {
        boolean rv = false;

        if (!contains(o))
        {
            contents.add(getReference(o));
            rv = true;
        }

        return (rv);
    }

    /**
     * Get the current size of the Set. This is not an entirely cheap operation,
     * as it walks the entire iterator to make sure all entries are still valid
     * references.
     */
    @Override
    public int size()
    {
        int rv = 0;
        for (Iterator<T> i = iterator(); i.hasNext();)
        {
            i.next();
            rv++;
        }
        return (rv);
    }

    /**
     * Get an iterator.
     * 
     * This iterator does not support removing entries due to limitations with
     * HashMap and Iterator that would otherwise require me to duplicate all of
     * HashMap.
     */
    @Override
    public Iterator<T> iterator()
    {
        return (new ReferenceIterator<T>(contents.iterator()));
    }

    /**
     * Obtain the desired type of reference to the given object.
     * 
     * <p>
     * Unfortunately, java doesn't give me a way to enforce this in the language
     * (i.e. at compile time), but subclasses of ReferenceSet must implement
     * hashCode() and equals() in such a way that they return what the
     * referenced object would return if the object were not a reference. If the
     * reference has disappeared, equals() should return false, and hashCode
     * should return 0.
     * </p>
     * 
     * @param o
     *            an object
     * @return a reference to that object
     */
    protected abstract Reference<T> getReference(T o);

    static class ReferenceIterator<T> extends Object implements Iterator<T>
    {

        private Iterator<Reference<T>> backIterator = null;
        private boolean hasNext = false;
        private T current = null;
        private Reference<T> currentRef = null;

        public ReferenceIterator(Iterator<Reference<T>> i)
        {
            super();

            this.backIterator = i;
            findNext();
        }

        public boolean hasNext()
        {
            return (hasNext);
        }

        public T next() throws NoSuchElementException
        {
            if (!hasNext)
            {
                throw new NoSuchElementException("All out.");
            }
            T rv = current;
            findNext();
            return (rv);
        }

        public void remove()
        {
            throw new UnsupportedOperationException("This is currently not supported");
        }

        private void findNext()
        {
            current = null;
            while (current == null && backIterator.hasNext())
            {
                currentRef = backIterator.next();
                current = currentRef.get();
                // If the reference is dead, get rid of our copy
                if (current == null)
                {
                    backIterator.remove();
                }
            }
            hasNext = current != null;
        } // findNext

    }

}