package com.underwood.idgenerator

import com.underwood.config.TConfig

/**
 * 基于Twitter的Snowflake算法实现
 *
 * <br>
 * SnowFlake的结构如下(每部分用-分开):<br>
 * <br>
 * 0 - 0000000000 0000000000 0000000000 0000000000 0 - 00000 - 00000 - 000000000000 <br>
 * <br>
 * 1位标识，由于long基本类型在Java中是带符号的，最高位是符号位，正数是0，负数是1，所以id一般是正数，最高位是0<br>
 * <br>
 * 41位时间截(毫秒级)，注意，41位时间截不是存储当前时间的时间截，而是存储时间截的差值（当前时间截 - 开始时间截)
 * 得到的值），这里的的开始时间截，一般是我们的id生成器开始使用的时间，由我们程序来指定的（如下下面程序IdWorker类的startTime属性）。41位的时间截，可以使用69年，年T = (1L << 41) / (1000L * 60 * 60 * 24 * 365) = 69<br>
 * <br>
 * 10位的数据机器位，可以部署在1024个节点，包括5位datacenterId和5位workerId<br>
 * <br>
 * 12位序列，毫秒内的计数，12位的计数顺序号支持每个节点每毫秒(同一机器，同一时间截)产生4096个ID序号<br>
 * <br>
 * <br>
 * 加起来刚好64位，为一个Long型。<br>
 * SnowFlake的优点是，整体上按照时间自增排序，并且整个分布式系统内不会产生ID碰撞(由数据中心ID和机器ID作区分)，并且效率较高，经测试，SnowFlake每秒能够产生26万ID左右。
 *
 * @author underwood/tianqiyu119@126.com
 */
//TODO: 增加基因法生成器
object IDGenerator {
    /** 起始时间戳，用于用当前时间戳减去这个时间戳，算出偏移量  */
    private const val startTime = 1519740777809L
    /** workerId占用的位数5（表示只允许workId的范围为：0-1023） */
    private const val workerIdBits = 5L
    /** dataCenterId占用的位数：5  */
    private const val dataCenterIdBits = 5L
    /** 序列号占用的位数：12（表示只允许workId的范围为：0-4095） */
    private const val sequenceBits = 12L

    /** workerId可以使用的最大数值：31  */
    private const val maxWorkerId = -1L xor (-1L shl workerIdBits.toInt())
    /** dataCenterId可以使用的最大数值：31  */
    private const val maxDataCenterId = -1L xor (-1L shl dataCenterIdBits.toInt())

    private const val workerIdShift = sequenceBits
    private const val dataCenterIdShift = sequenceBits + workerIdBits
    private const val timestampLeftShift = sequenceBits + workerIdBits + dataCenterIdBits

    /** 用mask防止溢出:位与运算保证计算的结果范围始终是 0-4095  */
    private const val sequenceMask = -1L xor (-1L shl sequenceBits.toInt())

    private var workerId: Long = 0L
    private var dataCenterId: Long = 0L
    private var sequence = 0L
    private var lastTimestamp = -1L
    private var isClock = false

    private val lock = Object()

    @Synchronized fun nextId(): Long {
        if (workerId == 0L && dataCenterId == 0L) {
            init()
        }
        var timestamp = timeGen()
        if (timestamp < lastTimestamp) {
            val offset = lastTimestamp - timestamp
            if (offset <= 5) {
                try {
                    lock.wait(offset shl 1)
                    timestamp = timeGen()
                    if (timestamp < lastTimestamp) {
                        throw RuntimeException("Clock moved backwards. Refusing to generate id for $offset milliseconds")
                    }
                } catch (e: Exception) {
                    throw RuntimeException(e)
                }
            } else {
                throw RuntimeException("Clock moved backwards. Refusing to generate id for $offset milliseconds")
            }
        }
        // 解决跨毫秒生成ID序列号始终为偶数的缺陷:如果是同一时间生成的，则进行毫秒内序列
        if (lastTimestamp == timestamp) {
            // 通过位与运算保证计算的结果范围始终是 0-4095
            sequence = sequence + 1 and sequenceMask
            if (sequence == 0L) {
                timestamp = tilNextMillis(lastTimestamp)
            }
        } else {
            // 时间戳改变，毫秒内序列重置
            sequence = 0L
        }

        lastTimestamp = timestamp

        /*
         * 1.左移运算是为了将数值移动到对应的段(41、5、5，12那段因为本来就在最右，因此不用左移)
         * 2.然后对每个左移后的值(la、lb、lc、sequence)做位或运算，是为了把各个短的数据合并起来，合并成一个二进制数
         * 3.最后转换成10进制，就是最终生成的id
         */
        return timestamp - startTime shl timestampLeftShift.toInt() or
                (dataCenterId shl dataCenterIdShift.toInt()) or
                (workerId shl workerIdShift.toInt()) or
                sequence
    }

    private fun init() {
        val config = TConfig.instance()
        workerId = config.getLong("workerId", 24L)
        dataCenterId = config.getLong("dataCenterId", 24L)
        isClock = config.getBoolean("isClock", false)
        if (workerId > maxWorkerId || workerId < 0) {
            throw IllegalArgumentException("worker Id can't be greater than $maxWorkerId or less than 0")
        }
        if (dataCenterId > maxDataCenterId || dataCenterId < 0) {
            throw IllegalArgumentException("dataCenter Id can't be greater than $maxDataCenterId or less than 0")
        }
    }

    private fun timeGen(): Long {
        return if (isClock) SystemClock.now() else System.currentTimeMillis()
    }

    /**
     * 保证返回的毫秒数在参数之后(阻塞到下一个毫秒，直到获得新的时间戳)
     *
     * @param lastTimestamp
     * @return
     */
    private fun tilNextMillis(lastTimestamp: Long): Long {
        var timestamp = timeGen()
        while (timestamp <= lastTimestamp) {
            timestamp = timeGen()
        }
        return timestamp
    }
}