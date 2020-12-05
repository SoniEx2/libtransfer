package io.github.soniex2.libtransfer;

import java.util.Objects;

/**
 * @author soniex2
 */
public interface SizedElement<T extends SizedElement<T>> {
	/**
	 * Retrieve how many elements this object contains.
	 *
	 * @return How many elements this object contains.
	 */
	int getCount();

	/**
	 * Create a new SizedElement, compatible with this one, with the given count.
	 *
	 * @param count The count.
	 * @return The new SizedElement, compatible with this one, with the given count.
	 * @throws IllegalStateException If this SizedElement {@link #isEmpty()} and {@code count > 0}.
	 * @throws IllegalArgumentException If {@code count > this.getMaxCount() || count < 0}.
	 */
	T withCount(int count);

	/**
	 * Retrieve whether this SizedElement has the same type as the given SizedElement.
	 *
	 * @param other The SizedElement whose type to compare to.
	 * @return Whether this SizedElement has the same type as the given SizedElement.
	 * @throws NullPointerException If {@code other} is {@code null}.
	 */
	boolean hasSameType(T other);

	/**
	 * Retrieve whether this SizedElement's type can combine with the given SizedElement's type.
	 *
	 * @param other The SizedElement whose type to compare to.
	 * @return Whether this SizedElement's type can combine with the given SizedElement's type.
	 * This is defined as {@code this.isEmpty() || other.isEmpty() || this.hasSameType(other)}
	 * @throws NullPointerException If {@code other} is {@code null}.
	 */
	default boolean hasCombinableType(T other) {
		Objects.requireNonNull(other);
		return this.isEmpty() || other.isEmpty() || this.hasSameType(other);
	}

	/**
	 * Retrieve whether this SizedElement can combine with the given SizedElement.
	 * <p>
	 * Combining elements is defined for elements of the same type. The combined element has
	 * the same type as its parts, but the sum of their sizes.
	 * </p>
	 *
	 * @param other The SizedElement with which to combine.
	 * @return Whether this SizedElement can combine with the given SizedElement.
	 * This is defined as {@code this.hasCombinableType(other) && this.getCount() <= this.getMaxCount() - other.getCount()}.
	 * @throws NullPointerException If {@code other} is {@code null}.
	 */
	default boolean canCombine(T other) {
		return this.hasCombinableType(other) && this.getCount() <= this.getMaxCount() - other.getCount();
	}

	/**
	 * Attempt to combine this SizedElement with the given SizedElement.
	 * <p>
	 * Combining elements is defined for elements of the same type. The combined element has
	 * the same type as its parts, but the sum of their sizes.
	 * </p>
	 *
	 * @param other The SizedElement with which to combine.
	 * @return A new SizedElement which combines this SizedElement with the given SizedElement.
	 * @throws IllegalArgumentException If {@code !this.canCombine(other)}.
	 * @throws NullPointerException If {@code other} is {@code null}.
	 */
	default T combine(T other) {
		if (!this.canCombine(other)) {
			throw new IllegalArgumentException("Can't combine");
		}
		return this.isEmpty() ? other : this.withCount(Math.addExact(this.getCount(), other.getCount()));
	}

	/**
	 * Retrieve whether this SizedElement can be split by the given SizedElement.
	 * <p>
	 * Splitting elements is defined for elements of the same type. The split element has
	 * the same type as its parts, but the difference of their sizes.
	 * </p>
	 *
	 * @param other The SizedElement by which to split.
	 * @return Whether this SizedElement can be split by the given SizedElement.
	 * This is defined as {@code this.hasCombinableType(other) && this.getCount() - other.getCount() >= 0}.
	 * @throws NullPointerException If {@code other} is {@code null}.
	 */
	default boolean canSplit(T other) {
		return this.hasCombinableType(other) && this.getCount() - other.getCount() >= 0;
	}

	default T split(T other) {
		if (!this.canSplit(other)) {
			throw new IllegalArgumentException("Can't split");
		}
		return this.withCount(Math.subtractExact(this.getCount(), other.getCount()));
	}

	/**
	 * Retrieve how many elements this object can have.
	 * <p>
	 * Empty elements should return the maximum maximum size, e.g. {@link Integer#MAX_VALUE}.
	 * </p>
	 * <p>
	 * SizedElements with the same type, as defined by {@link #hasSameType(SizedElement)}, must return the same max count.
	 * </p>
	 *
	 * @return How many elements this object can have.
	 */
	int getMaxCount();

	/**
	 * Retrieve whether this SizedElement is empty.
	 *
	 * @return Whether this SizedElement is empty.
	 */
	boolean isEmpty();
}
