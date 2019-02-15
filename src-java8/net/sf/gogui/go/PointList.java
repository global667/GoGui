// PointList.java

package net.sf.gogui.go;

import java.util.ArrayList;
import java.util.Iterator;

/** List containing points. */
public final class PointList
    extends ArrayList<GoPoint>
    implements ConstPointList
{
    public class ConstIterator
        implements Iterator<GoPoint>
    {
        @Override
        public boolean hasNext()
        {
            return (m_index < size());
        }

        @Override
        public GoPoint next()
        {
            return get(m_index++);
        }

        @Override
        public void remove()
        {
            throw new UnsupportedOperationException();
        }

        private int m_index;
    }

    /** Construct empty point list. */
    public PointList()
    {
        this(0);
    }

    /** Construct empty point list with initial capacity.
        @param initialCapacity The number of points to reserve memory for. */
    public PointList(int initialCapacity)
    {
        super(initialCapacity);
    }

    /** Construct point list with a single element.
        @param p The initial point element. */
    public PointList(GoPoint p)
    {
        this(1);
        add(p);
    }

    /** Construct point list as a copy of another point list.
        @param list The list to copy the points from. */
    public PointList(ConstPointList list)
    {
        super((PointList)list);
    }

    /** Add points of another list  at the end of this list.
     * @param list */
    public void addAllFromConst(ConstPointList list)
    {
        addAll((PointList)list);
    }

    /** *  Get an empty constant point list.Can be used at places where an empty temporary point list is needed
        that is never modified to avoid memory allocation.
     * @return  */
    public static ConstPointList getEmptyList()
    {
        return EMPTY_LIST;
    }

    /** *  Returns an iterator over the points elements in this list.An iterator of type PointList.ConstIterator is returned, which
        does not support Iterator.remove(), to allow for-each-loops for
        ConstPointList references.
     * @return  */
    @Override
    public Iterator<GoPoint> iterator()
    {
        return new ConstIterator();
    }

    /** *  Remove and return last element.Requires that list is not empty.
     * @return  */
    public GoPoint pop()
    {
        int index = size() - 1;
        if (index < 0)
        {
            assert false;
            return null;
        }
        GoPoint p = get(index);
        remove(index);
        return p;
    }

    @Override
    public String toString()
    {
        StringBuilder buffer = new StringBuilder();
        for (int i = 0; i < size(); ++i)
        {
            if (i > 0)
                buffer.append(' ');
            buffer.append(get(i));
        }
        return buffer.toString();
    }

    /** *  Convert point list to string.Null arguments will be converted to an empty string.
     * @param list
     * @return  */
    public static String toString(ConstPointList list)
    {
        if (list == null)
            return "";
        else
            return list.toString();
    }

    private static final ConstPointList EMPTY_LIST = new PointList();
}
