package com.persondic.ui.common

import com.persondic.data.model.CommitmentStatus
import com.persondic.data.model.Direction
import com.persondic.data.model.FactCategory
import com.persondic.data.model.InteractionKind
import com.persondic.data.model.Sensitivity
import com.persondic.data.model.Volatility

fun categoryLabel(category: FactCategory): String = when (category) {
    FactCategory.CONTEXT -> "관계/계기"
    FactCategory.PREFERENCE -> "취향"
    FactCategory.LIFE -> "가족/건강/근황"
    FactCategory.HOOK -> "다음 화제"
}

fun volatilityLabel(volatility: Volatility): String = when (volatility) {
    Volatility.PERMANENT -> "영구"
    Volatility.SLOW -> "천천히 변함"
    Volatility.SEASONAL -> "계절성"
    Volatility.EVENT -> "일회성"
}

fun sensitivityLabel(sensitivity: Sensitivity): String = when (sensitivity) {
    Sensitivity.NORMAL -> "보통"
    Sensitivity.PRIVATE -> "비공개"
    Sensitivity.RESTRICTED -> "제한"
}

fun interactionKindLabel(kind: InteractionKind): String = when (kind) {
    InteractionKind.MEET -> "만남"
    InteractionKind.CALL -> "통화"
    InteractionKind.MESSAGE -> "메시지"
    InteractionKind.OTHER -> "기타"
}

fun directionLabel(direction: Direction): String = when (direction) {
    Direction.I_OWE -> "내가 해야 할 것"
    Direction.THEY_OWE -> "확인할 것"
    Direction.MUTUAL -> "서로"
}

fun commitmentStatusLabel(status: CommitmentStatus): String = when (status) {
    CommitmentStatus.OPEN -> "진행 중"
    CommitmentStatus.DONE -> "완료"
    CommitmentStatus.DROPPED -> "취소됨"
}
