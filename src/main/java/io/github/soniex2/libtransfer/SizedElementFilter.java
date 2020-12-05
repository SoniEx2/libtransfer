package io.github.soniex2.libtransfer;

/**
 * @author soniex2
 */
public interface SizedElementFilter<T extends SizedElement<T>> {
	T get();
}
