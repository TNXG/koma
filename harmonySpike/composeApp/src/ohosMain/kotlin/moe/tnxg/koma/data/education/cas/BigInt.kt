/*
 * 最小化纯 Kotlin 大整数：仅覆盖 CAS security.js 协议所需的模幂运算
 * （替代 java.math.BigInteger，Kotlin/Native 无该类）。
 * 内部为小端 30-bit limb，恒为非负数（RSA 消息恒小于模数）。
 */
package moe.tnxg.koma.data.education.cas

internal class BigInt private constructor(private val limbs: IntArray) {

    val isZero: Boolean get() = limbs.isEmpty() || (limbs.size == 1 && limbs[0] == 0)

    fun bitLength(): Int {
        val top = limbs.lastOrNull() ?: return 0
        return (limbs.size - 1) * BITS + (32 - top.countLeadingZeroBits())
    }

    fun testBit(index: Int): Boolean {
        val limb = index / BITS
        if (limb >= limbs.size) return false
        return (limbs[limb] shr (index % BITS)) and 1 == 1
    }

    fun shl(bits: Int): BigInt {
        if (isZero || bits == 0) return this
        val limbShift = bits / BITS
        val bitShift = bits % BITS
        val out = IntArray(limbs.size + limbShift + 1)
        for (i in limbs.indices) {
            val v = limbs[i].toLong()
            if (bitShift == 0) {
                out[i + limbShift] = (out[i + limbShift].toLong() or v and MASK.toLong()).toInt()
            } else {
                out[i + limbShift] = ((out[i + limbShift].toLong() or (v shl bitShift)) and MASK.toLong()).toInt()
                out[i + limbShift + 1] = ((out[i + limbShift + 1].toLong() or (v shr (BITS - bitShift))) and MASK.toLong()).toInt()
            }
        }
        return of(out)
    }

    fun shr(bits: Int): BigInt {
        val limbShift = bits / BITS
        if (limbShift >= limbs.size) return ZERO
        val bitShift = bits % BITS
        val out = IntArray(limbs.size - limbShift)
        for (i in out.indices) {
            val low = limbs[i + limbShift].toLong() shr bitShift
            val high = if (bitShift == 0 || i + limbShift + 1 >= limbs.size) 0L
            else limbs[i + limbShift + 1].toLong() shl (BITS - bitShift)
            out[i] = ((low or high) and MASK.toLong()).toInt()
        }
        return of(out)
    }

    /** 要求 this >= other。 */
    fun minus(other: BigInt): BigInt {
        val out = IntArray(limbs.size)
        var borrow = 0L
        for (i in out.indices) {
            val sub = if (i < other.limbs.size) other.limbs[i].toLong() else 0L
            val cur = limbs[i].toLong() - sub - borrow
            out[i] = (cur and MASK.toLong()).toInt()
            borrow = if (cur < 0) 1L else 0L
        }
        return of(out)
    }

    operator fun times(other: BigInt): BigInt {
        val out = LongArray(limbs.size + other.limbs.size)
        for (i in limbs.indices) {
            var carry = 0L
            val ai = limbs[i].toLong()
            if (ai == 0L) continue
            for (j in other.limbs.indices) {
                val cur = out[i + j] + ai * other.limbs[j] + carry
                out[i + j] = cur and MASK.toLong()
                carry = cur shr BITS
            }
            out[i + other.limbs.size] += carry
        }
        var carry = 0L
        for (i in out.indices) {
            val cur = out[i] + carry
            out[i] = cur and MASK.toLong()
            carry = cur shr BITS
        }
        return of(out.map { it.toInt() }.toIntArray())
    }

    /** 按位或（本用例中 shl 8 后低位恒 0，与加法等价）。 */
    infix fun or(other: BigInt): BigInt {
        val n = maxOf(limbs.size, other.limbs.size)
        val out = IntArray(n)
        for (i in 0 until n) {
            val a = if (i < limbs.size) limbs[i] else 0
            val b = if (i < other.limbs.size) other.limbs[i] else 0
            out[i] = a or b
        }
        return of(out)
    }

    operator fun compareTo(other: BigInt): Int {
        val a = limbs; val b = other.limbs
        if (a.size != b.size) return a.size.compareTo(b.size)
        for (i in a.indices.reversed()) {
            if (a[i] != b[i]) return a[i].compareTo(b[i])
        }
        return 0
    }

    /** 二进制长除法取余（逐 shift 下移，终值 a < m）。 */
    fun mod(m: BigInt): BigInt {
        var a = this
        if (a < m) return a
        var shifted = m.shl(a.bitLength() - m.bitLength())
        while (!shifted.isZero) {
            if (a >= shifted) a = a.minus(shifted)
            shifted = shifted.shr(1)
        }
        return a
    }

    fun modPow(exp: BigInt, mod: BigInt): BigInt {
        var result = ONE
        var base = this.mod(mod)
        for (i in exp.bitLength() - 1 downTo 0) {
            result = (result * result).mod(mod)
            if (exp.testBit(i)) result = (result * base).mod(mod)
        }
        return result
    }

    /** 与 java.math.BigInteger.toString(16) 一致：无前导零，空为 "0"。 */
    fun toHexString(): String {
        if (isZero) return "0"
        val totalBits = bitLength()
        val byteLen = (totalBits + 7) / 8
        val out = ByteArray(byteLen)
        for (i in limbs.indices) {
            for (bit in 0 until BITS) {
                if (limbs[i] and (1 shl bit) != 0) {
                    val bitIndex = i * BITS + bit
                    if (bitIndex < totalBits) {
                        out[byteLen - 1 - bitIndex / 8] =
                            (out[byteLen - 1 - bitIndex / 8].toInt() or (1 shl (bitIndex % 8))).toByte()
                    }
                }
            }
        }
        return out.dropWhile { it.toInt() == 0 }
            .joinToString("") { it.toUByte().toString(16).padStart(2, '0') }
            .ifEmpty { "0" }
    }

    companion object {
        private const val BITS = 30
        private const val MASK = (1L shl BITS) - 1
        val ZERO = BigInt(IntArray(0))
        val ONE = BigInt(intArrayOf(1))

        private fun of(limbs: IntArray): BigInt {
            var top = limbs.size
            while (top > 0 && limbs[top - 1] == 0) top--
            return BigInt(limbs.copyOf(top))
        }

        fun valueOf(value: Long): BigInt {
            if (value == 0L) return ZERO
            return of(intArrayOf((value and MASK).toInt(), ((value shr BITS) and MASK).toInt()))
        }

        /** 与 java.math.BigInteger(hex, 16) 语义一致（前导零忽略）。 */
        fun fromHex(hex: String): BigInt {
            val bytes = ByteArray((hex.length + 1) / 2)
            var byteIndex = 0
            var high = true
            for (c in hex) {
                val v = when (c) {
                    in '0'..'9' -> c - '0'
                    in 'a'..'f' -> c - 'a' + 10
                    in 'A'..'F' -> c - 'A' + 10
                    else -> throw IllegalArgumentException("非法十六进制字符: $c")
                }
                if (high) {
                    bytes[byteIndex] = (v shl 4).toByte(); high = false
                } else {
                    bytes[byteIndex] = (bytes[byteIndex].toInt() or v).toByte(); byteIndex++; high = true
                }
            }
            val limbCount = (bytes.size * 8 + BITS - 1) / BITS
            val out = IntArray(limbCount)
            for (i in bytes.indices) {
                val b = bytes[bytes.size - 1 - i].toLong() and 0xFF
                val bitPos = i * 8
                val limbIndex = bitPos / BITS
                val offset = bitPos % BITS
                val v = b shl offset
                out[limbIndex] = (out[limbIndex].toLong() or (v and MASK)).toInt()
                if (offset + 8 > BITS && limbIndex + 1 < out.size) {
                    out[limbIndex + 1] = (out[limbIndex + 1].toLong() or (v shr BITS)).toInt()
                }
            }
            return of(out)
        }
    }
}
