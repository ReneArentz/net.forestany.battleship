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
        val i_comSenderTimeoutMs = 10000
        val i_comReceiverTimeoutMs = 10000
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
                        } catch (o_exc: RuntimeException) {
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

                                if (a_deleteEntries.size > 0) {
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
                        } catch (o_exc: RuntimeException) {
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
                val e_type = net.forestany.forestj.lib.net.sock.com.Type.TCP_RECEIVE_WITH_ANSWER
                val o_communicationConfig = getCommunicationConfig(
                    "/",
                    e_type,
                    net.forestany.forestj.lib.net.sock.com.Cardinality.Equal,
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

                /* add receive socket task(s) */
                val o_receiveSocketTask: net.forestany.forestj.lib.net.sock.task.Task<*> = object: net.forestany.forestj.lib.net.sock.task.Task<java.net.ServerSocket?>(net.forestany.forestj.lib.net.sock.Type.TCP) {
                    override fun getSocketTaskClassType(): Class<*> {
                        return net.forestany.forestj.lib.net.sock.task.Task::class.java
                    }

                    override fun cloneFromOtherTask(p_o_sourceTask: net.forestany.forestj.lib.net.sock.task.Task<java.net.ServerSocket?>) {
                        this.cloneBasicFields(p_o_sourceTask)
                    }

                    @Throws(java.lang.Exception::class)
                    override fun runTask() {
                        try {
                            /* get request object */
                            val s_request = this.requestObject as String?

                            /* evaluate request */
                            if (s_request != null) {
                                net.forestany.forestj.lib.Global.ilog("message to server: '$s_request'")

                                // client request message
                                if ((s_request.startsWith("PING")) || (s_request.startsWith("SNACKBAR"))) {
                                    val a_requestParts = s_request.split("|")

                                    if (a_requestParts.size == 10) {
                                        // enqueue message for snackbar
                                        if (a_requestParts[0].startsWith("SNACKBAR")) {
                                            GlobalInstance.get().enqueueSnackbarBox(a_requestParts[0].substring(9))
                                        }

                                        // update other board state
                                        val otherBoardState: BattleshipActivity.BoardState = when (a_requestParts[5]) {
                                            "OWN_BOARD" -> BattleshipActivity.BoardState.OWN_BOARD
                                            "OTHER_BOARD" -> BattleshipActivity.BoardState.OTHER_BOARD
                                            "OTHER_BOARD_TARGET" -> BattleshipActivity.BoardState.OTHER_BOARD_TARGET
                                            "END" -> BattleshipActivity.BoardState.END
                                            else -> BattleshipActivity.BoardState.PLACEMENT
                                        }

                                        GlobalInstance.get().setOtherBoardState(otherBoardState)

                                        // update other user
                                        GlobalInstance.get().s_otherUser = a_requestParts[6]

                                        // update client grid, but not if player is placing a target at the moment
                                        if ((GlobalInstance.get().getBoardState() != BattleshipActivity.BoardState.OTHER_BOARD_TARGET) && (a_requestParts[8].length == 100)) {
                                            val otherGrid: Array<Array<CellState>> = Array(GRID_ROWS) { Array(GRID_COLS) { CellState.EMPTY } }

                                            var k = 0

                                            for (i in 0 ..< GRID_ROWS) {
                                                for (j in 0 ..< GRID_COLS) {
                                                    otherGrid[i][j] = when (a_requestParts[8][k++]) {
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

                                            if (!a_requestParts[8].contains("#")) {
                                                val otherGridNoTarget: Array<Array<CellState>> = Array(GRID_ROWS) { Array(GRID_COLS) { CellState.EMPTY } }

                                                k = 0

                                                for (i in 0 ..< GRID_ROWS) {
                                                    for (j in 0 ..< GRID_COLS) {
                                                        otherGridNoTarget[i][j] = when (a_requestParts[8][k++]) {
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
                                        if ((GlobalInstance.get().getBoardState() == BattleshipActivity.BoardState.END) && (a_requestParts[8].length == 100)) {
                                            GlobalInstance.get().setOtherGridEnd(a_requestParts[8])
                                        }
                                    } else {
                                        net.forestany.forestj.lib.Global.ilogSevere("invalid amount request parts: " + a_requestParts.size)
                                    }
                                } else if (s_request.startsWith("FIRE")) {
                                    val a_requestParts = s_request.split("|")

                                    if (a_requestParts.size == 10) {
                                        val a_commandParts = a_requestParts[0].split(";")

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
                                        } else {
                                            net.forestany.forestj.lib.Global.ilogSevere("invalid amount command parts: " + a_commandParts.size)
                                        }

                                        // update other grid in end state
                                        if ((GlobalInstance.get().getBoardState() == BattleshipActivity.BoardState.END) && (a_requestParts[8].length == 100)) {
                                            GlobalInstance.get().setOtherGridEnd(a_requestParts[8])
                                        }
                                    } else {
                                        net.forestany.forestj.lib.Global.ilogSevere("invalid amount request parts: " + a_requestParts.size)
                                    }
                                } else if (s_request.startsWith("ROUND_FINISHED")) {
                                    GlobalInstance.get().setBoardState(BattleshipActivity.BoardState.OTHER_BOARD_TARGET)
                                    GlobalInstance.get().setOtherBoardState(BattleshipActivity.BoardState.OWN_BOARD)
                                } else if (s_request.startsWith("EXIT")) {
                                    GlobalInstance.get().b_serverClosed = true
                                }
                            }

                            // prepare answer
                            var s_answer = "PING"

                            if ( (GlobalInstance.get().getMessageBoxAmount() > 0) ) {
                                val o_message: Any? = GlobalInstance.get().currentMessage()

                                if (o_message != null) {
                                    val s_message = o_message.toString()

                                    if (s_message.contentEquals("CLIENT_START")) {
                                        if (GlobalInstance.get().getOtherBoardState() != BattleshipActivity.BoardState.OTHER_BOARD_TARGET) {
                                            s_answer = s_message
                                        } else {
                                            GlobalInstance.get().dequeueMessageBox()
                                        }
                                    } else if (s_message.startsWith("FIRE")) {
                                        val a_messageParts = s_message.split(";")

                                        if (
                                            (GlobalInstance.get().getOtherGridCellState(a_messageParts[2].toInt(), a_messageParts[1].toInt()) == CellState.EMPTY) ||
                                            (GlobalInstance.get().getOtherGridCellState(a_messageParts[2].toInt(), a_messageParts[1].toInt()) == CellState.SHIP) ||
                                            (GlobalInstance.get().getOtherGridCellState(a_messageParts[2].toInt(), a_messageParts[1].toInt()) == CellState.TARGET)
                                        ) {
                                            s_answer = s_message
                                        } else {
                                            GlobalInstance.get().dequeueMessageBox()
                                        }
                                    } else if ((s_message.startsWith("SNACKBAR")) || (s_message.startsWith("ROUND_FINISHED")) || (s_message.startsWith("EXIT"))) {
                                        s_answer = s_message
                                        GlobalInstance.get().dequeueMessageBox()
                                    }
                                }
                            }

                            /* set answer object */
                            this.answerObject = Message(s_answer).toString()

                            net.forestany.forestj.lib.Global.ilog("answer set: '${this.answerObject}'")
                        } catch (o_exc: java.lang.Exception) {
                            net.forestany.forestj.lib.Global.logException(o_exc)
                        }
                    }
                }

                o_communicationConfig.addReceiveSocketTask(o_receiveSocketTask)

                GlobalInstance.get().o_communicationBattleship = net.forestany.forestj.lib.net.sock.com.Communication(o_communicationConfig)
                GlobalInstance.get().o_communicationBattleship?.start()
            }
            else /* CLIENT */
            {
                val e_type = net.forestany.forestj.lib.net.sock.com.Type.TCP_SEND_WITH_ANSWER
                val o_communicationConfig = getCommunicationConfig(
                    "/",
                    e_type,
                    net.forestany.forestj.lib.net.sock.com.Cardinality.Equal,
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
                        while (true) {
                            if (!GlobalInstance.get().b_isServer) { /* CLIENT only */
                                // prepare request
                                var s_request = ""

                                if ( (GlobalInstance.get().getMessageBoxAmount() > 0) ) {
                                    val o_message: Any? = GlobalInstance.get().currentMessage()

                                    if (o_message != null) {
                                        var s_message = o_message.toString()

                                        if (s_message.startsWith("FIRE")) {
                                            val a_messageParts = s_message.split(";")

                                            if (
                                                (GlobalInstance.get().getOtherGridCellState(a_messageParts[2].toInt(), a_messageParts[1].toInt()) == CellState.EMPTY) ||
                                                (GlobalInstance.get().getOtherGridCellState(a_messageParts[2].toInt(), a_messageParts[1].toInt()) == CellState.SHIP) ||
                                                (GlobalInstance.get().getOtherGridCellState(a_messageParts[2].toInt(), a_messageParts[1].toInt()) == CellState.TARGET)
                                            ) {
                                                s_message = Message(s_message).toString()
                                            } else {
                                                // dequeue message, and skip current iteration
                                                GlobalInstance.get().dequeueMessageBox()
                                                continue
                                            }
                                        } else if ((s_message.startsWith("SNACKBAR")) || (s_message.startsWith("ROUND_FINISHED")) || (s_message.startsWith("EXIT"))) {
                                            s_message = Message(s_message).toString()
                                            GlobalInstance.get().dequeueMessageBox()
                                        }

                                        // use current message of queue as server request
                                        s_request = s_message
                                    }
                                } else {
                                    // prepare ping message
                                    s_request = Message("PING").toString()
                                }

                                // send request
                                while (!GlobalInstance.get().o_communicationBattleship?.enqueue(s_request)!!) {
                                    net.forestany.forestj.lib.Global.ilogWarning("could not enqueue message")
                                }

                                net.forestany.forestj.lib.Global.ilog("message enqueued: '$s_request'")

                                // wait for answer
                                val o_answer: Any? = GlobalInstance.get().o_communicationBattleship?.dequeueWithWaitLoop(2500)

                                if (o_answer != null) {
                                    val s_answer = o_answer.toString()
                                    net.forestany.forestj.lib.Global.ilog("answer from server: $s_answer")

                                    if ((s_answer.startsWith("PING")) || (s_answer.startsWith("SNACKBAR"))) {
                                        val a_answerParts = s_answer.split("|")

                                        if (a_answerParts.size == 10) {
                                            // enqueue message for snackbar
                                            if (a_answerParts[0].startsWith("SNACKBAR")) {
                                                GlobalInstance.get().enqueueSnackbarBox(a_answerParts[0].substring(9))
                                            }

                                            // update game mode received from server
                                            GlobalInstance.get().s_gameMode = when(a_answerParts[1]) {
                                                GAME_MODE_ALTERNATE -> GAME_MODE_ALTERNATE
                                                GAME_MODE_SHOT_EACH_SHIP -> GAME_MODE_SHOT_EACH_SHIP
                                                GAME_MODE_3_SHOTS -> GAME_MODE_3_SHOTS
                                                GAME_MODE_5_SHOTS -> GAME_MODE_5_SHOTS
                                                else -> GAME_MODE_NO_MODE
                                            }

                                            // update game additional option one received from server
                                            GlobalInstance.get().b_gameAdditionalOptionOne = when(a_answerParts[2]) {
                                                "1" -> true
                                                else -> false
                                            }

                                            // update game additional option two received from server
                                            GlobalInstance.get().b_gameAdditionalOptionTwo = when(a_answerParts[3]) {
                                                "1" -> true
                                                else -> false
                                            }

                                            // update game fleet index received from server
                                            GlobalInstance.get().i_fleetIndex = when(a_answerParts[4]) {
                                                "0" -> 0
                                                "1" -> 1
                                                else -> 0
                                            }

                                            // update server board state
                                            val otherBoardState: BattleshipActivity.BoardState = when (a_answerParts[5]) {
                                                "OWN_BOARD" -> BattleshipActivity.BoardState.OWN_BOARD
                                                "OTHER_BOARD" -> BattleshipActivity.BoardState.OTHER_BOARD
                                                "OTHER_BOARD_TARGET" -> BattleshipActivity.BoardState.OTHER_BOARD_TARGET
                                                "END" -> BattleshipActivity.BoardState.END
                                                else -> BattleshipActivity.BoardState.PLACEMENT
                                            }

                                            GlobalInstance.get().setOtherBoardState(otherBoardState)

                                            // update other user
                                            GlobalInstance.get().s_otherUser = a_answerParts[6]

                                            // update server grid, but not if player is placing a target at the moment
                                            if ((GlobalInstance.get().getBoardState() != BattleshipActivity.BoardState.OTHER_BOARD_TARGET) && (a_answerParts[8].length == 100)) {
                                                val otherGrid: Array<Array<CellState>> = Array(GRID_ROWS) { Array(GRID_COLS) { CellState.EMPTY } }

                                                var k = 0

                                                for (i in 0 ..< GRID_ROWS) {
                                                    for (j in 0 ..< GRID_COLS) {
                                                        otherGrid[i][j] = when (a_answerParts[8][k++]) {
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

                                                if (!a_answerParts[8].contains("#")) {
                                                    val otherGridNoTarget: Array<Array<CellState>> = Array(GRID_ROWS) { Array(GRID_COLS) { CellState.EMPTY } }

                                                    k = 0

                                                    for (i in 0 ..< GRID_ROWS) {
                                                        for (j in 0 ..< GRID_COLS) {
                                                            otherGridNoTarget[i][j] = when (a_answerParts[8][k++]) {
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
                                            if ((GlobalInstance.get().getBoardState() == BattleshipActivity.BoardState.END) && (a_answerParts[8].length == 100)) {
                                                GlobalInstance.get().setOtherGridEnd(a_answerParts[8])
                                            }
                                        } else {
                                            net.forestany.forestj.lib.Global.ilogSevere("invalid amount answer parts: " + a_answerParts.size)
                                        }
                                    } else if (s_answer.startsWith("CLIENT_START")) {
                                        GlobalInstance.get().setBoardState(BattleshipActivity.BoardState.OTHER_BOARD_TARGET)
                                        GlobalInstance.get().setOtherBoardState(BattleshipActivity.BoardState.OWN_BOARD)
                                    } else if (s_answer.startsWith("ROUND_FINISHED")) {
                                        GlobalInstance.get().setBoardState(BattleshipActivity.BoardState.OTHER_BOARD_TARGET)
                                        GlobalInstance.get().setOtherBoardState(BattleshipActivity.BoardState.OWN_BOARD)
                                    } else if (s_answer.startsWith("EXIT")) {
                                        GlobalInstance.get().b_serverClosed = true
                                    } else if (s_answer.startsWith("FIRE")) {
                                        val a_answerParts = s_answer.split("|")

                                        if (a_answerParts.size == 10) {
                                            val a_commandParts = a_answerParts[0].split(";")

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
                                            } else {
                                                net.forestany.forestj.lib.Global.ilogSevere("invalid amount command parts: " + a_commandParts.size)
                                            }

                                            // update other grid in end state
                                            if ((GlobalInstance.get().getBoardState() == BattleshipActivity.BoardState.END) && (a_answerParts[8].length == 100)) {
                                                GlobalInstance.get().setOtherGridEnd(a_answerParts[8])
                                            }
                                        } else {
                                            net.forestany.forestj.lib.Global.ilogSevere("invalid amount answer parts: " + a_answerParts.size)
                                        }
                                    }
                                } else {
                                    net.forestany.forestj.lib.Global.ilogWarning("could not receive any answer data")
                                }
                            }

                            sleep(1000)
                        }
                    } catch (o_exc: RuntimeException) {
                        /* ignore if communication is not running */
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
}