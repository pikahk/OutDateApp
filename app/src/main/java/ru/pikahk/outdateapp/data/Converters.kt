package ru.pikahk.outdateapp.data

import androidx.room.TypeConverter
import java.time.LocalDate

class Converters {
    @TypeConverter
    fun localDateToEpochDay(date: LocalDate?): Long? = date?.toEpochDay()

    @TypeConverter
    fun epochDayToLocalDate(epochDay: Long?): LocalDate? = epochDay?.let(LocalDate::ofEpochDay)

    @TypeConverter
    fun periodTypeToString(period: PeriodType): String = period.name

    @TypeConverter
    fun stringToPeriodType(period: String): PeriodType = PeriodType.valueOf(period)
}
