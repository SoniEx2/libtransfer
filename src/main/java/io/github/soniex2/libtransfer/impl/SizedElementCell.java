package io.github.soniex2.libtransfer.impl;

import io.github.soniex2.libtransfer.SizedElement;
import io.github.soniex2.libtransfer.Transaction;
import io.github.soniex2.libtransfer.TransactionStatus;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;

/**
 * Helper class.
 *
 * @author soniex2
 */
public class SizedElementCell<T extends SizedElement<T>> extends AtomicReference<T> {
	private final AtomicReference<T> postInsert;
	private final AtomicReference<T> postExtract;
	private final AtomicBoolean valid;
	private final Lock readLock;

	public SizedElementCell(T initialValue, AtomicBoolean valid, Lock readLock) {
		// We assume the write lock is being held by the thread calling us.
		super(Objects.requireNonNull(initialValue));
		postInsert = new AtomicReference<>(initialValue);
		postExtract = new AtomicReference<>(initialValue);
		this.valid = valid;
		this.readLock = readLock;
	}

	/**
	 * Extract the given element(s) from this cell.
	 *
	 * <p>
	 * Users who would like to only simulate an extraction
	 * can do so by immediately calling {@link Transaction#revert()} on the returned transaction.
	 * <br />
	 * However, you might want to keep hold of the transaction if you originally planned on
	 * re-creating it just to confirm it: Your assumptions may change in a concurrent system.
	 * </p>
	 *
	 * @param element The element(s) to extract from this cell.
	 * @param strong Whether this is a strong transaction.
	 * @return A transaction with the element(s) actually extracted.
	 * @throws NullPointerException If {@code element} is {@code null}.
	 */
	public Transaction<T> extract(T element, boolean strong) {
		Objects.requireNonNull(element);
		return new ExtractTransaction(element, strong);
	}

	/**
	 * Insert the given element(s) into this cell.
	 *
	 * <p>
	 * Users who would like to only simulate an insertion
	 * can do so by immediately calling {@link Transaction#revert()} on the returned transaction.
	 * <br />
	 * However, you might want to keep hold of the transaction if you originally planned on
	 * re-creating it just to confirm it: Your assumptions may change in a concurrent system.
	 * </p>
	 *
	 * @param element The element(s) to insert into this cell.
	 * @param strong Whether this is a strong transaction.
	 * @return A transaction with the element(s) actually inserted.
	 * @throws NullPointerException If {@code element} is {@code null}.
	 */
	public Transaction<T> insert(T element, boolean strong) {
		Objects.requireNonNull(element);
		return new InsertTransaction(element, strong);
	}

	private class InsertTransaction implements Transaction<T> {
		private final T transactionSize;
		private TransactionStatus status = TransactionStatus.OPEN;
		private boolean strong;

		InsertTransaction(T element, boolean strong) {
			if (strong) {
				readLock.lock();
			}
			this.strong = strong;
			readLock.lock();
			try {
				if (!valid.get() || element.isEmpty()) {
					transactionSize = element.isEmpty() ? element : element.withCount(0);
				} else {
					T old, tmp, diff;
					do {
						old = postInsert.get();
						if (!old.hasCombinableType(element)) {
							diff = element.withCount(0);
							break;
						}
						tmp = old.combine(diff = element.withCount(Math.min(element.getCount(), old.getMaxCount() - old.getCount())));
					} while (!postInsert.compareAndSet(old, tmp));
					transactionSize = diff;
				}
			} catch (RuntimeException | Error e) {
				if (strong) {
					readLock.unlock();
				}
				throw e;
			} finally {
				readLock.unlock();
			}
		}

		@Override
		public T get() {
			return transactionSize;
		}

		@Override
		public boolean commit() {
			readLock.lock();
			if (!getStatus().canConfirm()) {
				readLock.unlock();
				throw new IllegalStateException();
			}
			try {
				if (transactionSize.isEmpty()) {
					status = TransactionStatus.CONFIRMED;
					return true;
				}
				T old, tmp;
				// first, do the actual amount, then do postExtract.
				// otherwise, you could extract items before they're even inserted, and overflow the actual amount.
				do {
					old = SizedElementCell.this.get();
					assert old.hasCombinableType(transactionSize);
					tmp = old.combine(transactionSize);
				} while (!SizedElementCell.this.compareAndSet(old, tmp));

				do {
					old = postExtract.get();
					assert old.hasCombinableType(transactionSize);
					tmp = old.combine(transactionSize);
				} while (!postExtract.compareAndSet(old, tmp));

				status = TransactionStatus.CONFIRMED;

				return true;
			} finally {
				readLock.unlock();
				if (strong) {
					readLock.unlock();
				}
			}
		}

		@Override
		public boolean revert() {
			readLock.lock();
			if (!getStatus().canRevert()) {
				readLock.unlock();
				throw new IllegalStateException();
			}
			try {
				if (transactionSize.isEmpty()) {
					status = TransactionStatus.REVERTED;
					return true;
				}
				T old, tmp;

				do {
					old = postInsert.get();
					assert old.hasCombinableType(transactionSize);
					tmp = old.split(transactionSize);
				} while (!postInsert.compareAndSet(old, tmp));

				status = TransactionStatus.REVERTED;

				return true;
			} finally {
				readLock.unlock();
				if (strong) { // lock is reentrant
					readLock.unlock();
				}
			}
		}

		@Override
		public TransactionStatus getStatus() {
			if (status == TransactionStatus.OPEN && !valid.get()) {
				status = TransactionStatus.INVALIDATED;
				if (strong) {
					strong = false;
					readLock.unlock();
				}
			}
			return status;
		}

		@Override
		public boolean isStrong() {
			return strong;
		}

		@Override
		public void makeStrong() {
			if (!strong && getStatus() == TransactionStatus.OPEN) {
				readLock.lock();
				if (getStatus() != TransactionStatus.OPEN) {
					readLock.unlock();
				} else {
					strong = true;
				}
			}
		}

		@Override
		public void makeWeak() {
			if (strong && getStatus() == TransactionStatus.OPEN) {
				readLock.unlock();
				strong = false;
			}
		}

		@Override
		public boolean isConcurrent() {
			return true;
		}
	}

	private class ExtractTransaction implements Transaction<T> {
		private final T transactionSize;
		private TransactionStatus status = TransactionStatus.OPEN;
		private boolean strong;

		ExtractTransaction(T element, boolean strong) {
			readLock.lock();
			if (strong) {
				readLock.lock();
			}
			this.strong = strong;
			try {
				if (!valid.get() || element.isEmpty()) {
					transactionSize = element.withCount(0);
				} else {
					T old, tmp, diff;
					do {
						old = postExtract.get();
						if (!old.hasCombinableType(element)) {
							diff = element.withCount(0);
							break;
						}
						tmp = old.split(diff = element.withCount(Math.min(element.getCount(), old.getCount())));
					} while (!postExtract.compareAndSet(old, tmp));
					transactionSize = diff;
				}
			} catch (RuntimeException | Error e) {
				if (strong) {
					readLock.unlock();
				}
				throw e;
			} finally {
				readLock.unlock();
			}
		}

		@Override
		public T get() {
			return transactionSize;
		}

		@Override
		public boolean commit() {
			readLock.lock();
			if (!getStatus().canConfirm()) {
				readLock.unlock();
				throw new IllegalStateException();
			}
			try {
				if (transactionSize.isEmpty()) {
					status = TransactionStatus.CONFIRMED;
					return true;
				}
				T old, tmp;
				// first, do the actual amount, then do postInsert.
				// otherwise, you could insert items before they're even extracted, and overflow the actual amount.
				do {
					old = SizedElementCell.this.get();
					assert old.hasCombinableType(transactionSize);
					tmp = old.split(transactionSize);
				} while (!SizedElementCell.this.compareAndSet(old, tmp));

				do {
					old = postInsert.get();
					assert old.hasCombinableType(transactionSize);
					tmp = old.split(transactionSize);
				} while (!postInsert.compareAndSet(old, tmp));

				status = TransactionStatus.CONFIRMED;

				return true;
			} finally {
				readLock.unlock();
				if (strong) {
					readLock.unlock();
				}
			}
		}

		@Override
		public boolean revert() {
			readLock.lock();
			if (!getStatus().canRevert()) {
				readLock.unlock();
				throw new IllegalStateException();
			}
			try {
				if (transactionSize.isEmpty()) {
					status = TransactionStatus.REVERTED;
					return true;
				}
				T old, tmp;

				do {
					old = postExtract.get();
					assert old.hasCombinableType(transactionSize);
					tmp = old.combine(transactionSize);
				} while (!postExtract.compareAndSet(old, tmp));

				status = TransactionStatus.REVERTED;

				return true;
			} finally {
				readLock.unlock();
				if (strong) { // lock is reentrant
					readLock.unlock();
				}
			}
		}

		@Override
		public TransactionStatus getStatus() {
			if (status == TransactionStatus.OPEN && !valid.get()) {
				status = TransactionStatus.INVALIDATED;
				if (strong) {
					strong = false;
					readLock.unlock();
				}
			}
			return status;
		}

		@Override
		public boolean isStrong() {
			return strong;
		}

		@Override
		public void makeStrong() {
			if (!strong && getStatus() == TransactionStatus.OPEN) {
				readLock.lock();
				if (getStatus() != TransactionStatus.OPEN) {
					readLock.unlock();
				} else {
					strong = true;
				}
			}
		}

		@Override
		public void makeWeak() {
			if (strong && getStatus() == TransactionStatus.OPEN) {
				readLock.unlock();
				strong = false;
			}
		}

		@Override
		public boolean isConcurrent() {
			return true;
		}
	}
}
