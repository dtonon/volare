package com.fiatjaf.volare.data.room.entity.sets

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import com.fiatjaf.volare.core.PubkeyHex
import com.fiatjaf.volare.data.event.ValidatedTopicSet
import com.fiatjaf.volare.data.room.entity.AccountEntity

@Entity(
    tableName = "topicSet",
    primaryKeys = ["identifier"],
    foreignKeys = [ForeignKey(
        entity = AccountEntity::class,
        parentColumns = ["pubkey"],
        childColumns = ["myPubkey"],
        onDelete = ForeignKey.CASCADE,
        onUpdate = ForeignKey.NO_ACTION
    )],
    indices = [Index(value = ["myPubkey"], unique = false)], // ksp suggestion: "Highly advised"
)
data class TopicSetEntity(
    val identifier: String,
    val myPubkey: PubkeyHex,
    val title: String,
    @ColumnInfo(defaultValue = "") val description: String,
    val createdAt: Long,
    @ColumnInfo(defaultValue = "0") val deleted: Boolean,
) {
    companion object {
        fun from(set: ValidatedTopicSet): TopicSetEntity {
            return TopicSetEntity(
                identifier = set.identifier,
                myPubkey = set.myPubkey,
                title = set.title,
                description = set.description,
                createdAt = set.createdAt,
                deleted = false
            )
        }
    }
}
