package io.github.soniex2.libtransfer;

/**
 * A transaction which supports overcommit.
 * <p>
 * <b>Warning:</b> This interface is not currently stable, and may be removed or changed at any time!
 * It exists for testing/experimental purposes only!
 * </p>
 *
 * @author soniex2
 * @see Transaction
 */
public interface OvercommitTransaction<T extends SizedElement<T>> extends Transaction<T> {
	/**
	 * Initialize the contents of this transaction. The results from {@link #get()} are not stable until this method is called.
	 * <p>
	 * This allocates the space for the transaction, but doesn't perform it. An overcommit transaction is indistinguishable from
	 * a normal transaction after this method has been called.
	 * </p>
	 */
	void init();
}
