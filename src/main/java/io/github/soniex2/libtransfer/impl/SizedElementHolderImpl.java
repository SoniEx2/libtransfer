package io.github.soniex2.libtransfer.impl;

import io.github.soniex2.libtransfer.SizedElement;
import io.github.soniex2.libtransfer.SizedElementHolder;
import io.github.soniex2.libtransfer.Transaction;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Supplier;

/**
 * @author soniex2
 */
public class SizedElementHolderImpl<T extends SizedElement<T>> implements SizedElementHolder<T> {

	private final ReadWriteLock rwlock = new ReentrantReadWriteLock(true);
	private final Supplier<T> emptyFactory;
	// For use with getSlotLimit only!
	private final T empty;

	private volatile AtomicReferenceArray<SizedElementCell<T>> array;
	private volatile AtomicBoolean valid;

	/**
	 * Construct a new SizedElementHolderImpl with the given size and empty element factory.
	 * <p>
	 * The empty element factory must not return null!
	 * </p>
	 *
	 * @param size The size.
	 * @param emptyFactory The empty element factory.
	 */
	public SizedElementHolderImpl(int size, Supplier<T> emptyFactory) {
		this.emptyFactory = Objects.requireNonNull(emptyFactory, "Factory must not be null");
		empty = Objects.requireNonNull(emptyFactory.get(), "Empty instance must not be null");
		clearAndResize(size);
	}

	@Override
	public T get(int slot) {
		// if this throws, it's a problem in our code.
		return Objects.requireNonNull(array.get(slot).get());
	}

	@Override
	public Transaction<T> extract(int slot, T element, boolean strong) {
		Objects.requireNonNull(element);
		rwlock.readLock().lock();
		try {
			return array.get(slot).extract(element, strong);
		} finally {
			rwlock.readLock().unlock();
		}
	}

	@Override
	public Transaction<T> insert(int slot, T element, boolean strong) {
		Objects.requireNonNull(element);
		rwlock.readLock().lock();
		try {
			return array.get(slot).insert(element, strong);
		} finally {
			rwlock.readLock().unlock();
		}
	}

	@Override
	public int getSlotLimit(int slot) {
		return empty.getMaxCount();
	}

	@Override
	public int getSlots() {
		return array.length();
	}

	@Override
	public boolean isConcurrent() {
		return true;
	}

	/**
	 * Invalidate this SizedElementHolderImpl.
	 */
	public void invalidate() {
		rwlock.writeLock().lock();
		try {
			if (valid != null) {
				valid.set(false);
			}
		} finally {
			rwlock.writeLock().unlock();
		}
	}

	/**
	 * Clear this SizedElementHolderImpl and initialize it with a new size.
	 *
	 * @param newsize The new size.
	 */
	public void clearAndResize(int newsize) {
		rwlock.writeLock().lock();
		try {
			invalidate();
			valid = new AtomicBoolean(true);
			array = new AtomicReferenceArray<>(newsize);
			for (int i = 0; i < array.length(); i++) {
				array.set(i, new SizedElementCell<>(emptyFactory.get(), valid, rwlock.readLock()));
			}
		} finally {
			rwlock.writeLock().unlock();
		}
	}
}
