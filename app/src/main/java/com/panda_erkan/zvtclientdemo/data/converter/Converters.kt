package com.panda_erkan.zvtclientdemo.data.converter

import androidx.room.TypeConverter
import com.panda_erkan.zvtclientdemo.data.model.OperationType

class Converters {

    @TypeConverter
    fun fromOperationType(value: OperationType): String = value.name

    @TypeConverter
    fun toOperationType(value: String): OperationType = OperationType.valueOf(value)
}
