/**
 * Copyright 2010 The Apache Software Foundation
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.hadoop.hbase.regionserver;

import org.apache.hadoop.hbase.KeyValue;

import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;
import java.util.NavigableSet;
import java.util.SortedSet;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;

/**
 * A {@link java.util.Set} of {@link KeyValue}s implemented on top of a
 * {@link java.util.concurrent.ConcurrentSkipListMap}.  Works like a
 * {@link java.util.concurrent.ConcurrentSkipListSet} in all but one regard:
 * An add will overwrite if already an entry for the added key.  In other words,
 * where CSLS does "Adds the specified element to this set if it is not already
 * present.", this implementation "Adds the specified element to this set EVEN
 * if it is already present overwriting what was there previous".  The call to
 * add returns true if no value in the backing map or false if there was an
 * entry with same key (though value may be different).
 * <p>Otherwise,
 * has same attributes as ConcurrentSkipListSet: e.g. tolerant of concurrent
 * get and set and won't throw ConcurrentModificationException when iterating.
 */
class KeyValueSkipListSet implements NavigableSet<KeyValue> {
  private final ConcurrentNavigableMap<KeyValue, KeyValue> delegatee;
  private volatile MemstoreBloomFilterContainer bloomFilterContainer = null;

  KeyValueSkipListSet(final KeyValue.KVComparator c,
      final MemstoreBloomFilterContainer bloomFilterContainer) {
    this(c);
    this.bloomFilterContainer = bloomFilterContainer;
  }

  KeyValueSkipListSet(final KeyValue.KVComparator c) {
    this.delegatee = new ConcurrentSkipListMap<KeyValue, KeyValue>(c);
  }

  KeyValueSkipListSet(final ConcurrentNavigableMap<KeyValue, KeyValue> m) {
    this.delegatee = m;
  }

  /**
   * Iterator that maps Iterator calls to return the value component of the
   * passed-in Map.Entry Iterator.
   */
  static class MapEntryIterator implements Iterator<KeyValue> {
    private final Iterator<Map.Entry<KeyValue, KeyValue>> iterator;

    MapEntryIterator(final Iterator<Map.Entry<KeyValue, KeyValue>> i) {
      this.iterator = i;
    }

    public boolean hasNext() {
      return this.iterator.hasNext();
    }

    public KeyValue next() {
      return this.iterator.next().getValue();
    }

    public void remove() {
      this.iterator.remove();
    }
  }

  public KeyValue ceiling(KeyValue e) {
    throw new UnsupportedOperationException("Not implemented");
  }

  public Iterator<KeyValue> descendingIterator() {
    return new MapEntryIterator(this.delegatee.descendingMap().entrySet().
      iterator());
  }

  public NavigableSet<KeyValue> descendingSet() {
    throw new UnsupportedOperationException("Not implemented");
  }

  public KeyValue floor(KeyValue e) {
    throw new UnsupportedOperationException("Not implemented");
  }

  public SortedSet<KeyValue> headSet(final KeyValue toElement) {
    return headSet(toElement, false);
  }

  public NavigableSet<KeyValue> headSet(final KeyValue toElement,
      boolean inclusive) {
    return new KeyValueSkipListSet(this.delegatee.headMap(toElement, inclusive));
  }

  public KeyValue higher(KeyValue e) {
    throw new UnsupportedOperationException("Not implemented");
  }

  public Iterator<KeyValue> iterator() {
    return new MapEntryIterator(this.delegatee.entrySet().iterator());
  }

  public KeyValue lower(KeyValue e) {
    throw new UnsupportedOperationException("Not implemented");
  }

  public KeyValue pollFirst() {
    throw new UnsupportedOperationException("Not implemented");
  }

  public KeyValue pollLast() {
    throw new UnsupportedOperationException("Not implemented");
  }

  public SortedSet<KeyValue> subSet(KeyValue fromElement, KeyValue toElement) {
    throw new UnsupportedOperationException("Not implemented");
  }

  public NavigableSet<KeyValue> subSet(KeyValue fromElement,
      boolean fromInclusive, KeyValue toElement, boolean toInclusive) {
    throw new UnsupportedOperationException("Not implemented");
  }

  public SortedSet<KeyValue> tailSet(KeyValue fromElement) {
    return tailSet(fromElement, true);
  }

  public NavigableSet<KeyValue> tailSet(KeyValue fromElement, boolean inclusive) {
    return new KeyValueSkipListSet(this.delegatee.tailMap(fromElement, inclusive));
  }

  public Comparator<? super KeyValue> comparator() {
    throw new UnsupportedOperationException("Not implemented");
  }

  public KeyValue first() {
    return this.delegatee.get(this.delegatee.firstKey());
  }

  public KeyValue last() {
    return this.delegatee.get(this.delegatee.lastKey());
  }

  public boolean add(KeyValue e) {
    this.bloomFilterContainer.add(e);
    return this.delegatee.put(e, e) == null;
  }

  public boolean addAll(Collection<? extends KeyValue> c) {
    throw new UnsupportedOperationException("Not implemented");
  }

  public void clear() {
    this.delegatee.clear();
  }

  public boolean contains(Object o) {
    //noinspection SuspiciousMethodCalls
    return this.delegatee.containsKey(o);
  }

  public boolean containsAll(Collection<?> c) {
    throw new UnsupportedOperationException("Not implemented");
  }

  public boolean isEmpty() {
    return this.delegatee.isEmpty();
  }

  public boolean remove(Object o) {
    return this.delegatee.remove(o) != null;
  }

  public boolean removeAll(Collection<?> c) {
    throw new UnsupportedOperationException("Not implemented");
  }

  public boolean retainAll(Collection<?> c) {
    throw new UnsupportedOperationException("Not implemented");
  }

  public int size() {
    return this.delegatee.size();
  }

  public Object[] toArray() {
    throw new UnsupportedOperationException("Not implemented");
  }

  public <T> T[] toArray(T[] a) {
    throw new UnsupportedOperationException("Not implemented");
  }

  public boolean containsRowPrefixForKeyValue(KeyValue kv) {
    return this.bloomFilterContainer.containsRowPrefixForKeyValue(kv);
  }
}
