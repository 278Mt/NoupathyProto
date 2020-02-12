package com.jam.dentsu.noupathyproto

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.util.Log
import java.io.IOException
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.*
import kotlin.concurrent.schedule
import kotlin.experimental.and

class neuroNicleService private constructor(context: Context) {

    var isConnected = false
    var isFitting = false
    var calibTime = 60
    var noiseDetected = false
    var batteryAlert = false

    external fun judgeNoiseC(raw1: Int, raw2: Int): Boolean

    companion object {

        init {
            System.loadLibrary("noise_detector")
        }
        private var _instance: neuroNicleService? = null

//        private lateinit var context: Context

        private var listener: NNListener? = null
        interface NNListener {
            fun onDataReceived(Ch1: Int, Ch2: Int)
        }
        fun setListener(listener: NNListener?) {
            this.listener = listener
        }

        private val REQUEST_ENABLE_BT = 3

        internal var prev1 = 0
        internal var _prev1 = 0
        internal var _curt1 = 0

        internal var prev2 = 0
        internal var _prev2 = 0
        internal var _curt2 = 0

        internal var mBluetoothAdapter: BluetoothAdapter? = null

        internal var MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

        internal var btDevice: BluetoothDevice? = null
        internal var btSocket: BluetoothSocket? = null

        private class BTFoundDevice(val deviceMac: String, val deviceName: String)

        var times = mutableListOf<Date>()
        val format = SimpleDateFormat("yyyyMMdd-HH:mm:ss.SSS", Locale.getDefault())

        var Ch1 = 0
        var Ch2 = 0

        var pcd: Byte = 0
        var packetCount = 0

        var calib_count = 0

        fun onCreateApplication(applicationContext: Context) {

            _instance = neuroNicleService(applicationContext)
        }

        val instance: neuroNicleService
            get() {
                _instance?.let {
                    return it
                } ?: run {
                    throw RuntimeException("nnService should be initialized.")
                }
            }

//        fun setContext(con: Context) {
//            context=con
//        }

        fun StartNN() {
            mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
            if (mBluetoothAdapter == null) {
                // Bluetoothをサポートしていないデバイス
                //Toast.makeText(this, "Bluetoothを使用することができません", Toast.LENGTH_LONG).show()
                return
            }

            Timer().schedule(500, 1000) {

                if (!neuroNicleService.instance.isConnected) {
                    connectDevice()
                } else {
                    this.cancel()
                }
            }
        }

        fun connectDevice() {
            val pairedDevices = mBluetoothAdapter!!.bondedDevices
            if (pairedDevices.size > 0) { // 複数検出された場合

                for (device in pairedDevices) {
                    val deviceClass = BTFoundDevice(device.address, device.name)

                    if (deviceClass.deviceName == "neuroNicle E2") {
                        btDevice = mBluetoothAdapter!!.getRemoteDevice(deviceClass.deviceMac)
                        btSocket = null
                        try {
                            btSocket = btDevice?.createRfcommSocketToServiceRecord(MY_UUID)
                        } catch (e: IOException) {
                            e.printStackTrace()
                        }

                        try {
                            btSocket!!.connect()
                        } catch (connectException: IOException) {
                            connectException.printStackTrace()
                            try {
                                btSocket!!.close()
                            } catch (closeException: IOException) {
                            }

                            return
                        }

                        neuroNicleService.instance.isConnected = true

                        val connectedThread = ConnectedThread(btSocket!!)
                        connectedThread.start()
                    }
                }
            }
        }

        private fun BitJudge(data: Int, flag_place: Int): Boolean {
            try {
                val mask = 1 shl flag_place
    //            Log.d("bit",data.toString())
    //            Log.d("bit",mask.toString())
                val judge = data and mask != 0
                return judge
            } catch (e: IOException) {
                return false
            }
        }

        private class ConnectedThread(private val mmSocket: BluetoothSocket) : Thread() {
            private val mmInStream: InputStream?

            init {
                var tmpIn: InputStream? = null

                // BluetoothSocketの inputstream と outputstreamを得る
                try {
                    tmpIn = mmSocket.inputStream
                } catch (e: IOException) {
                    e.printStackTrace()
                }

                mmInStream = tmpIn
            }

            override fun run() {
                var buf = ""
                var Data: String
                val packet = StringBuffer()

                while (true) {

                    try {
                        // InputStreamから受け取った値を読む
                        Data = Integer.toHexString(mmInStream!!.read())
                        buf = Data
                        if (buf == "ff") {
                            Data = Integer.toHexString(mmInStream.read())
                            buf = Data
                            if (buf == "fe") {
                                val str = packet.toString()

                                //neuroNicleService.instance.isConnected = true

                                val arr = str.split(" ".toRegex()).dropLastWhile({ it.isEmpty() }).toTypedArray()
                                for (i in arr.indices) {

                                    if (i == 3) { // CRD_PUD2_PCDT
                                        try {
                                            val bit = Integer.parseInt(arr[i],16)

                                            if (bit == 56) {
                                                neuroNicleService.instance.isFitting = true

                                            } else {
                                                neuroNicleService.instance.isFitting = false
                                                calib_count = 0
                                                neuroNicleService.instance.calibTime = 60
                                            }
                                        } catch (e: IOException) {
                                            e.printStackTrace()
                                        }
                                    }
                                    if (i == 4) { // packetCount
                                        packetCount = Integer.parseInt(arr[i], 16)
                                    }

                                    if (i == 6) { // PCD
                                        pcd = Integer.parseInt(arr[i], 16).toByte()

                                        if(packetCount==0){
                                            neuroNicleService.instance.batteryAlert = BitJudge(Integer.parseInt(arr[i], 16),2)
                                        }
                                    }

                                    if (i == 7 || i == 8) { // Ch1
                                        if (prev1 == 0) {
                                            prev1 = 1
                                            _prev1 = Integer.parseInt(arr[i], 16)
                                            _prev1 = _prev1 * 256

                                        } else if (prev1 == 1) {
                                            prev1 = 0
                                            _curt1 = Integer.parseInt(arr[i], 16)
                                            Ch1 = _curt1 + _prev1

                                            if (neuroNicleService.instance.isFitting && neuroNicleService.instance.calibTime > 0) {
                                                calib_count++
                                                if (calib_count % 250 == 0) {

                                                    neuroNicleService.instance.calibTime--
                                                }
                                            }
                                        }
                                    }

                                    if (i == 9 || i == 10) { // Ch2
                                        if (prev2 == 0) {
                                            prev2 = 1
                                            _prev2 = Integer.parseInt(arr[i], 16)
                                            _prev2 = _prev2 * 256

                                        } else if (prev2 == 1) {
                                            prev2 = 0
                                            _curt2 = Integer.parseInt(arr[i], 16)
                                            Ch2 = _curt2 + _prev2

                                            if(listener != null) {
                                                // 値を通知
                                                listener!!.onDataReceived(Ch1, Ch2)
                                            }
                                        }
                                    }
                                }
                                neuroNicleService.instance.noiseDetected = neuroNicleService.instance.judgeNoiseC(Ch1, Ch2)
                                packet.setLength(0)
                                packet.append("ff fe")
                            } else {
                                packet.append(" ff")
                                packet.append(" $buf")
                            }
                        } else {
                            packet.append(" $buf")
                        }
                    } catch (e: IOException) {
                        println("切断")
                        neuroNicleService.instance.isConnected = false
                        connectDevice()

                        break
                    }

                }
            }
        }

        private fun getByteBinaryStr( byte: Byte ): String {

            var result = ""

            var _byte = byte

            var counter = java.lang.Byte.SIZE
            val mask: Byte = (0b10000000).toByte()

            while ( counter > 0 ) {

                val c = if ( _byte.and(mask) == mask ) '1' else '0'
                result += c

                _byte = _byte.toInt().shl(1).toByte()
                counter -= 1
            }

            return result
        }

        private fun getByteArrayFromInt(number: Int): Array<Byte> {

            val result = Array<Byte>(java.lang.Integer.BYTES, {0})
            var _number = number
            var mask = 0xFF // binary 1111 1111

            for (i in 0 until result.size) {

                result[i] = _number.and(mask).toByte()
                _number = _number.shr(8)
            }

            result.reverse()

            return result
        }

    }
}
