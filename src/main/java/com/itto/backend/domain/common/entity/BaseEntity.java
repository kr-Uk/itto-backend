package com.itto.backend.domain.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import org.springframework.data.annotation.LastModifiedDate;

import java.time.LocalDateTime;

/**
 * [생성 시각 + 수정 시각 자동 기록 - 대부분의 엔티티가 상속하는 공통 부모 클래스]
 *
 * BaseTimeEntity(생성 시각)를 상속받고, 여기에 수정 시각(updated_at)을 추가합니다.
 *
 * 상속 구조:
 *   BaseTimeEntity (created_at)
 *       └─ BaseEntity (created_at + updated_at)  ← 이 클래스
 *              └─ User, Photo 등 실제 엔티티들
 *
 * 이렇게 하면 모든 엔티티에 "언제 만들어졌고, 언제 마지막으로 수정되었는지"가
 * 별도 코드 없이 자동으로 기록됩니다.
 */
@MappedSuperclass  // JPA: 이 클래스의 필드를 상속받는 자식 엔티티의 테이블 컬럼으로 포함시킴
@Getter            // Lombok: getUpdatedAt() 메서드를 자동 생성
public abstract class BaseEntity extends BaseTimeEntity {

    @LastModifiedDate                               // Spring Data: 데이터가 수정될 때마다 현재 시각을 자동으로 갱신
    @Column(name = "updated_at", nullable = false)  // DB 컬럼명: updated_at, 필수값
    private LocalDateTime updatedAt;  // 데이터가 마지막으로 수정된 시각
}
