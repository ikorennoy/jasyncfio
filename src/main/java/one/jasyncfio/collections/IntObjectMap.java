package one.jasyncfio.collections;

import java.util.Arrays;
import java.util.Objects;

public class IntObjectMap<T> {
    private T[] values;

    private int size = 0;
    public IntObjectMap(int initialCapacity) {
        values = (T[]) new Object[initialCapacity];
    }

    public int put(T value) {
        int currentPosition = 0;
        while (true) {
            if (values[currentPosition] == null) {
                values[currentPosition] = value;
                size += 1;
                return currentPosition;
            } else {
                currentPosition += 1;
                if (currentPosition == values.length) {
                    grow();
                }
            }
        }
    }

    public T remove(int item) {
        T value = values[item];
        values[item] = null;
        size -= 1;
        return value;
    }

    private void grow() {
        int newCapacity = values.length * 2;
        T[] newArray = (T[]) new Object[newCapacity];
        System.arraycopy(values, 0, newArray, 0, values.length);
        values = newArray;
    }

    public int getSize() {
        return size;
    }

    @Override
    public String toString() {
        return "IntObjectMap{" +
                "values=" + Arrays.toString(Arrays.stream(values).filter(Objects::nonNull).toArray()) +
                ", size=" + size +
                '}';
    }
}
