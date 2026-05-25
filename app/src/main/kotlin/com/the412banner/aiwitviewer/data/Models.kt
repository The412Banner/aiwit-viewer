package com.the412banner.aiwitviewer.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ApiEnvelope<T>(
    val resultCode: Int,
    val msg: String = "",
    val content: T? = null,
)

@Serializable
data class LoginContent(
    val session_id: String,
    val username: String,
    val uid: Long,
    val master: Int = 0,
)

@Serializable
data class DeviceListContent(
    val list: List<Device> = emptyList(),
    val config: ConnectionConfig? = null,
)

@Serializable
data class ConnectionConfig(
    val cmd_servers: List<HostPortTls> = emptyList(),
    val p2p_servers: List<HostPort> = emptyList(),
    val p2p_encrypt_servers: List<HostPort> = emptyList(),
    val stun_servers: List<HostPort> = emptyList(),
    val chat_servers: List<HostPort> = emptyList(),
    val enable_encryption: Int = 0,
    val enable_tls_encryption: Int = 0,
)

@Serializable
data class HostPort(
    val ip: String = "",
    val port: Int = 0,
)

@Serializable
data class HostPortTls(
    val ip: String = "",
    val port: Int = 0,
    val ssl_port: Int = 0,
    val tls_port: Int = 0,
)

@Serializable
data class Device(
    val device_sn: String,
    val name: String = "",
    val oem: String = "",
    val mac: String = "",
    val firmware_ver: String = "",
    val battery_level: Int = 0,
    val timezone: String = "America/New_York",
    val cloud_service: Int = 0,
    val state: Int = 0,
    val ossFromId: Int = 5,
    val endpoint: String = "",
    val bucket: String = "",
)

@Serializable
data class RecordingsContent(
    val items: List<Recording> = emptyList(),
    val item_max_page: Int = 0,
    val item_count: Int = 0,
    val item_date: Long = 0,
)

@Serializable
data class Recording(
    val deviceSn: String,
    val deviceName: String = "",
    val fileName: String,
    val ossFromId: Int = 5,
    val length: Long = 0,
    val type: String = "",
    val duration: Long = 0,
    val pk: String = "",
    val bucket: String = "",
    val endpoint: String = "",
    val time: String = "",
)

@Serializable
data class OssToken(
    @SerialName("AccessKeyId") val accessKeyId: String,
    @SerialName("AccessKeySecret") val accessKeySecret: String,
    @SerialName("Expiration") val expiration: String,
    @SerialName("SecurityToken") val securityToken: String,
    @SerialName("OssFromId") val ossFromId: Int = 5,
)
