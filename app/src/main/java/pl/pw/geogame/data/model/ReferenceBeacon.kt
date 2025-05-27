package pl.pw.geogame.data.model

import com.google.gson.annotations.SerializedName

data class ReferenceBeacon(
    val longitude: Double,
    val latitude: Double,
    @SerializedName("beaconUid") val beaconUid: String?,
)

data class BeaconFileWrapper(
    val items: List<ReferenceBeacon>
)


