package de.zabuza.webcrawler.util;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Utility class for maps.
 * 
 * @author Zabuza {@literal <zabuza.dev@gmail.com>}
 */
public final class MapUtil {

	/**
	 * Sorts a map by values and returns it.
	 * 
	 * @param <K>
	 *            The actual key class of the map
	 * @param <V>
	 *            The actual value class of the map
	 * @param map
	 *            Map to sort by values
	 * @return Sorted map
	 */
	public static <K, V extends Comparable<? super V>> Map<K, V> sortByValue(final Map<K, V> map) {
		final List<Map.Entry<K, V>> list = new LinkedList<>(map.entrySet());
		Collections.sort(list, new Comparator<Map.Entry<K, V>>() {
			@Override
			public int compare(final Map.Entry<K, V> o1, final Map.Entry<K, V> o2) {
				return (o1.getValue()).compareTo(o2.getValue());
			}
		});

		final Map<K, V> result = new LinkedHashMap<>();
		for (final Map.Entry<K, V> entry : list) {
			result.put(entry.getKey(), entry.getValue());
		}
		return result;
	}
}