package com.snowwave.p2p.component.transport.encoder;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.handler.codec.http.*;
import io.netty.util.IllegalReferenceCountException;

public class ZeroCPHttpResponse extends DefaultHttpResponse implements FullHttpResponse{

    private ByteBuf content;
    private final HttpHeaders trailingHeaders;
    private int hash;

    public ZeroCPHttpResponse(HttpVersion version, HttpResponseStatus status) {
        this(version, status, true);
    }

    public void setContent(ByteBuf content){
        this.content = content;
    }

    public ZeroCPHttpResponse(HttpVersion version, HttpResponseStatus status, boolean validateHeaders) {
        this(version, status, validateHeaders, false);
    }

    public ZeroCPHttpResponse(HttpVersion version, HttpResponseStatus status, boolean validateHeaders, boolean singleFieldHeaders) {
        super(version, status, validateHeaders, singleFieldHeaders);
        this.trailingHeaders = singleFieldHeaders ? new CombinedHttpHeaders(validateHeaders) : new DefaultHttpHeaders(validateHeaders);
    }

    public HttpHeaders trailingHeaders() {
        return this.trailingHeaders;
    }

    public ByteBuf content() {
        return this.content;
    }

    public int refCnt() {
        return this.content.refCnt();
    }

    public FullHttpResponse retain() {
        this.content.retain();
        return this;
    }

    public FullHttpResponse retain(int increment) {
        this.content.retain(increment);
        return this;
    }

    public FullHttpResponse touch() {
        this.content.touch();
        return this;
    }

    public FullHttpResponse touch(Object hint) {
        this.content.touch(hint);
        return this;
    }

    public boolean release() {
        return this.content.release();
    }

    public boolean release(int decrement) {
        return this.content.release(decrement);
    }

    public FullHttpResponse setProtocolVersion(HttpVersion version) {
        super.setProtocolVersion(version);
        return this;
    }

    public FullHttpResponse setStatus(HttpResponseStatus status) {
        super.setStatus(status);
        return this;
    }

    public FullHttpResponse copy() {
        return this.replace(this.content().copy());
    }

    public FullHttpResponse duplicate() {
        return this.replace(this.content().duplicate());
    }

    public FullHttpResponse retainedDuplicate() {
        return this.replace(this.content().retainedDuplicate());
    }

    public FullHttpResponse replace(ByteBuf content) {
        FullHttpResponse response = new DefaultFullHttpResponse(this.protocolVersion(), this.status(), content, this.headers().copy(), this.trailingHeaders().copy());
        response.setDecoderResult(this.decoderResult());
        return response;
    }

    public int hashCode() {
        int hash = this.hash;
        if (hash == 0) {
            if (ByteBufUtil.isAccessible(this.content())) {
                try {
                    hash = 31 + this.content().hashCode();
                } catch (IllegalReferenceCountException var3) {
                    hash = 31;
                }
            } else {
                hash = 31;
            }

            hash = 31 * hash + this.trailingHeaders().hashCode();
            hash = 31 * hash + super.hashCode();
            this.hash = hash;
        }

        return hash;
    }

    public boolean equals(Object o) {
        if (!(o instanceof DefaultFullHttpResponse)) {
            return false;
        } else {
            DefaultFullHttpResponse other = (DefaultFullHttpResponse)o;
            return super.equals(other) && this.content().equals(other.content()) && this.trailingHeaders().equals(other.trailingHeaders());
        }
    }
}
