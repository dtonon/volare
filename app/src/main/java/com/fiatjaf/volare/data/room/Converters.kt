package com.fiatjaf.volare.data.room

import androidx.room.TypeConverter
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import com.fiatjaf.volare.core.utils.BlurHashDef

class Converters {
    @TypeConverter
    fun blurHashDefListToString(list: List<BlurHashDef>?): String? {
        if (list == null || list.size == 0) return null
        return Json.encodeToString(list)
    }

    @TypeConverter
    fun stringToBlurHashDefList(data: String?): List<BlurHashDef>? {
        if (data == null) return null
        try {
            return Json.decodeFromString<List<BlurHashDef>>(data)
        } catch (e: Exception) {
            return null
        }
    }
}
