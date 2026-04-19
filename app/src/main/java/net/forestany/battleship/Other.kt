package net.forestany.battleship

import net.forestany.battleship.MainActivity.Companion.GAME_MODE_3_SHOTS
import net.forestany.battleship.MainActivity.Companion.GAME_MODE_5_SHOTS
import net.forestany.battleship.MainActivity.Companion.GAME_MODE_ALTERNATE
import net.forestany.battleship.MainActivity.Companion.GAME_MODE_NO_MODE
import net.forestany.battleship.MainActivity.Companion.GAME_MODE_SHOT_EACH_SHIP
import net.forestany.battleship.MainActivity.Companion.GRID_COLS
import net.forestany.battleship.MainActivity.Companion.GRID_ROWS
import net.forestany.battleship.game.BattleshipActivity
import net.forestany.battleship.game.BattleshipGridAdapter.CellState
import net.forestany.battleship.game.Message
import net.forestany.battleship.game.SoundManager

class Other {
    @Throws(java.lang.Exception::class)
    private fun getCommunicationConfig(
        p_s_currentDirectory: String,
        p_e_comType: net.forestany.forestj.lib.net.sock.com.Type,
        p_e_comCardinality: net.forestany.forestj.lib.net.sock.com.Cardinality,
        p_s_host: String,
        p_i_port: Int,
        p_s_localHost: String,
        p_i_localPort: Int,
        p_b_symmetricSecurity128: Boolean,
        p_b_symmetricSecurity256: Boolean,
        p_b_asymmetricSecurity: Boolean,
        p_b_highSecurity: Boolean,
        p_b_securityTrustAll: Boolean,
        p_b_useMarshalling: Boolean,
        p_b_useMarshallingWholeObject: Boolean,
        p_i_marshallingDataLengthInBytes: Int,
        p_b_marshallingUsePropertyMethods: Boolean,
        p_b_marshallingSystemUsesLittleEndian: Boolean
    ): net.forestany.forestj.lib.net.sock.com.Config {
        val s_resourcesDirectory = p_s_currentDirectory + net.forestany.forestj.lib.io.File.DIR + "certs" + net.forestany.forestj.lib.io.File.DIR

        if ( (p_b_asymmetricSecurity) && (!net.forestany.forestj.lib.io.File.folderExists(s_resourcesDirectory)) ) {
            throw Exception("cannot find directory '$s_resourcesDirectory' where files are needed for asymmetric security communication")
        } else if ((p_b_asymmetricSecurity) && (p_b_securityTrustAll)) {
            System.setProperty("javax.net.ssl.trustStore", s_resourcesDirectory + "all/TrustStore-all.p12")
            System.setProperty("javax.net.ssl.trustStorePassword", "123456")
        }

        val i_comAmount = 1
        val i_comMessageBoxLength = 1500
        val i_comSenderTimeoutMs = 5000
        val i_comReceiverTimeoutMs = 5000
        val i_comSenderIntervalMs = 25
        val i_comQueueTimeoutMs = 25
        val i_comUDPReceiveAckTimeoutMs = 300
        val i_comUDPSendAckTimeoutMs = 125
        val s_comSecretPassphrase = GlobalInstance.get().getPreferences()["tcp_common_passphrase"].toString()

        val o_communicationConfig = net.forestany.forestj.lib.net.sock.com.Config(p_e_comType, p_e_comCardinality)
        o_communicationConfig.socketReceiveType = net.forestany.forestj.lib.net.sock.recv.ReceiveType.SERVER

        if (p_e_comCardinality == net.forestany.forestj.lib.net.sock.com.Cardinality.EqualBidirectional) {
            o_communicationConfig.amountSockets = 1
            o_communicationConfig.amountMessageBoxes = 2
            o_communicationConfig.addMessageBoxLength(i_comMessageBoxLength)
            o_communicationConfig.addMessageBoxLength(i_comMessageBoxLength)
        } else {
            o_communicationConfig.amount = i_comAmount
            o_communicationConfig.addMessageBoxLength(i_comMessageBoxLength)
        }

        o_communicationConfig.addHostAndPort(java.util.AbstractMap.SimpleEntry(p_s_host, p_i_port))
        o_communicationConfig.senderTimeoutMilliseconds = i_comSenderTimeoutMs
        o_communicationConfig.receiverTimeoutMilliseconds = i_comReceiverTimeoutMs
        o_communicationConfig.senderIntervalMilliseconds = i_comSenderIntervalMs
        o_communicationConfig.queueTimeoutMilliseconds = i_comQueueTimeoutMs
        o_communicationConfig.udpReceiveAckTimeoutMilliseconds = i_comUDPReceiveAckTimeoutMs
        o_communicationConfig.udpSendAckTimeoutMilliseconds = i_comUDPSendAckTimeoutMs

        if (!net.forestany.forestj.lib.Helper.isStringEmpty(p_s_localHost)) {
            o_communicationConfig.localAddress = p_s_localHost
        }

        if (p_i_localPort > 0) {
            o_communicationConfig.localPort = p_i_localPort
        }

        if (p_b_symmetricSecurity128) {
            if (p_b_highSecurity) {
                o_communicationConfig.communicationSecurity = net.forestany.forestj.lib.net.sock.com.Security.SYMMETRIC_128_BIT_HIGH
            } else {
                o_communicationConfig.communicationSecurity = net.forestany.forestj.lib.net.sock.com.Security.SYMMETRIC_128_BIT_LOW
            }

            o_communicationConfig.commonSecretPassphrase = s_comSecretPassphrase
        } else if (p_b_symmetricSecurity256) {
            if (p_b_highSecurity) {
                o_communicationConfig.communicationSecurity = net.forestany.forestj.lib.net.sock.com.Security.SYMMETRIC_256_BIT_HIGH
            } else {
                o_communicationConfig.communicationSecurity = net.forestany.forestj.lib.net.sock.com.Security.SYMMETRIC_256_BIT_LOW
            }

            o_communicationConfig.commonSecretPassphrase = s_comSecretPassphrase
        } else if (p_b_asymmetricSecurity) {
            if (p_e_comType == net.forestany.forestj.lib.net.sock.com.Type.UDP_RECEIVE || p_e_comType == net.forestany.forestj.lib.net.sock.com.Type.UDP_RECEIVE_WITH_ACK || p_e_comType == net.forestany.forestj.lib.net.sock.com.Type.TCP_RECEIVE || p_e_comType == net.forestany.forestj.lib.net.sock.com.Type.TCP_RECEIVE_WITH_ANSWER) {
                o_communicationConfig.addSSLContextToList(
                    net.forestany.forestj.lib.Cryptography.createSSLContextWithOneCertificate(
                        s_resourcesDirectory + "server/KeyStore-srv.p12",
                        "123456",
                        "test_server2"
                    )
                )
                o_communicationConfig.communicationSecurity = net.forestany.forestj.lib.net.sock.com.Security.ASYMMETRIC
            } else {
                if (!p_b_securityTrustAll) {
                    o_communicationConfig.setTrustStoreProperties(
                        s_resourcesDirectory + "client/TrustStore-clt.p12",
                        "123456"
                    )
                } else {
                    o_communicationConfig.addSSLContextToList(
                        net.forestany.forestj.lib.Cryptography.createSSLContextWithOneCertificate(
                            s_resourcesDirectory + "client/KeyStore-clt.p12",
                            "123456",
                            "test_client"
                        )
                    )
                }

                o_communicationConfig.communicationSecurity = net.forestany.forestj.lib.net.sock.com.Security.ASYMMETRIC
            }
        }

        o_communicationConfig.useMarshalling = p_b_useMarshalling
        o_communicationConfig.useMarshallingWholeObject = p_b_useMarshallingWholeObject
        o_communicationConfig.marshallingDataLengthInBytes = p_i_marshallingDataLengthInBytes
        o_communicationConfig.marshallingUsePropertyMethods = p_b_marshallingUsePropertyMethods
        o_communicationConfig.marshallingSystemUsesLittleEndian = p_b_marshallingSystemUsesLittleEndian

        o_communicationConfig.debugNetworkTrafficOn = false

        return o_communicationConfig
    }

    @Throws(java.lang.Exception::class)
    fun netLobby(
        udpMulticastIp: String,
        udpMulticastPort: Int,
        udpMulticastTTL: Int,
        gameName: String,
        localIp: String,
        serverPort: Int
    ) {
        if (GlobalInstance.get().o_communicationLobby != null) {
            try {
                GlobalInstance.get().o_communicationLobby?.stop()
            } catch (_: Exception) {

            }
        }

        GlobalInstance.get().o_communicationLobby = null

        val b_symmetricSecurity128 = false
        val b_symmetricSecurity256 = (GlobalInstance.get().getPreferences()["tcp_encryption"] as Boolean)
        val b_asymmetricSecurity = false
        val b_highSecurity = false
        val b_securityTrustAll = false

        val b_useMarshalling = true
        val b_useMarshallingWholeObject = false
        val i_marshallingDataLengthInBytes = 2
        val b_marshallingUsePropertyMethods = false
        val b_marshallingSystemUsesLittleEndian = false

        try {
            /* interrupt and null thread lobby if it is still running */
            if (GlobalInstance.get().o_threadLobby != null) {
                try {
                    GlobalInstance.get().o_threadLobby?.interrupt()
                    GlobalInstance.get().o_threadLobby = null
                } catch (_: Exception) {

                }
            }

            if (GlobalInstance.get().b_isServer) { /* SERVER */
                val e_type = net.forestany.forestj.lib.net.sock.com.Type.UDP_MULTICAST_SENDER
                val o_communicationConfig = getCommunicationConfig(
                    "/",
                    e_type,
                    net.forestany.forestj.lib.net.sock.com.Cardinality.Equal,
                    udpMulticastIp,
                    udpMulticastPort,
                    "",
                    0,
                    b_symmetricSecurity128,
                    b_symmetricSecurity256,
                    b_asymmetricSecurity,
                    b_highSecurity,
                    b_securityTrustAll,
                    b_useMarshalling,
                    b_useMarshallingWholeObject,
                    i_marshallingDataLengthInBytes,
                    b_marshallingUsePropertyMethods,
                    b_marshallingSystemUsesLittleEndian
                )
                o_communicationConfig.udpMulticastSenderTTL = udpMulticastTTL
                GlobalInstance.get().o_communicationLobby = net.forestany.forestj.lib.net.sock.com.Communication(o_communicationConfig)
                GlobalInstance.get().o_communicationLobby?.start()

                GlobalInstance.get().o_threadLobby = object : Thread() {
                    override fun run() {
                        try {
                            while (true) {
                                while (!GlobalInstance.get().o_communicationLobby?.enqueue( ("$gameName|$localIp:$serverPort") )!!) {
                                    net.forestany.forestj.lib.Global.ilog("could not enqueue message")
                                }

                                net.forestany.forestj.lib.Global.ilog("message enqueued: '$gameName|$localIp:$serverPort'")
                                sleep(1000)
                            }
                        } catch (_: RuntimeException) {
                            /* ignore if communication is not running */
                        } catch (o_exc: java.lang.Exception) {
                            net.forestany.forestj.lib.Global.logException(o_exc)
                        }
                    }
                }

                GlobalInstance.get().o_threadLobby?.start()
            }
            else /* CLIENT */
            {
                val e_type = net.forestany.forestj.lib.net.sock.com.Type.UDP_MULTICAST_RECEIVER
                val o_communicationConfig = getCommunicationConfig(
                    "/",
                    e_type,
                    net.forestany.forestj.lib.net.sock.com.Cardinality.Equal,
                    udpMulticastIp,
                    udpMulticastPort,
                    "",
                    0,
                    b_symmetricSecurity128,
                    b_symmetricSecurity256,
                    b_asymmetricSecurity,
                    b_highSecurity,
                    b_securityTrustAll,
                    b_useMarshalling,
                    b_useMarshallingWholeObject,
                    i_marshallingDataLengthInBytes,
                    b_marshallingUsePropertyMethods,
                    b_marshallingSystemUsesLittleEndian
                )
                o_communicationConfig.udpMulticastReceiverNetworkInterfaceName = GlobalInstance.get().getPreferences()["udp_network_interface_name"].toString()
                GlobalInstance.get().o_communicationLobby = net.forestany.forestj.lib.net.sock.com.Communication(o_communicationConfig)
                GlobalInstance.get().o_communicationLobby?.start()

                GlobalInstance.get().o_threadLobby = object : Thread() {
                    override fun run() {
                        try {
                            while (true) {
                                val a_deleteEntries: MutableList<java.time.LocalDateTime> = ArrayList()

                                for ((o_key) in GlobalInstance.get().getClientLobbyEntries()) {
                                    if (java.time.Duration.between( o_key, java.time.LocalDateTime.now() ).seconds > 30) {
                                        a_deleteEntries.add(o_key)
                                    }
                                }

                                if (a_deleteEntries.isNotEmpty()) {
                                    for (o_key in a_deleteEntries) {
                                        GlobalInstance.get().removeClientLobbyEntry(o_key)
                                    }
                                }

                                var s_connectionInfo: String?

                                do {
                                    s_connectionInfo = GlobalInstance.get().o_communicationLobby?.dequeue() as String?

                                    if (s_connectionInfo != null) {
                                        net.forestany.forestj.lib.Global.ilog("message received: '$s_connectionInfo'")

                                        if (!s_connectionInfo.contains(":")) {
                                            continue
                                        }

                                        val i_readingPort = s_connectionInfo.split(":".toRegex())
                                            .dropLastWhile { it.isEmpty() }
                                            .toTypedArray()[1].toInt()

                                        if (i_readingPort != serverPort) {
                                            continue
                                        }

                                        if (!GlobalInstance.get().getClientLobbyEntries().containsValue(s_connectionInfo)) {
                                            GlobalInstance.get().addClientLobbyEntry( java.time.LocalDateTime.now(), s_connectionInfo )
                                        }
                                    }
                                } while (s_connectionInfo != null)

                                sleep(1000)
                            }
                        } catch (_: RuntimeException) {
                            /* ignore if communication is not running */
                        } catch (o_exc: java.lang.Exception) {
                            net.forestany.forestj.lib.Global.logException(o_exc)
                        }
                    }
                }

                GlobalInstance.get().o_threadLobby?.start()
            }
        } catch (o_exc: java.lang.Exception) {
            net.forestany.forestj.lib.Global.logException(o_exc)
        }
    }

    @Throws(java.lang.Exception::class)
    fun netBattleship(
        serverIp: String,
        serverPort: Int
    ) {
        if (GlobalInstance.get().o_communicationBattleship != null) {
            try {
                GlobalInstance.get().o_communicationBattleship?.stop()
                GlobalInstance.get().o_communicationBattleship = null
            } catch (_: java.lang.Exception) {

            }
        }

        val b_symmetricSecurity128 = false
        val b_symmetricSecurity256 = (GlobalInstance.get().getPreferences()["tcp_encryption"] as Boolean)
        val b_asymmetricSecurity = false
        val b_highSecurity = false
        val b_securityTrustAll = false

        val b_useMarshalling = true
        val b_useMarshallingWholeObject = false
        val i_marshallingDataLengthInBytes = 2
        val b_marshallingUsePropertyMethods = false
        val b_marshallingSystemUsesLittleEndian = false

        try {
            if (GlobalInstance.get().b_isServer) { /* SERVER */
                val e_type = net.forestany.forestj.lib.net.sock.com.Type.TCP_RECEIVE
                val o_communicationConfig = getCommunicationConfig(
                    "/",
                    e_type,
                    net.forestany.forestj.lib.net.sock.com.Cardinality.EqualBidirectional,
                    serverIp,
                    serverPort,
                    "",
                    0,
                    b_symmetricSecurity128,
                    b_symmetricSecurity256,
                    b_asymmetricSecurity,
                    b_highSecurity,
                    b_securityTrustAll,
                    b_useMarshalling,
                    b_useMarshallingWholeObject,
                    i_marshallingDataLengthInBytes,
                    b_marshallingUsePropertyMethods,
                    b_marshallingSystemUsesLittleEndian
                )

                o_communicationConfig.setReceiverTimeoutMilliseconds(GlobalInstance.get().getPreferences()["bidirectional_timeout"].toString().toInt())
                o_communicationConfig.setSenderTimeoutMilliseconds(GlobalInstance.get().getPreferences()["bidirectional_timeout"].toString().toInt())

                GlobalInstance.get().o_communicationBattleship = net.forestany.forestj.lib.net.sock.com.Communication(o_communicationConfig)
                GlobalInstance.get().o_communicationBattleship?.start()
            }
            else /* CLIENT */
            {
                val e_type = net.forestany.forestj.lib.net.sock.com.Type.TCP_SEND
                val o_communicationConfig = getCommunicationConfig(
                    "/",
                    e_type,
                    net.forestany.forestj.lib.net.sock.com.Cardinality.EqualBidirectional,
                    serverIp,
                    serverPort,
                    "",
                    0,
                    b_symmetricSecurity128,
                    b_symmetricSecurity256,
                    b_asymmetricSecurity,
                    b_highSecurity,
                    b_securityTrustAll,
                    b_useMarshalling,
                    b_useMarshallingWholeObject,
                    i_marshallingDataLengthInBytes,
                    b_marshallingUsePropertyMethods,
                    b_marshallingSystemUsesLittleEndian
                )

                o_communicationConfig.setReceiverTimeoutMilliseconds(GlobalInstance.get().getPreferences()["bidirectional_timeout"].toString().toInt())
                o_communicationConfig.setSenderTimeoutMilliseconds(GlobalInstance.get().getPreferences()["bidirectional_timeout"].toString().toInt())

                GlobalInstance.get().o_communicationBattleship = net.forestany.forestj.lib.net.sock.com.Communication(o_communicationConfig)
                GlobalInstance.get().o_communicationBattleship?.start()
            }

            /* interrupt and null thread battleship if it is still running */
            if (GlobalInstance.get().o_threadBattleship != null) {
                try {
                    GlobalInstance.get().o_threadBattleship?.interrupt()
                    GlobalInstance.get().o_threadBattleship = null
                } catch (_: java.lang.Exception) {

                }
            }

            GlobalInstance.get().o_threadBattleship = object : Thread() {
                override fun run() {
                    try {
                        //val i_messageLogLength = 100

                        // SERVER receives first
                        var b_receive = true

                        if (!GlobalInstance.get().b_isServer) { // CLIENT is not receiving first
                            b_receive = false
                        }

                        while (true) {
                            try {
                                if (b_receive) {
                                    val start = System.currentTimeMillis()
                                    val o_incomingMessage: Any? = GlobalInstance.get().o_communicationBattleship?.dequeueWithWaitLoop(GlobalInstance.get().getPreferences()["communication_wait"].toString().toInt())
                                    val end = System.currentTimeMillis()
                                    GlobalInstance.get().setPing(end - start)

                                    //if (o_incomingMessage != null) {
                                    //    android.util.Log.v("BattleshipOther", "recv: ${o_incomingMessage.toString().substring(0, i_messageLogLength)}" + "\tout: ${GlobalInstance.get().getMessageBoxAmount()}\t" + GlobalInstance.get().getPing() + "ms")
                                    //}// else {
                                    //    android.util.Log.v("BattleshipOther", "recv: null" + "\tout: ${GlobalInstance.get().getMessageBoxAmount()}\t" + GlobalInstance.get().getPing() + "ms")
                                    //}

                                    receive(o_incomingMessage)

                                    b_receive = false
                                } else {
                                    val s_outgoingMessage = send()

                                    while (!GlobalInstance.get().o_communicationBattleship?.enqueue(s_outgoingMessage)!!) {
                                        net.forestany.forestj.lib.Global.ilogWarning("could not enqueue message")
                                    }

                                    //android.util.Log.v("BattleshipOther", "send: ${s_outgoingMessage.substring(0, i_messageLogLength)}\tout: ${GlobalInstance.get().getMessageBoxAmount()}")

                                    b_receive = true

                                    //sleep(500)
                                }
                            } catch (o_exc: java.lang.Exception) {
                                net.forestany.forestj.lib.Global.logException(o_exc)
                            }
                        }
                    } catch (o_exc: java.lang.Exception) {
                        net.forestany.forestj.lib.Global.logException(o_exc)
                    }
                }
            }

            GlobalInstance.get().o_threadBattleship?.start()
        } catch (o_exc: java.lang.Exception) {
            net.forestany.forestj.lib.Global.logException(o_exc)
        }
    }

    @Throws(java.lang.Exception::class)
    private fun receive(p_o_incomingMessage: Any?) {
        if (p_o_incomingMessage == null) {
            return
        }

        val s_request = p_o_incomingMessage.toString()

        net.forestany.forestj.lib.Global.ilog("incoming message: '$s_request'")

        // PING or SNACKBAR message from other side
        if ((s_request.startsWith("PING")) || (s_request.startsWith("SNACKBAR")))
        {
            val a_messageParts = s_request.split("|")

            if (a_messageParts.size == 10) {
                // enqueue message for snackbar
                if (a_messageParts[0].startsWith("SNACKBAR")) {
                    GlobalInstance.get().enqueueSnackbarBox(a_messageParts[0].substring(9))
                }

                // only client
                if (!GlobalInstance.get().b_isServer) {
                    // update game mode received from server
                    GlobalInstance.get().s_gameMode = when(a_messageParts[1]) {
                        GAME_MODE_ALTERNATE -> GAME_MODE_ALTERNATE
                        GAME_MODE_SHOT_EACH_SHIP -> GAME_MODE_SHOT_EACH_SHIP
                        GAME_MODE_3_SHOTS -> GAME_MODE_3_SHOTS
                        GAME_MODE_5_SHOTS -> GAME_MODE_5_SHOTS
                        else -> GAME_MODE_NO_MODE
                    }

                    // update game additional option one received from server
                    GlobalInstance.get().b_gameAdditionalOptionOne = when(a_messageParts[2]) {
                        "1" -> true
                        else -> false
                    }

                    // update game additional option two received from server
                    GlobalInstance.get().b_gameAdditionalOptionTwo = when(a_messageParts[3]) {
                        "1" -> true
                        else -> false
                    }

                    // update game fleet index received from server
                    GlobalInstance.get().i_fleetIndex = when(a_messageParts[4]) {
                        "0" -> 0
                        "1" -> 1
                        else -> 0
                    }
                }

                // update other board state
                val otherBoardState: BattleshipActivity.BoardState = when (a_messageParts[5]) {
                    "PLACEMENT_FINISHED_SERVER" -> BattleshipActivity.BoardState.PLACEMENT_FINISHED_SERVER
                    "PLACEMENT_FINISHED_CLIENT" -> BattleshipActivity.BoardState.PLACEMENT_FINISHED_CLIENT
                    "ROUND_SERVER" -> BattleshipActivity.BoardState.ROUND_SERVER
                    "ROUND_CLIENT" -> BattleshipActivity.BoardState.ROUND_CLIENT
                    "ROUND_SERVER_TARGET" -> BattleshipActivity.BoardState.ROUND_SERVER_TARGET
                    "ROUND_CLIENT_TARGET" -> BattleshipActivity.BoardState.ROUND_CLIENT_TARGET
                    "ROUND_SERVER_FINISHED" -> BattleshipActivity.BoardState.ROUND_SERVER_FINISHED
                    "ROUND_CLIENT_FINISHED" -> BattleshipActivity.BoardState.ROUND_CLIENT_FINISHED
                    "ROUND_SERVER_KEEP" -> BattleshipActivity.BoardState.ROUND_SERVER_KEEP
                    "ROUND_CLIENT_KEEP" -> BattleshipActivity.BoardState.ROUND_CLIENT_KEEP
                    "END" -> BattleshipActivity.BoardState.END
                    else -> BattleshipActivity.BoardState.PLACEMENT
                }

                GlobalInstance.get().setOtherBoardState(otherBoardState)

                // update other user
                GlobalInstance.get().s_otherUser = a_messageParts[6]

                // own board state - a_requestParts[7] -> nothing to do

                // update other grid, but not if player is placing a target at the moment
                if (
                    (!(
                        (((GlobalInstance.get().getOtherBoardState() == BattleshipActivity.BoardState.ROUND_SERVER_TARGET) || (GlobalInstance.get().getOtherBoardState() == BattleshipActivity.BoardState.ROUND_SERVER)) && (GlobalInstance.get().b_isServer)) ||
                        (((GlobalInstance.get().getOtherBoardState() == BattleshipActivity.BoardState.ROUND_CLIENT_TARGET) || (GlobalInstance.get().getOtherBoardState() == BattleshipActivity.BoardState.ROUND_CLIENT))) && (!GlobalInstance.get().b_isServer))
                    )
                    && (a_messageParts[8].length == 100)
                ) {
                    val otherGrid: Array<Array<CellState>> = Array(GRID_ROWS) { Array(GRID_COLS) { CellState.EMPTY } }

                    var k = 0

                    for (i in 0 ..< GRID_ROWS) {
                        for (j in 0 ..< GRID_COLS) {
                            otherGrid[i][j] = when (a_messageParts[8][k++]) {
                                '~' -> CellState.EMPTY
                                '.' -> CellState.MISS
                                'o' -> CellState.SHIP
                                'x' -> CellState.HIT
                                '#' -> CellState.TARGET
                                'q', 'w', 'e', 'r', 'f', 'v' -> CellState.SHIP
                                'a', 's', 'd', 't', 'g', 'b' -> CellState.HIT
                                else -> CellState.EMPTY
                            }
                        }
                    }

                    GlobalInstance.get().setOtherGrid(otherGrid)

                    if (!a_messageParts[8].contains("#")) {
                        val otherGridNoTarget: Array<Array<CellState>> = Array(GRID_ROWS) { Array(GRID_COLS) { CellState.EMPTY } }

                        k = 0

                        for (i in 0 ..< GRID_ROWS) {
                            for (j in 0 ..< GRID_COLS) {
                                otherGridNoTarget[i][j] = when (a_messageParts[8][k++]) {
                                    '~' -> CellState.EMPTY
                                    '.' -> CellState.MISS
                                    'o' -> CellState.SHIP
                                    'x' -> CellState.HIT
                                    '#' -> CellState.TARGET
                                    else -> CellState.EMPTY
                                }
                            }
                        }

                        GlobalInstance.get().setOtherGridNoTarget(otherGridNoTarget)
                    }
                }

                // update other grid in end state
                if ((GlobalInstance.get().getBoardState() == BattleshipActivity.BoardState.END) && (a_messageParts[8].length == 100)) {
                    GlobalInstance.get().setOtherGridEnd(a_messageParts[8])
                }
            } else {
                net.forestany.forestj.lib.Global.ilogSevere("invalid amount message parts: " + a_messageParts.size)
            }
        }
        else if (s_request.startsWith("FIRE")) // FIRE message from other side
        {
            val a_messageParts = s_request.split("|")

            if (a_messageParts.size == 10) {
                val a_commandParts = a_messageParts[0].split(";")

                if (a_commandParts.size == 3) {
                    val x = a_commandParts[1].toInt()
                    val y = a_commandParts[2].toInt()

                    if (GlobalInstance.get().getOwnGridCellState(y, x) == CellState.EMPTY) {
                        GlobalInstance.get().setOwnGridCellState(y, x, CellState.MISS)
                        SoundManager.playSound(2)
                    } else if (GlobalInstance.get().getOwnGridCellState(y, x) == CellState.SHIP) {
                        GlobalInstance.get().setOwnGridCellState(y, x, CellState.HIT)
                        SoundManager.playSound(3)
                    }

                    // passive player changes turn state, only place where setBoardState is used in this class
                    if ((GlobalInstance.get().getBoardState() == BattleshipActivity.BoardState.ROUND_CLIENT_TARGET) && (GlobalInstance.get().b_isServer)) {
                        GlobalInstance.get().setBoardState(BattleshipActivity.BoardState.ROUND_CLIENT)
                    } else if ((GlobalInstance.get().getBoardState() == BattleshipActivity.BoardState.ROUND_CLIENT) && (GlobalInstance.get().b_isServer)) {
                        GlobalInstance.get().setBoardState(BattleshipActivity.BoardState.ROUND_CLIENT_FINISHED)
                    } else if ((GlobalInstance.get().getBoardState() == BattleshipActivity.BoardState.ROUND_SERVER_TARGET) && (!GlobalInstance.get().b_isServer)) {
                        GlobalInstance.get().setBoardState(BattleshipActivity.BoardState.ROUND_SERVER)
                    } else if ((GlobalInstance.get().getBoardState() == BattleshipActivity.BoardState.ROUND_SERVER) && (!GlobalInstance.get().b_isServer)) {
                        GlobalInstance.get().setBoardState(BattleshipActivity.BoardState.ROUND_SERVER_FINISHED)
                    }
                } else {
                    net.forestany.forestj.lib.Global.ilogSevere("invalid amount command parts: " + a_commandParts.size)
                }
            } else {
                net.forestany.forestj.lib.Global.ilogSevere("invalid amount message parts: " + a_messageParts.size)
            }
        }
        else if (s_request.startsWith("EXIT")) // EXIT message from other side
        {
            GlobalInstance.get().b_serverClosed = true
        }
    }

    @Throws(java.lang.Exception::class)
    private fun send(): String {
        // prepare ping message
        var s_outgoingMessage = "PING"

        if ( (GlobalInstance.get().getMessageBoxAmount() > 0) ) {
            val o_message: Any? = GlobalInstance.get().currentMessage()

            if (o_message != null) {
                val s_message = o_message.toString()

                if (s_message.startsWith("FIRE"))
                {
                    val a_messageParts = s_message.split(";")

                    // check if FIRE has already been evaluated
                    if (
                        (GlobalInstance.get().getOtherGridCellState(a_messageParts[2].toInt(), a_messageParts[1].toInt()) == CellState.EMPTY) ||
                        (GlobalInstance.get().getOtherGridCellState(a_messageParts[2].toInt(), a_messageParts[1].toInt()) == CellState.SHIP) ||
                        (GlobalInstance.get().getOtherGridCellState(a_messageParts[2].toInt(), a_messageParts[1].toInt()) == CellState.TARGET)
                    ) {
                        // do not dequeue FIRE message until we got answer from other side
                        s_outgoingMessage = s_message
                    } else if (
                        ((GlobalInstance.get().getOtherBoardState() == BattleshipActivity.BoardState.ROUND_SERVER_FINISHED) && (GlobalInstance.get().b_isServer)) ||
                        ((GlobalInstance.get().getOtherBoardState() == BattleshipActivity.BoardState.ROUND_CLIENT_FINISHED) && (!GlobalInstance.get().b_isServer))
                    ) {
                        // HIT or MISS and other side approved end of turn, so we can now dequeue FIRE message
                        GlobalInstance.get().dequeueMessageBox()
                    }
                } else if (
                    (s_message.startsWith("SNACKBAR")) ||
                    (s_message.startsWith("EXIT"))
                ) {
                    s_outgoingMessage = s_message
                    GlobalInstance.get().dequeueMessageBox()
                }
            }
        }

        return Message(s_outgoingMessage).toString()
    }
}