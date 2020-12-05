package io.github.soniex2.libtransfer;

/**
 * @author soniex2
 */
public interface SizedElementHolder<T extends SizedElement<T>> {
	/**
	 * Retrieve the element(s) in the given slot.
	 *
	 * @param slot The slot.
	 * @return The element(s) in the given slot.
	 * @throws IndexOutOfBoundsException If {@code slot} is out of range.
	 */
	T get(int slot);

	/**
	 * Extract the given element(s) from the given slot.
	 *
	 * <p>
	 * Users who would like to only simulate an extraction
	 * can do so by immediately calling {@link Transaction#revert()} on the returned transaction.
	 * <br />
	 * However, you might want to keep hold of the transaction if you originally planned on
	 * re-creating it just to confirm it: Your assumptions may change in a concurrent system.
	 * </p>
	 *
	 * @param slot The slot.
	 * @param element The element(s) to extract from the given slot.
	 * @param strong Whether this is a strong transaction.
	 * @return A transaction with the element(s) actually extracted.
	 * @throws IndexOutOfBoundsException If {@code slot} is out of range.
	 * @throws NullPointerException If {@code element} is {@code null}.
	 * @throws IllegalArgumentException If {@code strong} is {@code true} but {@link #isConcurrent()} is {@code false}.
	 */
	Transaction<T> extract(int slot, T element, boolean strong);

	/**
	 * Insert the given element(s) into the given slot.
	 *
	 * <p>
	 * Users who would like to only simulate an insertion
	 * can do so by immediately calling {@link Transaction#revert()} on the returned transaction.
	 * <br />
	 * However, you might want to keep hold of the transaction if you originally planned on
	 * re-creating it just to confirm it: Your assumptions may change in a concurrent system.
	 * </p>
	 *
	 * @param slot The slot.
	 * @param element The element(s) to insert into the given slot.
	 * @param strong Whether this is a strong transaction.
	 * @return A transaction with the element(s) actually inserted.
	 * @throws IndexOutOfBoundsException If {@code slot} is out of range.
	 * @throws NullPointerException If {@code element} is {@code null}.
	 * @throws IllegalArgumentException If {@code strong} is {@code true} but {@link #isConcurrent()} is {@code false}.
	 */
	Transaction<T> insert(int slot, T element, boolean strong);

	/**
	 * Retrieve the maximum number of elements that can exist in the given slot, regardless of
	 * the contained object's maximum count.
	 *
	 * <p>
	 * Users should check the contained object's maximum count separately from the slot limit.
	 * </p>
	 *
	 * @param slot The slot.
	 * @return The maximum number of elements that can exist in the given slot.
	 * @throws IndexOutOfBoundsException If {@code slot} is out of range.
	 */
	int getSlotLimit(int slot);

	/**
	 * Retrieve how many slots this holder has.
	 *
	 * @return The number of slots this holder has.
	 */
	int getSlots();

	/**
	 * Retrieve whether this SizedElementHolder is concurrent.
	 *
	 * @return {@code true} if this SizedElementHolder is concurrent. {@code false} if it's single-threaded.
	 */
	boolean isConcurrent();
}
