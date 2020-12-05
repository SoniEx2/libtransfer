package io.github.soniex2.libtransfer.algorithm;

import io.github.soniex2.libtransfer.SizedElement;
import io.github.soniex2.libtransfer.SizedElementFilter;
import io.github.soniex2.libtransfer.SizedElementHolder;
import io.github.soniex2.libtransfer.Transaction;

import java.util.ArrayList;
import java.util.List;

/**
 * @author soniex2
 */
public class SimpleMove<T extends SizedElement<T>> {
	public boolean move(SizedElementHolder<T> from, SizedElementHolder<T> to, SizedElementFilter<T> filter) {
		T el = filter.get();
		T el2 = el;
		List<Transaction<T>> transactions = new ArrayList<>();
		try {
			for (int i = 0; i < from.getSlots(); i++) {
				Transaction<T> ex = from.extract(i, el, from.isConcurrent());
				if (!ex.get().isEmpty()) {
					el = el.split(ex.get());
					transactions.add(ex);
					if (el.isEmpty()) {
						break;
					}
				} else {
					ex.revert(); // free locks
				}
			}
			if (!el.isEmpty()) {
				for (Transaction<T> transaction : transactions) {
					transaction.revert();
				}
				transactions.clear();
				return false;
			}
			for (int i = 0; i < to.getSlots(); i++) {
				Transaction<T> in = to.insert(i, el2, to.isConcurrent());
				if (!in.get().isEmpty()) {
					el2 = el2.split(in.get());
					transactions.add(in);
					if (el2.isEmpty()) {
						break;
					}
				} else {
					in.revert();
				}
			}
			if (!el2.isEmpty()) {
				for (Transaction<T> transaction : transactions) {
					transaction.revert();
				}
				transactions.clear();
				return false;
			}
		} finally {
			for (Transaction<T> transaction : transactions) {
				transaction.commit();
			}
		}
		return true;
	}
}
