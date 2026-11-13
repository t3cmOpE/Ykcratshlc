package com.notiflog.app.utils

import android.content.Context
import android.os.Build
import android.provider.Settings
import com.notiflog.app.database.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import org.json.JSONObject
import java.io.*
import java.net.*
import java.util.UUID

/** Sync NotifLog <-> NotifLog di WiFi lokal yang sama. Hanya teks/metadata, tanpa server luar. */
object SyncManager {
    const val PORT = 8765
    const val DPORT = 8766
    class Req(val name: String, val model: String, val decision: CompletableDeferred<Boolean>)

    val code = MutableStateFlow<String?>(null)
    val qr = MutableStateFlow<String?>(null)
    val request = MutableStateFlow<Req?>(null)

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val jobs = mutableListOf<Job>()
    private var server: ServerSocket? = null
    private var udp: DatagramSocket? = null

    fun deviceName(ctx: Context): String =
        Settings.Global.getString(ctx.contentResolver, "device_name") ?: Build.MODEL

    fun localIp(): String? = try {
        NetworkInterface.getNetworkInterfaces().toList().flatMap { it.inetAddresses.toList() }
            .firstOrNull { !it.isLoopbackAddress && it is Inet4Address && it.isSiteLocalAddress }?.hostAddress
    } catch (e: Exception) { null }

    // ---------- HP utama (host) ----------
    fun startHost(ctx: Context) {
        stopHost()
        val c = (100000..999999).random().toString()
        code.value = c
        qr.value = "notiflog://pair?h=${localIp()}&c=$c"
        val dao = AppDb.get(ctx).dao()
        val myName = deviceName(ctx)
        jobs += scope.launch {
            try {
                val u = DatagramSocket(DPORT); udp = u
                val buf = ByteArray(256)
                while (isActive) {
                    val p = DatagramPacket(buf, buf.size); u.receive(p)
                    if (String(p.data, 0, p.length) == "NLQ:$c") {
                        val r = "NLR:$myName".toByteArray()
                        u.send(DatagramPacket(r, r.size, p.address, p.port))
                    }
                }
            } catch (e: Exception) { }
        }
        jobs += scope.launch {
            try {
                val s = ServerSocket(PORT); server = s
                while (isActive) { val cl = s.accept(); launch { handle(cl, c, dao, myName) } }
            } catch (e: Exception) { }
        }
    }

    fun stopHost() {
        try { server?.close() } catch (e: Exception) { }
        try { udp?.close() } catch (e: Exception) { }
        jobs.forEach { it.cancel() }; jobs.clear()
        code.value = null; qr.value = null; request.value = null
    }

    private suspend fun handle(cl: Socket, c: String, dao: NotifDao, myName: String) {
        try {
            cl.use { s ->
                s.soTimeout = 90000
                val a = s.inetAddress
                if (!(a.isSiteLocalAddress || a.isLoopbackAddress)) return
                val r = BufferedReader(InputStreamReader(s.getInputStream()))
                val w = PrintWriter(OutputStreamWriter(s.getOutputStream()), true)
                val p = (r.readLine() ?: return).split("\t")
                when (p[0]) {
                    "HELLO" -> {
                        if (p.getOrNull(1) != c) { w.println("NO"); return }
                        val name = p.getOrElse(2) { "HP" }; val model = p.getOrElse(3) { "" }
                        val d = CompletableDeferred<Boolean>()
                        request.value = Req(name, model, d)           // izin dari HP utama
                        val ok = withTimeoutOrNull(60000) { d.await() } ?: false
                        request.value = null
                        if (ok) {
                            val tk = UUID.randomUUID().toString()
                            dao.upsertDevice(DeviceEntity(tk.take(8), name, model, null, tk, System.currentTimeMillis()))
                            w.println("OK\t$tk\t$myName\t${Build.MODEL}")
                        } else w.println("NO")
                    }
                    "PULL" -> {
                        val dev = dao.deviceByToken(p.getOrNull(1) ?: "")
                        if (dev == null) { w.println("NO"); return }
                        dao.since(p.getOrNull(2)?.toLongOrNull() ?: 0L).forEach { w.println(toJson(it).toString()) }
                        w.println("END")
                        dao.upsertDevice(dev.copy(lastActive = System.currentTimeMillis()))
                    }
                }
            }
        } catch (e: Exception) { }
    }

    // ---------- HP kedua (klien) ----------
    suspend fun discover(c: String): String? = withContext(Dispatchers.IO) {
        try {
            DatagramSocket().use { s ->
                s.broadcast = true; s.soTimeout = 3000
                val b = "NLQ:$c".toByteArray()
                s.send(DatagramPacket(b, b.size, InetAddress.getByName("255.255.255.255"), DPORT))
                val p = DatagramPacket(ByteArray(256), 256); s.receive(p)
                p.address.hostAddress
            }
        } catch (e: Exception) { null }
    }

    suspend fun pair(ctx: Context, host: String, c: String): String = withContext(Dispatchers.IO) {
        try {
            Socket().use { s ->
                s.connect(InetSocketAddress(host, PORT), 5000); s.soTimeout = 70000
                val w = PrintWriter(OutputStreamWriter(s.getOutputStream()), true)
                w.println("HELLO\t$c\t${deviceName(ctx)}\t${Build.MODEL}")
                val r = BufferedReader(InputStreamReader(s.getInputStream())).readLine()?.split("\t")
                if (r != null && r[0] == "OK") {
                    val dev = DeviceEntity(r[1].take(8), r.getOrElse(2) { "HP utama" }, r.getOrElse(3) { "" }, host, r[1], System.currentTimeMillis())
                    AppDb.get(ctx).dao().upsertDevice(dev)
                    "Terhubung dengan ${dev.name}"
                } else "Ditolak oleh HP utama atau kode salah"
            }
        } catch (e: Exception) { "Gagal terhubung: pastikan kedua HP di WiFi yang sama" }
    }

    suspend fun pull(ctx: Context, d: DeviceEntity): String = withContext(Dispatchers.IO) {
        val host = d.host ?: return@withContext "Perangkat ini yang menjadi HP utama; sinkron dilakukan dari HP kedua"
        try {
            val dao = AppDb.get(ctx).dao()
            var n = 0
            Socket().use { s ->
                s.connect(InetSocketAddress(host, PORT), 5000); s.soTimeout = 30000
                PrintWriter(OutputStreamWriter(s.getOutputStream()), true).println("PULL\t${d.token}\t0")
                val r = BufferedReader(InputStreamReader(s.getInputStream()))
                while (true) {
                    val line = r.readLine() ?: break
                    if (line == "END") break
                    if (line == "NO") return@withContext "HP utama menolak (sudah diputuskan?)"
                    if (dao.insert(fromJson(JSONObject(line))) != -1L) n++
                }
            }
            dao.upsertDevice(d.copy(lastActive = System.currentTimeMillis()))
            "Sinkron selesai: $n pesan baru"
        } catch (e: Exception) { "Gagal sinkron: pastikan HP utama menampilkan kode & WiFi sama" }
    }

    private fun toJson(e: NotifEntity) = JSONObject().put("uid", e.uid).put("appName", e.appName).put("pkg", e.pkg)
        .put("chatKey", e.chatKey).put("chatName", e.chatName).put("sender", e.sender).put("phone", e.phone ?: JSONObject.NULL)
        .put("title", e.title).put("text", e.text).put("time", e.time).put("isGroup", e.isGroup)
        .put("replyTo", e.replyTo ?: JSONObject.NULL).put("replyText", e.replyText ?: JSONObject.NULL)
        .put("category", e.category).put("mediaType", e.mediaType ?: JSONObject.NULL).put("viewOnce", e.viewOnce)

    private fun JSONObject.str(k: String): String? = if (isNull(k)) null else getString(k)

    private fun fromJson(j: JSONObject) = NotifEntity(
        uid = j.getString("uid"), appName = j.getString("appName"), pkg = j.getString("pkg"),
        chatKey = j.getString("chatKey"), chatName = j.getString("chatName"), sender = j.getString("sender"),
        phone = j.str("phone"), title = j.getString("title"), text = j.getString("text"), time = j.getLong("time"),
        isGroup = j.getBoolean("isGroup"), groupName = if (j.getBoolean("isGroup")) j.getString("chatName") else null,
        replyTo = j.str("replyTo"), replyText = j.str("replyText"), category = j.getString("category"),
        mediaType = j.str("mediaType"), viewOnce = j.getBoolean("viewOnce")
    )
}
