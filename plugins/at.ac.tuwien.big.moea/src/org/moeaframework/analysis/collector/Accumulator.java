package org.moeaframework.analysis.collector;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Accumulator implements Serializable {
   private static final long serialVersionUID = 1L;

   private final Map<String, List<Serializable>> map = new HashMap<>();

   public Accumulator() {}

   public void add(final String key, final Serializable value) {
      List<Serializable> list = map.get(key);
      if(list == null) {
         list = new ArrayList<>();
         map.put(key, list);
      }
      list.add(value);
   }

   public Serializable get(final String key, final int index) {
      final List<Serializable> list = map.get(key);
      if(list == null || index < 0 || index >= list.size()) {
         throw new IndexOutOfBoundsException("Index " + index + " out of bounds");
      }
      return list.get(index);
   }

   public Set<String> keySet() {
      return map.keySet();
   }

   public int size(final String key) {
      final List<Serializable> list = map.get(key);
      return list == null ? 0 : list.size();
   }
}
