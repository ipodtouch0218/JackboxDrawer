package me.ipodtouch0218.jackboxdrawer.util;

import java.util.ArrayList;
import java.util.Iterator;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
public class SizeLimitedList<T> {

	private final ArrayList<T> internalList = new ArrayList<T>();
	@Getter private final int maxSize;
	
	public boolean add(T value) {
		if (internalList.add(value)) {
			while (internalList.size() > maxSize) {
				internalList.remove(0);
			}
			return true;
		}
		return false;
	}

	public void removeAfter(int index) {
		int i = 0;
		Iterator<T> it = internalList.iterator();
		while (it.hasNext()) {
			it.next();
			if (i++ > index)
				it.remove();
		}
	}
	
	public T get(int index) {
		return internalList.get(index);
	}
	
	public int size() {
		return internalList.size();
	}
	
	public void clear() {
		internalList.clear();
	}
	
	@Override
	public String toString() {
		return internalList.toString();
	}
}
