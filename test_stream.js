// JavaScript implementation of BinaryStream for quick verification
class BinaryStream {
    constructor(initialCapacityOrBuffer = 1024) {
        if (typeof initialCapacityOrBuffer === 'number') {
            this.buffer = new Uint8Array(initialCapacityOrBuffer);
            this._length = 0;
        } else {
            this.buffer = initialCapacityOrBuffer;
            this._length = initialCapacityOrBuffer.length;
        }
        this.view = new DataView(this.buffer.buffer, this.buffer.byteOffset, this.buffer.byteLength);
        this._offset = 0;
        this.keyIndex = null;
    }

    get offset() { return this._offset; }
    set offset(val) {
        if (val < 0 || val > this.buffer.length) {
            throw new RangeError(`Offset ${val} is out of bounds`);
        }
        this._offset = val;
    }

    get length() { return this._length; }

    clear() {
        this._offset = 0;
        this._length = 0;
        this.keyIndex = null;
    }

    getBuffer() {
        return this.buffer.subarray(0, this._length);
    }

    ensureCapacity(additionalBytes) {
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

    writeInt8(value) {
        this.ensureCapacity(1);
        this.view.setInt8(this._offset, value);
        this._offset += 1;
        this._length = Math.max(this._length, this._offset);
    }

    writeUint8(value) {
        this.ensureCapacity(1);
        this.view.setUint8(this._offset, value);
        this._offset += 1;
        this._length = Math.max(this._length, this._offset);
    }

    writeInt16(value, littleEndian = true) {
        this.ensureCapacity(2);
        this.view.setInt16(this._offset, value, littleEndian);
        this._offset += 2;
        this._length = Math.max(this._length, this._offset);
    }

    writeUint16(value, littleEndian = true) {
        this.ensureCapacity(2);
        this.view.setUint16(this._offset, value, littleEndian);
        this._offset += 2;
        this._length = Math.max(this._length, this._offset);
    }

    writeInt32(value, littleEndian = true) {
        this.ensureCapacity(4);
        this.view.setInt32(this._offset, value, littleEndian);
        this._offset += 4;
        this._length = Math.max(this._length, this._offset);
    }

    writeUint32(value, littleEndian = true) {
        this.ensureCapacity(4);
        this.view.setUint32(this._offset, value, littleEndian);
        this._offset += 4;
        this._length = Math.max(this._length, this._offset);
    }

    writeFloat32(value, littleEndian = true) {
        this.ensureCapacity(4);
        this.view.setFloat32(this._offset, value, littleEndian);
        this._offset += 4;
        this._length = Math.max(this._length, this._offset);
    }

    writeFloat64(value, littleEndian = true) {
        this.ensureCapacity(8);
        this.view.setFloat64(this._offset, value, littleEndian);
        this._offset += 8;
        this._length = Math.max(this._length, this._offset);
    }

    readInt8() {
        if (this._offset + 1 > this._length) throw new RangeError("Read out of bounds");
        const val = this.view.getInt8(this._offset);
        this._offset += 1;
        return val;
    }

    readUint8() {
        if (this._offset + 1 > this._length) throw new RangeError("Read out of bounds");
        const val = this.view.getUint8(this._offset);
        this._offset += 1;
        return val;
    }

    readInt16(littleEndian = true) {
        if (this._offset + 2 > this._length) throw new RangeError("Read out of bounds");
        const val = this.view.getInt16(this._offset, littleEndian);
        this._offset += 2;
        return val;
    }

    readUint16(littleEndian = true) {
        if (this._offset + 2 > this._length) throw new RangeError("Read out of bounds");
        const val = this.view.getUint16(this._offset, littleEndian);
        this._offset += 2;
        return val;
    }

    readInt32(littleEndian = true) {
        if (this._offset + 4 > this._length) throw new RangeError("Read out of bounds");
        const val = this.view.getInt32(this._offset, littleEndian);
        this._offset += 4;
        return val;
    }

    readUint32(littleEndian = true) {
        if (this._offset + 4 > this._length) throw new RangeError("Read out of bounds");
        const val = this.view.getUint32(this._offset, littleEndian);
        this._offset += 4;
        return val;
    }

    readFloat32(littleEndian = true) {
        if (this._offset + 4 > this._length) throw new RangeError("Read out of bounds");
        const val = this.view.getFloat32(this._offset, littleEndian);
        this._offset += 4;
        return val;
    }

    readFloat64(littleEndian = true) {
        if (this._offset + 8 > this._length) throw new RangeError("Read out of bounds");
        const val = this.view.getFloat64(this._offset, littleEndian);
        this._offset += 8;
        return val;
    }

    writeVarint(value) {
        if (value < 0) {
            throw new RangeError("Varint must be non-negative");
        }
        while (value >= 0x80) {
            this.writeUint8((value & 0x7F) | 0x80);
            value >>>= 7;
        }
        this.writeUint8(value);
    }

    readVarint() {
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

    writeString(value) {
        const encoded = BinaryStream.encoder.encode(value);
        this.writeVarint(encoded.length);
        this.ensureCapacity(encoded.length);
        this.buffer.set(encoded, this._offset);
        this._offset += encoded.length;
        this._length = Math.max(this._length, this._offset);
    }

    readString() {
        const length = this.readVarint();
        if (this._offset + length > this._length) {
            throw new RangeError("Read out of bounds");
        }
        const sub = this.buffer.subarray(this._offset, this._offset + length);
        this._offset += length;
        return BinaryStream.decoder.decode(sub);
    }

    writeBytes(value) {
        this.writeVarint(value.length);
        this.ensureCapacity(value.length);
        this.buffer.set(value, this._offset);
        this._offset += value.length;
        this._length = Math.max(this._length, this._offset);
    }

    readBytes(copy = false) {
        const length = this.readVarint();
        if (this._offset + length > this._length) {
            throw new RangeError("Read out of bounds");
        }
        const sub = this.buffer.subarray(this._offset, this._offset + length);
        this._offset += length;
        return copy ? new Uint8Array(sub) : sub;
    }

    writeStringByKey(key, value) {
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

    readStringByKey(key) {
        if (!this.keyIndex) {
            this.buildKeyIndex();
        }
        const entry = this.keyIndex.get(key);
        if (!entry) return undefined;
        if (entry.type !== 1) {
            throw new TypeError(`Key "${key}" is not a string`);
        }

        const savedOffset = this._offset;
        this._offset = entry.valueOffset;
        const sub = this.buffer.subarray(this._offset, this._offset + entry.valueLength);
        this._offset = savedOffset;
        return BinaryStream.decoder.decode(sub);
    }

    writeBytesByKey(key, value) {
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

    readBytesByKey(key, copy = false) {
        if (!this.keyIndex) {
            this.buildKeyIndex();
        }
        const entry = this.keyIndex.get(key);
        if (!entry) return undefined;
        if (entry.type !== 2) {
            throw new TypeError(`Key "${key}" is not a binary array`);
        }

        const savedOffset = this._offset;
        this._offset = entry.valueOffset;
        const sub = this.buffer.subarray(this._offset, this._offset + entry.valueLength);
        this._offset = savedOffset;
        return copy ? new Uint8Array(sub) : sub;
    }

    hasKey(key) {
        if (!this.keyIndex) {
            this.buildKeyIndex();
        }
        return this.keyIndex.has(key);
    }

    keys() {
        if (!this.keyIndex) {
            this.buildKeyIndex();
        }
        return Array.from(this.keyIndex.keys());
    }

    updateKeyIndex(key, type, valueOffset, valueLength, startOffset) {
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

    buildKeyIndex() {
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

                this._offset += valueLength;

                this.keyIndex.set(key, {
                    type,
                    valueOffset,
                    valueLength,
                    entryStartOffset: startOffset,
                    entryEndOffset: this._offset
                });
            } catch (e) {
                break;
            }
        }
        this._offset = savedOffset;
    }
}

BinaryStream.encoder = new TextEncoder();
BinaryStream.decoder = new TextDecoder();

// Test Runner
function runTests() {
    console.log("Starting BinaryStream Tests...");
    const stream = new BinaryStream();

    // 1. Test primitives
    stream.writeUint8(255);
    stream.writeInt8(-128);
    stream.writeInt32(123456789);
    stream.writeFloat32(3.14159);
    stream.writeFloat64(2.7182818284);

    stream.offset = 0;
    console.assert(stream.readUint8() === 255, "Uint8 mismatch");
    console.assert(stream.readInt8() === -128, "Int8 mismatch");
    console.assert(stream.readInt32() === 123456789, "Int32 mismatch");
    console.assert(Math.abs(stream.readFloat32() - 3.14159) < 1e-4, "Float32 mismatch");
    console.assert(Math.abs(stream.readFloat64() - 2.7182818284) < 1e-9, "Float64 mismatch");
    console.log("✅ Primitive types read/write passed.");

    // 2. Test Varint & variable strings/bytes
    stream.clear();
    stream.writeVarint(127);
    stream.writeVarint(300);
    stream.writeString("Hello, World!");
    
    const bytes = new Uint8Array([10, 20, 30, 40, 50]);
    stream.writeBytes(bytes);

    stream.offset = 0;
    console.assert(stream.readVarint() === 127, "Varint 127 mismatch");
    console.assert(stream.readVarint() === 300, "Varint 300 mismatch");
    console.assert(stream.readString() === "Hello, World!", "String mismatch");
    
    const readB = stream.readBytes();
    console.assert(readB.length === 5 && readB[0] === 10 && readB[4] === 50, "Bytes mismatch");
    console.log("✅ Varints, variable strings, and byte arrays passed.");

    // 3. Test Keyed Values
    stream.clear();
    stream.writeStringByKey("username", "alice");
    stream.writeStringByKey("role", "admin");
    stream.writeBytesByKey("avatar", new Uint8Array([0xAA, 0xBB, 0xCC]));

    // Read by key
    console.assert(stream.readStringByKey("username") === "alice", "Keyed username mismatch");
    console.assert(stream.readStringByKey("role") === "admin", "Keyed role mismatch");
    
    const avatar = stream.readBytesByKey("avatar");
    console.assert(avatar.length === 3 && avatar[0] === 0xAA && avatar[2] === 0xCC, "Keyed avatar mismatch");
    
    // Check hasKey & keys
    console.assert(stream.hasKey("username") === true, "hasKey failed for username");
    console.assert(stream.hasKey("notexist") === false, "hasKey failed for non-existing");
    
    const keys = stream.keys();
    console.assert(keys.includes("username") && keys.includes("role") && keys.includes("avatar"), "keys list mismatch");
    console.log("✅ Keyed reads and writes passed.");

    // 4. Test Key Overwriting (Append updates index)
    stream.writeStringByKey("role", "superadmin");
    console.assert(stream.readStringByKey("role") === "superadmin", "Keyed overwrite failed");
    console.log("✅ Key overwriting (index update) passed.");

    // 5. Test Deserialization from buffer
    const buffer = stream.getBuffer();
    const stream2 = new BinaryStream(buffer);
    console.assert(stream2.readStringByKey("username") === "alice", "Deserialized username mismatch");
    console.assert(stream2.readStringByKey("role") === "superadmin", "Deserialized role mismatch");
    console.log("✅ Buffer deserialization passed.");

    console.log("🎉 All BinaryStream tests passed successfully!");
}

runTests();
