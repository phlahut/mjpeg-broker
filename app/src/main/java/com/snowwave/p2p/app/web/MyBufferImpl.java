package com.snowwave.p2p.app.web;

import io.netty.buffer.ByteBuf;
import io.netty.util.CharsetUtil;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.buffer.impl.VertxByteBufAllocator;
import io.vertx.core.impl.Arguments;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

public class MyBufferImpl implements Buffer {
    private ByteBuf buffer;

    public static Buffer buffer(int initialSizeHint) {
        return new MyBufferImpl(initialSizeHint);
    }

    public static Buffer buffer() {
        return new MyBufferImpl();
    }

    public static Buffer buffer(String str) {
        return new MyBufferImpl(str);
    }

    public static Buffer buffer(String str, String enc) {
        return new MyBufferImpl(str, enc);
    }

    public static Buffer buffer(byte[] bytes) {
        return new MyBufferImpl(bytes);
    }

    public static Buffer buffer(ByteBuf byteBuffer) {
        return new MyBufferImpl(byteBuffer);
    }

    public MyBufferImpl() {
        this(0);
    }

    MyBufferImpl(int initialSizeHint) {
        this.buffer = VertxByteBufAllocator.DEFAULT.directBuffer(initialSizeHint, Integer.MAX_VALUE);
    }

    MyBufferImpl(byte[] bytes) {
        this.buffer = VertxByteBufAllocator.DEFAULT.directBuffer(bytes.length, Integer.MAX_VALUE).writeBytes(bytes);
    }

    MyBufferImpl(String str, String enc) {
        this(str.getBytes(Charset.forName(Objects.requireNonNull(enc))));
    }

    MyBufferImpl(String str, Charset cs) {
        this(str.getBytes(cs));
    }

    MyBufferImpl(String str) {
        this(str, StandardCharsets.UTF_8);
    }

    MyBufferImpl(ByteBuf buffer) {
        this.buffer = buffer;
    }

    public String toString() {
        return this.buffer.toString(StandardCharsets.UTF_8);
    }

    public String toString(String enc) {
        return this.buffer.toString(Charset.forName(enc));
    }

    public String toString(Charset enc) {
        return this.buffer.toString(enc);
    }

    public JsonObject toJsonObject() {
        return new JsonObject(this);
    }

    public JsonArray toJsonArray() {
        return new JsonArray(this);
    }

    public byte getByte(int pos) {
        this.checkUpperBound(pos, 1);
        return this.buffer.getByte(pos);
    }

    public short getUnsignedByte(int pos) {
        this.checkUpperBound(pos, 1);
        return this.buffer.getUnsignedByte(pos);
    }

    public void setBuffer(ByteBuf buffer){
        this.buffer = buffer;
    }

    public int getInt(int pos) {
        this.checkUpperBound(pos, 4);
        return this.buffer.getInt(pos);
    }

    public int getIntLE(int pos) {
        this.checkUpperBound(pos, 4);
        return this.buffer.getIntLE(pos);
    }

    public long getUnsignedInt(int pos) {
        this.checkUpperBound(pos, 4);
        return this.buffer.getUnsignedInt(pos);
    }

    public long getUnsignedIntLE(int pos) {
        this.checkUpperBound(pos, 4);
        return this.buffer.getUnsignedIntLE(pos);
    }

    public long getLong(int pos) {
        this.checkUpperBound(pos, 8);
        return this.buffer.getLong(pos);
    }

    public long getLongLE(int pos) {
        this.checkUpperBound(pos, 8);
        return this.buffer.getLongLE(pos);
    }

    public double getDouble(int pos) {
        this.checkUpperBound(pos, 8);
        return this.buffer.getDouble(pos);
    }

    public float getFloat(int pos) {
        this.checkUpperBound(pos, 4);
        return this.buffer.getFloat(pos);
    }

    public short getShort(int pos) {
        this.checkUpperBound(pos, 2);
        return this.buffer.getShort(pos);
    }

    public short getShortLE(int pos) {
        this.checkUpperBound(pos, 2);
        return this.buffer.getShortLE(pos);
    }

    public int getUnsignedShort(int pos) {
        this.checkUpperBound(pos, 2);
        return this.buffer.getUnsignedShort(pos);
    }

    public int getUnsignedShortLE(int pos) {
        this.checkUpperBound(pos, 2);
        return this.buffer.getUnsignedShortLE(pos);
    }

    public int getMedium(int pos) {
        this.checkUpperBound(pos, 3);
        return this.buffer.getMedium(pos);
    }

    public int getMediumLE(int pos) {
        this.checkUpperBound(pos, 3);
        return this.buffer.getMediumLE(pos);
    }

    public int getUnsignedMedium(int pos) {
        this.checkUpperBound(pos, 3);
        return this.buffer.getUnsignedMedium(pos);
    }

    public int getUnsignedMediumLE(int pos) {
        this.checkUpperBound(pos, 3);
        return this.buffer.getUnsignedMediumLE(pos);
    }

    private void checkUpperBound(int index, int size) {
        int length = this.buffer.writerIndex();
        if ((index | length - (index + size)) < 0) {
            throw new IndexOutOfBoundsException(index + " + " + size + " > " + length);
        }
    }

    public byte[] getBytes() {
        byte[] arr = new byte[this.buffer.writerIndex()];
        this.buffer.getBytes(0, arr);
        return arr;
    }

    public byte[] getBytes(int start, int end) {
        Arguments.require(end >= start, "end must be greater or equal than start");
        byte[] arr = new byte[end - start];
        this.buffer.getBytes(start, arr, 0, end - start);
        return arr;
    }

    public Buffer getBytes(byte[] dst) {
        return this.getBytes(dst, 0);
    }

    public Buffer getBytes(byte[] dst, int dstIndex) {
        return this.getBytes(0, this.buffer.writerIndex(), dst, dstIndex);
    }

    public Buffer getBytes(int start, int end, byte[] dst) {
        return this.getBytes(start, end, dst, 0);
    }

    public Buffer getBytes(int start, int end, byte[] dst, int dstIndex) {
        Arguments.require(end >= start, "end must be greater or equal than start");
        this.buffer.getBytes(start, dst, dstIndex, end - start);
        return this;
    }

    public Buffer getBuffer(int start, int end) {
        return new MyBufferImpl(this.getBytes(start, end));
    }

    public String getString(int start, int end, String enc) {
        byte[] bytes = this.getBytes(start, end);
        Charset cs = Charset.forName(enc);
        return new String(bytes, cs);
    }

    public String getString(int start, int end) {
        byte[] bytes = this.getBytes(start, end);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    public Buffer appendBuffer(Buffer buff) {
        MyBufferImpl impl = (MyBufferImpl)buff;
        ByteBuf byteBuf = impl.buffer;
        this.buffer.writeBytes(impl.buffer, byteBuf.readerIndex(), impl.buffer.readableBytes());
        return this;
    }

    public Buffer appendBuffer(Buffer buff, int offset, int len) {
        MyBufferImpl impl = (MyBufferImpl)buff;
        ByteBuf byteBuf = impl.buffer;
        int from = byteBuf.readerIndex() + offset;
        this.buffer.writeBytes(byteBuf, from, len);
        return this;
    }

    public Buffer appendBytes(byte[] bytes) {
        this.buffer.writeBytes(bytes);
        return this;
    }

    public Buffer appendBytes(byte[] bytes, int offset, int len) {
        this.buffer.writeBytes(bytes, offset, len);
        return this;
    }

    public Buffer appendByte(byte b) {
        this.buffer.writeByte(b);
        return this;
    }

    public Buffer appendUnsignedByte(short b) {
        this.buffer.writeByte(b);
        return this;
    }

    public Buffer appendInt(int i) {
        this.buffer.writeInt(i);
        return this;
    }

    public Buffer appendIntLE(int i) {
        this.buffer.writeIntLE(i);
        return this;
    }

    public Buffer appendUnsignedInt(long i) {
        this.buffer.writeInt((int)i);
        return this;
    }

    public Buffer appendUnsignedIntLE(long i) {
        this.buffer.writeIntLE((int)i);
        return this;
    }

    public Buffer appendMedium(int i) {
        this.buffer.writeMedium(i);
        return this;
    }

    public Buffer appendMediumLE(int i) {
        this.buffer.writeMediumLE(i);
        return this;
    }

    public Buffer appendLong(long l) {
        this.buffer.writeLong(l);
        return this;
    }

    public Buffer appendLongLE(long l) {
        this.buffer.writeLongLE(l);
        return this;
    }

    public Buffer appendShort(short s) {
        this.buffer.writeShort(s);
        return this;
    }

    public Buffer appendShortLE(short s) {
        this.buffer.writeShortLE(s);
        return this;
    }

    public Buffer appendUnsignedShort(int s) {
        this.buffer.writeShort(s);
        return this;
    }

    public Buffer appendUnsignedShortLE(int s) {
        this.buffer.writeShortLE(s);
        return this;
    }

    public Buffer appendFloat(float f) {
        this.buffer.writeFloat(f);
        return this;
    }

    public Buffer appendDouble(double d) {
        this.buffer.writeDouble(d);
        return this;
    }

    public Buffer appendString(String str, String enc) {
        return this.append(str, Charset.forName(Objects.requireNonNull(enc)));
    }

    public Buffer appendString(String str) {
        return this.append(str, CharsetUtil.UTF_8);
    }

    public Buffer setByte(int pos, byte b) {
        this.ensureLength(pos + 1);
        this.buffer.setByte(pos, b);
        return this;
    }

    public Buffer setUnsignedByte(int pos, short b) {
        this.ensureLength(pos + 1);
        this.buffer.setByte(pos, b);
        return this;
    }

    public Buffer setInt(int pos, int i) {
        this.ensureLength(pos + 4);
        this.buffer.setInt(pos, i);
        return this;
    }

    public Buffer setIntLE(int pos, int i) {
        this.ensureLength(pos + 4);
        this.buffer.setIntLE(pos, i);
        return this;
    }

    public Buffer setUnsignedInt(int pos, long i) {
        this.ensureLength(pos + 4);
        this.buffer.setInt(pos, (int)i);
        return this;
    }

    public Buffer setUnsignedIntLE(int pos, long i) {
        this.ensureLength(pos + 4);
        this.buffer.setIntLE(pos, (int)i);
        return this;
    }

    public Buffer setMedium(int pos, int i) {
        this.ensureLength(pos + 3);
        this.buffer.setMedium(pos, i);
        return this;
    }

    public Buffer setMediumLE(int pos, int i) {
        this.ensureLength(pos + 3);
        this.buffer.setMediumLE(pos, i);
        return this;
    }

    public Buffer setLong(int pos, long l) {
        this.ensureLength(pos + 8);
        this.buffer.setLong(pos, l);
        return this;
    }

    public Buffer setLongLE(int pos, long l) {
        this.ensureLength(pos + 8);
        this.buffer.setLongLE(pos, l);
        return this;
    }

    public Buffer setDouble(int pos, double d) {
        this.ensureLength(pos + 8);
        this.buffer.setDouble(pos, d);
        return this;
    }

    public Buffer setFloat(int pos, float f) {
        this.ensureLength(pos + 4);
        this.buffer.setFloat(pos, f);
        return this;
    }

    public Buffer setShort(int pos, short s) {
        this.ensureLength(pos + 2);
        this.buffer.setShort(pos, s);
        return this;
    }

    public Buffer setShortLE(int pos, short s) {
        this.ensureLength(pos + 2);
        this.buffer.setShortLE(pos, s);
        return this;
    }

    public Buffer setUnsignedShort(int pos, int s) {
        this.ensureLength(pos + 2);
        this.buffer.setShort(pos, s);
        return this;
    }

    public Buffer setUnsignedShortLE(int pos, int s) {
        this.ensureLength(pos + 2);
        this.buffer.setShortLE(pos, s);
        return this;
    }

    public Buffer setBuffer(int pos, Buffer buff) {
        this.ensureLength(pos + buff.length());
        MyBufferImpl impl = (MyBufferImpl)buff;
        ByteBuf byteBuf = impl.buffer;
        this.buffer.setBytes(pos, byteBuf, byteBuf.readerIndex(), byteBuf.readableBytes());
        return this;
    }

    public Buffer setBuffer(int pos, Buffer buffer, int offset, int len) {
        this.ensureLength(pos + len);
        MyBufferImpl impl = (MyBufferImpl)buffer;
        ByteBuf byteBuf = impl.buffer;
        this.buffer.setBytes(pos, byteBuf, byteBuf.readerIndex() + offset, len);
        return this;
    }

    public MyBufferImpl setBytes(int pos, ByteBuffer b) {
        this.ensureLength(pos + b.limit());
        this.buffer.setBytes(pos, b);
        return this;
    }

    public Buffer setBytes(int pos, byte[] b) {
        this.ensureLength(pos + b.length);
        this.buffer.setBytes(pos, b);
        return this;
    }

    public Buffer setBytes(int pos, byte[] b, int offset, int len) {
        this.ensureLength(pos + len);
        this.buffer.setBytes(pos, b, offset, len);
        return this;
    }

    public Buffer setString(int pos, String str) {
        return this.setBytes(pos, str, CharsetUtil.UTF_8);
    }

    public Buffer setString(int pos, String str, String enc) {
        return this.setBytes(pos, str, Charset.forName(enc));
    }

    public int length() {
        return this.buffer.writerIndex();
    }

    public Buffer copy() {
        return this.buffer.isReadOnly() ? this : new MyBufferImpl(this.buffer.copy());
    }

    public Buffer slice() {
        return new MyBufferImpl(this.buffer.slice());
    }

    public Buffer slice(int start, int end) {
        return new MyBufferImpl(this.buffer.slice(start, end - start));
    }

    public ByteBuf byteBuf() {
        return this.buffer;
    }

    public ByteBuf getByteBuf() {
        return this.buffer.slice();
    }

    private Buffer append(String str, Charset charset) {
        byte[] bytes = str.getBytes(charset);
        this.ensureExpandableBy(bytes.length);
        this.buffer.writeBytes(bytes);
        return this;
    }

    private Buffer setBytes(int pos, String str, Charset charset) {
        byte[] bytes = str.getBytes(charset);
        this.ensureLength(pos + bytes.length);
        this.buffer.setBytes(pos, bytes);
        return this;
    }

    private void ensureLength(int newLength) {
        int capacity = this.buffer.capacity();
        int over = newLength - capacity;
        int writerIndex = this.buffer.writerIndex();
        if (over > 0) {
            int maxCapacity = this.buffer.maxCapacity();
            if (capacity + over > maxCapacity) {
                this.setFullMaxCapacity(capacity + over);
            }

            this.buffer.ensureWritable(newLength - writerIndex);
        }

        if (newLength > writerIndex) {
            this.buffer.writerIndex(newLength);
        }

    }

    private void ensureExpandableBy(int amount) {
        int minMaxCapa = this.buffer.writerIndex() + amount;
        if (minMaxCapa > this.buffer.maxCapacity()) {
            this.setFullMaxCapacity(minMaxCapa);
        }

    }

    private void setFullMaxCapacity(int capacity) {
        ByteBuf tmp = this.buffer.alloc().heapBuffer(capacity, Integer.MAX_VALUE);
        tmp.writeBytes(this.buffer);
        this.buffer = tmp;
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (o != null && this.getClass() == o.getClass()) {
            MyBufferImpl buffer1 = (MyBufferImpl)o;
            return Objects.equals(this.buffer, buffer1.buffer);
        } else {
            return false;
        }
    }

    public int hashCode() {
        return this.buffer != null ? this.buffer.hashCode() : 0;
    }

    public void writeToBuffer(Buffer buff) {
        buff.appendInt(this.length());
        buff.appendBuffer(this);
    }

    public int readFromBuffer(int pos, Buffer buffer) {
        int len = buffer.getInt(pos);
        MyBufferImpl impl = (MyBufferImpl)buffer.getBuffer(pos + 4, pos + 4 + len);
        this.buffer = impl.getByteBuf();
        return pos + 4 + len;
    }
}
