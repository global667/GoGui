// ConstPointList.java

package net.sf.gogui.go;

import java.util.Iterator;

/** Const functions of go.PointList.
    @see PointList */
public interface ConstPointList
    extends Iterable<GoPoint>
{
    boolean contains(Object elem);

    @Override
    boolean equals(Object object);

    GoPoint get(int index);

    @Override
    int hashCode();

    boolean isEmpty();

    @Override
    Iterator<GoPoint> iterator();

    int size();

    /**
     *
     * @return
     */
    @Override
    String toString();
}
