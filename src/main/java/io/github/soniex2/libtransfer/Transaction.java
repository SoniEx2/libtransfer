package io.github.soniex2.libtransfer;

/**
 * A transaction.
 * <p>
 * On thread-safety: While transactions provide a thread-safe mechanism for moving elements, they are not themselves thread-safe.
 * </p>
 * <p>
 * On overcommit: Some transactions may implement overcommit. Overcommit should not available for strong transactions. See {@link OvercommitTransaction}.
 * </p>
 *
 * @author soniex2
 */
public interface Transaction<T extends SizedElement<T>> {

	/**
	 * Retrieve the element(s) moved in this transaction.
	 *
	 * @return The element(s) moved in this transaction.
	 */
	T get();

	/**
	 * Attempt to confirm this transaction.
	 * <p>
	 * Implementations should do their best to make sure this method cannot fail for strong transactions
	 * under normal circumstances.
	 * </p>
	 *
	 * @return {@code true} if this transaction could be confirmed. {@code false} otherwise.
	 * @throws IllegalStateException If {@code !this.getStatus().canConfirm()}.
	 */
	boolean commit();

	/**
	 * Attempt to revert this transaction.
	 * <p>
	 * Implementations should do their best to make sure this method cannot fail for strong transactions
	 * under normal circumstances.
	 * </p>
	 *
	 * @return {@code true} if this transaction could be reverted. {@code false} otherwise.
	 * @throws IllegalStateException If {@code !this.getStatus().canRevert()}.
	 */
	boolean revert();

	/**
	 * Retrieve this transaction's current status, as defined in {@link TransactionStatus}.
	 *
	 * @return This transaction's status.
	 */
	TransactionStatus getStatus();

	/**
	 * Retrieve whether this is a strong or weak transaction.
	 *
	 * @return Whether this is a strong or weak transaction.
	 */
	boolean isStrong();

	/**
	 * Make this a strong transaction.
	 *
	 * @throws IllegalStateException If this transaction's {@link #isConcurrent()} is {@code false}.
	 */
	void makeStrong();

	/**
	 * Make this a weak transaction.
	 */
	void makeWeak();

	/**
	 * Retrieve whether this Transaction (more specifically, the holder backing this transaction) is concurrent.
	 *
	 * @return {@code true} if this transaction is concurrent, {@code false} if it's single-threaded.
	 */
	boolean isConcurrent();
}
