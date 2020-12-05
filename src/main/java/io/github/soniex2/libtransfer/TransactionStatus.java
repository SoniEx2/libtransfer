package io.github.soniex2.libtransfer;

/**
 * The current status of a transaction.
 *
 * @author soniex2
 */
public enum TransactionStatus {
	/**
	 * Signifies a transaction that has neither been confirmed nor reverted.
	 */
	OPEN(true, true),
	/**
	 * Signifies a transaction that has been confirmed.
	 */
	CONFIRMED(false, false),
	/**
	 * Signifies a transaction that has been reverted.
	 */
	REVERTED(false, false),
	/**
	 * Signifies a transaction that has been invalidated by an external process.
	 */
	INVALIDATED(false, false);

	private final boolean canConfirm;
	private final boolean canRevert;

	TransactionStatus(boolean canConfirm, boolean canRevert) {
		this.canConfirm = canConfirm;
		this.canRevert = canRevert;
	}

	/**
	 * Returns whether a transaction with this status can be confirmed.
	 *
	 * @return {@code true} if a transaction with this status can be confirmed, {@code false} otherwise.
	 */
	public boolean canConfirm() {
		return this.canConfirm;
	}

	/**
	 * Returns whether a transaction with this status can be reverted.
	 *
	 * @return {@code true} a transaction with this status can be reverted, {@code false} otherwise.
	 */
	public boolean canRevert() {
		return this.canRevert;
	}
}
