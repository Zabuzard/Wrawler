package de.zabuza.webcrawler.util;

import java.util.Comparator;

/**
 * Comparator that compares strings case insensitive.
 * 
 * @author Zabuza {@literal <zabuza.dev@gmail.com>}
 * 
 */
public final class InsensitiveComparator implements Comparator<String> {

	@Override
	public int compare(final String o1, final String o2) {
		return o1.toLowerCase().compareTo(o2.toLowerCase());
	}

}
