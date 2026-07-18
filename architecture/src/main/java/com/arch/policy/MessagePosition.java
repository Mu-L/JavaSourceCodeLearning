package com.arch.policy;

public final class MessagePosition implements Comparable<MessagePosition> {
    public static final MessagePosition BEGINNING = new MessagePosition(0);
    private final long value;

    public MessagePosition(long value) {
        if (value < 0) throw new IllegalArgumentException("position must be non-negative");
        this.value = value;
    }

    public long getValue() { return value; }

    @Override public int compareTo(MessagePosition other) { return Long.compare(value, other.value); }
    @Override public boolean equals(Object other) {
        return other instanceof MessagePosition && value == ((MessagePosition) other).value;
    }
    @Override public int hashCode() { return Long.valueOf(value).hashCode(); }
    @Override public String toString() { return Long.toString(value); }
}
