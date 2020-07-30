/*
 * Part of the Realistic Ore Veins Mod by AlcatrazEscapee
 * Work under Copyright. See the project LICENSE.md for details.
 */

package com.alcatrazescapee.oreveins.util.collections;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Random;
import javax.annotation.Nonnull;

public interface IWeightedList<E> extends Iterable<E>
{
    static <E> IWeightedList<E> empty()
    {
        return new IWeightedList<E>()
        {
            @Override
            public void add(double weight, E element) {}

            @Override
            public E get(Random random)
            {
                return null;
            }

            @Override
            public Collection<E> values()
            {
                return Collections.emptyList();
            }

            @Override
            public boolean isEmpty()
            {
                return true;
            }

            @Nonnull
            @Override
            public Iterator<E> iterator()
            {
                return Collections.emptyIterator();
            }
        };
    }

    static <E> IWeightedList<E> singleton(E element)
    {
        return new IWeightedList<E>()
        {
            private final Collection<E> elementSet = Collections.singleton(element);

            @Override
            public void add(double weight, E element) {}

            @Override
            public E get(Random random)
            {
                return element;
            }

            @Override
            public Collection<E> values()
            {
                return elementSet;
            }

            @Override
            public boolean isEmpty()
            {
                return false;
            }

            @Nonnull
            @Override
            public Iterator<E> iterator()
            {
                return elementSet.iterator();
            }

            @Override
            public String toString()
            {
                return "[" + element + "]";
            }
        };
    }

    void add(double weight, E element);

    E get(Random random);

    Collection<E> values();

    boolean isEmpty();
}
