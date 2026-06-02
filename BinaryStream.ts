/**
 * High-performance, memory-efficient Binary Stream Serializer and Deserializer
 * written in TypeScript/JavaScript.
 *
 * Features:
 * 1. Supports reading and writing variable byte size data (strings / binary arrays)
 *    using Varint prefixing or string keys.
 * 2. Auto-incrementing offset with explicit seek (offset get/set) support.
 * 3. Uses standard `Uint8Array` and `DataView` for browser/Node compatibility.
 * 4. Efficient buffer growing strategy (capacity doubling) to minimize allocations.
 * 5. O(1) keyed read/write using a lazy-built index.
 */

export class BinaryStream {
    private buffer: Uint8Array;
    private view: DataView;
    private _offset: number = 0;
    private _length: number = 0;

    private static encoder = new TextEncoder();
    private static decoder = new TextDecoder();

    // Cache to store keyed offsets for fast O(1) lookups
    private keyIndex: Map<string, {
        type: number;
        valueOffset: number;
        valueLength: number;
        entryStartOffset: number;
        entryEndOffset: number;
    }> | null = null;

    /**
     * Creates a new BinaryStream.
     * @param initialCapacityOrBuffer Initial buffer capacity or an existing Uint8Array to deserialize.
     */
    constructor(initialCapacityOrBuffer: number | Uint8Array = 1024) {
        if (typeof initialCapacityOrBuffer === 'number') {
            this.buffer = new Uint8Array(initialCapacityOrBuffer);
            this._length = 0;
        } else {
            this.buffer = initialCapacityOrBuffer;
            this._length = initialCapacityOrBuffer.length;
        }
        this.view = new DataView(this.buffer.buffer, this.buffer.byteOffset, this.buffer.byteLength);
    }

    /**
     * Gets the current stream offset (cursor position).
     */
    get offset(): number {
        return this._offset;
    }

    /**
     * Sets the current stream offset (cursor position).
     */
    set offset(val: number) {
        if (val < 0 || val > this.buffer.length) {
            throw new RangeError(`Offset ${val} is out of bounds (0 - ${this.buffer.length})`);
        }
        this._offset = val;
    }

    /**
     * Gets the active length of the written data.
     */
    get length(): number {
        return this._length;
    }

    /**
     * Clears the stream, resetting offset, length, and the keyed index.
     */
    clear() {
        this._offset = 0;
        this._length = 0;
        this.keyIndex = null;
    }

    /**
     * Returns the compacted Uint8Array view of the written data without extra allocated capacity.
     */
    getBuffer(): Uint8Array {
        return this.buffer.subarray(0, this._length);
    }

    /**
     * Ensures the buffer has enough capacity to write the specified number of bytes.
     * Doubles the buffer size if it exceeds the current capacity.
     */
    private ensureCapacity(additionalBytes: number) {
        const required = this._offset + additionalBytes;
        if (required > this.buffer.length) {
            let newCapacity = this.buffer.length * 2;
            while (newCapacity < required) {
                newCapacity *= 2;
            }
            const newBuffer = new Uint8Array(newCapacity);
            newBuffer.set(this.buffer);
            this.buffer = newBuffer;
            this.view = new DataView(this.buffer.buffer, this.buffer.byteOffset, this.buffer.byteLength);
        }
    }

    // ── Primitive Write Methods ──────────────────────────────────────────────

    writeInt8(value: number) {
        this.ensureCapacity(1);
        this.view.setInt8(this._offset, value);
        this._offset += 1;
        this._length = Math.max(this._length, this._offset);
    }

    writeUint8(value: number) {
        this.ensureCapacity(1);
        this.view.setUint8(this._offset, value);
        this._offset += 1;
        this._length = Math.max(this._length, this._offset);
    }

    writeInt16(value: number, littleEndian: boolean = true) {
        this.ensureCapacity(2);
        this.view.setInt16(this._offset, value, littleEndian);
        this._offset += 2;
        this._length = Math.max(this._length, this._offset);
    }

    writeUint16(value: number, littleEndian: boolean = true) {
        this.ensureCapacity(2);
        this.view.setUint16(this._offset, value, littleEndian);
        this._offset += 2;
        this._length = Math.max(this._length, this._offset);
    }

    writeInt32(value: number, littleEndian: boolean = true) {
        this.ensureCapacity(4);
        this.view.setInt32(this._offset, value, littleEndian);
        this._offset += 4;
        this._length = Math.max(this._length, this._offset);
    }

    writeUint32(value: number, littleEndian: boolean = true) {
        this.ensureCapacity(4);
        this.view.setUint32(this._offset, value, littleEndian);
        this._offset += 4;
        this._length = Math.max(this._length, this._offset);
    }

    writeFloat32(value: number, littleEndian: boolean = true) {
        this.ensureCapacity(4);
        this.view.setFloat32(this._offset, value, littleEndian);
        this._offset += 4;
        this._length = Math.max(this._length, this._offset);
    }

    writeFloat64(value: number, littleEndian: boolean = true) {
        this.ensureCapacity(8);
        this.view.setFloat64(this._offset, value, littleEndian);
        this._offset += 8;
        this._length = Math.max(this._length, this._offset);
    }

    // ── Primitive Read Methods ───────────────────────────────────────────────

    readInt8(): number {
        if (this._offset + 1 > this._length) throw new RangeError("Read out of bounds");
        const val = this.view.getInt8(this._offset);
        this._offset += 1;
        return val;
    }

    readUint8(): number {
        if (this._offset + 1 > this._length) throw new RangeError("Read out of bounds");
        const val = this.view.getUint8(this._offset);
        this._offset += 1;
        return val;
    }

    readInt16(littleEndian: boolean = true): number {
        if (this._offset + 2 > this._length) throw new RangeError("Read out of bounds");
        const val = this.view.getInt16(this._offset, littleEndian);
        this._offset += 2;
        return val;
    }

    readUint16(littleEndian: boolean = true): number {
        if (this._offset + 2 > this._length) throw new RangeError("Read out of bounds");
        const val = this.view.getUint16(this._offset, littleEndian);
        this._offset += 2;
        return val;
    }

    readInt32(littleEndian: boolean = true): number {
        if (this._offset + 4 > this._length) throw new RangeError("Read out of bounds");
        const val = this.view.getInt32(this._offset, littleEndian);
        this._offset += 4;
        return val;
    }

    readUint32(littleEndian: boolean = true): number {
        if (this._offset + 4 > this._length) throw new RangeError("Read out of bounds");
        const val = this.view.getUint32(this._offset, littleEndian);
        this._offset += 4;
        return val;
    }

    readFloat32(littleEndian: boolean = true): number {
        if (this._offset + 4 > this._length) throw new RangeError("Read out of bounds");
        const val = this.view.getFloat32(this._offset, littleEndian);
        this._offset += 4;
        return val;
    }

    readFloat64(littleEndian: boolean = true): number {
        if (this._offset + 8 > this._length) throw new RangeError("Read out of bounds");
        const val = this.view.getFloat64(this._offset, littleEndian);
        this._offset += 8;
        return val;
    }

    // ── Varint (Variable Byte Size Integer) ──────────────────────────────────

    /**
     * Writes a variable-length non-negative integer using 7-bit encoding.
     */
    writeVarint(value: number) {
        if (value < 0) {
            throw new RangeError("Varint must be non-negative");
        }
        while (value >= 0x80) {
            this.writeUint8((value & 0x7F) | 0x80);
            value >>>= 7;
        }
        this.writeUint8(value);
    }

    /**
     * Reads a variable-length non-negative integer.
     */
    readVarint(): number {
        let value = 0;
        let shift = 0;
        while (true) {
            const byte = this.readUint8();
            value |= (byte & 0x7F) << shift;
            if ((byte & 0x80) === 0) {
                break;
            }
            shift += 7;
            if (shift >= 35) {
                throw new RangeError("Varint overflow");
            }
        }
        return value;
    }

    // ── Variable Size Data (Strings & Bytes) ─────────────────────────────────

    /**
     * Writes a variable-length string. Length is prefixed as a Varint.
     */
    writeString(value: string) {
        const encoded = BinaryStream.encoder.encode(value);
        this.writeVarint(encoded.length);
        this.ensureCapacity(encoded.length);
        this.buffer.set(encoded, this._offset);
        this._offset += encoded.length;
        this._length = Math.max(this._length, this._offset);
    }

    /**
     * Reads a variable-length string. Length is read as a Varint first.
     */
    readString(): string {
        const length = this.readVarint();
        if (this._offset + length > this._length) {
            throw new RangeError("Read out of bounds");
        }
        const sub = this.buffer.subarray(this._offset, this._offset + length);
        this._offset += length;
        return BinaryStream.decoder.decode(sub);
    }

    /**
     * Writes a variable-length binary array. Length is prefixed as a Varint.
     */
    writeBytes(value: Uint8Array) {
        this.writeVarint(value.length);
        this.ensureCapacity(value.length);
        this.buffer.set(value, this._offset);
        this._offset += value.length;
        this._length = Math.max(this._length, this._offset);
    }

    /**
     * Reads a variable-length binary array. Length is read as a Varint first.
     * @param copy If true, clones the byte subarray. Otherwise returns a direct view on the stream buffer.
     */
    readBytes(copy: boolean = false): Uint8Array {
        const length = this.readVarint();
        if (this._offset + length > this._length) {
            throw new RangeError("Read out of bounds");
        }
        const sub = this.buffer.subarray(this._offset, this._offset + length);
        this._offset += length;
        return copy ? new Uint8Array(sub) : sub;
    }

    // ── Keyed Variable Size Data ─────────────────────────────────────────────

    /**
     * Writes a string value associated with a string key.
     * Layout: [Key String] [Type: 1] [Value Bytes (prefixed by Varint length)]
     */
    writeStringByKey(key: string, value: string) {
        const startOffset = this._offset;
        this.writeString(key);
        this.writeUint8(1); // Type 1: String

        const encodedValue = BinaryStream.encoder.encode(value);
        this.writeVarint(encodedValue.length);
        const valueOffset = this._offset;

        this.ensureCapacity(encodedValue.length);
        this.buffer.set(encodedValue, this._offset);
        this._offset += encodedValue.length;
        this._length = Math.max(this._length, this._offset);

        this.updateKeyIndex(key, 1, valueOffset, encodedValue.length, startOffset);
    }

    /**
     * Reads a string value associated with a string key.
     * @returns The string value, or undefined if the key is not found.
     */
    readStringByKey(key: string): string | undefined {
        if (!this.keyIndex) {
            this.buildKeyIndex();
        }
        const entry = this.keyIndex?.get(key);
        if (!entry) return undefined;
        if (entry.type !== 1) {
            throw new TypeError(`Key "${key}" is not a string (type ${entry.type})`);
        }

        const savedOffset = this._offset;
        this._offset = entry.valueOffset;
        const sub = this.buffer.subarray(this._offset, this._offset + entry.valueLength);
        this._offset = savedOffset; // Restore offset
        return BinaryStream.decoder.decode(sub);
    }

    /**
     * Writes a binary array value associated with a string key.
     * Layout: [Key String] [Type: 2] [Value Bytes (prefixed by Varint length)]
     */
    writeBytesByKey(key: string, value: Uint8Array) {
        const startOffset = this._offset;
        this.writeString(key);
        this.writeUint8(2); // Type 2: Bytes

        this.writeVarint(value.length);
        const valueOffset = this._offset;

        this.ensureCapacity(value.length);
        this.buffer.set(value, this._offset);
        this._offset += value.length;
        this._length = Math.max(this._length, this._offset);

        this.updateKeyIndex(key, 2, valueOffset, value.length, startOffset);
    }

    /**
     * Reads a binary array value associated with a string key.
     * @returns The Uint8Array view/copy, or undefined if the key is not found.
     */
    readBytesByKey(key: string, copy: boolean = false): Uint8Array | undefined {
        if (!this.keyIndex) {
            this.buildKeyIndex();
        }
        const entry = this.keyIndex?.get(key);
        if (!entry) return undefined;
        if (entry.type !== 2) {
            throw new TypeError(`Key "${key}" is not a binary array (type ${entry.type})`);
        }

        const savedOffset = this._offset;
        this._offset = entry.valueOffset;
        const sub = this.buffer.subarray(this._offset, this._offset + entry.valueLength);
        this._offset = savedOffset; // Restore offset
        return copy ? new Uint8Array(sub) : sub;
    }

    /**
     * Checks if a key exists in the stream.
     */
    hasKey(key: string): boolean {
        if (!this.keyIndex) {
            this.buildKeyIndex();
        }
        return this.keyIndex?.has(key) || false;
    }

    /**
     * Gets a list of all unique keys in the stream.
     */
    keys(): string[] {
        if (!this.keyIndex) {
            this.buildKeyIndex();
        }
        return Array.from(this.keyIndex?.keys() || []);
    }

    // ── Key Index Builders ───────────────────────────────────────────────────

    private updateKeyIndex(key: string, type: number, valueOffset: number, valueLength: number, startOffset: number) {
        if (!this.keyIndex) {
            this.keyIndex = new Map();
        }
        this.keyIndex.set(key, {
            type,
            valueOffset,
            valueLength,
            entryStartOffset: startOffset,
            entryEndOffset: this._offset
        });
    }

    /**
     * Scans the stream from start to build a map of key offsets.
     * This runs lazily on the first key lookup and is highly optimized by skipping actual bytes.
     */
    private buildKeyIndex() {
        this.keyIndex = new Map();
        const savedOffset = this._offset;
        this._offset = 0;

        while (this._offset < this._length) {
            try {
                const startOffset = this._offset;
                const key = this.readString();
                const type = this.readUint8();
                const valueLength = this.readVarint();
                const valueOffset = this._offset;

                this._offset += valueLength; // Fast seek past value bytes

                this.keyIndex.set(key, {
                    type,
                    valueOffset,
                    valueLength,
                    entryStartOffset: startOffset,
                    entryEndOffset: this._offset
                });
            } catch (e) {
                // Hitting EOF or non-keyed data stops index builder
                break;
            }
        }
        this._offset = savedOffset;
    }
}
