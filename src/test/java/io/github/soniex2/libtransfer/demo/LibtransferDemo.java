package io.github.soniex2.libtransfer.demo;

import io.github.soniex2.libtransfer.SizedElement;
import io.github.soniex2.libtransfer.SizedElementFilter;
import io.github.soniex2.libtransfer.SizedElementHolder;
import io.github.soniex2.libtransfer.algorithm.SimpleMove;
import io.github.soniex2.libtransfer.impl.SizedElementHolderImpl;

/**
 * @author soniex2
 */
public class LibtransferDemo {
	public static void main(String[] args) {
		SizedElementHolder<MyElement> in = new SizedElementHolderImpl<>(8, () -> MyElement.EMPTY);
		SizedElementHolder<MyElement> out = new SizedElementHolderImpl<>(8, () -> MyElement.EMPTY);
		SizedElementFilter<MyElement> filter = () -> MyElement.EMPTY; // change me
		boolean ok;
		System.out.println(MyElement.EMPTY);
		ok = new SimpleMove<MyElement>().move(in, out, filter);
		System.out.println("ok = " + ok);
		if (!filter.get().isEmpty()) {
			in.insert(0, filter.get(), in.isConcurrent()).commit();
		}
		if (!filter.get().isEmpty()) {
			out.insert(0, filter.get().withCount(62), in.isConcurrent()).commit();
		}
		System.out.println(in.get(0).count);
		System.out.println(out.get(0).count);
		System.out.println(out.get(1).count);
		ok = new SimpleMove<MyElement>().move(in, out, filter);
		System.out.println("ok = " + ok);
		System.out.println(in.get(0).count);
		System.out.println(out.get(0).count);
		System.out.println(out.get(1).count);
	}

	public static class MyElement implements SizedElement<MyElement> {
		public static final MyElement EMPTY = new MyElement();

		private final int type;
		private final int count;

		private MyElement() {
			type = -1;
			count = 0;
		}

		private MyElement(int type, int count) {
			if (type < 0 || count < 1 || count > getMaxCount()) {
				throw new IllegalArgumentException();
			}
			this.type = type;
			this.count = count;
		}

		private MyElement(MyElement myElement, int count) {
			this(myElement.type, count);
		}

		@Override
		public int getCount() {
			return count;
		}

		@Override
		public MyElement withCount(int count) {
			if (count == 0) {
				return EMPTY;
			}
			if (type < 0) {
				throw new IllegalStateException();
			}
			return new MyElement(this, count);
		}

		@Override
		public boolean hasSameType(MyElement other) {
			return type == other.type;
		}

		@Override
		public int getMaxCount() {
			return 64;
		}

		@Override
		public boolean isEmpty() {
			return this == EMPTY;
		}

		public static MyElement of(int type, int count) {
			if (type < -1) {
				throw new IllegalArgumentException();
			}
			if (count == 0) {
				return EMPTY;
			}
			return new MyElement(type, count);
		}

		public static MyElement of(MyElement type, int count) {
			return type.withCount(count);
		}
	}
}
